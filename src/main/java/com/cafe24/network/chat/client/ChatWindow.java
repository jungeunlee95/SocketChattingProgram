package com.cafe24.network.chat.client;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

public class ChatWindow {

	private Frame frame;
	private Panel pannel;
	private Button buttonSend;
	private TextField textField;
	private TextArea textArea;
	private Socket socket;
	private String name;
	
	private PrintWriter pw;
	private BufferedReader br;
	
	
	public ChatWindow(String name, Socket socket) {
		this.socket = socket;
		this.name = name;
		frame = new Frame(name); // 큰 틀
		pannel = new Panel(); // 아래 대화 입력
		buttonSend = new Button("Send"); // 패널의 자식
		textField = new TextField(); // 패널의 자석
		textArea = new TextArea(30, 80); // 대화 뜨는 공간
	}

	private void finish() {
		// socket 정리
		System.exit(0);
	}

	public void show() throws IOException, UnsupportedEncodingException {
		// Button
		buttonSend.setBackground(Color.GRAY);
		buttonSend.setForeground(Color.WHITE);
		buttonSend.addActionListener(new ActionListener() { // 버튼이 눌려지는지 기다림
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				sendMessage();
			}
		});

		// Textfield
		textField.setColumns(80);
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				char keyCode = e.getKeyChar();
				if (keyCode == KeyEvent.VK_ENTER) {
					sendMessage();
				}
			}
		}); // 엔터 쳤을 때

		// Pannel
		pannel.setBackground(Color.LIGHT_GRAY);
		pannel.add(textField);
		pannel.add(buttonSend);
		frame.add(BorderLayout.SOUTH, pannel);

		// TextArea
		textArea.setEditable(false);
		frame.add(BorderLayout.CENTER, textArea);

		// Frame
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				finish();
			}
		});
		frame.setVisible(true);
		frame.pack();

		// pw, br
		pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"), true);
		br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));


		// Thread 생성
		new ReceiveThread().start();
	}

	private void updateTextArea(String message) { // thread에서 불러야함
		textArea.append(message);
		textArea.append("\n");
	}

	
	private void sendMessage() {
		String message = textField.getText();

		pw.println("message:" + message);
				
		textField.setText("");
		textField.requestFocus();
	}
	
	private class ReceiveThread extends Thread {
		@Override
		public void run() {
			while(true){
				try {
					
					String reply = br.readLine(); // blocking

					if (reply == null){
						break;
					}
					
					updateTextArea(reply);

				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		
	}
}
