package mg.mgmap.test;

import android.app.Application;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;

public class TestControlComposer {

    Application application;

    public TestControlComposer(Application application) {
        this.application = application;
    }

    private void setupTestcaseList(ArrayList<String> testcaseList) {
        try {
            BufferedReader in = new BufferedReader( new InputStreamReader( application.getAssets().open("testcases.list") ));
            String line;
            while ((line = in.readLine()) != null){
                testcaseList.add(line);
            }
        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
    }

    public void compose(TestControl tc, String testset, ArrayList<Testcase> testcases){
        Log.d(MGMapApplication.LABEL, NameUtil.context()+" Pattern(s) for testcase selection: "+testset);
        String[] setArray = testset.split("\n");

        ArrayList<String> testcaseList = new ArrayList<>();
        ArrayList<String> matchedList = new ArrayList<>();
        ArrayList<String> matches = new ArrayList<>();
        setupTestcaseList(testcaseList);

        for (String set : setArray){
            matches.clear();
            for (String testcase : testcaseList){
                if (testcase.matches(set)){
                    matchedList.add( testcase );
                    matches.add( testcase );
                }
            }
            testcaseList.removeAll(matches);
        }
        Log.d(MGMapApplication.LABEL, NameUtil.context()+" "+ matchedList);
        Log.d(MGMapApplication.LABEL, NameUtil.context()+" "+ matchedList.size());

        for (String testcaseName : matchedList){
            try {
                Class<?> clazz = Class.forName(testcaseName);
                Constructor<?> clazzConstructor = clazz.getConstructor(TestControl.class);
                Testcase testcase = (Testcase)clazzConstructor.newInstance(tc);

                testcases.add(testcase);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
