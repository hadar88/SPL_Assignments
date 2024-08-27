package bguspl.set.ex;

import bguspl.set.Env;

import java.util.LinkedList;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * Game entities.
     */
    private final Dealer dealer;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    /**
     * Holds the slots that the player want to put a token on them.
     */
    private BlockingQueue<Integer> queue;

    /**
     * lock the player until the freeze end.
     */
    public Object freezeLock;

    /**
     * lock the player until the player press a key.
     */
    public Object keyLock;

    /**
     * Holds the time that the player's theard should be released.
     */
    public long time;

    /**
     * Flag that say if the player can do something.
     */
    public boolean flag;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer = dealer;
        this.queue = new LinkedBlockingQueue<>();
        this.freezeLock = new Object();
        this.keyLock = new Object();
        this.time = 0;
        this.flag = false;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();
        while (!terminate) {
            if(dealer.working){
                synchronized(keyLock){
                    try{
                        keyLock.wait();
                        while(queue.size()!=0){
                            int slot = queue.poll();
                            if(table.check(id,slot)){
                                table.removeToken(id, slot);
                            }
                            else{
                                int amountOfTokens = table.getAmountOfTokens(id);
                                if(amountOfTokens < 3){
                                    table.placeToken(id, slot);
                                }
                                if(table.getAmountOfTokens(id) == 3 && amountOfTokens == 2){    
                                    synchronized(freezeLock){
                                        dealer.addToQueue(id);
                                        synchronized(dealer.dealerLock){
                                            dealer.dealerLock.notify();
                                        }
                                        flag = true;
                                        freezeLock.wait();
                                    }
                                }
                            }
                        }
                    }catch (InterruptedException e) {}
                }
            }
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("AI thread " + Thread.currentThread().getName() + " starting.");
            // AI's main loop
            while (!terminate) {
                try {
                    Random random = new Random();
                    int slot = random.nextInt(env.config.tableSize-1);
                    while(table.slotToCard[slot] == null){
                       slot = random.nextInt(env.config.tableSize-1);
                    }
                    keyPressed(slot);
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}
            }
            env.logger.info("AI thread " + Thread.currentThread().getName() + " terminated.");
        }, "AI-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        terminate = true;
        playerThread.interrupt();
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        if(dealer.working){
            if(!flag){
                synchronized(keyLock){
                    queue.add(slot);
                    keyLock.notify();
                }
            }
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        time = System.currentTimeMillis() + env.config.pointFreezeMillis;
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        time = System.currentTimeMillis() + env.config.penaltyFreezeMillis;
    }

    public int score() {
        return score;
    }

    /**
     * Check if the player hold a set and give a point or penalty.
     */
    public void checkCards(){
        int [] cards = new int [3];
        int j=0;
        LinkedList<Integer> slots = table.getTokensPlaces(id);
        int size = slots.size();
        for(int i=0; i<size; i++){
            int slot = slots.removeFirst();
            int card = table.slotToCard[slot];
            cards[j] = card;
            j++;
        }
        if(env.util.testSet(cards)){
            point();
            dealer.addCards(cards);
        }
        else{
            penalty();
        }
    }
}