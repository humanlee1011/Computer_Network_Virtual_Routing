import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.text.DefaultEditorKit.InsertBreakAction;

import jdk.internal.org.objectweb.asm.util.CheckAnnotationAdapter;

import java.nio.channels.ShutdownChannelGroupException;
import java.util.ArrayList;
import java.util.HashMap;

public class LStable {
	public List<edge> LSTable_;
	public int [][]graph;
	public Map index;
	public int num_ip;
	
    public LStable() {
        LSTable_ = new ArrayList<>();
        num_ip = 0;
        graph = new int[100][100];
        index = new HashMap<String,Integer>();
        //edge g1 = new edge("0", "1", 1);
        //edge g2 = new edge("0", "1", 1);
        //edge g3 = new edge("0", "1", 1);
        //edge g4 = new edge("0", "1", 1);
        //edge g5 = new edge("0", "1", 1);
        //edge g6 = new edge("0", "1", 1);
       // edge g7 = new edge("0", "1", 1);
        //LSTable_.add(g1);
        //LSTable_.add(g2);
        //LSTable_.add(g3);
        //LSTable_.add(g4);
        //LSTable_.add(g5);
        //LSTable_.add(g6);
        //LSTable_.add(g7);
        //show();
    };
    public void changetograph() {
    	index.clear();
    	num_ip = 0;
		for(int i = 0; i < 50; i++){
			for(int j = 0; j < 50; j++){
				graph[i][j] = 1000;
			}
		}
		for(edge r: LSTable_){
			String a = r.v1;
			String b = r.v2;
			int c = r.cost;
			boolean it = index.containsKey(a);
			boolean it2 = index.containsKey(b);
			if(!it){
				index.put(a, num_ip);
				num_ip++;
			}
			if(!it2){
				index.put(b, num_ip);
				num_ip++;
			}
			graph[(int) index.get(a)][(int) index.get(b)] = c;
			graph[(int) index.get(b)][(int) index.get(a)] = c;
		}
		for(int i = 0; i < num_ip; i++){
			for(int j = 0; j < num_ip; j++){
				graph[i][i] = 0;
			}
		}
    }
    public String findnext(String myip, String ip) {
    	changetograph();
		int s = (int) index.get(myip);
		int end = (int) index.get(ip);
		int n = index.size();
		boolean []vis = new boolean[1000];
		int []d = new int[1000];
		int []pre = new int[1000];
		 
		for(int i = 0; i < n;i++){
			pre[i] = i;
			d[i] = 1000;
			vis[i] = false;
		} 
   		d[s] = 0;                                              //���s��������ľ���Ϊ0
   		for (int i = 0; i < n; i++)
   		{
          	int u = -1;                                     //�ҵ�d[u]��С��u
          	int MIN = 1000;                                  //��¼��С��d[u]
          	for (int j = 0; j < n; j++)                     //��ʼѰ����С��d[u]
          	{
                 if (vis[j] == false && d[j] < MIN)
                 {
                       u = j;
                       MIN = d[j];
                 }
          	}
          //�Ҳ���С��INF��d[u]��˵��ʣ�µĶ�������s����ͨ
          	vis[u] = true; 
			if(u == -1){
				break;
			}                               //���u�ѱ�����
          	for (int v = 0; v < n; v++)
          	{
                 //�������ж��㣬���vδ������&&u�ܹ�����v&&��uΪ�н�����ʹd[v]����
                 if (vis[v] == false && d[u] + graph[u][v] < d[v]) {
                       d[v] = d[u] + graph[u][v];             //����d[v]
                       pre[v] = u;                        //��¼v��ǰ������Ϊu������ӣ�
                 }
          	}
   		}

   		List<Integer>res = new ArrayList<>();
   		while(true){
   			if(end == s){
   				res.add(s);
   				break;
			}
			res.add(end);
			end = pre[end];
   		}

		int ip_num = res.get(res.size()-2);
		Object[] x = index.entrySet().toArray();
		for(int i = 0; i < index.size(); i++){
			Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) x[i];
			if(entry.getValue().equals(ip_num)){
				System.out.println("��һ����ַ:");
				System.out.println(entry.getKey());
				return entry.getKey();	
			}
		}
		return "error";
    }
    public void insert(String a_,String b_, int c) {
    	edge g1 = new edge(a_, b_, c);
        int length = LSTable_.size();
        if(length == 0) {
        	LSTable_.add(g1);
        	return;
        }
        boolean flag = false;
        for (int j = 0; j < length; j++) {
            String aa, bb;
            aa = LSTable_.get(j).v1;
            bb = LSTable_.get(j).v2;
            if (((aa.equals(a_)) && (bb.equals(b_))) || 
            		((aa.equals(b_)) && (bb.equals(a_)))	) {
            	flag = true;
            }
        }
        if(!flag) {
        	LSTable_.add(g1);
        }
        

    }
    public void update(String ip, String rt) {
    	System.out.println("������·�ɱ�");
    	//System.out.print(rt);
        String[] updateMessage = rt.split("@");
        // ���һ����path
        for (int i = 0; i < updateMessage.length - 1; i++) {
            String[] msg = updateMessage[i].split("\\:|,");
            String dest = msg[1];
            String nextHop = msg[3];
            String cost = msg[5];
            edge record = new edge(dest, nextHop, Integer.parseInt(cost));
            int length = LSTable_.size();
            boolean flag = false;
            for (int j = 0; j < length; j++) {
                String aa, bb;
                aa = LSTable_.get(j).v1;
                bb = LSTable_.get(j).v2;
                if (((aa.equals(dest)) && (bb.equals(nextHop))) || 
                		((aa.equals(nextHop)) && (bb.equals(dest)))	) {
                	flag = true;
                }
            }
            if(!flag) {
            	LSTable_.add(record);
            }
            
        }
        show();
    }
    
    public void show() {
    	for (edge r: LSTable_) {
    		System.out.print(r.toString());
    	}
		
		System.out.println("LS table show success!!");
    }
    public void remove(String ip1,String ip2) {
        int length = LSTable_.size();
        if(length == 0) {
        	System.out.println("���_�B�Ӝyԇʧ������-----------------------------");
        	return;
        }
        for (int i = 0; i < length; i++) {
            String dest, nextHop;
            dest = LSTable_.get(i).v1;
            nextHop = LSTable_.get(i).v2;
            if (((dest.equals(ip1)) && (nextHop.equals(ip2))) || 
            		((dest.equals(ip2)) && (nextHop.equals(ip1)))	) {
            	LSTable_.remove(i);
            	System.out.println("���_�B�Ӝyԇ�ɹ�������-------------------------");
            }
        }
    }
    public void check() {
    	int length = LSTable_.size();
    	if(length == 0) {
    		return;
    	}
        for (int i = 0; i < length; i++) {
        	for(int j = i+1; j < length; j++) {
        		edge t1 = LSTable_.get(i);
        		edge t2 = LSTable_.get(j);
        		if(t1.v1.equals(t2.v1) && t1.v2.equals(t2.v2)) {
        			
        		}
        	}
        }
    }
    
    @Override
    public String toString() {
    	String str = "";
    	for (edge r: LSTable_) {
    		str += r.toString();
    	}
    	return str;
    }
}
