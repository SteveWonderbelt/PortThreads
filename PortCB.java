/**
@author: STeven Childs 
@email: schilds@email.sc.edu
*/
package osp.Ports;

import java.util.*;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Memory.*;
import osp.Utilities.*;

/**
   The studends module for dealing with ports. The methods 
   that have to be implemented are do_create(), 
   do_destroy(), do_send(Message msg), do_receive(). 


   @OSPProject Ports
*/

public class PortCB extends IflPortCB
{
    /**
       Creates a new port. This constructor must have

	   super();

       as its first statement.

       @OSPProject Ports
    */
    public PortCB()
    {
        // your code goes here
		super();
    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Ports
    */
	
	public int portBufferLeft;//This is our variable for keeping track of how much of the buffer is remaining. 
    public static void init()
    {
        // your code goes here
			
    }

    /** 
        Sets the properties of a new port, passed as an argument. 
        Creates new message buffer, sets up the owner and adds the port to 
        the task's port list. The owner is not allowed to have more 
        than the maximum number of ports, MaxPortsPerTask.

        @OSPProject Ports
    */
    public static PortCB do_create()
    {
        // your code goes here
		PortCB newPort = new PortCB();//create new port
		TaskCB curTask = MMU.getPTBR().getTask();//Gets the current task
		if(curTask.addPort(newPort) == FAILURE){//what happens if MaxPortsPerTask is already maximum
			return null; //If the new port cannot be added, return null.
		}
		//Otherwise: 
		newPort.setTask(curTask);//set the ports task to the current task
		newPort.setStatus(PortLive);//set the ports status 
		newPort.portBufferLeft = PortBufferLength;//adjust the portBufferLeft var accordingly. 
			
		
		return newPort; //return the new port
    }

    /** Destroys the specified port, and unblocks all threads suspended 
        on this port. Delete all messages. Removes the port from 
        the owners port list.
        @OSPProject Ports
    */
    public void do_destroy()
    {
        // your code goes here
		this.setStatus(PortDestroyed); //set the ports status
		this.notifyThreads();//alert threads to change
		TaskCB temp = this.getTask(); //a temperorary task to hold the current task
		temp.removePort(this); //remove the port from the current task
		this.setTask(null); //set the current ports task to null
    }

    /**
       Sends the message to the specified port. If the message doesn't fit,
       keep suspending the current thread until the message fits, or the
       port is killed. If the message fits, add it to the buffer. If 
       receiving threads are blocked on this port, resume them all.

       @param msg the message to send.

       @OSPProject Ports
    */
    public int do_send(Message msg)
    {
        // your code goes here
		if(msg == null){
			return FAILURE; //no message = failure
		}else if(msg.getLength() > this.PortBufferLength){
			return FAILURE; //too large a message = failure
		}
		SystemEvent sysEvent = new SystemEvent("sysEvent"); //neeed a system event
		ThreadCB tempThread = null; //placeholder thread
		try{
			tempThread = MMU.getPTBR().getTask().getCurrentThread(); //Attempt to get the current thread but throw an exception if we cannot.
		}
		catch(Exception e){
			MyOut.print("No current thread for do_send", getFullStackTrace());
		}
		tempThread.suspend(sysEvent); //suspend the sysEvent from the thread
		
		boolean incomplete = true; 
		while(incomplete){ //This loop runs checks to ensure that a messsage can actually be sent. 
			//If there's no room in the buffer then we need to suspend the port
			if(msg.getLength() > this.portBufferLeft){
				tempThread.suspend(this);//cant fit into the buffer, so can't send message yet. 
			}if(tempThread.getStatus() == ThreadKill){
				this.removeThread(tempThread);//The thread is dead, so can't send message
				return FAILURE;
			}if(this.getStatus() != PortLive){
				sysEvent.notifyThreads();
				return FAILURE;
			}else{
				incomplete = false;//indicates success as far as these checks go. 
			}
			
		}
		boolean wasEmpty = this.isEmpty();
		this.appendMessage(msg);//add msg to buffer
		if(wasEmpty){//if the port is empty, notify the threads
			this.notifyThreads();
		}
		this.portBufferLeft = this.portBufferLeft - msg.getLength(); //adjust the bufferleft var
		sysEvent.notifyThreads(); 
		return SUCCESS;
    }

    /** Receive a message from the port. Only the owner is allowed to do this.
        If there is no message in the buffer, keep suspending the current 
	thread until there is a message, or the port is killed. If there
	is a message in the buffer, remove it from the buffer. If 
	sending threads are blocked on this port, resume them all.
	Returning null means FAILURE.

        @OSPProject Ports
    */
    public Message do_receive() 
    {
        // your code goes here
		
		//Much of this is the same as in the do_send method
		ThreadCB curThread = null; 
		try{
			curThread = MMU.getPTBR().getTask().getCurrentThread();
		}catch(Exception e){
			MyOut.print("No current thread to perform do_receive on", getFullStackTrace());
		}
		if(curThread.getTask() != this.getTask()){
			return null;
		}
		SystemEvent sysEvent = new SystemEvent("sysEvent");
		curThread.suspend(sysEvent);
		boolean incomplete = true;
		while(incomplete){
			if(curThread.getStatus() == ThreadKill){
				this.removeThread(curThread);
				sysEvent.notifyThreads();
				return null;
			}if(this.getStatus() != PortLive){
				sysEvent.notifyThreads();
				return null;
			}if(this.isEmpty()){
				curThread.suspend(this);
			}else{
				incomplete = false;
		}}
		Message message = this.removeMessage();
		this.portBufferLeft = this.portBufferLeft + message.getLength();
		this.notifyThreads();
		sysEvent.notifyThreads();
		return message; 
	}

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Ports
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
	@OSPProject Ports
    */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
