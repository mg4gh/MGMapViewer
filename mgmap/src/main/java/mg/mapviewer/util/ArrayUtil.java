package mg.mapviewer.util;

import java.lang.reflect.Array;

public class ArrayUtil {

    public static int[] concat(int[] a1, int[] a2){
        int[] res = new int[a1.length+a2.length];
        System.arraycopy(a1,0, res,0, a1.length);
        System.arraycopy(a2,0, res, a1.length, a2.length);
        return res;
    }

    public static <T> T[] concat(T[] a1, T[] a2) {
        @SuppressWarnings("unchecked")
        T[] res = (T[]) Array.newInstance(a1.getClass().getComponentType(), a1.length + a2.length);
        System.arraycopy(a1, 0, res, 0, a1.length);
        System.arraycopy(a2, 0, res, a1.length, a2.length);
        return res;
    }
}
