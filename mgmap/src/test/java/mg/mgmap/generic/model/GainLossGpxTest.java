package mg.mgmap.generic.model;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.gpx.GpxImporter;

public class GainLossGpxTest {

    ElevationProvider elevationProvider = new ElevationProvider() {
        @Override
        public void setElevation(TrackLogPoint tlp) {}
        @Override
        public void setElevation(WriteablePointModel wpm) {}
    };

    @Test
    public void testGpx() throws Exception {
        MGLog.setUnittest(true);

        GpxImporter gpxImporter = new GpxImporter(elevationProvider);
        // The test files in gpx folder are real world sample data. The name is constructed by following schema:
        // <date>_<time>_GPS_<Gain_BC14.16>_U<GainApp>_D<LossApp>
        // The summary allows to verify easily modified Statistic implementations
        File gpxDir = new File("src/test/assets/gpx");
        for (File file : Objects.requireNonNull(gpxDir.listFiles())){
            TrackLog trackLog = gpxImporter.parseTrackLog(file.getName(), Files.newInputStream(file.toPath()));
            MGLog.sd("file="+file.getName()+"   \nStatistic: "+trackLog.getTrackStatistic());
        }

    }




}
