import java.io.IOException;  
import java.net.DatagramPacket;  
import java.net.DatagramSocket;  
import java.net.InetAddress;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.lang.Thread;


public class Client {
	
	LStable rt;
	String controller;
	String hostIP;
	//ά������״̬�Ķ˿�
	//private boolean isPortBind = false;
	private int ListeningPort;
	//private boolean exit = false;
	//directly connected ip address
	private List<String> ipPool; 
	// host server thread
	private Thread serverThread = null;
	// periodically updates rt
	private Thread rtThread = null;
	private Map<String, Thread> threadPool = new HashMap<String, Thread>();
	//��ds�뷢����Ϣ�Ķ˿ڶ�Ӧ���Ա���Ӧ��ԭ���Ķ˿�
	private Map<DatagramSocket, Integer> portPool = new HashMap<DatagramSocket, Integer>();
	private Map<String, String> nextHops = new HashMap<String, String>();
	//��ʼ��Client
	public Client(String ip, int port, String con) {
		try {
			rt = new LStable();
			hostIP = ip;
			ListeningPort = port;
			controller = con;
			ipPool = new ArrayList<String>();
			serverThread = new ServerThread();
			serverThread.start();
			
			//connect(controller, 10000);
			rtThread = new ConnectThread();
			rtThread.start();
			System.out.println("��������������");
			
		} catch (Exception e) {
			e.printStackTrace();
			shutdown();
		}
		
	}
	
	public boolean isExist(String ip) {
		for (String s: ipPool) {
			if (s.equals(ip))
				return true;
		}
		return false;
	}
	
	
	public class ServerThread extends Thread {
		private DatagramSocket ds = null;
		
