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
		m_Identifier = 0;
	}
	public void sendNewMessage(String message,DatagramSocket socket){ // Generates UID and invokes sendMessage
		int ID = getUID();
		String msg = ID + " " + message;
		messages.put(ID, msg);
		sendMessage(msg,socket);
	}
	public void sendMessage(String message, DatagramSocket socket) {

		Random generator = new Random();
		double failure = generator.nextDouble();
		String[] splitedMsg = message.split(" ");
		Integer ID = Integer.parseInt(splitedMsg[0]);
				
		//messages.put(ID, message);
		
		
		if (failure > TRANSMISSION_FAILURE_RATE) {
			// TODO: send a message to this client using socket.
			byte[] sendData = new byte[256];
			sendData = message.getBytes();
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
		
		class MyThread implements Runnable {
			int message_id;
			DatagramSocket send_socket;
			public MyThread(int id,DatagramSocket socket) {
				// store parameter for later user
				message_id = id;
				send_socket = socket;
			}

			public void run() {
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (!messages.containsKey(message_id)){
					System.out.println("Message already gotten acknowledgement. Closing Thread.");
					return; // do not resend
				}
				else {
					System.out.println("[Server] Re-trying to send [" + Integer.toString(message_id) + "]");
					sendMessage(messages.get(message_id),send_socket);
				}

			}
		}
		Runnable r = new MyThread(ID,socket);
		if (messages.containsKey(ID)){ // Starts new thread to resend message after 400 ms if message is registered in the messageList
			new Thread(r).start();
		}
		
	}
	public int getUID(){
		m_Identifier++;
		return m_Identifier;
	}
	public String getName() {
		return m_name;
	}

	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}

}
