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
	Hashtable<Integer, String> messages = new Hashtable<Integer, String>();
	boolean clientAlive;

	public ClientConnection(String name, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
		m_Identifier = 0;
		clientAlive = true;
	}

	public void sendNewMessage(String message, DatagramSocket socket) { // Generates
																		// UID
																		// and
																		// invokes
																		// sendMessage
		int ID = getUID();
		String msg = ID + " " + message;
		messages.put(ID, msg);
		sendMessage(msg, socket);
	}

	public void sendMessage(String message, DatagramSocket socket) {
		boolean ackn = false;
		Random generator = new Random();
		double failure = generator.nextDouble();
		String[] splitedMsg = message.split(" ");
		Integer ID = Integer.parseInt(splitedMsg[0]);

		if (splitedMsg[1].equals("ackn")) {
			ackn = true;
		}

		if (failure > TRANSMISSION_FAILURE_RATE) {

			byte[] sendData = new byte[256];
			sendData = message.getBytes();
			DatagramPacket s = new DatagramPacket(sendData, sendData.length, m_address, m_port);

			try {
				socket.send(s);
			} catch (IOException e) {
				System.out.println("IOException at: " + e.getMessage());
			}

		} else {
			// Message got lost
			System.out.println("Message lost Server.");
		}

		// Starting resend thread if message is not an acknowledgement
		if (!ackn) {
			Thread r = new resendThread(ID, message, socket);
			new Thread(r).start();
		}

	}

	public int getUID() {
		m_Identifier++;
		return m_Identifier;
	}

	public String getName() {
		return m_name;
	}

	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}

	public void setClientAsDead() {
		clientAlive = false;
	}
	public void setClientAsAlive(){
		clientAlive = true;
	}

	public boolean isAlive() {
		return clientAlive;
	}
	class resendThread extends Thread {
		int message_id;
		DatagramSocket send_socket;
		String message;

		public resendThread(int id, String message, DatagramSocket socket) {
			// store parameter for later user
			message_id = id;
			send_socket = socket;
			this.message = message;
		}

		public void run() {

			do { // Resend loop

				if (!messages.containsKey(message_id)) {
					System.out.println("Message already gotten acknowledgement. Closing Thread.");
					this.interrupt();
					return;

				} else {
					byte[] sendData = new byte[256];
					sendData = message.getBytes();
					DatagramPacket s = new DatagramPacket(sendData, sendData.length, m_address, m_port);

					try {
						send_socket.send(s);
					} catch (IOException e) {
						System.out.println("IOException at: " + e.getMessage());
					}
					System.out.println("[Server] Re-trying to send [" + Integer.toString(message_id) + "]");
				}
				try {
					Thread.sleep(400); // Will try again (iterate) after 400 ms
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} while (true);

		}

	}

}
