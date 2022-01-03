package org.example.abd;

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
        if isWritable {
            throw IllegalStateException;
        }
        else {    
            execute(factory.newWriteRequest(v, (int)(System.currentTimeMillis())));
        }
    }

    private synchronized V execute(Command cmd){    
        // In `execute`, we simply send the command to a quorum of replicas and not to all (as in the course).
        // This avoids the need to handle late answers to a request.
        
        return null;
    }

    // Message handlers

    @Override
    public void receive(Message msg) {}

    private void send(Address dst, Command command) {
        try {
            Message message = new Message(dst,channel.getAddress(), command);
            channel.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
