package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.Connections;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.ConnectionHandler;

public class ConnectionsImpl<T> implements Connections<T>{
    private Map<Integer, ConnectionHandler<T>> map = new ConcurrentHashMap<Integer, ConnectionHandler<T>>();
    
    @Override
    public void connect(int connectionId, ConnectionHandler<T> handler){
        map.put(connectionId, handler);
    }

    @Override
    public boolean send(int connectionId, T msg){
        ConnectionHandler<T> handler = map.get(connectionId);
        handler.send(msg);
        return true;
    }

    @Override
    public void disconnect(int connectionId){
        map.remove(connectionId);

    }

}