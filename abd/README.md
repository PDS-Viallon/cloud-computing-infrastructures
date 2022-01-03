# A Single-Writer Multiple-Readers Register in Message-passing

The objective of this lab is to implement the ABD algorithm of Attiya et al.
ABD allows to share a register among a set of distributed processes in a message-passing system.
All the processes can access the register for reading its content, but a single process may modify it.
This abstraction is called a single-writer multiple-readers (SWMR) register.
The shared register implemented by AND is linearizable.
This means that, from the perspective of a reader/writer, the object behaves as if it was accessed locally.

In what follows, we first present the Java code base from which we start this practical.
As in the previous lab, we use the [JGroups](www.jgroups.org) library to communicate between nodes in the distributed system.
Then, we code ABD by implementing first a quorum system then the functions that handle the clients request.
For simplicity, we initially omit the read-repair mechanism.
This mechanism is later added to attain linearizability in case of failures.

## 1. Provided code

An initial code base for ABD is available under the `src` directory.
The management of dependencies relies on Apache Maven and a `pom.xml` file is available at the root of the project.
This section offers an overview of this code base.

### 1.1 Register

A register is the fundamental abstraction modeling the physical memory of a computer.
It allows two types of operations: a read and a write.
The interface `Register` contains the signature of these operations.

When a client wants to create a shared register, it instantiates first a `Manager` object, then call the `newRegister()` method.
This call invokes the constructor of the `RegisterImpl` class which contains the core of the implementation.
Two parameters are passed to the constructor: the name of the register and whether it is writable by the client, or not.
Recall that ABD implements an SWMR register.
As a consequence, when multiple clients access the register, we should take care that at most one is a writer.

### 1.2 Notion of commands

ABD is a quorum-based algorithm.
This means that when an operation is called, a request is sent to a quorum of the replicas in the system.
These replicas execute some computation, then return an appropriate reply to the caller.
The replies are used to compute the return value of the call.

To ease things, a package of marshallable commands is provided under `org.example.abd.cmd`.
These commands are either request or reply.
The `Command` class is the parent class of all the commands (`ReadRequest`, `ReadReply`, `WriteRequest` and `WriteReply`).

To create commands, we make use of a factory pattern.
The class `CommandFactory` is called with the appropriate parameters for the creation of a new command.
In the `RegisterImpl` class, a field named `factory` is already available.

### 1.3 Tests

The class `RegisterTest` under the `test` directory contains two unit tests to validate our implementation.
The first test is named `sequential` and validate that the code of `RegisterImpl` is correct when the operations on the register are sequential.

The `concurrent` test is more involved.
In this test, we create two readers and a writer that concurrently access the register.
These workers outputs on stdout some information when they invoke an operation and receive a corresponding response.
The logging mechanism is implemented in the parent class named `Worker`.

## 2. The ABD algorithm

### 3.1 Initialization

The very first step of every implementation consists in initializing the data structures that are used later on.
ABD makes uses of two variables at each process:

- `value` stores the content of the register, and
- `label` is the timestamp attached to this content (it marks the recency of the content).
  In addition, at the writer, ABD uses the variable `max` to store the highest label used so far.

**[Task]** Add the `value`, `label` and `max` fields to `RegisterImpl`.
In the code of the `init` method, properly set-up these variables.
Then, connect to the JChannel and register the `RegisterImpl` instance as a listener of the channel.

```java
    private int label;
    private int max;
    private V value;


    public RegisterImpl(String name) {
        this.name = name;
        this.factory = new CommandFactory<>();
    }

    public void init(boolean isWritable) throws Exception{
        value = new V();
        label = SystemClockFactory.getDatetime();
        max = label
        channel = new JChannel();
        channel.connect("ChatCluster");
    }
```

### 3.2 Quorum system

Several strategies are possible to implement a quorum system, each having its pros and cons.
In this practical, we focus on the most common one, i.e., the majority quorum.
A majority quorum takes its name from the notion of majority in a parliament.
This is simply a set of n/2+1 processes - where n is the total number of processes in the system.

