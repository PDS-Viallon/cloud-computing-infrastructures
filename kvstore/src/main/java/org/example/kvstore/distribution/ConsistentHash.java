package org.example.kvstore.distribution;

import org.jgroups.Address;
import org.jgroups.View;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class ConsistentHash implements Strategy{

    private TreeSet<Integer> ring;
    private Map<Integer,Address> addresses;

    public ConsistentHash(View view){

        for(Address member : view.getMembers()){
            int hash = member.hashCode();
            ring.add(hash);
            addresses.put(hash, member);
        }
    }

    @Override
    public Address lookup(Object key){

        int hash = key.hashCode();
        int hash_address = ring.higher(hash);

        if(addresses.containsKey(hash_address)){
            return addresses.get(hash_address);
        }
        else{ return addresses.get(ring.first());}
    }

}
