package eu.tsp.transactions;

import eu.tsp.transactions.base.BaseBank;
import eu.tsp.transactions.distributed.DistributedBank;

public class BankFactory{

  public Bank createBank(){
    return createDistributedBank();
  }
  
  public Bank createBaseBank(){
    return new BaseBank();
  }

  public Bank createDistributedBank(){
    return new DistributedBank();
  }

}
