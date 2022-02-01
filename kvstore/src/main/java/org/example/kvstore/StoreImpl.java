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
        channel.setReceiver(this);
        channel.connect("ChatCluster");
        data = new HashMap<K,V>();
        factory = new CommandFactory<K,V>();
        workers = Executors.newCachedThreadPool();
        pending = new CompletableFuture<V>();
    }

    public synchronized V get(K k) throws Exception {

        Address dest = strategy.lookup(k);
        if(dest == this.channel.getAddress()) {
            System.out.println("local get");
            return data.get(k);
        }

        return execute(factory.newGetCmd(k));
    }

    public synchronized V put(K k, V v) throws Exception {

        Address dest = strategy.lookup(k);
        if(dest == this.channel.getAddress()) {
            System.out.println("local put");
            return data.put(k,v);
        }

        return execute(factory.newPutCmd(k, v));
    }

    public synchronized V execute(Command cmd) throws Exception {
        V v = null;
        Address dest = strategy.lookup(cmd.getKey());
        send(dest, cmd);
        v = pending.get();
        return v;
    }

    @Override
    public String toString(){
        return "Store#"+name+"{"+data.toString()+"}";
    }

    @Override
    public void viewAccepted(View new_view) {
        strategy = new ConsistentHash(new_view);
    }

    public void send(Address dst, Command command) throws Exception {
        Message msg = new Message(dst, this.channel.getAddress(), command);
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
                v=get((K) command.getKey());
            }

            if(command instanceof Reply){
                pending.complete((V)command.getValue());
                return null;
            }

            Command reply = factory.newReplyCmd((K) command.getKey(), v);
            send(caller, reply);

            return null;
        }
    }

}
