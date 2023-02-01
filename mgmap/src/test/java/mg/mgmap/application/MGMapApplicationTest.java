package mg.mgmap.application;

import org.junit.Test;
import org.junit.Assert;

import mg.mgmap.generic.util.basic.MGLog;

public class MGMapApplicationTest {

    @Test
    public void testConstants(){
        MGLog.setUnittest(true);
        MGMapApplication application = new MGMapApplication();
        Assert.assertNotNull(application);
        Assert.assertNotNull(application.lastPositionsObservable);

    }

}
