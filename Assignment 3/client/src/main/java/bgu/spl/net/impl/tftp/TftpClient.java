package bgu.spl.net.impl.tftp;

import java.io.IOException;
import java.net.Socket;

import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpClient {
    
    public static void main(String[] args) {
        if(args.length == 0) {
            args = new String[]{"localhost", "7777"};
        }
        if(args.length < 2) {
            System.out.println("you must supply two arguments: host, port");
            System.exit(1);
        }
        Socket sock;
        try{
            sock = new Socket(args[0], Integer.parseInt(args[1]));
            
            MessagingProtocol<byte[]> protocol = new TftpProtocol();
            MessageEncoderDecoder<byte[]> encdec = new TftpEncoderDecoder();

            KeyboardThread KThread = new KeyboardThread(protocol, sock);
            Thread keyboardThread = new Thread(KThread);
            
            ListeningThread LThread = new ListeningThread(protocol, encdec, sock, KThread);
            Thread listeningThread = new Thread(LThread);

            keyboardThread.start();
            listeningThread.start();
        }
        catch (IOException ex) {}
    }
}