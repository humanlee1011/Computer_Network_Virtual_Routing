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


public class Controller {
	
	LStable rt;
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
	public Controller(String ip, int port) {
		try {
			rt = new LStable();
			hostIP = ip;
			ListeningPort = port;
			ipPool = new ArrayList<String>();
			serverThread = new ServerThread();
			serverThread.start();
			//rtThread = new ConnectThread();
			//rtThread.start();
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
			//System.out.println("Rt:\n" + rt.toString());
			//System.out.println(ip + ": nextHop" + rt.getNextHop(ip));
			//if (rt.findnext(hostIP, ip) == "error") {
				//return false;
			//} else {
				Random rand = new Random();
				//随意选择一个端口发送消息
				int sendPort = rand.nextInt(999) + 1024;
				DatagramSocket ds = new DatagramSocket(sendPort);
				InetAddress nexthop = InetAddress.getByName(ip);
				//发送消息到对方的对应端口
				System.out.println("IP: " + ip + "\nmsg: " + msg + "\nport: " + port);
				DatagramPacket dp_send = new DatagramPacket(msg.getBytes(), msg.length(), nexthop, port);
				ds.send(dp_send);
				ds.close();
				ds = null;
				return true;
			//}
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
				boolean recvResponse = false;	
				this.ds.receive(dp_recv);
				//如果接收到的数据不是来自目标地址，则继续转发
				String srcIP = dp_recv.getAddress().getHostAddress();
				recvResponse = true;
				if (recvResponse) {
					String str = new String(dp_recv.getData(), 0, dp_recv.getLength());
					//msgs[] = [srcIP, dstIP, path, data, cost]
					String[] msgs = str.split("#");
					if (msgs[1].equals(hostIP)) {
						//根据发送的命令来判断是否回复
						//该线程作为服务器的监听
						ServerAnalyse(str, srcIP, dp_recv.getPort());		
					}
					else {
						System.out.println("dstIP 不正确");
					}
				} else {
					System.out.println("No response -- give up");
					rt.remove(hostIP,this.src);
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
	
	//作为服务端接收到信息
	public void ServerAnalyse(String data, String srcIP, int port) {
		//msgs[] = srcIP#dstIP#path#connect/query#ip
		String[] msgs = data.split("#");
		String res = hostIP + "#" + srcIP + "#" + hostIP + "#" + "ACK";
		//System.out.println("Server" + Integer.toString(port));
		if (msgs[3].equals("connect") && msgs.length == 5) {
			//System.out.println("into connect function");
			ipPool.add(srcIP);
			rt.insert(hostIP,srcIP, Integer.parseInt(msgs[4]));
			System.out.println("srcIP:" + srcIP + ":" + res);
			System.out.println(rt.toString());
			send(srcIP, res, "ACK", port);
		} else if (msgs[3].equals("disconnect")) {
			ipPool.remove(srcIP);
			rt.remove(hostIP,srcIP);
			send(srcIP, res, "ACK", port);
		} else if (msgs[3].equals("query")) {
			//msgs[4] store the destination IP
			System.out.println("收到数据包：" + data);
			String nextHop = rt.findnext(srcIP, msgs[4]);
			String str = hostIP + "#" + srcIP + "#" + hostIP + "#reply#" + msgs[4] + "," + nextHop;
			send(srcIP, str, "ACK", port);
		} else if (msgs[3].equals("down")) {
			rt.remove(msgs[0], msgs[4]);
			send(srcIP, res, "ACK", port);
		} else if (msgs[3].equals("update")) {
			rt.update(msgs[0], msgs[2]);
			send(srcIP, res, "ACK", port);
		}
		//打印传送的数据
		System.out.println("received data from " + msgs[0] + ":" + Integer.toString(port) + "--->" + msgs[3]);
		
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
			Controller con = new Controller(ip, port);
			scanner.nextLine();
			System.out.println(con.hostIP + ":" + con.ListeningPort + "作为controller已启用");
			boolean flag = true;
			while (flag) {
				
			}
			scanner.close();
			System.out.println("该主机离开网络");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}