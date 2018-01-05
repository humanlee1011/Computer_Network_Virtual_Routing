
public class edge {
    public String v1;
    public String v2;
    public int cost;

    public edge(String aa, String bb, int c) {
        v1 = aa;
        v2 = bb;
        cost = c;
    }
    public String toString() {
        return "v1:" + v1 + ", v2:" + v2 + ", cost:" + cost + ",\n@";
    }
}
