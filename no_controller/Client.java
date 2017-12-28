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
	
	RouteTable rt;
	String hostIP;
	//维护连接状态的端口
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
	//将ds与发送消息的端口对应，以便响应到原来的端口
	private Map<DatagramSocket, Integer> portPool = new HashMap<DatagramSocket, Integer>();
	//初始化Client
	public Client(String ip, int port) {
		try {
			rt = new RouteTable();
			hostIP = ip;
			ListeningPort = port;
			ipPool = new ArrayList<String>();
			serverThread = new ServerThread();
			serverThread.start();
			rtThread = new ConnectThread();
			rtThread.start();
			System.out.println("该主机连入网络");
			
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

	
	//和相邻的机器相连,并定期检查连接
	public class ConnectThread extends Thread {
		public ConnectThread() {}

		public void run() {
			try {
				boolean flag = true;
				while (flag) {
					for (String ip: ipPool) {
						String str = hostIP + "#" + ip + "#" + hostIP + "#check";
						if (send(ip, str, "check", ListeningPort)) {
							String msg = hostIP + "#" + ip + "#" + rt.toString() + hostIP + "#update";
							send(ip, msg, "update", ListeningPort);
						}
					}
					sleep(40000);
				}
			} catch (InterruptedException e) {
				//遇到终止符时执行shutdown操作
				shutdown();
			}
		}
	}
	
	
	//与某一ip连接
	public synchronized void connect(String ipStr, int cost) {
		try {
			String str = hostIP + "#" + ipStr + "#" + hostIP + "#connect#" + Integer.toString(cost);
			rt.insert(ipStr, cost);
			send(ipStr, str, "connect", ListeningPort);
		} catch (Exception e) {
			e.printStackTrace();
			shutdown();
		}
	}
	
	//与某一断开连接
	@SuppressWarnings("deprecation")
	public synchronized boolean disconnect(String ip) {
		//确保ip存在
		if (isExist(ip)) {
			String str = hostIP + "#" + ip + "#" + hostIP + "#" + "disconnect";
			send(ip, str, "disconnect", ListeningPort);
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
	
	//关闭本机的所有连接
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
			System.out.println("Rt:\n" + rt.toString());
			//System.out.println(ip + ": nextHop" + rt.getNextHop(ip));
			if (rt.getNextHop(ip) == null) {
				return false;
			} else {
				Random rand = new Random();
				//随意选择一个端口发送消息
				int sendPort = rand.nextInt(999) + 1024;
				DatagramSocket ds = new DatagramSocket(sendPort);
				InetAddress nexthop = InetAddress.getByName(ip);
				//发送消息到对方的对应端口
				System.out.println("IP: " + ip + "\nmsg: " + msg + "\nport: " + port);
				DatagramPacket dp_send = new DatagramPacket(msg.getBytes(), msg.length(), nexthop, port);
				ds.send(dp_send);
				if (!command.equals("ACK")) {
					portPool.put(ds, sendPort);
					Thread t = new RecvThread(ds, rt.getNextHop(ip), command);
					t.start();
					threadPool.put(rt.getNextHop(ip), t);
				} else {//如果本机是原数据包的目标主机，则直接发送且关闭ds
					ds.close();
					//isPortBind = false;
					ds = null;
				}
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			shutdown();
			return false;
		}
	}
	
	//给指定ip发送消息
	public class RecvThread extends Thread {
		private String command = null;
		private DatagramSocket ds = null;
		//作为客户端发送数据的目标地址
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
					this.ds.setSoTimeout(30000);
				boolean recvResponse = false;	
				this.ds.receive(dp_recv);
				//如果接收到的数据不是来自目标地址，则继续转发
				String srcIP = dp_recv.getAddress().getHostAddress();
				if (!isServer() && !srcIP.equals(this.src)) {
					throw new IOException("Received packet from an unknown source.");
				} else {
					recvResponse = true;
				} 
				if (recvResponse) {
					String str = new String(dp_recv.getData(), 0, dp_recv.getLength());
					//msgs[] = [srcIP, dstIP, path, data, cost]
					String[] msgs = str.split("#");
					if (msgs[1].equals(hostIP)) {
						//根据发送的命令来判断是否回复
						//该线程作为服务器的监听
						if (isServer()) {
							ServerAnalyse(str, srcIP, dp_recv.getPort());
						}
						else { //该线程作为客户端的线程
							ClientAnalyse(str, command, srcIP);
						}
					}
					else { //在path上增加本机IP地址再转发
						System.out.println("received data from " + msgs[0] + ":" + Integer.toString(dp_recv.getPort()) + "--->" + msgs[2] + " ");
						msgs[2] += "->" + hostIP;
						
						send(rt.getNextHop(msgs[1]), msgs[0] + "#" + msgs[1] + "#" + msgs[2] + "#" + msgs[3], null, ds.getPort());
					}
				} else {
					System.out.println("No response -- give up");
					rt.remove(this.src);
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
	
	
	//将本机的路由表广播到相邻主机
//	public synchronized void broadcast() {
//		for (String ip: ipPool)  {
//			String str = hostIP + "#" + ip + "#" + rt.toString() +  hostIP + "#" + "update";
//			send(ip, str, "update", ListeningPort);
//		}
//	} 
	
	//作为服务端接收到信息
	public void ServerAnalyse(String data, String srcIP, int port) {
		String[] msgs = data.split("#");
		String res = hostIP + "#" + srcIP + "#" + hostIP + "#" + "ACK";
		//System.out.println("Server" + Integer.toString(port));
		if (msgs[3].equals("connect") && msgs.length == 5) {
			//System.out.println("into connect function");
			ipPool.add(srcIP);
			rt.insert(srcIP, Integer.parseInt(msgs[4]));
			System.out.println("srcIP:" + srcIP + ":" + res);
			System.out.println(rt.toString());
			send(srcIP, res, "ACK", port);
		} else if (msgs[3].equals("disconnect")) {
			ipPool.remove(srcIP);
			rt.remove(srcIP);
			send(srcIP, res, "ACK", port);
		} else if (msgs[3].equals("check")) {
			send(srcIP, res, "ACK", port);
		} else if (msgs[3].equals("update")) {
			//更新拓扑图结构
			rt.update(msgs[0], msgs[2]);
		} else {
			send(srcIP, res, "ACK", port);
		}
		//打印传送的数据
		System.out.println("received data from " + msgs[0] + ":" + Integer.toString(port) + "--->" + msgs[3]);
		
	}
	
	//作为客户端接收到信息
	public void ClientAnalyse(String data, String command, String srcIP) {
		//msgs = [srcIP#dstIP#path#data#cost]
		String[] msgs = data.split("#");
		if (command.equals("connect") && msgs[3].equals("ACK")) {
			ipPool.add(srcIP);
			System.out.println("成功与" + srcIP + "连接");
		} else if (command.equals("disconnect") && msgs[3].equals("ACK")) {
			ipPool.remove(srcIP);
			rt.remove(srcIP);
			System.out.println("成功与" + srcIP + "断开连接");
		} else if (command.equals("check") && msgs[3].equals("ACK")) {
			//不进行任何操作
		} else if (command.equals("update") && msgs[3].equals("ACK")) {
			rt.update(msgs[0], msgs[2]);
		} else if (command.equals("send") && msgs[3].equals("ACK")) {
			System.out.println("成功给" + srcIP + "发送消息");
		}
	}
	
	public static void main(String[] args) {
		try {
			String ip = "127.0.0.1";
			Scanner scanner = new Scanner(System.in);
			
			if (args.length == 0)
				ip = InetAddress.getLocalHost().getHostAddress().toString();
			else if (args.length == 1)
				ip = args[0];
			System.out.println("输入端口号");
			int port = scanner.nextInt();
			Client client = new Client(ip, port);
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
						System.out.println(inputs[1] + "已经断开连接.");
				} else if (inputs[0].equals("send") && inputs.length == 3) {
					String str = ip + "#" + inputs[1] + "#" + ip + "#" + inputs[2];
					if (!client.send(inputs[1], str, "send", port))
						System.out.println(inputs[1] + "无法达到");
				} else {
					System.out.println("该命令无效");
				}
			}
			scanner.close();
			System.out.println("该主机离开网络");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}