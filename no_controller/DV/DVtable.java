import java.util.ArrayList;
import java.util.List;

public class DVtable {
	public List<Record> DvTable;
	
	public DVtable() {
		DvTable = new ArrayList<>();
	}
	
	@Override
    public String toString() {
    	String str = "";
    	for (Record r: DvTable) {
    		str += r.toString();
    	}
    	return str;
    }
	
	// 还原邻居的路由表，nextHop全都改为邻居的ip
    public List<Record> recoverNeighborRouteTable(String ip, String rt) {
        String[] updateMessage = rt.split("@");

        List<Record> neighbor;
        neighbor = new ArrayList<>();
        // 最后一条是path
        for (int i = 0; i < updateMessage.length - 1; i++) {
            String[] msg = updateMessage[i].split("\\:|,");
            String dest = msg[1];
            //String nextHop = msg[3];
            String cost = msg[5];
            // System.out.print("\ncost:"+cost+"\n");
            Record record = new Record(dest, ip, Integer.parseInt(cost));
            neighbor.add(record);
        }

        // 打印邻居路由表
        System.out.print("The routeTable from your neighbor:\n");
        for (Record n : neighbor) {
            System.out.print(n.toString());
            System.out.print("\n");
        }

        return neighbor;
    }
    
    public Record findByDst(String ip) {
    	int size = DvTable.size();
    	for (int i = 0; i < size; i ++) {
    		if (DvTable.get(i).getDst_().equals(ip)) {
    			return DvTable.get(i);
    		}
    	}
    	return null;
    }
    
    public void update(String ip, String rt) {
    	List<Record> neighbor = recoverNeighborRouteTable(ip, rt);
    	int first_cost = findByDst(ip).getCost();
    	int size = neighbor.size();
    	for (int i = 0; i < size; i ++) {
    		Record temp = neighbor.get(i);
    		String dst_ = temp.getDst_();
    		if (dst_.equals(Client.hostIP)) {
    			continue;
    		}
    		if (findByDst(dst_) == null) {
    			DvTable.add(new Record(dst_, ip, first_cost + temp.getCost()));
    		} else {
    			if (first_cost + temp.getCost() < findByDst(dst_).getCost()) {
    				findByDst(dst_).setCost_(first_cost + temp.getCost());
    				findByDst(dst_).setNextHop_(ip);
    			}
    		}
    	}
    	show();
    }
    
    public String findnext(String myip, String ip) {
    	for (int i = 0; i < DvTable.size(); i ++) {
    		if (DvTable.get(i).getDst_().equals(ip)) {
    			System.out.println("下一跳：" + DvTable.get(i).getNextHop_());
    			return DvTable.get(i).getNextHop_();
    		}
    	}
    	return "error";
    }
    
    public void insert(String a_,String b_, int c) {
    	boolean exist = false;
    	for (int i = 0; i < DvTable.size(); i ++) {
    		if (DvTable.get(i).getDst_().equals(b_)) {
    			exist = true;
    			if (DvTable.get(i).getCost() > c) {
    				DvTable.get(i).setCost_(c);
    				DvTable.get(i).setNextHop_(b_);;
    			}
    		}
    	}
    	if (!exist) {
    		DvTable.add(new Record(b_, b_, c));
    	}
    }
    
    public void show() {
    	for (Record r: DvTable) {
    		System.out.print(r.toString());
    	}
		
		System.out.println("DV table show success!!");
    }
    
}
