/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author brom
 */

public class ServerConnection {

	// Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private DatagramSocket m_socket = null;
	private InetAddress m_serverAddress = null;
	private int m_serverPort = -1;
	int m_Identifier;
	int m_UID = 0;
	Hashtable<Integer, String> messages = new Hashtable<Integer, String>();
	Hashtable<Integer, Integer> recievedIdentifiers = new Hashtable<Integer, Integer>();
	private String m_name;

	public ServerConnection(String hostName, int port) {
		m_serverPort = port;

		// TODO:
		// * get address of host based on parameters and assign it to
		// m_serverAddress
		// * set up socket and assign it to m_socket
		try {
			m_serverAddress = InetAddress.getByName(hostName);

		} catch (UnknownHostException e) {
			System.out.println("Unknown host expection at: " + e.getMessage());
		}

		DatagramSocket temp_socket = null; // Creating temporary socket to
		// assign port
		try {
			temp_socket = new DatagramSocket();
		} catch (NullPointerException e) {
			System.out.println("Exception Nullpointer @ " + e.getMessage());
		} catch (Exception e) {
			System.out.println("Exception at " + e.getMessage());
		} finally {
			m_socket = temp_socket; // Assigning Servers Socket with given port
			// temp_socket.close(); // Closing temporary socket
			System.out.println("Socket created and attached to port: " + temp_socket.getPort());
		}

	}

	public boolean handshake(String name) {
		// TODO:
		// * marshal connection message containing user name
		// * send message via socket
		// * receive response message from server
		// * unmarshal response message to determine whether connection was
		// successful
		// * return false if connection failed (e.g., if user name was taken)
		m_name = name;
		String message = null;
		String cmd = " /connect ";
		message = getUID() + " " + name + cmd + name;

		byte[] buf = new byte[256];
		buf = message.getBytes();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, m_serverAddress, m_serverPort);
		// * send a chat message to the server
		try {
			m_socket.send(packet);
		} catch (IOException e) {
			System.out.println("IO exception at: " + e.getMessage());
		} catch (NullPointerException e) {
			System.out.println("Nullptr exception at: " + e.getMessage());
		}

		String string = null;
		byte[] resp = new byte[8];
		DatagramPacket p = new DatagramPacket(resp, resp.length, m_serverAddress, m_serverPort);
		
		try {
			m_socket.receive(p);
		} catch (IOException e) {
			System.out.println("IO Exception at: " + e.getMessage());
		} finally {
			String sentance = new String(p.getData(), 0, p.getLength());
			string = sentance;
		}
		if (string == "0") {
			System.out.println("Connection to server failed...");
			return false;
		}

		System.out.println("Connection to " + m_serverAddress + ":" + m_serverPort + " established.");
		return true;

	}

	public String receiveChatMessage() {
		// TODO:
		// * receive message from server
		// * unmarshal message if necessary

		// Note that the main thread can block on receive here without
		// problems, since the GUI runs in a separate thread
		String string = null;
		String sender = null;
		int id = 0;
		boolean ackn = false;
		int acknowledgedMessage = 0; // keeps the id of the process that got its
										// response
		byte[] buf = new byte[256];
		DatagramPacket p = new DatagramPacket(buf, buf.length, m_serverAddress, m_serverPort);
		try {
			m_socket.receive(p); // Receiving packet
		} catch (IOException e) {
			System.out.println("IO Exception at: " + e.getMessage());
		} finally {
			String sentance = new String(p.getData(), 0, p.getLength());
			string = sentance;
		}
		String[] split = string.split("\\s+"); // splitting message by spaces
		id = Integer.parseInt(split[0]);
		if (!recievedIdentifiers.containsKey(id)) { // Only if id isnt received
													// already

			recievedIdentifiers.put(id, id); // adding msg to list
			split[0] = "";
			string = String.join(" ", split);
			if (split[1].equals("connectionAllowed")) { // Otherwise add the id
														// to the list
				sender = split[2];
				string = "Connected!";
			}
			if (split[1].equals("disconnect")) {
				System.exit(0);
			}
			if (split[1].equals("checkConnection")){
				sendNewChatMessage(m_name + " /alive"); // If server asks if we're still connected. Respond with /alive
				string = "";
			}
			if (split[1].equals("ackn")) {
				ackn = true;
				// acknowledgement received
				acknowledgedMessage = Integer.parseInt(split[2]);
				split[0] = "";
				split[1] = "";
				sender = split[3];
				
				// Removing from resend table
				messages.remove(acknowledgedMessage);
				string = "";
			}
		} else {
			string = "";
		}
		// Send response to ensure server message got through
		if (!ackn)
			sendNewChatMessage(m_name + " /ackn " + Integer.toString(id));

		// Update to return message contents
		return string;

	}

	public Integer getUID() { // Generates new UID for messages.
		m_UID++;
		return m_UID;
	}

	public void sendNewChatMessage(String message) {
		int UID = getUID();
		String outPut = Integer.toString(UID) + " " + message;
		messages.put(UID, outPut);
		sendChatMessage(outPut); // outputs message with UID in front.
	}

	public void sendChatMessage(String message) {
		Random generator = new Random();
		boolean ackn = false;
		double failure = generator.nextDouble();
		String[] split = message.split(" ");
		String outPut = message;
		Integer id = Integer.parseInt(split[0]);

		if (split.length > 1 && split[2].equals("/ackn")) {
			ackn = true;
		}
		if (failure > TRANSMISSION_FAILURE_RATE) {
			// TODO:
			// * marshal message if necessary
			byte[] buf = new byte[256];
			buf = outPut.getBytes();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, m_serverAddress, m_serverPort);
			// * send a chat message to the server
			try {
				m_socket.send(packet);
			} catch (IOException e) {
				System.out.println("IO exception at: " + e.getMessage());
			}

		} else {
			// Message got lost
			

		}

		if (!ackn) { // Do not open new thread if message is an acknowledgement
			Thread r = new resendThread(id, outPut);
			new Thread(r).start();
		}
	}

	class resendThread extends Thread {
		int message_id;
		String msg;

		public resendThread(int id, String message) {
			// store parameter for later user
			message_id = id;
			msg = message;
			System.out.println("Starting new resend thread for [" + id + "].");
		}

		public void run() {
			do {

				// Checking if message is still in resend list
				if (!messages.containsKey(message_id)) {
					// Nope... No need to resend
					System.out.println("Closing resend thread for [" + message_id + "].");
					this.interrupt();
					return;
				} else {
					byte[] buf = new byte[256];
					buf = msg.getBytes();
					DatagramPacket packet = new DatagramPacket(buf, buf.length, m_serverAddress, m_serverPort);
					// * send a chat message to the server
					try {
						m_socket.send(packet);
					} catch (IOException e) {
						System.out.println("IO exception at: " + e.getMessage());
					}
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
