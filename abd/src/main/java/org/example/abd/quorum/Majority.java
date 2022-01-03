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

        // System.out.println("TEST");

        List<Address> members = view.getMembers();

        ArrayList<Integer> randomIndex =new ArrayList<Integer>();

        for(int i=0; i<members.size(); ++i){
            randomIndex.add(i);
        }

        Collections.shuffle(randomIndex);

        ArrayList<Address> ret = new ArrayList<Address>();

        for(int i : randomIndex){
            ret.add(members.get(i));
        }



        return  ret;
    }
}
