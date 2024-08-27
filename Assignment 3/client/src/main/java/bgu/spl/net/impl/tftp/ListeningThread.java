package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;

public class ListeningThread implements Runnable{
    private BufferedInputStream in;
    private MessagingProtocol<byte[]> protocol;
    private MessageEncoderDecoder<byte[]> encdec;
    private KeyboardThread keyboardThread;

    public ListeningThread(MessagingProtocol<byte[]> protocol, MessageEncoderDecoder<byte[]> encdec, Socket sock, KeyboardThread keyboardThread){
        this.protocol = protocol;
        this.encdec = encdec;
        try {
            this.in = new BufferedInputStream(sock.getInputStream());
        } catch (IOException e) {}
        this.keyboardThread = keyboardThread;
    }

    @Override
    public void run(){
        int read;
        byte[] msg;
        try{
			System.out.println("Client started");
            while (!protocol.shouldTerminate() && (read = in.read()) >= 0) {
                msg = encdec.decodeNextByte((byte)read);
                if (msg != null) {
                    byte[] ans = protocol.process(msg);
                    if (ans != null){
                        keyboardThread.send(ans);
                    }   
                    synchronized(keyboardThread.lock){
                        keyboardThread.lock.notify();
                    }
                }
            }
        } catch (IOException ex) {}
        System.out.println("client closed!!!");
    }
}
