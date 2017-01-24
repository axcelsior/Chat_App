/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

/**
 * 
 * @author brom
 */
public class ClientConnection {

	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private final String m_name;
	private final InetAddress m_address;
	private final int m_port;

	public ClientConnection(String name, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
	}

	public void sendMessage(String message, DatagramSocket socket) {

		Random generator = new Random();
		double failure = generator.nextDouble();
		String msg = m_name +" "+ message;
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
		}

	}

	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}

}
