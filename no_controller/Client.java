import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.IOException;  
import java.io.InterruptedIOException;  
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
	//维护连接状态的端口固定为9000
	private int ListeningPort = 9000;
	private boolean isPortBind = false;
	private List<String> ipPool; 
	private Thread serverThread = null;
	private Thread rtThread = null;
	private Map<String, Thread> threadPool = new HashMap<String, Thread>();
	private Map<DatagramSocket, Integer> portPool = new HashMap<DatagramSocket, Integer>();
	//初始化Client
	public Client(String ip) {
		try {
			rt = new RouteTable();
			hostIP = ip;
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
		public void run() {
			try {
				while (true) {
					if (!isPortBind) {
						isPortBind = true;
						ds = new DatagramSocket(ListeningPort);
						serverThread = new RecvThread(ds);
						serverThread.start();
					}
				}
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
					if (ipPool.size() > 0) {
						for (String ip: ipPool) {
							String str = hostIP + "#" + ip + "#" + hostIP + "#check";
							send(ip, str, "check", 9000);
						}
					}
					sleep(100000);
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
			send(ipStr, str, "connect", 9000);
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
			send(ip, str, "disconnect", 9000);
			for (Map.Entry<String, Thread> entry: threadPool.entrySet()) {
				if (entry.getKey().equals(ip)) {
					Thread t = entry.getValue();
					if (t != null) {
						threadPool.replace(ip, t);
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
			if (rt.getNextHop(ip).equals("")) {
				return false;
			} else {
				Random rand = new Random();
				int sendPort = rand.nextInt(9999);
				DatagramSocket ds = new DatagramSocket(sendPort);
				InetAddress nexthop = InetAddress.getByName(ip);
				DatagramPacket dp_send = new DatagramPacket(msg.getBytes(), msg.length(), nexthop, port);
				portPool.put(ds, sendPort);
				ds.send(dp_send);
				Thread t = new RecvThread(ds, rt.getNextHop(ip), command);
				t.start();
				threadPool.put(ip, t);
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
		private String src = null;
		public RecvThread(DatagramSocket ds, String src, String command) {
			this.ds = ds;
			this.src = src;
			this.command = command;
		}
		
		public RecvThread(DatagramSocket ds) {
			this.ds = ds;
		}
		
		public void run() {
			try {
				byte[] buf = new byte[1024];
				DatagramPacket dp_recv = new DatagramPacket(buf, 1024);
				if (command != null)
					this.ds.setSoTimeout(50000);
				boolean recvResponse = false;	
				this.ds.receive(dp_recv);
				//如果接收到的数据不是来自目标地址，则继续转发
				String srcIP = dp_recv.getAddress().getHostAddress();
				if (src != null && !srcIP.equals(this.src)) {
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
						//如果command 为 空，则该线程作为服务器的监听
						if (command == null) {
							ServerAnalyse(str, srcIP, dp_recv.getPort());
						}
						else { //如果command不为空，则该线程作为客户端的线程
							ClientAnalyse(str, command, srcIP, dp_recv.getPort());
						}
					}
					else { //在path上增加本机IP地址再转发
						System.out.println("received data from " + msgs[0] + ":" + Integer.toString(dp_recv.getPort()) + "--->" + msgs[2] + " ");
						msgs[2] += "->" + hostIP;
						send(msgs[1], msgs[0] + "#" + msgs[1] + "#" + msgs[2] + "#" + msgs[3], null, ds.getPort());
					}
				} else {
					System.out.println("No response -- give up");
					//rt.remove(this.src);
				}	
				portPool.remove(ds);
				ds.close();
				if (command == null) 
					isPortBind = false;
			} catch (Exception e) {
				e.printStackTrace();
				ds.close();
			}
		}
		public boolean isServer() {
			return command == null;
		}
	}
	
	
	//TODO: 将本机的路由表广播到相邻主机
	public synchronized void broadcast() {
		for (String ip: ipPool)  {
			String str = hostIP + "#" + ip + "#" + hostIP + "#" + "update";
			//send(ip, str, "update", Random().next);
		}
	} 
	
	//作为服务端接收到信息
	public void ServerAnalyse(String data, String srcIP, int port) {
		String[] msgs = data.split("#");
		String res = hostIP + "#" + srcIP + "#" + hostIP + "#" + "ACK";
		if (msgs[3].equals("connect")) {
			ipPool.add(srcIP);
			//rt.insert(srcIP, Integer.parseInt(msgs[4]));
			send(srcIP, res + msgs[4], null, port);
		} else if (msgs[3].equals("disconnect")) {
			ipPool.remove(srcIP);
			//rt.remove(srcIP);
			send(srcIP, res, null, port);
		} else if (msgs[3].equals("check")) {
			send(srcIP, res, null, port);
		} else if (msgs[3].equals("update")) {
			//更新拓扑图结构
			////rt.update()
		}
		//打印传送的数据
		System.out.println("received data from " + msgs[0] + ":" + Integer.toString(port) + "--->" + msgs[3]);
		
	}
	
	//作为客户端接收到信息
	public void ClientAnalyse(String data, String command, String srcIP, int port) {
		//msgs = [srcIP#dstIP#path#data#cost]
		String[] msgs = data.split("#");
		if (command.equals("connect") && msgs[3].equals("ACK")) {
			ipPool.add(srcIP);
			//rt.insert(srcIP, Integer.parseInt(msgs[4]));
		} else if (command.equals("disconnect") && msgs[3].equals("ACK")) {
			ipPool.remove(srcIP);
			//rt.remove(srcIP);
		} else if (command.equals("check") && msgs[3].equals("ACK")) {
			////rt.update();
		} else if (command.equals("update") && msgs[3].equals("ACK")) {
			////rt.update();
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
			//System.out.println("输入端口号");
			Client client = new Client(ip);
			scanner.nextLine();
			boolean flag = true;
			while (flag) {
				System.out.println(client.hostIP + ":" + client.ListeningPort + ">");
				String op = scanner.nextLine();
				String[] inputs = op.split("#");
				System.out.println(inputs[0]);
				if (inputs[0].equals("connect") && inputs.length == 3) {
					client.connect(inputs[1], Integer.parseInt(inputs[2]));
					System.out.println("成功与" + inputs[1] + "连接");
				} else if (inputs[0].equals("disconnect") && inputs.length == 2) {
					if (client.disconnect(inputs[1]))
						System.out.println("成功与" + inputs[1] + "断开连接");
					else 
						System.out.println(inputs[1] + "已经断开连接.");
				} else if (inputs[0].equals("send") && inputs.length == 3) {
					String str = ip + "#" + inputs[1] + "#" + ip + "#" + inputs[2];
					if (client.send(inputs[1], str, "send", 9000))
						System.out.println("成功给" + inputs[1] + "发送消息：" + inputs[2]);
					else
						System.out.println(inputs[1] + "无法达到");
				} else {
					System.out.println("该命令无效");
				}
			}
			System.out.println("该主机离开网络");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}