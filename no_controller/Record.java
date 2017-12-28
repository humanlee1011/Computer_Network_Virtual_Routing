public class Record {
	private String dst_;
	private String nextHop_;
	private int cost_;
	
	public Record(String dst, String nextHop, int cost) {
		dst_ = dst;
		nextHop_ = nextHop;
		cost_ = cost;
	}
	
	public String toString() {
		return "dst: " + dst_ + "\nnext hop: " + nextHop_ + "\ncost: " + cost_;
	}
	
	public int getCost() {
		return cost_;
	}
}