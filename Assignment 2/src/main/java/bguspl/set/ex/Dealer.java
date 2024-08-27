package bguspl.set.ex;

import bguspl.set.Env;
import bguspl.set.Main;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Collections;
import java.util.LinkedList;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * Holds the id's of the players that their cards should be cheaked.
     */
    private BlockingQueue<Integer> queue;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    /**
     * Holds the cards that should be removed from the table.
     */
    private LinkedList<Integer> finishedCards;

    /**
     * Flag that say if the players can do things(if the dealer doesn't working on the table).
     */
    public boolean working;

    /**
     * Lock the dealer if he doesn't have something to do.
     */
    public Object dealerLock;

    private boolean hint;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        this.finishedCards = new LinkedList<>();
        this.queue = new LinkedBlockingQueue<Integer>();
        this.working = true;
        this.dealerLock = new Object();
        this.hint = true;
    }

    /**
     * Adding players id's to the queue.
     */
    public void addToQueue(int id){
        this.queue.add(id);
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        for(Player player : players){
            Thread playerThread = new Thread(player);
            playerThread.start();
        }
        while (!shouldFinish()) {
            placeCardsOnTable();
            working = true;
            timerLoop();
            updateTimerDisplay(false);
            working = false;
            removeAllCardsFromTable();
        }
        announceWinners();
        terminate();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        updateTimerDisplay(true);
        hint = true;
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            working = false;
            removeCardsFromTable();
            placeCardsOnTable();
            working = true;
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        for(int i = env.config.players-1; i>=0; i--){
            Player player = players[i];
            player.terminate();
        }
        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Adding the finished cards to a list that stores them until the dealer remove them from the table.
     */
    public void addCards(int [] cards){
        for(int card : cards)
            finishedCards.add(card);
    }

    /**
     * Remove tokens from the table.
     */
    public void removeTokens(int id, int [] cards){
        for(int card : cards){
            int slot = table.cardToSlot[card];
            table.removeToken(id, slot);
        }
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        while(!queue.isEmpty()){
            int id = queue.poll();
            players[id].checkCards();
            while(!finishedCards.isEmpty()){
                Integer card = finishedCards.poll();  
                table.removeCard(table.cardToSlot[card]);
                updateTimerDisplay(true);
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        if(env.util.findSets(deck, 1).size() == 0 && table.isEmpty()){
            terminate = true;
        }
        else{
            Collections.shuffle(deck);
            for(int slot = 0; slot< env.config.tableSize && !deck.isEmpty(); slot++){
                if(table.slotToCard[slot] == null){
                 table.placeCard(deck.remove(0), slot);
                }
            }
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        try {
            synchronized(dealerLock){
                dealerLock.wait(25);
            }
        } catch (Exception e) {}
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        if(reset){
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        }
        else{
            long timetoSet = reshuffleTime - System.currentTimeMillis();
            env.ui.setCountdown(timetoSet < 0 ? 0 : timetoSet, timetoSet < env.config.turnTimeoutWarningMillis);
            if(env.config.hints  && hint && timetoSet <= env.config.turnTimeoutWarningMillis){
                table.hints();
                hint = false;
            }
            for (int id = 0; id < env.config.players; id++) {
                if(players[id].time != 0){
                    env.ui.setFreeze(id, players[id].time - System.currentTimeMillis());
                    if(System.currentTimeMillis() >= players[id].time){
                        synchronized (players[id].freezeLock) {
                            players[id].time = 0;
                            env.ui.setFreeze(id, (long)0);
                            players[id].freezeLock.notify();
                            players[id].flag = false;
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        for(int slot=0; slot<env.config.tableSize; slot++){
            if(table.slotToCard[slot]!=null){
                int card = table.slotToCard[slot];
                table.removeCard(slot);
                deck.add(card);
            }
        }
        
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int [] winners;
        int max=0;
        int counter=0;
        for(Player player : players){
            if(player.score() > max){
                max = player.score();
            }
        }
        for(Player player : players){
            if(player.score() == max){
                counter++;
            }
        }
        winners = new int[counter];
        int i = 0;
        for(Player player :players){
            if(player.score() == max){
                winners[i] = player.id;
                i++;
            }
        }
        env.ui.announceWinner(winners);
    }
}
