/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 
 * @author brom
 */
public class ClientConnection {

	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private final String m_name;
	private final InetAddress m_address;
	private final int m_port;
	int m_Identifier;
	DatagramSocket m_resendSocket;
	Hashtable<Integer, String> messages = new Hashtable<Integer, String>();

	public ClientConnection(String name, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
	}

	public void sendMessage(String message, DatagramSocket socket) {

		Random generator = new Random();
		double failure = generator.nextDouble();
		String msg = m_Identifier + " " + message;
		
				
		messages.put(m_Identifier, message);
		
		
		if (failure > TRANSMISSION_FAILURE_RATE) {
			// TODO: send a message to this client using socket.
			byte[] sendData = new byte[256];
			sendData = msg.getBytes();
			DatagramPacket s = new DatagramPacket(sendData, sendData.length,m_address,m_port);

			try {
				socket.send(s);
			} catch (IOException e) {
				System.out.println("IOException at: " + e.getMessage());
			}
			
			
			
		} else {
			// Message got lost
			System.out.println("Message lost Server.");
//			sendMessage(message,socket);
		}
		
		
		
		m_resendSocket = socket;
		if (messages.containsKey(m_Identifier)) {
			Timer timer = new Timer();
			TimerTask task = new TimerTask() {
				int i = m_Identifier;

				@Override
				public void run() {
					if (!messages.containsKey(i)){
						return; // do not resend
					}
					else {
						System.out.println("[Server] Re-trying to send [" + Integer.toString(i) + "]");
						sendMessage(messages.get(i),m_resendSocket);
					}
				}
			};
			timer.schedule(task, 400);
		}
		m_Identifier++;
	}

	public String getName() {
		return m_name;
	}

	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}

}
