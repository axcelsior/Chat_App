package Server;

import java.io.IOException;

//
// Source file for the server side. 
//
// Created by Sanny Syberfeldt
// Maintained by Marcus Brohede
//

import java.net.*;
//import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;

public class Server {

	private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
	private DatagramSocket m_socket;
	Hashtable<String, ArrayList<Integer>> recievedIdentifiers = new Hashtable<String, ArrayList<Integer>>();

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: java Server portnumber");
			System.exit(-1);
		}
		try {
			Server instance = new Server(Integer.parseInt(args[0]));
			instance.listenForClientMessages();
		} catch (NumberFormatException e) {
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Server(int portNumber) {
		// TODO: create a socket, attach it to port based on portNumber, and
		// assign it to m_socket
		DatagramSocket temp_socket = null; // Creating temporary socket to
											// assign port

		try {
			temp_socket = new DatagramSocket(portNumber);
		} catch (NullPointerException e) {
			System.out.println("Exception Nullpointer @ " + e.getMessage());
		} catch (Exception e) {
			System.out.println("Exception at " + e.getMessage());
		} finally {
			m_socket = temp_socket; // Assigning Servers Socket with given port
			// temp_socket.close(); // Closing temporary socket
			System.out.println("Socket created and attached to port: " + portNumber);
		}
	}

	private void checkClientConnection() {

	}

	private void listenForClientMessages() {
		System.out.println("Waiting for client messages... ");
		connectionChecker c_check = new connectionChecker();
		c_check.start(); // Starting connection checker who will check if
							// clients are timed out or not.

		do {
			String message = null;
			byte[] buf = new byte[256];

			DatagramPacket p = new DatagramPacket(buf, buf.length);
			try {
				m_socket.receive(p);
			} catch (IOException e) {
				System.out.println("IO exception at: " + e.getMessage());
			} finally {
				String sentance = new String(p.getData(), 0, p.getLength());
				System.out.println("Recieved Message: " + sentance);
				message = sentance;
			}

			// message argument variables
			String command = null;
			String text = null;
			String sender = null;
			String identifier = null;
			boolean isCMD = false;
			boolean ackn = false;

			String[] splited = message.split("\\s+"); // Splitting message by
														// spaces
			identifier = splited[0];
			sender = splited[1]; // Sender name
			int id = Integer.parseInt(identifier); // Message identifier
			System.out.println("SERVER RECIEVED ID: " + identifier);
			System.out.println(recievedIdentifiers);

			String ipandport = p.getAddress().getHostName() + ":" + p.getPort();
			if (!recievedIdentifiers.containsKey(ipandport)) { 
				recievedIdentifiers.put(ipandport, new ArrayList());
			}
			if (recievedIdentifiers.containsKey(ipandport)) { 
				if (!recievedIdentifiers.get(ipandport).contains(id)) {
					
					recievedIdentifiers.get(ipandport).add(id);

					splited[0] = "";
					splited[1] = "";

					if (splited[2].startsWith("/")) { // If the 2nd arg starts
														// with
														// / its a command
						isCMD = true;
						command = splited[2];
						splited[2] = "";
					} else {
						isCMD = false;
						// then its a broadcast
						text = String.join(" ", splited);
					}

					if (isCMD) {
						if (command.equals("/ackn")) {
							int ackn_ID = Integer.parseInt(splited[3]);
							System.out.println("Recieved acknoledgement from client! Removing: " + ackn_ID);
							// Remove acknowledged message from the re-send
							// hashtable
							removeMessage(ackn_ID, sender);
							ackn = true;
						}
						if (command.equals("/list")) {
							sendPrivateMessage(getList(), sender);
						}
						if (command.equals("/leave")) {
							sendPrivateMessage("[Server] You are disconnected.", sender);
							byte[] sendData = new byte[8];
							String st = "0 disconnect";
							sendData = st.getBytes();
							DatagramPacket s = new DatagramPacket(sendData, sendData.length, p.getAddress(),
									p.getPort());
							try {
								m_socket.send(s);
							} catch (IOException e) {
								System.out.println("IOException at: " + e.getMessage());
							}
							broadcast("[Server] " + sender + " has left.");
							removeClient(sender);
						}
						if (command.equals("/tell")) {
							String recieverName = null;
							recieverName = splited[3];
							splited[2] = "";
							text = String.join(" ", splited);
							sendPrivateMessage("[Private] from -> " + sender + ": " + text, recieverName);
							sendPrivateMessage("[Private] to -> " + recieverName + ": " + text, sender);
						}
						if (command.equals("/alive")) {
							setAlive(sender);
						}
						if (command.equals("/connect")) {
							System.out.println("User " + sender + " trying to connect...");
							if (!addClient(sender, p.getAddress(), p.getPort())) {

								byte[] sendData = new byte[8];
								String st = "0";
								sendData = st.getBytes();
								DatagramPacket s = new DatagramPacket(sendData, sendData.length, p.getAddress(),
										p.getPort());

								try {
									m_socket.send(s);
								} catch (IOException e) {
									System.out.println("IOException at: " + e.getMessage());
								}
								System.out.println("User: " + sender + " already exist! Connection failed.");
							} else {
								byte[] sendData = new byte[8];
								String t = "1";
								sendData = t.getBytes();
								DatagramPacket s = new DatagramPacket(sendData, sendData.length, p.getAddress(),
										p.getPort());

								try {
									m_socket.send(s);
									broadcast("[Server] " + sender + " connected to the chatroom!");
								} catch (IOException e) {
									System.out.println("IOException at: " + e.getMessage());
								}

							}
						} else {
							// if not connect

						}
					} else {
						broadcast(sender + ": " + text);
					}

				}
			}
			// Send acknowledgement
			if (!ackn)
				sendPrivateMessage("ackn " + id + " " + sender, sender);

		} while (true);
	}

	private void removeMessage(Integer identifier, String name) {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				if (c.messages.containsKey(identifier)) {
					c.messages.remove(identifier);
				}
			} else {
				System.out.println("Name: " + name + " not found failed to remove [" + identifier + "]");
			}
		}
	}

	private String getList() {
		String returnValue = null;
		String list = "Connected clients: ";
		for (int i = 0; i < m_connectedClients.size(); i++) {
			list += m_connectedClients.get(i).getName();
			if (i <= m_connectedClients.size() - 2) {
				list += ", ";
			}
		}
		returnValue = list;
		return returnValue;
	}

	public boolean addClient(String name, InetAddress address, int port) {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				return false; // Already exists a client with this name
			}
		}
		m_connectedClients.add(new ClientConnection(name, address, port));
		return true;
	}

	public boolean removeClient(String name) {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				m_connectedClients.remove(c);
				return true; // successful
			}
		}
		return false;
	}

	public void sendPrivateMessage(String message, String name) {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				c.sendNewMessage(message, m_socket);
			}
		}
	}

	public void broadcast(String message) {
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			itr.next().sendNewMessage(message, m_socket);
		}
	}

	public void cleanUpClients() {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (!c.isAlive()) {
				m_connectedClients.remove(c);
				broadcast("[Server] " + c.getName() + " timed out... Bye!");
			}
		}
	}

	public void setAlive(String name) {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				c.setClientAsAlive(); // Client responded its still alive.
			}
		}
	}

	class connectionChecker extends Thread {
		String m_message;

		public connectionChecker() {
			m_message = "checkConnection";
		}

		public void run() {
			do {
				ClientConnection c;
				for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
					c = itr.next();
					c.setClientAsDead(); // Sets client as dead. Wont be set as
											// alive until response is recieved
					c.sendNewMessage(m_message, m_socket);
				}

				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}

				// Clean up unresponsive clients
				cleanUpClients();

			} while (true);

		}
	}

}
