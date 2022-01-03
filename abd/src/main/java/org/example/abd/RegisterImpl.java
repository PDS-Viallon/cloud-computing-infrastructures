package org.example.abd;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.example.abd.cmd.Command;
import org.example.abd.cmd.CommandFactory;
import org.example.abd.cmd.ReadReply;
import org.example.abd.cmd.ReadRequest;
import org.example.abd.cmd.WriteReply;
import org.example.abd.cmd.WriteRequest;
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

    private int label;
    private int max;
    private V value;
    private int writeReplyCounter = 0;
    private boolean isWritable;
    private Majority quorumSystem;
    private CompletableFuture<V> pending;
    private ArrayList<Command<V>> readReplyList;


    public RegisterImpl(String name) {
        this.name = name;
        this.factory = new CommandFactory<>();
    }

    public void init(boolean isWritable) throws Exception{
        this.isWritable=isWritable;
        value = null;
        label =0;
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
        readReplyList = new ArrayList<Command<V>>();
        return execute(factory.newReadRequest());
    }

    @Override
    public void write(V v) {
        writeReplyCounter = 0;

        if(isWritable) {
            throw new IllegalStateException();
        }
        else {
            int l = ++max;    
            execute(factory.newWriteRequest(v, l));
        }
    }

    private synchronized V execute(Command<V> cmd){    
        // In `execute`, we simply send the command to a quorum of replicas and not to all (as in the course).
        // This avoids the need to handle late answers to a request.
        V v = null; 
       for(Address address : quorumSystem.pickQuorum()){
           send(address, cmd);
       }

       try {
        v  = pending.get();
    } catch (Exception e) {
        e.printStackTrace();
    } 
        
        return v;
    }

    // Message handlers

    @Override
    public void receive(Message msg) {

        Command<V> command = (Command<V>) msg.getObject();

        if (command instanceof ReadRequest) {
            send(msg.getSrc(), factory.newReadReply(this.value, this.label));
        }
        if (command instanceof ReadReply) {
            readReplyList.add(command);

            if(readReplyList.size()>=quorumSystem.quorumSize()){
                int lmax = 0;
                V vmax = null;
                for(Command rr : readReplyList ){
                    if(lmax<rr.getTag()){
                        lmax = rr.getTag();
                        vmax = (V)rr.getValue();
                    }
                }

                pending.complete(vmax);
            }
           
        }

        if (command instanceof WriteRequest) {

            if (command.getTag() > label){
                label= command.getTag();
                value= command.getValue(); 
            }
            send(msg.getSrc(),factory.newWriteReply());
        }  

        if (command instanceof WriteReply) {
            writeReplyCounter ++;
            if(writeReplyCounter >= quorumSystem.quorumSize()){
                pending.complete(null);
            }
           
        }
    }

    private void send(Address dst, Command<V> command) {
        try {
            Message message = new Message(dst,channel.getAddress(), command);
            channel.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}