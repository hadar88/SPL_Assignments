package bgu.spl.net.impl.tftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.net.api.MessagingProtocol;

public class TftpProtocol implements MessagingProtocol<byte[]>{
    private boolean shouldTerminate = false;
    private byte[] latestAction;
    private Queue<byte[]> in = new ConcurrentLinkedQueue<byte[]>();
    private Queue<byte[]> out = new ConcurrentLinkedQueue<byte[]>();
    

    public byte[] process(byte[] message){
        short opCode = bytesToShort(message);
        if(opCode == 1 || opCode == 6 || opCode == 7 || opCode == 8 || opCode == 10){//RRQ or DIRQ or LOGRQ or DELRQ or DISC
            this.latestAction = message;
            return null;
        }
        else if(opCode == 2){//WRQ
            this.latestAction = message;
            String fileName = bytesToString(latestAction, 2, latestAction.length - 1);
            File file = new File(fileName);
            try(FileInputStream readableFile = new FileInputStream(file)){
                short blockNumber = 1;
                long length = file.length();
                while(length >= 512){
                    byte[] data = new byte[512];
                    for(int i = 0; i < 512; i++){
                        data[i] = (byte)readableFile.read();
                    }
                    byte[] block = shortToBytes(blockNumber);
                    byte[] size = shortToBytes((short)512);
                    byte[] packet = getDataPacket(size, block, data);
                    out.add(packet);
                    blockNumber++;
                    length = length - 512;
                }
                byte[] block = shortToBytes(blockNumber);
                byte[] size = shortToBytes((short)length);
                byte[] data = new byte[(int)length];
                for(int i = 0; i < length; i++){
                    data[i] = (byte)readableFile.read();
                }
                byte[] packet = getDataPacket(size, block, data);
                out.add(packet);
            } catch (IOException e) {}
            return null;
        }
        else if(opCode == 3){//DATA
            byte[] data = bytesToBytes(message, 6, message.length - 1);
            in.add(data);
            if(data.length < 512){
                short currentAction = bytesToShort(latestAction);
                if(currentAction == 1){//RRQ
                    String fileName = bytesToString(latestAction, 2, latestAction.length - 1);
                    File newFile = new File(fileName);
                    try (FileOutputStream fStream = new FileOutputStream(newFile, true)){
                        while(!in.isEmpty()){
                            byte[] packet = in.remove();
                            fStream.write(packet);
                        }
                    } catch (IOException e) {}
                    System.out.println("RRQ " + fileName + " complete");
                }
                else if(currentAction == 6){//DIRQ
                    String p = "";
                    while(!in.isEmpty()){
                        byte[] packet = in.remove();
                        p = p + bytesToString(packet, 0 , packet.length);
                    }
                    String[] parts = p.split("0");
                    for (String part : parts) {
                        System.out.println(part);
                    }
                }
            }
            byte[] block = new byte[]{message[4], message[5]};
            return getAckPacket(block);
        }

        else if(opCode == 4){//ACK
            short currentAction = bytesToShort(latestAction);
            byte[] blockNumber = new byte[]{message[2], message[3]};
            short block = bytesToShort(blockNumber);
            System.out.println("ACK " + block);
            if(currentAction == 2){//WRQ
                if(out.isEmpty()){
                    String fileName = bytesToString(latestAction, 2, latestAction.length);
                    System.out.println("WRQ " + fileName + " complete");
                }
                else{
                    return out.remove();
                }
            }
            else if(currentAction == 10){//DISC
                shouldTerminate = true;
            }
            return null;
        }

        else if(opCode == 5){//ERROR
            short currentAction = bytesToShort(latestAction);
            byte[] error = new byte[]{message[2], message[3]};
            short errorNumber = bytesToShort(error);
            String msg = bytesToString(message, 4, message.length - 1);
            System.out.println("Error " + errorNumber + " " + msg);
            if(currentAction == 1){//RRQ
                String fileName = bytesToString(latestAction, 2, latestAction.length - 1);
                String path = "client/" + fileName;
                File file = new File(path);
                file.delete();
            }
            else if(currentAction == 2){//WRQ
                out.clear();
            }
            else if(currentAction == 10){//DISC
                shouldTerminate = true;
            }
            return null;
        }

        else if(opCode == 9){//BCAST
            String msg = "BCAST ";
            if(message[2] ==(byte)0){
                msg = msg + "del ";
            }
            else{
                msg = msg + "add ";
            }
            String fileName = bytesToString(message, 3, latestAction.length);
            msg = msg + fileName;
            if(bytesToShort(latestAction) == 2){
                System.out.println("WRQ " + fileName + " complete");
            }
            
            System.out.println(msg);
            return null;
        }
        else{
            return null;
        }
    }

    public boolean shouldTerminate(){
        return shouldTerminate;
    }

    private short bytesToShort(byte[] bytes){
        return (short) ((short) (((short) bytes[0]) << 8 | (short) (bytes[1]) & 0x00ff));
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

    private byte[] getDataPacket(byte[] size, byte[] blockNumber, byte[] data){
        List <Byte> msg = new LinkedList<Byte>();
        msg.add((byte)0);
        msg.add((byte)3);
        msg.add(size[0]);
        msg.add(size[1]);
        msg.add(blockNumber[0]);
        msg.add(blockNumber[1]);
        for(byte b : data){
            msg.add(b);
        }
        return listToBytes(msg);
    }

    private byte[] getAckPacket(byte[] blockNumber){
        List <Byte> ack = new LinkedList<Byte>();
        ack.add((byte)0);
        ack.add((byte)4);
        ack.add(blockNumber[0]);
        ack.add(blockNumber[1]);
        return listToBytes(ack);
    }
    
    private byte[] bytesToBytes(byte[] bytes, int start, int end){
        return Arrays.copyOfRange(bytes, start, end + 1);
    }

    private String bytesToString(byte[] bytes, int start, int end){
        byte[] copy = Arrays.copyOfRange(bytes, start, end);
        String name = new String(copy, StandardCharsets.UTF_8);
        return name;
    }

    private byte[] shortToBytes(short val){
        return new byte[]{(byte) (val >> 8), (byte) (val & 0xff)};
    }
}
