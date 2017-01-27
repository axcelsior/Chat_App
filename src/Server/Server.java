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
	Hashtable<Integer, Integer> messages = new Hashtable<Integer, Integer>();

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
		/*
		 * for (Iterator<ClientConnection> itr = m_connectedClients.iterator();
		 * itr.hasNext();) { String message = "S|checkConnection";
		 * itr.next().sendMessage(message, m_socket);
		 * 
		 * String response = null; byte[] buf = new byte[256];
		 * 
		 * DatagramPacket p = new DatagramPacket(buf, buf.length); try {
		 * m_socket.receive(p); } catch (IOException e) {
		 * System.out.println("IO exception at: " + e.getMessage()); } finally {
		 * String sentance = new String(p.getData(), 0, p.getLength());
		 * System.out.println("Recieved Message: " + sentance); response =
		 * sentance; }
		 * 
		 * }
		 */
	}

	private void listenForClientMessages() {
		System.out.println("Waiting for client messages... ");

		do {
			// TODO: Listen for client messages.
			// On reception of message, do the following:
			// * Unmarshal message
			// * Depending on message type, either
			// - Try to create a new ClientConnection using addClient(), send
			// response message to client detailing whether it was successful
			// - Broadcast the message to all connected users using broadcast()
			// - Send a private message to a user using sendPrivateMessage()

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

			String command = null;
			String name = null;
			String text = null;
			String sender = null;
			String identifier = null;
			boolean isCMD = false;

			String[] splited = message.split("\\s+");
			identifier = splited[0];
			int id = Integer.parseInt(identifier);
			if (!messages.containsKey(id)) {
				
				messages.put(id, id);

				sender = splited[1];
				splited[0] = "";
				splited[1] = "";

				// Send acknoledgement
				sendPrivateMessage(identifier + " IDENTIFIER", sender);
				if (splited[2].startsWith("/")) {
					isCMD = true;
					command = splited[2];
					splited[2] = "";
				} else {
					isCMD = false;
					// then its a broadcast
					text = String.join(" ", splited);
				}

				if (isCMD) {
					if (command.equals("/list")) {
						sendPrivateMessage(getList(), sender);
					}
					if (command.equals("/leave")) {
						sendPrivateMessage("[Server] You are disconnected.", sender);
						byte[] sendData = new byte[8];
						String st = "0 disconnect";
						sendData = st.getBytes();
						DatagramPacket s = new DatagramPacket(sendData, sendData.length, p.getAddress(), p.getPort());
						try {
							m_socket.send(s);
						} catch (IOException e) {
							System.out.println("IOException at: " + e.getMessage());
						}
						broadcast("0 [Server] " + sender + " has left.");
						removeClient(sender);
					}
					if (command.equals("/tell")) {
						String recieverName = null;
						recieverName = splited[3];
						splited[2] = "";
						text = String.join(" ", splited);
						sendPrivateMessage(identifier + " [Private] from -> " + sender + ": " + text, recieverName);
						sendPrivateMessage(identifier + " [Private] to -> " + recieverName + ": " + text, sender);
					}

					if (command.equals("/connect")) {
						name = splited[3];
						System.out.println("User " + name + " trying to connect...");
						if (!addClient(name, p.getAddress(), p.getPort())) {
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
							System.out.println("User: " + name + " already exist! Connection failed.");
						} else {
							byte[] sendData = new byte[8];
							String t = "1";
							sendData = t.getBytes();
							DatagramPacket s = new DatagramPacket(sendData, sendData.length, p.getAddress(),
									p.getPort());

							try {
								m_socket.send(s);
								broadcast("0 [Server] " + name + " connected to the chatroom!");
							} catch (IOException e) {
								System.out.println("IOException at: " + e.getMessage());
							}
						}
					}
				} else {
					broadcast(identifier + " " + sender + ": " + text);
				}
			}
		} while (true);
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
				c.sendMessage(message, m_socket);
			}
		}
	}

	public void broadcast(String message) {
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			itr.next().sendMessage(message, m_socket);
		}
	}
}