The `Majority` class contains a skeleton for a quorum system using majority quorums.
It relies on the `Address` class in JGroups to identify a process.
The class `Majority` contains

- a method to return the size of majority, and
- a method to pick a _random_ majority among all the nodes in the system.

**[Task]** Complete the `Majority` class.

**[Task]** Create a field `quorumSystem` in `RegisterImpl`.
Upon a new view change, initialize this field by creating a new instance of `Majority`.

```java
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
```

### 3.3 Sending and replying to requests

In ABD, every client request (read or write) starts by sending some message to a quorum of replicas.
To leverage this observation, the provided code contains a method named `execute`.
This method takes as input a command forged in either the `read` or `write` method of `RegisterImpl`.
When `execute` is called it should

1. create a `CompletableFuture` to hold the result of the command,
2. send the command to a quorum of replicas, then
3. await for the completion of the future.

**[Task]** Complete the code of the methods `read`,

```java
   public V read() {
        readReplyList = new ArrayList<Command<V>>();
        return execute(factory.newReadRequest());
    }
```

`write`

```java
    public void write(V v) {
        writeReplyCounter = 0;

        if(!isWritable) {
            throw new IllegalStateException();
        }
        else {
            int l = ++max;
            execute(factory.newWriteRequest(v, l));
        }
    }
```

and `execute`

```java
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
```

in `RegisterImpl`.

If the client executes `write`, but the register is not writable, the method throws a new `IllegalStateException`.
In `execute`, we simply send the command to a quorum of replicas and not to all (as in the course).
This avoids the need to handle late answers to a request.

Replicas should now answer to requests by applying the core of the ABD algorithm.
This is the `receive` method that is in charge of executing this logic.

**[Task]** Study carefully the pseudo-code of ABD provided in the course.

**[Task]** Implement the logic of the algorithm when a request is received.

The `receive` method handles not only (read and write) requests, but also the corresponding replies.

**[Task]** Add a field named `replies` to store the replies of a command.
Amend the code of the method `receive` to update the `CompletableFuture` when a quorum of response was received.
For the moment, we do not implement the read-repair mechanism.

```java
public void receive(Message msg) {

        Command<V> command = (Command<V>) msg.getObject();

        if (command instanceof ReadRequest) {
            send(msg.getSrc(), factory.newReadReply(this.value, this.label));
        }
        if (command instanceof ReadReply) {
            replies.add(command);

            if(replies.size()>=quorumSystem.quorumSize()){
                int lmax = 0;
                V vmax = null;
                for(Command<V> rr : replies ){
                    if(lmax<rr.getTag()){
                        lmax = rr.getTag();
                        vmax = rr.getValue();
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
```

**[Task]** Validate your implementation using the methods `sequential` and `concurrent` in `RegisterTest`.

![270245348_314255957271000_8312134730116242143_n](https://user-images.githubusercontent.com/61706817/147945925-bb452248-6436-47b0-9ddc-8ecd128a0d59.png)


## 4. Repairing incomplete writes

As seen in the course, the read-repair mechanism is necessary when the writer fails in the middle of a write operation.

**[Task]** What type of inconsistencies may arise in a scenario where the writer fails?

It may happen that a writer writes a new value to less than a quorum and fails before sending the value to everybody. We thus have few (less than a quorum) registers with a latest value.
A first reader can read this latest value and thus decides it as the one actually returned while a second reader can read a quorum which does not contain this latest value. This is an inconsitency!

**[Task]** Implement the read-repair mechanism of ABD.

_(hint)_
To code this part of the algorithm, we advice you to use a `Pair` object in the value of the `CompletableFuture` (this class is part of the apache-commons-lang3 library).
The pair should store the value and its corresponding label computed during the first phase of a read.
Both objects are used during the second phase of the read to repair an incomplete write.
