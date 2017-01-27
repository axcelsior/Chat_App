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
	int identifier;
	Hashtable<Integer, String> messages = new Hashtable<Integer, String>();

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
		String message = null;
		String cmd = " /connect ";
		message = "0 " + name + cmd + name;

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
		String outPut = null;
		int id = 0;
		byte[] buf = new byte[256];
		DatagramPacket p = new DatagramPacket(buf, buf.length, m_serverAddress, m_serverPort);
		try {
			m_socket.receive(p);
		} catch (IOException e) {
			System.out.println("IO Exception at: " + e.getMessage());
		} finally {
			String sentance = new String(p.getData(), 0, p.getLength());
			string = sentance;
		}
		String[] split = string.split("\\s+");

		if (split[1].equals("disconnect")) {
			System.exit(0);
		}
		if (split[1].equals("IDENTIFIER")) {
			// acknoledgement recieved
			id = Integer.parseInt(split[0]);
			split[0] = "";
			split[1] = "";
			System.out.println("Acnoledgement gotten");
			messages.remove(id);
		}
		split[0] = "";
		string = String.join(" ", split);
		// Update to return message contents
		return string;

	}

	public void sendChatMessage(String message) {
		Random generator = new Random();
		double failure = generator.nextDouble();
		String outPut = identifier + " " + message;
		messages.put(identifier, message);

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

			// tråd som väntar
			// kolla om meddelande kommit fram

		} else {
			System.out.println("Message lost Client");
			// sendChatMessage(messages.get(identifier));

		}
		/*
		 * Thread thread = new Thread() { int i = identifier; public void run()
		 * { System.out.println("Thread Running for id: " + i);
		 * 
		 * 
		 * } };
		 * 
		 * thread.start();
		 * 
		 * 
		 */
		if (messages.containsKey(identifier)) {
			Timer timer = new Timer();
			TimerTask task = new TimerTask() {
				int i = identifier;

				@Override
				public void run() {
					if (messages.containsKey(i)) {
						System.out.println("Re-trying...");
						sendChatMessage(messages.get(i));
					} else {
						return;
					}
				}
			};
			timer.schedule(task, 400);
		}

		identifier++;
	}

}
