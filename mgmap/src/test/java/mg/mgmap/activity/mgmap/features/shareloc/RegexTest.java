package mg.mgmap.activity.mgmap.features.shareloc;

import org.junit.Test;

import java.util.regex.Pattern;

public class RegexTest {


    @Test
    public void _04_regex() {
        Pattern pb = Pattern.compile("^[a-zA-Z0-9]+([.\\-][a-zA-Z0-9]+)*@[a-zA-Z0-9]+([.\\-][a-zA-Z0-9]+)*[.][a-zA-Z0-9]+");
        boolean b1 = pb.matcher("abc.def@ab-de.de").matches();
        boolean b2 = pb.matcher("abc.def@ab-de.de-ef").matches();
        boolean b3 = pb.matcher("abc.def@ab-de.de-ef.gh").matches();
        boolean b4 = pb.matcher("abc..def@ab-de.de").matches();
        boolean b5 = pb.matcher("a-bc.def@ab-de.de").matches();
        boolean b6 = pb.matcher("a-bc.de_f@ab-de.de").matches();
        boolean b7 = pb.matcher(".def@ab-de.de").matches();

        Pattern pc = Pattern.compile("^[a-zA-Z0-9.\\-@]*");
        boolean c1 = pc.matcher("a.def@ab-de.de").matches();
        boolean c2 = pc.matcher("a.def@ab-de.de ").matches();
        boolean c3 = pc.matcher("\na.def@ab-de.de").matches();
        boolean c4 = pc.matcher("a.d@ef@ab-de.de").matches();
        boolean c5 = pc.matcher("a.de%f@ab-de.de").matches();
        boolean c6 = pc.matcher("a.def@ab+de.de").matches();

        Pattern pd = Pattern.compile("[a-f0-9]{8}");
        boolean d1 = pd.matcher("12345678").matches();
        boolean d2 = pd.matcher("123456aa").matches();
        boolean d3 = pd.matcher("12345678a").matches();
        boolean d4 = pd.matcher("1234567").matches();
        boolean d5 = pd.matcher("123456-8").matches();
        boolean d6 = pd.matcher("1fg456d8").matches();
        boolean d7 = pd.matcher("ff3456d8").matches();

        System.out.println(b1);
    }
}
