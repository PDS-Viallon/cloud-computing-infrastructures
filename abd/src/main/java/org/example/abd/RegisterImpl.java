package org.example.abd;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.example.abd.cmd.Command;
import org.example.abd.cmd.CommandFactory;
import org.example.abd.quorum.Majority;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

public class RegisterImpl<V> extends ReceiverAdapter implements Register<V>{

    private String name;

    private CommandFactory<V> factory;
    private JChannel channel;

    private long label;
    private long max;
    private V value;
    private boolean isWritable;
    private Majority quorumSystem;
    private CompletableFuture<V> pending;


    public RegisterImpl(String name) {
        this.name = name;
        this.factory = new CommandFactory<>();
    }

    public void init(boolean isWritable) throws Exception{
        this.isWritable=isWritable;
        value = null;
        label = (int)(System.currentTimeMillis());
        max = label;
        channel = new JChannel();
        channel.connect("ChatCluster");
        pending = new CompletableFuture<V>();
    }

    @Override
    public void viewAccepted(View view) {
        quorumSystem = new Majority(view);

    }

    // Client part

    @Override
    public V read() {
        return value;
    }

    @Override
    public void write(V v) {
        // If the client executes `write`, but the register is not writable, the method throws a new `IllegalStateException`.
        if(isWritable) {
            throw new IllegalStateException();
        }
        else {    
            execute(factory.newWriteRequest(v, (int)(System.currentTimeMillis())));
        }
    }

    private synchronized V execute(Command cmd){    
        // In `execute`, we simply send the command to a quorum of replicas and not to all (as in the course).
        // This avoids the need to handle late answers to a request.
       for(Address address : quorumSystem.pickQuorum()){
           send(address, cmd);
       }

       try {
        value  = pending.get();
    } catch (Exception e) {
        e.printStackTrace();
    } 
        
        return null;
    }

    // Message handlers

    @Override
    public void receive(Message msg) {
        if (msg.get(0) == 'Write') {
            if (l > label){
                label = msg.get(1).l;
                value = msg.buf.v            
            }
            send(msg.src,"Ack")
        }
        if (msg.get(0) == 'Read') {
            send(msg.src,(label, value))
        }        
    }

    private void send(Address dst, Command command) {
        try {
            Message message = new Message(dst,channel.getAddress(), command);
            channel.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}