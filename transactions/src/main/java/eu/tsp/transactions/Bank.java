package eu.tsp.transactions;

import javax.transaction.*;

public interface Bank{
  void createAccount(int id) throws IllegalArgumentException;
  int getBalance(int id) throws IllegalArgumentException;
  void performTransfer(int from, int to, int amount) throws IllegalArgumentException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException;
  void clear();
  void open();
  void close();
}
