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
	int m_UID;
	Hashtable<Integer, String> messages = new Hashtable<Integer, String>();
	Hashtable<Integer, Integer> recievedIdentifiers = new Hashtable<Integer, Integer>();

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
		int acknowledgedMessage = 0; // keeps the id of the process that got its response
		boolean duped = false; // says if the message was a duplication or not
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
		if (!recievedIdentifiers.containsKey(Integer.parseInt(split[0]))) { // Only do if message identifier isnt 
			recievedIdentifiers.put(id, id);								// already registered in the id list
			if (split[1].equals("connectionAllowed")){						// Otherwise add the id to the list
				sender = split[2];
				string = "Connected!";
			}
			if (split[1].equals("disconnect")) {
				System.exit(0);
			}
			if (split[1].equals("ackn")) {
				// acknowledgement received
				acknowledgedMessage = Integer.parseInt(split[2]);
				split[0] = "";
				split[1] = "";
				sender = split[3];
				System.out.println("Acnoledgement gotten. Removing: " + split[2]);
				messages.remove(acknowledgedMessage);
				string = "";
			}
			if (split[1].equals("ackndupe")) {
				// acknowledgement received
				acknowledgedMessage = Integer.parseInt(split[2]);
				sender = split[3];
				split[0] = "";
				split[1] = "";
				System.out.println("Acknowledgement of duped message gotten. Removing: " + split[2]);
				messages.remove(acknowledgedMessage);
				string = "";
				duped = true;
			}

			// Send response to ensure server message got through
			if (!duped) {
				sendNewChatMessage(sender + " /ackn " + Integer.toString(id));
			}

		} else {
			string = "";
			System.out.println(id + " Duplicate message revieved.");
			sendNewChatMessage(sender + " /ackndupe " + Integer.toString(id));
		}
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
		double failure = generator.nextDouble();
		String[] split = message.split(" ");
		String outPut = message;
		Integer id = Integer.parseInt(split[0]);

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
			System.out.println("Message lost Client");
		}

		class MyThread implements Runnable {
			int message_id;

			public MyThread(int id) {
				// store parameter for later user
				message_id = id;
			}

			public void run() {
				try {
					Thread.sleep(400); // Will resend message after 400 ms
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Checking message [" + message_id + "] if it needs to resend");
				if (!messages.containsKey(message_id)) {
					System.out.println("Message already gotten acknowledgement. Closing Thread.");
					return;
				} else {
					System.out.println("Resending Message...[" + message_id + "]");
					sendChatMessage(messages.get(message_id));
				}

			}
		}
		Runnable r = new MyThread(id);
		if (messages.containsKey(id)) { // Starts resendthread if and only if message is in the re-send list
			new Thread(r).start();
		}
	}

}
