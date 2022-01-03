package org.example.abd.quorum;

import org.jgroups.Address;
import org.jgroups.View;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class Majority {

    private View view;

    public Majority(View view){
        this.view = view;
    }
    
    public int quorumSize(){
        return view.getMembers().size()/2 +1;
    }

    public List<Address> pickQuorum(){
        List<Address> members = new ArrayList<Address>();
        List<Address> members_copy = view.getMembers();
        for (int i=0; i<quorumSize(); i++){
            int random_index = (int)Math.random()*members_copy.size();
            members.add(members_copy.remove(random_index));
        }
        return members;
    }
}