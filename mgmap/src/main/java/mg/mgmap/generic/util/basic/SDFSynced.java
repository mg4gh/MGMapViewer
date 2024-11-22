package mg.mgmap.generic.util.basic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SDFSynced {
    final SimpleDateFormat sdf;
    public SDFSynced(String pattern, Locale locale){
        sdf = new SimpleDateFormat(pattern, locale);
    }
    public String format(Object object){
        synchronized (sdf){
            return sdf.format(object);
        }
    }
    public String format(Date object){
        synchronized (sdf){
            return sdf.format(object);
        }
    }
    public Date parse(String s) throws ParseException {
        synchronized ((sdf)){
            return sdf.parse(s);
        }
    }
}
