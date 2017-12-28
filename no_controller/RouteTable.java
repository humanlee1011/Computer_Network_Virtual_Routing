import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by carol_YT on 2017/12/26.
 */
public class RouteTable {
    private List<Record> routeTable_;

    public RouteTable() {
        routeTable_ = new ArrayList<>();
    };

    //insert a new Record into RouteTable
    // directly connected link
    // ����ֱ����·
    @SuppressWarnings("unlikely-arg-type")
	public void insert(String ip, int cost) {
        Record record = new Record(ip, ip, cost);
        for (int i = 0; i < routeTable_.size(); i++) {
            if ((routeTable_.get(i).getDst_().equals(ip)) && (routeTable_.get(i).getNextHop_().equals(ip))) {
                System.out.print("You have already had such an item:\n" + routeTable_.get(i).toString());
                System.out.print("Are you sure you want to change it? yes/no");
                Scanner scanner = new Scanner(System.in);
                if (scanner.equals("yes")) {
                    routeTable_.set(i, record);
                    scanner.close();
                    return;
                }
                scanner.close();
            }
        }
        routeTable_.add(record);
        return;
    }
    //remove a record from Routetable
    //  disconnect directed link
    // ɾ��ֱ����·
    public void remove(String ip) {
        int length = routeTable_.size();
        for (int i = 0; i < length; i++) {
            String dest, nextHop;
            dest = routeTable_.get(i).getDst_();
            nextHop = routeTable_.get(i).getNextHop_();
            if ((dest.equals(ip)) && (nextHop.equals(ip))) {
                routeTable_.remove(i);
            }
        }
    }

    //update routetable and broadcast other hosts to
    // ����IP��ַΪip������������·�ɱ���Ϣrt����·�ɱ�
    public synchronized void update(String ip, String rt) {
        String[] updateMessage = rt.split("@");
        // ��ԭ�ھӷ�����·�ɱ�
        List<Record> neighbor;
        neighbor = new ArrayList<>();
        // ���һ����path
        for (int i = 0; i < updateMessage.length - 1; i++) {
            String[] msg = updateMessage[i].split("\\:|,");
            String dest = msg[1];
            //String nextHop = msg[3];
            String cost = msg[5];
            Record record = new Record(dest, ip, Integer.parseInt(cost));
            neighbor.add(record);
            System.out.println("record" + i + ":" + record.toString());
        }
        // �����ھӵı�����Լ���·�ɱ�
        int direct_cost = computeCost(ip);
        for (Record n : neighbor) {
            // �����ɴ�����
            if(computeCost(n.getDst_()) == -1) {
                routeTable_.add(new Record(n.getDst_(), ip, direct_cost + n.getCost()));
            }
            for (Record r : routeTable_) {
                // ֮ǰ�ɴ�������·������
                if (r.getDst_().equals(n.getDst_())) {
                    if (((direct_cost + n.getCost()) < r.getCost()) || r.getNextHop_().equals(ip)) {
                        r.setCost_(direct_cost + n.getCost());
                        r.setNextHop_(ip);
                    }
                }
            }
        }
    }
    // ֱ�Ӵӵ�ǰ·�ɱ��õ�IP��ַΪip�������Ĵ��ۣ������ɴﷵ��-1
    public int computeCost(String ip) {
        for (Record r : routeTable_) {
            if (r.getDst_().equals(ip)) {
                return r.getCost();
            }
        }
        return -1;
    }

    /* return the next hop from the host to dst
        use different routing algorithms DV & LS */
    // �õ�����dst����һ��·������IP��ַ
    public String getNextHop(String dst) {
        String result = null;
        for (int i = 0; i < routeTable_.size(); i++) {
            if (routeTable_.get(i).getDst_().equals(dst)) {
                result = routeTable_.get(i).getNextHop_();
            }
        }
        return result;
    }
    // �õ�·�ɱ�
    public List<Record> getRouteTable_() {
        return routeTable_;
    }
    
    @Override
    public String toString() {
    	String str = "";
    	for (Record r: routeTable_) {
    		str += r.toString();
    	}
    	return str;
    }
}

