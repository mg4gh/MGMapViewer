package mg.mgmap.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import mg.mgmap.generic.util.basic.MGLog;

public class MGMapApplicationTest {

    @Test
    void testConstants(){
        MGLog.setUnittest(true);
        MGMapApplication application = new MGMapApplication();
        assertNotNull(application);
        assertNotNull(application.lastPositionsObservable);

    }

}
