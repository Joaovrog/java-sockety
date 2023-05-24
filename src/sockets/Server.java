package sockets;


//import com.vdurmont.emoji.EmojiParser;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class Server {
	private int port;
	private List<PrintStream> clients;
	private ServerSocket server;
	private JTextArea chatArea;
	private List<String> messageHistory;

	public static void main(String[] args) throws IOException {
		new Server(12345).run();
	}

	public Server(int port) {
		this.port = port;
		this.clients = new ArrayList<PrintStream>();
		this.messageHistory = new ArrayList<String>();
	}

	public void run() throws IOException {
		server = new ServerSocket(port) {
			protected void finalize() throws IOException {
				this.close();
			}
		};
		System.out.println("Port 12345 is now open.");

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});

		while (true) {
			Socket client = server.accept();
			System.out.println("Connection established with client: " + client.getInetAddress().getHostAddress());

			clients.add(new PrintStream(client.getOutputStream()));

			sendHistoryToClient(client);

			new Thread(new ClientHandler(this, client.getInputStream())).start();
		}
	}

	private void createAndShowGUI() {
		JFrame frame = new JFrame("Chat Server");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 300);

		JPanel mainPanel = new JPanel(new BorderLayout());

		chatArea = new JTextArea();
		chatArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(chatArea);
		mainPanel.add(scrollPane, BorderLayout.CENTER);

		frame.getContentPane().add(mainPanel);
		frame.setVisible(true);
	}

	public void broadcastMessages(String msg) {
		for (PrintStream client : clients) {
			client.println(msg);
		}
		chatArea.append(msg + "\n");
		messageHistory.add(msg);
	}

	public void broadcastImage(String fileName, byte[] imageData) {
		String imageBase64 = Base64.getEncoder().encodeToString(imageData);
		String message = "[Image: " + fileName + "]\n" + imageBase64;
		broadcastMessages(message);
	}

	private void sendHistoryToClient(Socket client) {
		try {
			PrintWriter output = new PrintWriter(client.getOutputStream(), true);
			for (String message : messageHistory) {
				output.println(message);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class ClientHandler implements Runnable {
	private Server server;
	private InputStream client;

	public ClientHandler(Server server, InputStream client) {
		this.server = server;
		this.client = client;
	}

	public void run() {
		String message;
		Scanner sc = new Scanner(this.client);
		while (sc.hasNextLine()) {
			message = sc.nextLine();
			server.broadcastMessages(message);
		}
		sc.close();
	}
}



