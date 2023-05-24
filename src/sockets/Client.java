package sockets;

//import com.vdurmont.emoji.EmojiParser;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import javax.swing.border.Border;

public class Client {
	private String host;
	private int port;
	private String nickname;
	private JTextArea chatArea;
	private JTextField messageField;
	private PrintWriter output;
	private JPanel imagePanel;

	public static void main(String[] args) throws UnknownHostException, IOException {
		new Client("127.0.0.1", 12345).run();
	}

	public Client(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void run() throws UnknownHostException, IOException {
		Socket client = new Socket(host, port);
		System.out.println("Client successfully connected to server!");

		JFrame frame = new JFrame("Chat Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 300);

		JPanel mainPanel = new JPanel(new BorderLayout());
//		JPanel imagePanel = new JPanel();
//		mainPanel.add(imagePanel);

		chatArea = new JTextArea();
		chatArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(chatArea);
		mainPanel.add(scrollPane, BorderLayout.CENTER);

		messageField = new JTextField();
		messageField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}
		});
		mainPanel.add(messageField, BorderLayout.SOUTH);

		imagePanel = new JPanel();
		mainPanel.add(imagePanel, BorderLayout.WEST);

		JButton sendImageButton = new JButton("Enviar Imagem");
		sendImageButton.setPreferredSize(new Dimension(150, 1));
		sendImageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chooseAndSendImage();
			}
		});
		mainPanel.add(sendImageButton, BorderLayout.EAST);

		frame.getContentPane().add(mainPanel);
		frame.setVisible(true);

		output = new PrintWriter(client.getOutputStream(), true);

		nickname = JOptionPane.showInputDialog(frame, "Enter a nickname:");
		if (nickname == null || nickname.trim().isEmpty()) {
			nickname = "Guest";
		}
		output.println(nickname + " se conectou!");

		new Thread(new ReceivedMessagesHandler(client.getInputStream(), chatArea, imagePanel)).start();
	}

	private void sendMessage() {
		String message = messageField.getText();
		output.println(nickname + ": " + message);
		messageField.setText("");
	}

	private void chooseAndSendImage() {
		JFileChooser fileChooser = new JFileChooser();
		int result = fileChooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			try {
				sendImage(selectedFile);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Error sending image: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void sendImage(File file) throws IOException {
		String fileName = file.getName();
		byte[] imageData = Files.readAllBytes(file.toPath());
		String imageBase64 = java.util.Base64.getEncoder().encodeToString(imageData);

		output.println("[Image: " + fileName + "]");
		output.println(imageBase64);
	}
}

class ReceivedMessagesHandler implements Runnable {
	private InputStream server;
	private JTextArea chatArea;
	private JPanel imagePanel;

	public ReceivedMessagesHandler(InputStream server, JTextArea chatArea, JPanel imagePanel) {
		this.server = server;
		this.chatArea = chatArea;
		this.imagePanel = imagePanel;
	}

	public void run() {
		Scanner s = new Scanner(server);
		while (s.hasNextLine()) {
			String message = s.nextLine();
			if (message.startsWith("[Image: ")) {
				String fileName = message.substring(8, message.indexOf("]"));
				String imageData = s.nextLine();
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						showImage(fileName, imageData);
					}
				});
			} else {
				chatArea.append(message + "\n");
			}
		}
		s.close();
	}

	private void showImage(String fileName, String imageData) {
		byte[] imageBytes = java.util.Base64.getDecoder().decode(imageData);
		ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
		try {
			Image image = ImageIO.read(bis);
			JLabel imageLabel = new JLabel(new ImageIcon(image));
			imagePanel.removeAll();
			imagePanel.add(imageLabel);
			imagePanel.revalidate();
			imagePanel.repaint();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}


