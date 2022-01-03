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
        List<Address> members_copy_shuffled = view.getMembers();
        Collections.shuffle(members_copy_shuffled);
        for (int i=0; i<quorumSize(); i++){
            members.add(members_copy_shuffled.get(i));
        }
        return members;
    }


}


