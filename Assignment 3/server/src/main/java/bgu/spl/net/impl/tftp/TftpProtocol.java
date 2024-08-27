package bgu.spl.net.impl.tftp;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Arrays;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private int connectionId;
    private boolean shouldTerminate = false;
    private Connections<byte[]> connections;
    private String fileNameCheck;
    private String UserName;
    private Queue<byte[]> out = new ConcurrentLinkedQueue<byte[]>();
    private Queue<byte[]> in = new ConcurrentLinkedQueue<byte[]>();

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public void process(byte[] message) {
        short opCode = bytesToShort(message);
        if(opCode == 20 || opCode == 5 || opCode == 9){//Error 4
            byte[] b = new byte[]{(byte)0, (byte)4};
            byte[] send = getErrorPacket(b, "UnKnown Opcode.");
            connections.send(connectionId, send);
        }

        else if(opCode == 1){//RRQ
            String fileName = bytesToString(message, 2, message.length - 1);
            String filePath = "Files/" + fileName;
            File file = new File(filePath);
            if(!file.exists()){//Error 1
                byte[] b = new byte[]{(byte)0, (byte)1};
                byte[] send = getErrorPacket(b, "Non-existing file.");
                connections.send(connectionId, send);
            }
            else if(!Holder.login.containsKey(connectionId)){//Error 6
                byte[] b = new byte[]{(byte)0, (byte)6};
                byte[] send = getErrorPacket(b, "User not logged in.");
                connections.send(connectionId, send);
            }
            else{//No error
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
                    byte[] send = out.peek();
                    connections.send(connectionId, send);
                }catch (IOException e) {}
            }
        }

        else if(opCode == 2){//WRQ
            String fileName = bytesToString(message, 2, message.length - 1);
            String filePath = "Files/" + fileName;
            File file = new File(filePath);
            if(file.exists()){//Error 5
                byte[] b = new byte[]{(byte)0, (byte)5};
                byte[] send = getErrorPacket(b, "The file already exist.");
                connections.send(connectionId, send);
            }
            else if(!Holder.login.containsKey(connectionId)){//Error 6
                byte[] b = new byte[]{(byte)0, (byte)6};
                byte[] send = getErrorPacket(b, "User not logged in.");
                connections.send(connectionId, send);
            }
            else{//No error
                fileNameCheck = fileName;
                Path path = Paths.get(filePath);
                try {
                    Files.createFile(path);
                } catch (IOException e) {}
                byte[] b = new byte[]{(byte)0, (byte)0};
                byte[] send = getAckPacket(b);
                connections.send(connectionId, send);
            }
        }

        else if(opCode == 3){//DATA
            String Path = "Files/" + fileNameCheck;
            File file = new File(Path);
            if(!Holder.login.containsKey(connectionId)){//Error 6
                byte[] b = new byte[]{(byte)0, (byte)6};
                byte[] send = getErrorPacket(b, "User not logged in.");
                connections.send(connectionId, send);
            }
            else{//No error
                in.add(message);
                byte[] number = new byte[]{message[4], message[5]};
                byte[] send = getAckPacket(number);
                connections.send(connectionId, send);
                byte[] size = new byte[]{message[2], message[3]};
                short pSize = bytesToShort(size);
                if(pSize < 512){
                    try (FileOutputStream fStream = new FileOutputStream(file, true)){
                        while(!in.isEmpty()){
                            byte[] packet = in.remove();
                            byte[] data = bytesToBytes(packet, 6, packet.length - 1);
                            fStream.write(data);
                        } 
                    }catch (IOException e) {}
                    byte[] msg = getBcastPacket((byte)1, fileNameCheck);
                    for(int id : Holder.login.keySet()){
                        connections.send(id, msg);
                    }
                    fileNameCheck = "";
                }
            }
        }

        else if(opCode == 4){//ACK
            byte[] blockNumber = bytesToBytes(message, 2, 3);
            short bn = bytesToShort(blockNumber);
            byte[] send = out.peek();
            byte[] packetBlockNumber = bytesToBytes(send, 4, 5);
            short pbn = bytesToShort(packetBlockNumber);
            if(bn != pbn){//Error 0
                byte[] b = new byte[]{(byte)0, (byte)0};
                byte[] error = getErrorPacket(b, "Not defined.");
                connections.send(connectionId, error);
            }
            if(!Holder.login.containsKey(connectionId)){//Error 6
                byte[] b = new byte[]{(byte)0, (byte)6};
                byte[] error = getErrorPacket(b, "User not logged in.");
                connections.send(connectionId, error);
            }
            out.remove();
            send = out.peek();
            connections.send(connectionId, send);
        }

        else if(opCode == 6){//DIRQ
            if(!Holder.login.containsKey(connectionId)){//Error 6
                byte[] b = new byte[]{(byte)0, (byte)6};
                byte[] send = getErrorPacket(b, "User not logged in.");
                connections.send(connectionId, send);
            }
            else{//No error
                String msg="";
                String filesPath = "Files/";
                File directory = new File(filesPath);
                File[] files = directory.listFiles();
                msg = msg + files[0].getName();
                for(int i = 1; i < files.length; i++){
                    msg = msg + (byte)0 +files[i].getName();
                }
                short blockNumber = 1;
                short length = (short)msg.length();
                while(length >= 512){
                    byte[] data = new byte[512];
                    for(int i = 0; i < 512; i++){
                        data[i] = (byte)msg.charAt(0);
                        msg = msg.substring(1);
                    }
                    byte[] block = shortToBytes(blockNumber);
                    byte[] size = shortToBytes((short)512);
                    byte[] packet = getDataPacket(size, block, data);
                    out.add(packet);
                    blockNumber++;
                    length = (short) (length - (short)512);
                }
                byte[] block = shortToBytes(blockNumber);
                byte[] size = shortToBytes((short)length);
                byte[] data = new byte[length];
                for(int i = 0; i < length; i++){
                    data[i] = (byte)msg.charAt(0);
                    msg = msg.substring(1); 
                }
                byte[] packet = getDataPacket(size, block, data);
                out.add(packet);
                byte[] send = out.peek();
                connections.send(connectionId, send);
            }
        }

        else if(opCode == 7){//LOGRQ
            UserName = bytesToString(message, 2, message.length - 1);
            if(Holder.login.containsValue(UserName)){//Error 7
                byte[] b = new byte[]{(byte)0, (byte)7};
                String msg = "User " + UserName + " already logged in.";
                byte[] send = getErrorPacket(b, msg);
                connections.send(connectionId, send);
            }
            else{//No error
                byte[] b = new byte[]{(byte)0, (byte)0};
                byte[] send = getAckPacket(b);
                Holder.login.put(connectionId, UserName);
                connections.send(connectionId, send);
            }
        }

        else if(opCode == 8){//DELRQ
            String fileName = bytesToString(message, 2, message.length - 1);
            String filePath = "Files/" + fileName;
            File file = new File(filePath);
            if(!file.exists()){//Error 1
                byte[] b = new byte[]{(byte)0, (byte)1};
                byte[] send = getErrorPacket(b, "Non-existing file.");
                connections.send(connectionId, send);
            }
            if(!Holder.login.containsKey(connectionId)){//Error 6
                byte[] b = new byte[]{(byte)0, (byte)6};
                byte[] send = getErrorPacket(b, "User not logged in.");
                connections.send(connectionId, send);
            }
            else{//No error
                file.delete();
                byte[] b = new byte[]{(byte)0, (byte)0};
                byte[] send = getAckPacket(b);
                connections.send(connectionId, send);
                byte[] msg = getBcastPacket((byte)0, fileName);
                for(int id : Holder.login.keySet()){
                    connections.send(id, msg);
                }
            }
        }

        else if(opCode == 10){//DISC
            if(!Holder.login.containsKey(connectionId)){//Error 6
                byte[] b = new byte[]{(byte)0, (byte)6};
                byte[] send = getErrorPacket(b, "User not logged in.");
                connections.send(connectionId, send);
            }
            else{//No error
                byte[] b = new byte[]{(byte)0, (byte)0};
                byte[] send = getAckPacket(b);
                connections.send(connectionId, send);
                Holder.login.remove(connectionId);
                connections.disconnect(connectionId);
                shouldTerminate = true;
            }
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    } 

    private short bytesToShort(byte[] bytes){
        return (short) (((short) (bytes[0] & 0xFF)) << 8 | (short) (bytes[1] & 0xFF));
    }

    private byte[] getErrorPacket(byte[] errorCode, String msg){
        List <Byte> error = new LinkedList<Byte>();
        error.add((byte)0);
        error.add((byte)5);
        error.add(errorCode[0]);
        error.add(errorCode[1]);
        byte[] message = msg.getBytes(StandardCharsets.UTF_8);
        for(byte b : message){
            error.add(b);
        }
        error.add((byte)0);
        return listToBytes(error);
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

    private byte[] getBcastPacket(byte b, String msg){
        List<Byte> bcast = new LinkedList<Byte>();
        bcast.add((byte)0);
        bcast.add((byte)9);
        bcast.add(b);
        byte[] message = msg.getBytes(StandardCharsets.UTF_8);
        for(byte m : message){
            bcast.add(m);
        }
        bcast.add((byte)0);
        return listToBytes(bcast);
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

    private String bytesToString(byte[] bytes, int start, int end){
        byte[] copy = Arrays.copyOfRange(bytes, start, end);
        String name = new String(copy, StandardCharsets.UTF_8);
        return name;
    }

    private byte[] bytesToBytes(byte[] bytes, int start, int end){
        return Arrays.copyOfRange(bytes, start, end + 1);
    }

    private byte[] shortToBytes(short val){
        return new byte[]{(byte) (val >> 8), (byte) (val & 0xff)};
    }
}
