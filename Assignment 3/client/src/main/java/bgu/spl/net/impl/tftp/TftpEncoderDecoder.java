package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;
import java.util.LinkedList;
import java.util.List;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private List <Byte> bytes = new LinkedList<Byte>();
    private short opcode;
    private short length = -1;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        if(bytes.size()<2){ 
            if(nextByte != (byte)0 && bytes.size() == 0){
                bytes.clear();
                return new byte[]{(byte)0, (byte)20};
            }
            
            bytes.add(nextByte);
            if(bytes.size() == 2){
                byte[] b = listToBytes(bytes);
                opcode = bytesToShort(b);
                if(opcode < 1 || opcode > 10){
                    bytes.clear();
                    return new byte[]{(byte)0, (byte)20};
                }
                if(opcode == 6 || opcode == 10 ){// DIRQ or DISC
                    bytes.clear();
                    return b;
                }
            }
            return null;
        }
        else{
            if(opcode == 1){// RRQ
                bytes.add(nextByte);
                if(nextByte == (byte)0){
                    byte[] temp = listToBytes(bytes);
                    bytes.clear();
                    return temp;
                }

            }
            else if(opcode == 2){// WRQ
                bytes.add(nextByte);
                if(nextByte == (byte)0){
                    byte[] temp = listToBytes(bytes);
                    bytes.clear();
                    return temp;
                }
            }
            else if(opcode == 3){// DATA
                bytes.add(nextByte);
                if(bytes.size() == 4){
                    byte[] b = new byte[]{bytes.get(2), bytes.get(3)};
                    length = bytesToShort(b);
                }
                if(bytes.size() - 6 == length){
                    byte[] temp = listToBytes(bytes);
                    bytes.clear();
                    return temp;
                }
            }
            else if(opcode == 4){// ACK
                bytes.add(nextByte);
                if(bytes.size() == 4){
                    byte[] temp = listToBytes(bytes);
                    bytes.clear();
                    return temp;
                }
            }
            else if(opcode == 5){//ERROR
                bytes.add(nextByte);
                if(nextByte == (byte)0 && bytes.size() > 4){
                    byte[] temp = listToBytes(bytes);
                    bytes.clear();
                    return temp;
                }
                
            }
            else if(opcode == 7){// LOGRQ
                bytes.add(nextByte);
                if(nextByte == (byte)0){
                    byte[] temp = listToBytes(bytes);
                    bytes.clear();
                    return temp;
                }
            }
            else if(opcode == 8){// DELRQ
                bytes.add(nextByte);
                if(nextByte == (byte)0){
                    byte[] temp = listToBytes(bytes);
                    bytes.clear();
                    return temp;
                }
            }
            else if(opcode == 9){// BCAST
                bytes.add(nextByte);
                if(nextByte == (byte)0 && bytes.size() > 3){
                    byte[] temp = listToBytes(bytes);
                    bytes.clear();
                    return temp;
                }
            }
        }
        return null;
    }

    @Override
    public byte[] encode(byte[] message) {
        return message;
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
}