/**
 * 
 */
package no.hvl.dat110.middleware;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.hvl.dat110.rpc.interfaces.NodeInterface;
import no.hvl.dat110.util.LamportClock;
import no.hvl.dat110.util.Util;

/**
 * @author tdoy
 *
 */
public class MutualExclusion {
		
	/** lock variables */
	private boolean CS_BUSY = false;						// indicate to be in critical section (accessing a shared resource) 
	private boolean WANTS_TO_ENTER_CS = false;				// indicate to want to enter CS
	private List<Message> queueack; 						// queue for acknowledged messages
	private List<Message> mutexqueue;						// queue for storing process that are denied permission. We really don't need this for quorum-protocol
	
	private LamportClock clock;								// lamport clock
	private Node node;
	
	public MutualExclusion(Node node) throws RemoteException {
		this.node = node;
		
		clock = new LamportClock();
		queueack = new ArrayList<Message>();
		mutexqueue = new ArrayList<Message>();
	}
	
	public void acquireLock() {
		CS_BUSY = true;
	}
	
	public void releaseLocks() {
		WANTS_TO_ENTER_CS = false;
		CS_BUSY = false;
	}

	public boolean doMutexRequest(Message message, byte[] updates) throws RemoteException {
		
		System.out.println(node.nodename + " wants to access CS");
		
		queueack.clear();
		mutexqueue.clear();

		clock.increment();

		message.setClock(clock.getClock());

		WANTS_TO_ENTER_CS = true;

		List<Message> peers = removeDuplicatePeersBeforeVoting();

		multicastMessage(message, peers);

		boolean perm = areAllMessagesReturned(peers.size());

		if(perm) {
			acquireLock();
			node.broadcastUpdatetoPeers(updates);
			mutexqueue.clear();
		}
		
		return perm;
	}
	
	// multicast message to other processes including self
	private void multicastMessage(Message message, List<Message> activenodes) throws RemoteException {
		
		for (Message msg : activenodes) {
			NodeInterface stub = Util.getProcessStub(msg.getNodeIP(), msg.getPort());
			stub.onMutexRequestReceived(message);
		}
		
	}
	
	public void onMutexRequestReceived(Message message) throws RemoteException { //TODO

		// increment the local clock
		clock.increment();
		// if message is from self, acknowledge, and call


		int caseid = -1;

		// write if statement to transition to the correct caseid
		// caseid=0: Receiver is not accessing shared resource and does not want to
		// (send OK to sender)
		if(message.getNodeIP().equals(node.nodename)) {
			caseid = 0;
		}else {
			if(!CS_BUSY && !WANTS_TO_ENTER_CS) {
				caseid = 0;
			} else if (CS_BUSY) {
				caseid = 1;
			} else {
				caseid = 2;
			}
		}
		// caseid=1: Receiver already has access to the resource (dont reply but queue
		// the request)

		// caseid=2: Receiver wants to access resource but is yet to - compare own
		// message clock to received message's clock

		// check for decision
		doDecisionAlgorithm(message, mutexqueue, caseid);
	}

	public void doDecisionAlgorithm(Message message, List<Message> queue, int condition) throws RemoteException {


		String procName = message.getNodeIP(); // this is the same as nodeName in the Node class
		int port = message.getPort(); // port on which the registry for this stub is listening

		switch (condition) {

			/**
			 * case 1: Receiver is not accessing shared resource and does not want to (send
			 * OK to sender)
			 */
			case 0: {
				// get a stub for the sender from the registry
				NodeInterface proStub = Util.getProcessStub(procName, port);
				// acknowledge message
				message.setAcknowledged(true);
				// send acknowledgement back by calling onMutexAcknowledgementReceived()
				proStub.onMutexAcknowledgementReceived(message);
				break;
			}

			/**
			 * case 2: Receiver already has access to the resource (dont reply but queue the
			 * request)
			 */
			case 1: {

				// queue this message
				queue.add(message);
				break;
			}

			/**
			 * case 3: Receiver wants to access resource but is yet to (compare own message
			 * clock to received message's clock the message with lower timestamp wins) -
			 * send OK if received is lower. Queue message if received is higher
			 */
			case 2: {
				// check the clock of the sending process
				int senderClock = message.getClock();

				// own clock for the multicast message
				int ownClock = node.getMessage().getClock();
				// compare clocks, the lowest wins

				// if clocks are the same, compare nodeIDs, the lowest wins
				if (senderClock == ownClock || senderClock > ownClock) {
					if(message.getNodeID().compareTo(node.getNodeID()) == -1 || senderClock > ownClock) {

						NodeInterface stud = Util.getProcessStub(message.getNodeIP(), message.getPort());
						stud.onMutexAcknowledgementReceived(message);
					}
				}else {
					queue.add(message);
				}
				// if sender wins, acknowledge the message, obtain a stub and call

				// onMutexAcknowledgementReceived()

				// if sender looses, queue it

				break;
			}

			default:
				break;
		}
		
	}
	
	public void onMutexAcknowledgementReceived(Message message) throws RemoteException {
		
		// add message to queueack
		queueack.add(message);
		
	}
	
	// multicast release locks message to other processes including self
	public void multicastReleaseLocks(Set<Message> activenodes) throws RemoteException {
		
		for (Message message : activenodes) {
			try {
				NodeInterface stub = Util.getProcessStub(message.getNodeIP(), message.getPort());
				stub.releaseLocks();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean areAllMessagesReturned(int numvoters) throws RemoteException { //TODO

		if(numvoters == queueack.size()) {
			queueack.clear();
			return true;
		}

		return false;
	}
	
	private List<Message> removeDuplicatePeersBeforeVoting() {
		
		List<Message> uniquepeer = new ArrayList<Message>();
		for(Message p : node.activenodesforfile) {
			boolean found = false;
			for(Message p1 : uniquepeer) {
				if(p.getNodeIP().equals(p1.getNodeIP())) {
					found = true;
					break;
				}
			}
			if(!found)
				uniquepeer.add(p);
		}		
		return uniquepeer;
	}
}
