package com.cafe24.network.chat.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
	
	private static final int PORT = 9999;
//	public static List<Writer> listWriters = new ArrayList<Writer>();
//	public static Map<String, Object> listWriters = new HashMap<>();
	public static List<ChatUser> listWriters = new ArrayList<ChatUser>();
	
//	public static int checkNickname(String name) {
//		int check = 0;
//		for(Entry<String, Object> writer : listWriters.entrySet()) {
//			if(writer.getKey().equals(name)) {
//				check += 1;			
//			}
//		}
//		
//		return check;	
//	}
	
	public static void main(String[] args) {
		
		ServerSocket serverSocket = null;
		
		try {
			
			serverSocket = new ServerSocket();
			
			serverSocket.bind(new InetSocketAddress("0.0.0.0", PORT));
			System.out.println("server port : " + PORT);
			
			while(true) {
				Socket socket = serverSocket.accept();
				
				Thread thread = new ChatServerReceiveThread(socket, listWriters);
				thread.start();
			}
		
		}catch(IOException e) {
			System.out.println("[CharServer] #1");
		}finally {
			try {
				if(serverSocket != null && serverSocket.isClosed()) {
					serverSocket.close();
				}
			}catch(IOException e) {
				System.out.println("[CharServer] #2");
			}
		}
		
	}
	public static void log(String log) {
		System.out.println("[server# "+ Thread.currentThread().getId() + "] " + log);
		
	}

}
