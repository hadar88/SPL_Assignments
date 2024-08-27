package bgu.spl.net.impl.tftp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import bgu.spl.net.api.MessagingProtocol;

public class KeyboardThread implements Runnable{
    private MessagingProtocol<byte[]> protocol;
    private BufferedOutputStream out;
    private BufferedReader in;
    public Object lock;

    public KeyboardThread(MessagingProtocol<byte[]> protocol, Socket sock){
        this.protocol = protocol;
        try{
            this.out = new BufferedOutputStream(sock.getOutputStream());
            this.in = new BufferedReader(new InputStreamReader(System.in));
        } catch(IOException e){}
        this.lock = new Object();
    }

    @Override
    public void run(){
        String command;
        try {
            while(!protocol.shouldTerminate()){
                command = in.readLine();
                if(command != null){
                    byte[] msg = encode(command);
                    if(msg != null){
                        protocol.process(msg);
                        send(msg);
                        if(opCode(msg) == 10){
                            try {
                                synchronized(lock){
                                    lock.wait();
                                } 
                            }catch (InterruptedException e) {}
                        }
                    }
                }
            } 
            in.close();
            out.close();
        }catch (IOException e) {}
    }

    private byte[] encode(String command){
        String[] parts = command.split(" ", 2);
        String act = parts[0];
        String name="";
        if(parts.length > 1){
            name = parts[1];
        }
        
        if(act.equals("LOGRQ")){
            byte[] msg = getLogrqPacket(name);
            return msg;
        }

        else if(act.equals("DELRQ")){
            byte[] msg = getDelrqPacket(name);
            return msg;
        }

        else if(act.equals("RRQ")){
            Path filePath = Paths.get(name);
            File file = new File(name);
            if(file.exists()){
                System.out.println("File already exist");
                return null;
            }
            else{
                try {
                    Files.createFile(filePath);
                } catch (IOException e) {}
                byte[] msg = getRrqPacket(name);
                return msg;
            }
            
        }

        else if(act.equals("WRQ")){
            File file = new File(name);
            if(file.exists()){
                byte[] msg = getWrqPacket(name);
                return msg;
            }
            else{
                System.out.println("File does not exist");
                return null;
            }
        }

        else if(command.equals("DIRQ")){
            byte[] msg = getDirqPacket();
            return msg;
        }

        else if(command.equals("DISC")){
            byte[] msg = getDiscPacket();
            return msg;
        }
        else{
            System.out.println("Unknown Opcode");
            return null;
        }
    }

    private Short opCode(byte[] msg){
        return (short) ((short) (((short) msg[0]) << 8 | (short) (msg[1]) & 0x00ff));
    }

    private byte[] getLogrqPacket(String userName){
        List<Byte> packet = new LinkedList<Byte>();
        packet.add((byte)0);
        packet.add((byte)7);
        for(byte b : userName.getBytes()){
            packet.add(b);
        }
        packet.add((byte)0);
        return listToBytes(packet);
    }

    private byte[] getDelrqPacket(String fileName){
        List<Byte> packet = new LinkedList<Byte>();
        packet.add((byte)0);
        packet.add((byte)8);
        for(byte b : fileName.getBytes()){
            packet.add(b);
        }
        packet.add((byte)0);
        return listToBytes(packet);
    }
    private byte[] getRrqPacket(String fileName){
        List<Byte> packet = new LinkedList<Byte>();
        packet.add((byte)0);
        packet.add((byte)1);
        for(byte b : fileName.getBytes()){
            packet.add(b);
        }
        packet.add((byte)0);
        return listToBytes(packet);
    }  

    private byte[] getWrqPacket(String fileName){
        List<Byte> packet = new LinkedList<Byte>();
        packet.add((byte)0);
        packet.add((byte)2);
        for(byte b : fileName.getBytes()){
            packet.add(b);
        }
        packet.add((byte)0);
        return listToBytes(packet);
    } 

    private byte[] getDirqPacket(){
        List<Byte> packet = new LinkedList<Byte>();
        packet.add((byte)0);
        packet.add((byte)6);
        return listToBytes(packet);
    }

    private byte[] getDiscPacket(){
        List<Byte> packet = new LinkedList<Byte>();
        packet.add((byte)0);
        packet.add((byte)10);
        return listToBytes(packet);
    }

    public synchronized void send(byte[] msg){
        try {
            out.write(msg);
            out.flush();
        } catch (IOException e) {}
    }
    
    private byte[] listToBytes(List<Byte> tempBytes){
        byte[] b = new byte[tempBytes.size()];
        int i = 0;
        for(byte temp : tempBytes){
            b[i] = temp;
            i++;
        }
        return b;
    }
}
