import java.util.ArrayList;
import java.util.List;

public class RouteTable {
	private List<Record> routeTable_;
	
	public RouteTable() {
		routeTable_ = new ArrayList<>();
	};
	
	//insert a new Record into RouteTable
	public void insert(String ip, int cost) {
		
	}
	//remove a record from Routetable
	public void remove(String ip) {
		
	}
	
	//update routetable from ip
	public void update(RouteTable rt, String ip) {
		
	}
	
	/* return the next hop from the host to dst
		use different routing algorithms DV & LS */
	public String getNextHop(String dst) {
		return "172.18.137.227";
	}
	
	public int getCost(String ip) {
		return 1;
	}
}