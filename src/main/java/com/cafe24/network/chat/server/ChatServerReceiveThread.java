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
import java.util.List;

public class ChatServerReceiveThread extends Thread {

	private Socket socket;
	private String nickname;
	List<Writer> listWriters;
	PrintWriter pw = null;
	BufferedReader br = null;
	
	public ChatServerReceiveThread(Socket socket, List<Writer> listWriters) {
		this.socket = socket;
		this.listWriters = listWriters;
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
					System.out.println("갑자기 끔");
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
		String data = nickName + "님이 참여하셨습니다.";
		broadcast(data);
		
		// writer pool 에 현재 스레드 printWriter를 저장
		addWriter(writer);
		
		// ack 보내기
		pw.println("대화에 참가하셨습니다.");
		pw.flush();
		
	}
	
	private void addWriter(Writer writer) {
		synchronized (listWriters) {
			listWriters.add(writer);
		}
//		for(Writer writer2 : listWriters) {
//			System.out.println(writer2);
//		}
	}
	
	private void broadcast(String data) {
		synchronized (listWriters) {
			for(Writer writer : listWriters) {
				PrintWriter pw = (PrintWriter)writer;
				pw.println(nickname + " : " +data);
				pw.flush();
			}
		}
	}
	
	private void doMessage(String message) {
		broadcast(message);
	}
	
	private void doQuit(Writer writer) {
		removeWriter(writer);
		
		String data = nickname + "님이 퇴장하셨습니다.";
		synchronized (listWriters) {
			for(Writer writer2 : listWriters) {
				PrintWriter pw = (PrintWriter)writer2;
				pw.println("---------" +data+"---------");
				pw.flush();
			}
		}
	}
	
	private void removeWriter(Writer writer) {
		listWriters.remove(writer);
	}

}
