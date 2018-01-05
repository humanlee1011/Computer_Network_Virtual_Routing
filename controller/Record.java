/**
 * Created by carol_YT on 2017/12/26.
 */
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
        return "dst:" + dst_ + ", next hop:" + nextHop_ + ", cost:" + cost_ + ",\n@";
    }

    public int getCost() {
        return cost_;
    }

    public String getDst_() {
        return dst_;
    }

    public String getNextHop_() {
        return nextHop_;
    }

    public void setCost_(int cost_) {
        this.cost_ = cost_;
    }

    public void setNextHop_(String nextHop_) {
        this.nextHop_ = nextHop_;
    }

    public void setDst_(String dst_) {
        this.dst_ = dst_;
    }
}
