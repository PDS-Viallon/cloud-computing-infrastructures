package org.example.kvstore;

import org.example.kvstore.cmd.Command;
import org.example.kvstore.cmd.CommandFactory;
import org.example.kvstore.cmd.*;
import org.example.kvstore.distribution.ConsistentHash;
import org.example.kvstore.distribution.Strategy;
import org.jgroups.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreImpl<K,V> extends ReceiverAdapter implements Store<K,V> {

    private String name;
    private Strategy strategy;
    private Map<K,V> data;
    private CommandFactory<K,V> factory;
    private JChannel channel;
    private ExecutorService workers;
    private CompletableFuture<V> pending;

    public StoreImpl(String name) {
        this.name = name;
    }

    public void init() throws Exception{
        channel=new JChannel(); // use the default config, udp.xml
        channel.connect("ChatCluster");
        channel.setReceiver(this);
        data = new HashMap<K,V>();
        workers = Executors.newCachedThreadPool();
        pending = new CompletableFuture<V>();
        //viewAccepted(new View());

    }

    public V get(K k) {
        try {
            return execute(factory.newGetCmd(k));
        }catch(Exception e ){}
        return null;
    }

    public V put(K k, V v) {
        try {
            return execute(factory.newPutCmd(k, v));
        }catch(Exception e ){}
        return null;
    }

    public V localGet(K k) {return data.get(k);}

    public V localPut(K k, V v) {return data.put(k,v);}

    public V execute(Command cmd) {
        V v = null;

        Address dest = strategy.lookup(cmd.getKey());
        System.out.println("BLIBLI");
        if(dest!= channel.getAddress()){
            System.out.println("HELLO");
            synchronized (pending) {
                try {
                    send(dest, cmd);
                    v = pending.get();
                }catch(Exception e){}
            }
        }
        else{
            System.out.println("Salut");

            if(cmd instanceof Put){v = localPut((K)cmd.getKey(), (V)cmd.getValue());}
            if(cmd instanceof Get){v = localGet((K)cmd.getKey());}
        }
        return v;
    }

    @Override
    public String toString(){
        return "Store#"+name+"{"+data.toString()+"}";
    }

    @Override
    public void viewAccepted(View new_view) {
        System.out.println("BLABLA");
        strategy = new ConsistentHash(new_view);
    }
    public void send(Address dst, Command command) throws Exception {
        Message msg = new Message(dst, null, command);
        channel.send(msg);
    }

    public void receive(Message msg){
        Command command = (Command)msg.getObject();
        workers.submit(new CmdHandler(msg.getSrc(), command));

    }

    private class CmdHandler implements Callable<Void> {
        private Address caller;
        private Command command;

        public CmdHandler(Address caller, Command command){
            this.caller = caller;
            this.command = command;
        }

        @Override
        public Void call() throws Exception {

            V v = null;
            if(command instanceof Put){
                 v=put((K)command.getKey(), (V)command.getValue());
            }
            else if(command instanceof Get){
                v= get((K) command.getKey());
            }


            if(command instanceof Reply){
                pending.complete((V)command.getValue());
                return null;
            }
            Command reply = factory.newReplyCmd((K)command.getKey(), v);
            send(caller, reply);


            return null;
        }
    }

}