		@Override
		public void run() {
			try {
				//System.out.println("Open port 8080");
				ds = new DatagramSocket(ListeningPort);
				serverThread = new RecvThread(ds);
				serverThread.start();
					
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	//�����ڵĻ�������,�����ڼ������
	public class ConnectThread extends Thread {
		public ConnectThread() {}

		public void run() {
			try {
				boolean flag = true;
				while (flag) {
					String str = hostIP + "#" + controller + "#" + rt.toString() + hostIP + "#update#";
					//String msg = hostIP + "#" + ip + "#" + rt.toString() + hostIP + "#update";
					//����ֱ����ip
					send(controller, str, "update", ListeningPort);
					sleep(30000);
				}
			} catch (InterruptedException e) {
				//������ֹ��ʱִ��shutdown����
				shutdown();
			}
		}
	}
	
	
	//��ĳһip����
	public synchronized void connect(String ipStr, int cost) {
		try {
			String str = hostIP + "#" + ipStr + "#" + hostIP + "#connect#" + Integer.toString(cost);
			rt.insert(hostIP,ipStr, cost);
			//System.out.println("Into connection function");
			send(ipStr, str, "connect", ListeningPort);
		} catch (Exception e) {
			e.printStackTrace();
			shutdown();
		}
	}
	
	//��ĳһ�Ͽ�����
	@SuppressWarnings("deprecation")
	public synchronized boolean disconnect(String ip) {
		//ȷ��ip����
		if (isExist(ip)) {
			String str = hostIP + "#" + ip + "#" + hostIP + "#" + "disconnect";
			send(ip, str, "disconnect", ListeningPort);
			if (!ip.equals(controller) ) {
				String pck = hostIP + "#" + controller + "#" + hostIP + "#" + "down" + "#" + ip;
				send(controller, pck, "down", ListeningPort);
			}
			for (Map.Entry<String, Thread> entry: threadPool.entrySet()) {
				if (entry.getKey().equals(ip)) {
					Thread t = entry.getValue();
					if (t != null) {
						threadPool.remove(ip, t);
						t.stop();
					}
				}
			}
			
			ipPool.remove(ip);
			return true;
		} else {
			return false;
		}
	}
	
	//�رձ�������������
	@SuppressWarnings("deprecation")
	public void shutdown() {
		try {
			for (String ip: ipPool) {
				disconnect(ip);
			}
			if (serverThread != null)
				serverThread.stop();
			if (rtThread != null)
				rtThread.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	public boolean send(String ip, String msg, String command, int port) {
		try {		
			//System.out.println("into connection");
			Random rand = new Random();
			//����ѡ��һ���˿ڷ�����Ϣ
			int sendPort = rand.nextInt(999) + 1024;
			DatagramSocket ds = new DatagramSocket(sendPort);
			String next = "";
			if (ip.equals(controller)) {
				next = controller;
			} else if (command.equals("connect") || command.equals("ACK")) {
				next = ip;
				//System.out.println("����connect����");
			} else {
				next = QueryController(ip);
			}
			InetAddress nexthop = InetAddress.getByName(next);
			//������Ϣ���Է��Ķ�Ӧ�˿�
			System.out.println("IP: " + ip + "\nmsg: " + msg + "\nport: " + port);
			DatagramPacket dp_send = new DatagramPacket(msg.getBytes(), msg.length(), nexthop, port);
			ds.send(dp_send);
			//
			if (!command.equals("ACK")) {
				portPool.put(ds, sendPort);
				Thread t = new RecvThread(ds, ip, command);
				t.start();
				threadPool.put(ip, t);
				t.join();
				System.out.println("�����thread");
			} else {//���������ԭ���ݰ���Ŀ����������ֱ�ӷ����ҹر�ds
				ds.close();
				//isPortBind = false;
				ds = null;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			shutdown();
			return false;
		}
	}
	
	//��ָ��ip������Ϣ
	public class RecvThread extends Thread {
		private String command = null;
		private DatagramSocket ds = null;
		//��Ϊ�ͻ��˷������ݵ�Ŀ���ַ
		private String src = null;
		public RecvThread(DatagramSocket ds, String src, String command) {
			this.ds = ds;
			this.src = src;
			this.command = command;
		}
		
		public RecvThread(DatagramSocket ds) {
			this.ds = ds;
		}
		

		public boolean isServer() {
			return command == null || command.equals("");
		}
		
		@Override
		public void run() {
			try {
				byte[] buf = new byte[1024];
				DatagramPacket dp_recv = new DatagramPacket(buf, 1024);
				if (!isServer())
					this.ds.setSoTimeout(25000);
				boolean recvResponse = false;	
				this.ds.receive(dp_recv);
				//������յ������ݲ�������Ŀ���ַ�������ת��
				String srcIP = dp_recv.getAddress().getHostAddress();
				recvResponse = true;
				if (recvResponse) {
					String str = new String(dp_recv.getData(), 0, dp_recv.getLength());
					//msgs[] = [srcIP, dstIP, path, data, cost]
					String[] msgs = str.split("#");
					if (msgs[1].equals(hostIP)) {
						//���ݷ��͵��������ж��Ƿ�ظ�
						//���߳���Ϊ�������ļ���
						if (isServer()) {
							ServerAnalyse(str, srcIP, dp_recv.getPort());
						}
						else { //���߳���Ϊ�ͻ��˵��߳�
							ClientAnalyse(str, command, srcIP);
						}
					}
					else { //��path�����ӱ���IP��ַ��ת��
						System.out.println("�յ�����  from " + msgs[0] + ":" + Integer.toString(dp_recv.getPort()) + "--->" + msgs[2] + " ");
						msgs[2] = msgs[2].concat("-->" + hostIP);
						//����query��ȥcontrollerȥѯ����һ����
						String nextHop = QueryController(msgs[1]);
						if (nextHop != null && !nextHop.equals("")) {
							send(nextHop, msgs[0] + "#" + msgs[1] + "#" + msgs[2] + "#" + msgs[3], "forward", ds.getLocalPort());
							send(srcIP, hostIP + "#" + srcIP + "#" + hostIP + "#ACK", "ACK", dp_recv.getPort());
						}
					}
				} else {
					System.out.println("No response -- give up");
				}	
				portPool.remove(ds);
				if (isServer()) {
					//System.out.println("isPortBind changed");
					if (serverThread != null) {
						serverThread = null;
					}
					serverThread = new ServerThread();
					serverThread.start();
				}
				ds.close();
				ds = null;
			} catch (Exception e) {
				e.printStackTrace();
				ds.close();
			}
		}
	}
	
	public synchronized String QueryController(String ip) {
		int ControllerPort = 8080;
		String msg = hostIP + "#" + controller + "#" + hostIP + "#query#" + ip;
		send(controller, msg, "query", ControllerPort);
		String nextHop = "";
		for (Map.Entry<String, String> next: nextHops.entrySet()) {
			if (next.getKey().equals(ip)) {
				nextHop = next.getValue();
				break;
			}
		}
		return nextHop;
	}
	
	//��Ϊ����˽��յ���Ϣ
	public void ServerAnalyse(String data, String srcIP, int port) {
		String[] msgs = data.split("#");
		String res = hostIP + "#" + srcIP + "#" + hostIP + "#" + "ACK";
		//System.out.println("Server" + Integer.toString(port));
		if (msgs[3].equals("connect") && msgs.length == 5) {
			ipPool.add(srcIP);
			//send(srcIP, res, "ACK", port);
		} else if (msgs[3].equals("disconnect")) {
			ipPool.remove(srcIP);
			//send(srcIP, res, "ACK", port);
		}
//		} else if (msgs[3].equals("check")) {
//			//send(srcIP, res, "ACK", port);
//		} else if (msgs[3].equals("update")) {
//			//��������ͼ�ṹ
//			//send(srcIP, res, "ACK", port);
//		} else {
//			//send(srcIP, res, "ACK", port);
//		}
		send(srcIP, res, "ACK", port);
		//��ӡ���͵�����
		System.out.println("received data from " + msgs[0] + ":" + Integer.toString(port) + "--->" + msgs[3]);
		
	}
	
	//��Ϊ�ͻ��˽��յ���Ϣ
	public void ClientAnalyse(String data, String command, String srcIP) {
		//msgs = [srcIP#dstIP#path#data#cost]
		//msgs = [srcIP#dstIP#path#reply#dstIP,nexthop]
		String[] msgs = data.split("#");
		if (command.equals("connect") && msgs[3].equals("ACK")) {
			//rt.insert(msgs[0], msgs[1], Integer.parseInt(msgs[3]));
			ipPool.add(srcIP);
			System.out.println("�ɹ���" + srcIP + "����");
		} else if (command.equals("disconnect") && msgs[3].equals("ACK")) {
			rt.remove(hostIP, msgs[0]);
			ipPool.remove(srcIP);
			System.out.println("�ɹ���" + srcIP + "�Ͽ�����");
		} else if (command.equals("check") && msgs[3].equals("ACK")) {
			//�������κβ���
		} else if (command.equals("update") && msgs[3].equals("ACK")) {
			//rt.update(msgs[0], msgs[2]);
		} else if (command.equals("send") && msgs[3].equals("ACK")) {
			System.out.println("�ɹ���" + srcIP + "������Ϣ");
		} else if (command.equals("query") && msgs[3].equals("reply")) {
			String[] ip = msgs[4].split(",");
			nextHops.remove(ip[0]);
			nextHops.put(ip[0], ip[1]);
			System.out.println("�ɹ��յ���"+ ip[0] + "����һ��:" + ip[1]);
		} else if (command.equals("down") && msgs[3].equals("ACK")) {
			System.out.println("�ɹ���controller��������ͼ");
		}
		System.out.println("received " +  command + ": ACK");
	}
	
	public static void main(String[] args) {
		try {
			String ip = "127.0.0.1";
			Scanner scanner = new Scanner(System.in);
			
			if (args.length == 0)
				ip = InetAddress.getLocalHost().getHostAddress().toString();
			else if (args.length == 1)
				ip = args[0];
			System.out.println("����˿ں�");
			int port = scanner.nextInt();
			System.out.println("���������IP:");
			scanner.nextLine();
			String con = scanner.nextLine();
			Client client = new Client(ip, port, con);
			scanner.nextLine();
			boolean flag = true;
			while (flag) {
				System.out.println(client.hostIP + ":" + client.ListeningPort + ">");
				String op = scanner.nextLine();
				String[] inputs = op.split("#");
				System.out.println(inputs[0]);
				if (inputs[0].equals("connect") && inputs.length == 3) {
					client.connect(inputs[1], Integer.parseInt(inputs[2]));
				} else if (inputs[0].equals("disconnect") && inputs.length == 2) {
					if (!client.disconnect(inputs[1]))
						System.out.println(inputs[1] + "�Ѿ��Ͽ�����.");
				} else if (inputs[0].equals("send") && inputs.length == 3) {
					String str = ip + "#" + inputs[1] + "#" + ip + "#" + inputs[2];
					if (!client.send(inputs[1], str, "send", port))
						System.out.println(inputs[1] + "�޷��ﵽ");
				} else {
					System.out.println("��������Ч");
				}
			}
			scanner.close();
			System.out.println("�������뿪����");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}