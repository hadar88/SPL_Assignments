package bgu.spl.net.impl.tftp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Holder{
    
    public static Map<Integer, String> login = new ConcurrentHashMap<Integer, String>();

}
