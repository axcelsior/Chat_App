package Client;

import java.awt.event.*;
//import java.io.*;

public class Client implements ActionListener {

	private String m_name = null;
	private final ChatGUI m_GUI;
	private ServerConnection m_connection = null;
	public int identifier = 0;
	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java Client serverhostname serverportnumber username");
			System.exit(-1);
		}

		try {
			Client instance = new Client(args[2]);
			instance.connectToServer(args[0], Integer.parseInt(args[1]));
		} catch (NumberFormatException e) {
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Client(String userName) {
		m_name = userName;

		// Start up GUI (runs in its own thread)
		m_GUI = new ChatGUI(this, m_name);
	}

	private void connectToServer(String hostName, int port) {
		// Create a new server connection
		m_connection = new ServerConnection(hostName, port);
		if (m_connection.handshake(m_name)) {
			System.err.println("Connected.");
			listenForServerMessages();
		} else {
			System.err.println("Unable to connect to server");
			System.exit(-1);
		}
	}

	private void listenForServerMessages() {
		// Use the code below once m_connection.receiveChatMessage() has been
		// implemented properly.
		do {
			String message = m_connection.receiveChatMessage();
			if(message.equals("") ||message.equals(" ")){
				
			} else
			m_GUI.displayMessage(message);
		} while(true);
	}

	// Sole ActionListener method; acts as a callback from GUI when user hits
	// enter in input field
	@Override
	public void actionPerformed(ActionEvent e) {
		// Since the only possible event is a carriage return in the text input
		// field,
		// the text in the chat input field can now be sent to the server.
		
		m_connection.sendNewChatMessage(m_name+ " " + m_GUI.getInput()); // user name + gui input
		m_GUI.clearInput();
	}
}
