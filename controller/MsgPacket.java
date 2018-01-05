import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.ArrayList;


public class MsgPacket {
	//0为路由表，1为传送消息
	int type = -1;
	String srcIP = null;
	String dstIP = null;
	String rt = null;
	//存储路径
	List<String> path = new ArrayList<>();
	
	DatagramPacket dp = null;
	public MsgPacket(DatagramPacket dp_recv) {
		dp = dp_recv;
		String str = new String(dp_recv.getData(), 0, dp_recv.getLength());
		String[] msgs = str.split("#");
		if (msgs[0].charAt(0) == '0') {
			rt = msgs[1];
		} else if (msgs[0].charAt(0) == '1') {
			srcIP = msgs[0];
			dstIP = msgs[1];
			String[] ips = msgs[2].split("->");
			for (String ip: ips) {
				path.add(ip);
			}
		}
	}
	
	public MsgPacket(String srcIP, String dstIP, String msg) {
		
	}
	
	//public MsgPacket(String )
	
//	public void addPath(String ip) {
//		path.add(ip);
//		return this;
//	}
	
//	public String encode() {
//		
//	}
	
}