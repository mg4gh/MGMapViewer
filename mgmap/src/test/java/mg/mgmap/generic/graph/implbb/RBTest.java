package mg.mgmap.generic.graph.implbb;

import org.junit.Test;

import java.util.Random;
import java.util.TreeSet;

public class RBTest {

    @Test
    public void test1() {
        BRedBlackTest t = new BRedBlackTest();
        t.addEntry((short)55);
        t.addEntry((short)54);
        t.addEntry((short)53);
        t.addEntry((short)52);
        t.addEntry((short)51);
        t.addEntry((short)50);
        t.addEntry((short)49);



        System.out.println("**************************");

        int node = t.getFirstNode();
        System.out.println(node);

        while (node != BRedBlackEntries.NIL){
            System.out.println("node="+node+" value="+t.getValue(node));
            node = t.getNextNode(node);
        }

    }

    @Test
    public void test2() {
        BRedBlackTest t = new BRedBlackTest();
        Random random = new Random(System.currentTimeMillis());
        short[] values = new short[27*1000*1000];
        for (int i=0; i<values.length; i++){
            values[i] = (short)(random.nextInt());
        }

        long start = System.currentTimeMillis();
        for (int i=0; i<values.length; i++){
            t.addEntry(values[i]);
        }
        long end = System.currentTimeMillis();



        System.out.println("************************** duration="+(end-start));

        int node = t.getFirstNode();
//        System.out.println(node);
        int prev;

        int cnt = 0;
        while (node != BRedBlackEntries.NIL){
            prev = node;
//            System.out.println("node="+node+" value="+t.getValue(node));
            node = t.getNextNode(node);
            cnt++;
            if (node != BRedBlackEntries.NIL){
                assert (t.getValue(prev) <= t.getValue(node));
            }
        }
        assert (cnt == values.length);

    }

    public class Shorty implements Comparable<Shorty>{

        int idx;
        short value;

        public Shorty(int idx, short value){
            this.idx = idx;
            this.value = value;
        }

        @Override
        public int compareTo(Shorty o) {
            int res = Short.compare(value, o.value);
            if (res == 0) res = Integer.compare(idx, o.idx);
            return res;
        }
    }

    @Test
    public void test3() {
        TreeSet<Shorty> treeSet = new TreeSet<>();
        Random random = new Random(System.currentTimeMillis());
        short[] values = new short[9*1000*1000];
        for (int i=0; i<values.length; i++){
            values[i] = (short)(random.nextInt());
        }

        long start = System.currentTimeMillis();
        for (int i=0; i<values.length; i++){
            treeSet.add(new Shorty(i, values[i]));
        }
        long end = System.currentTimeMillis();



        System.out.println("************************** duration="+(end-start));

        Shorty node = treeSet.first();
//        System.out.println(node);
        Shorty prev;

        int cnt = 0;
        while (node != null){
            prev = node;
//            System.out.println("node="+node+" value="+t.getValue(node));
            node = treeSet.higher(node);
            cnt++;
            if (node != null){
                assert (prev.value <= node.value);
            }
        }
        assert (cnt == values.length);

    }


}
