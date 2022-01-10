#!/usr/bin/env python
import time, socket, os, uuid, sys, kazoo, logging, signal, utils, random
from election import Election
from utils import MASTER_PATH
from utils import TASKS_PATH
from utils import DATA_PATH
from utils import WORKERS_PATH
import random as rd

class Client:

    def __init__(self,zk):
        self.zk = zk
	
    def submit_task(self):
        self.id = zk.create(TASKS_PATH + "/", "".encode('ascii'), ephemeral=True, sequence=True)
        self.id =self.id.split("/")[-1]

        zk.create(DATA_PATH + "/"+ self.id, str(rd.randint(1,10)).encode('ascii') , ephemeral=True, sequence=False)

        zk.get_children(TASKS_PATH + "/" + self.id, self.task_completed)

		
    #REACT to changes on the submitted task..				   
    def task_completed(self, event):
  	  	#TO COMPLETE
        #print(f"dir(event)={dir(event)}")
        id_ = event.path 
        zk.delete(DATA_PATH + "/" +  id_.split("/")[-1])

	
    def submit_task_loop(self):
        #while True:
        self.submit_task()
        time.sleep(1)

if __name__ == '__main__':
    zk = utils.init()    
    client = Client(zk)
    client.submit_task_loop()
    while True:
        time.sleep(1)

