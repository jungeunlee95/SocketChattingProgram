package com.cafe24.network.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

public class ChatServerReceiveThread extends Thread {

	private Socket socket;
	private String nickname;
	Map<String, Object> listWriters;
	PrintWriter pw = null;
	BufferedReader br = null;
	
	int check = 0;
	
	public ChatServerReceiveThread(Socket socket, Map<String, Object> listWriters2) {
		this.socket = socket;
		this.listWriters = listWriters2;
	}

	@Override
	public void run() {
		InetSocketAddress inetRemoteSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
		String remoteHostAddress = inetRemoteSocketAddress.getAddress().getHostAddress();
		int remotePort = inetRemoteSocketAddress.getPort();
		System.out.println("[server] 입장 : " + remoteHostAddress + " : " + remotePort);

 		try {
 			br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
 			pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"), true);

			while(true) {
				String data = br.readLine();
				
				if (data == null) {
					System.out.println("사용자 종료!");
					doQuit(pw);
					break;
				}
				
//				System.out.println(">>>>>>received:" + data);
				
				// 프로토콜 분석
				String[] tokens = data.split(":");
				
				if("join".equals(tokens[0])) {
					doJoin(tokens[1], pw);
				}else if ("message".equals(tokens[0])) {
					if(tokens[1].isEmpty()==false) {
						doMessage(tokens[1]);
					}
				}else if("wisper".equals(tokens[0])){
//					System.out.println(tokens[0] + "@@@"+ tokens[1] + "@@@"+ tokens[2] + "@@@");
//					                     wisper @@@ /귓속말 아아  @@@ 안녕?@@@
					if (tokens.length<4) {
						pw.println("-------------------------------------------------------\n"
								+ "\t\t잘못된 입력입니다.\n-------------------------------------------------------");
					}else {
						doWisper(tokens[1].substring(5), tokens[2], tokens[3]);
					}
				}else {
					ChatServer.log("알수없는 요청");
				}
			}
			
		} catch (SocketException e) {
//			System.out.println(Thread.currentThread().getId());
			System.out.println("[server] sudden closed by client");
			doQuit(pw);
		} catch (IOException e) { // 정상종료 안하고 확 꺼버린 ..!
			e.printStackTrace();
		} finally {
			try {
				if (socket != null && socket.isClosed()) {
					socket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void doJoin(String nickName, Writer writer) {
		this.nickname = nickName;
//		System.out.println("@@@@닉네임:" + nickName);
		// 누가 들어왔다고
		String data = "-------------------------------------------------------\n"+
					  "\t\t" +nickName + "님이 참여하셨습니다.\n"+ 
					  "\t\t현재 참여 인원 : " + (listWriters.size()+1) +"\n"+
					  "-------------------------------------------------------\n";
		check =1;
		broadcast(data, check);
		check =0;
		
		// writer pool 에 현재 스레드 printWriter를 저장
		addWriter(writer, nickName);
		
		// ack 보내기
		pw.println(listWriters.size());
		pw.flush();
		
	}
	
	private void addWriter(Writer writer, String nickName) {
		synchronized (listWriters) {
			listWriters.put(nickName, writer);
			
		}
	}
	
	private void broadcast(String data, int check) {

		synchronized (listWriters) {
			for(Object writer : listWriters.values()) {
				PrintWriter pw = (PrintWriter)writer;
				if(check == 0) {
					pw.println(nickname + " : " +data);
				}else {
					pw.println(data);
				}
				pw.flush();
			}
		}

	}
	
	private void doMessage(String message) {
		broadcast(message, check);
	}
	
	private void doQuit(Writer writer) {
		removeWriter(writer);
		
		String data = "-------------------------------------------------------\n"+
					  "\t\t"+nickname + "님이 퇴장하셨습니다.\n"+ 
					  "\t\t 현재 참여 인원 : " + listWriters.size() +"\n"+
					  "-------------------------------------------------------";
		check =1;
		broadcast(data,check);
		check =0;

	}
	
	private void doWisper(String name, String message, String me) {
		
//		System.out.println(name + ", " + message+"@@@@@@@@@@@@@@@@@");
//		출력 >> 아아, 안녕?@@@@@@@@@@@@@@@@@
		name = name.trim();
		me = me.trim();
		
		PrintWriter pw2 = null;
		for(Entry<String, Object> writer : listWriters.entrySet()) {
			if(writer.getKey().equals(name)) {
//				System.out.println("알맞은 닉네임 찾아서 들어옴");
				pw2 = (PrintWriter) writer.getValue();				
			}
		}
		if(pw2 == null) {
			pw.println("-------------------------------------------------------\n "
					+ "\t\t사용자 닉네임을 올바르게 입력해 주세요.\n"
					+ "-------------------------------------------------------");
		}else {
//			System.out.println(name + ", "+ message + ", "+ me + ", " + pw);
			pw.println("[ 귓속말 >>" + name + " : " + message +" ]");
			pw2.println("[ 귓속말 <<" + me + " : " + message +" ]");
			pw2.flush();			
		}

	}
	
	private void removeWriter(Writer writer) {
		listWriters.values().removeAll(Collections.singleton(writer));
	}

}
