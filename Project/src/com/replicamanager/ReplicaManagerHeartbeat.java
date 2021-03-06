package com.replicamanager;

import java.net.InetAddress;
import java.util.concurrent.TimeoutException;

import com.reliableudp.OperationMessage;
import com.reliableudp.OperationMessageProcessorInterface;
import com.reliableudp.ReliableUDPSender;
import com.utils.ContactInformation;
import com.utils.SynchronizedLogger;

public class ReplicaManagerHeartbeat extends Thread {

	private	ReplicaManagerInformation	m_replicaManagerInformation;
	private	ContactInformation			m_replicaClientInformation;
	
	private ReplicaManagerInformation	m_unmonitoredReplicaManager;
	
	private int							m_parentReplicaManagerId;
	
	private static final int			ALIVETIMETOTIMEOUT  = 1000;
	private static final int			TIMETOBEAT			= 2000;
	
	private	boolean						m_on;
	
	private SynchronizedLogger			m_logger;
	
	
	ReplicaManagerHeartbeat(ReplicaManagerInformation[] distantReplicaManagers, int replicaManagerToMonitor, int replicaManagerUnmonitored, int parentReplicaManagerId) {
		
		
		m_replicaManagerInformation = distantReplicaManagers[replicaManagerToMonitor];
		m_replicaClientInformation = distantReplicaManagers[replicaManagerToMonitor].getReplicaClientInformation();
		
		m_unmonitoredReplicaManager = distantReplicaManagers[replicaManagerUnmonitored];
		
		m_parentReplicaManagerId = parentReplicaManagerId;
		
		m_logger = new SynchronizedLogger("Heartbeat_ParentRM" + parentReplicaManagerId + "_MointoredRM_" + replicaManagerToMonitor); 
		
		m_on = true;
		
		m_logger.addStartLine();
		m_logger.log("Heartbeat started.");
	}
	
	
	public void setOn() {
		
		m_logger.log("Heartbeat set on.");
		m_on = true;
		
	}
	
	public void setOff() {
		
		m_logger.log("Heartbeat set off.");
		m_on = false;
		
	}
	
	
	
	public void run() {
		
		ReliableUDPSender sender = new ReliableUDPSender(m_replicaClientInformation.getAddress(), m_replicaClientInformation.getPort());
		sender.setMaxTimeouts(ALIVETIMETOTIMEOUT);
		
		OperationMessage aliveMessage = new OperationMessage(OperationMessage.ALIVE);
		OperationMessage reply;
			
		// Loop on sending ALIVE messages.
		while (true) {
			
			// The heartbeat can be switched to off in case of crash recovery.
			while (!m_on) {
				continue;
			}
			m_logger.log("Starting new pinging..");

			
			// Timer loop.
			long startTime = System.currentTimeMillis();
			while ((System.currentTimeMillis() - startTime) < TIMETOBEAT) {
				continue;
			}
			
			reply = null;
			
			// Send message.
			try {
				reply = sender.send(aliveMessage);
				System.out.println("Client: received response; " + "message has protocol id: " + reply.getOpid());
				m_logger.log("Ping sent.");

			} catch (TimeoutException e) {

				m_logger.log("Replica did not respond within allotted time.");
				// We have not received a response within the specified time to timeout.
				// We can suppose that the replica client is unreachable.
				
				// Calling this method will inform that a crash may have happened on this replica client.
				// The parent ReplicaManager instance will be able to notice the crash if it receives a crash consensus agreement request.
				m_replicaManagerInformation.crashNoticed();
				
				m_logger.log("Crash notice set to true");
				
				// We engage in a crash consensus agreement with the unmonitored replica manager.
				OperationMessage notice = new OperationMessage(OperationMessage.FAILCONSENSUSREQ);
				
				
				// Add faulty replica manager id.
				notice.addMessageComponent(Integer.toString(m_replicaManagerInformation.getId()));
				// Add this replica manager id.
				
				// Sender to the other replica manager, which is not supposed to received alive messages from this instance.
				ReliableUDPSender sendToUnmonitoredReplicaManager = new ReliableUDPSender(m_unmonitoredReplicaManager.getAddress(), m_unmonitoredReplicaManager.getPort());
				OperationMessage crashAgreementResponse = null;
				try { crashAgreementResponse = sendToUnmonitoredReplicaManager.send(notice); } catch (TimeoutException e1) { e1.printStackTrace(); }
				
				m_logger.log("Collaborating server responded with message: " + crashAgreementResponse.getMessage());
				
				// Process response.
				switch(crashAgreementResponse.getOpid()) {
				
					case OperationMessage.FAILCONSENSUSREPLYPOSITIVE:
						
						m_logger.log("Collaborating server responded with POSITIVE notice to crash");
						
						// The other replica manager has also noticed the crash.
						// We need to verify if the other replica manager has green-lit the crash resolution from our end.					
						boolean shouldResolveCrash;
						String stringBoolean = crashAgreementResponse.getContentComponents().get(0);
						if (stringBoolean.equals("1")) {
							shouldResolveCrash = true;
						}
						else { shouldResolveCrash = false; }
						
						if (shouldResolveCrash) {
							
							m_logger.log("Heartbeat has priority for crash situation resolution");
							
							// We have priority.
							m_logger.log("Sending.. restart signal to faulty replica manager.");

							// Send restart message.
							// We have priority which means that we must be the one to send the restart signal to the faulty replica's replica manager.
							InetAddress address = m_replicaManagerInformation.getAddress();
							int			port	= m_replicaManagerInformation.getPort();
							ReliableUDPSender senderRestart = new ReliableUDPSender(address, port);
							try { senderRestart.send(new OperationMessage(OperationMessage.RESTART)); } catch (TimeoutException e2) { e2.printStackTrace(); }
							
							m_logger.log("Restart signal sent.");
							
							// Set off the heartbeat for now.
							setOff();
							
	
						}
						else {
							
							m_logger.log("Heartbeat doesn't have priority for crash situation resolution");

						}
						
						break;
						
					case OperationMessage.FAILCONSENSUSREPLYNEGATIVE:
						
						m_logger.log("Collaborating server responded with NEGATIVE notice to crash");
						
						// No crash has been noticed.
						// The timeout may have been due to delay.
						continue;
				}
				
			
			}

			
		}
		
		
	}
	
	
	

}
