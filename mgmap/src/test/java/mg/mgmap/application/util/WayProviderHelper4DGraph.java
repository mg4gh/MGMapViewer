package mg.mgmap.application.util;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.Way;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.util.WayProvider;

public class WayProviderHelper4DGraph implements WayProvider {
    @Override
    public List<Way> getWays(Tile tile) {
        List<Way> resultList = new ArrayList<>();
        if ((tile.tileX == 8586) && (tile.tileY == 5598)){

            LatLong p02 = new LatLong(49.410973,8.665);

            LatLong p11 = new LatLong(49.41,8.66);
            LatLong p12 = new LatLong(49.41,8.665);
            LatLong p13 = new LatLong(49.41,8.67);

            LatLong p21 = new LatLong(49.405,8.66);
            LatLong p22 = new LatLong(49.405,8.665);
            LatLong p23 = new LatLong(49.405,8.67);

            LatLong p31 = new LatLong(49.40,8.66);
            LatLong p32 = new LatLong(49.40,8.665);
            LatLong p33 = new LatLong(49.40,8.67);

            LatLong[][] llw1 = new LatLong[1][7];
            llw1[0][0] = p31;
            llw1[0][1] = p32;
            llw1[0][2] = p33;
            llw1[0][3] = p23;
            llw1[0][4] = p13;
            llw1[0][5] = p12;
            llw1[0][6] = p11;

            resultList.add( new Way((byte)14,List.of(new Tag("highway","track1")),llw1,new LatLong(0,0)) );

            LatLong[][] llw2 = new LatLong[1][6];
            llw2[0][0] = p11;
            llw2[0][1] = p21;
            llw2[0][2] = p31;
            llw2[0][3] = p22;
            llw2[0][4] = p13;
            llw2[0][5] = p02;
            resultList.add( new Way((byte)14,List.of(new Tag("highway","track2")),llw2,new LatLong(0,0)) );

            LatLong[][] llw3 = new LatLong[1][4];
            llw3[0][0] = p02;
            llw3[0][1] = p11;
            llw3[0][2] = p22;
            llw3[0][3] = p33;
            resultList.add( new Way((byte)14,List.of(new Tag("highway","track3")),llw3,new LatLong(0,0)) );

        }
        if ((tile.tileX == 8586) && (tile.tileY == 5597)){

            LatLong p02 = new LatLong(49.410973,8.665);

            LatLong p11 = new LatLong(49.42,8.66);
            LatLong p12 = new LatLong(49.42,8.665);
            LatLong p13 = new LatLong(49.42,8.67);

            LatLong[][] llw1 = new LatLong[1][3];
            llw1[0][0] = p11;
            llw1[0][1] = p12;
            llw1[0][2] = p13;
            resultList.add( new Way((byte)14,List.of(new Tag("highway","track4")),llw1,new LatLong(0,0)) );

            LatLong[][] llw2 = new LatLong[1][2];
            llw2[0][0] = p12;
            llw2[0][1] = p02;
            resultList.add( new Way((byte)14,List.of(new Tag("highway","track5")),llw2,new LatLong(0,0)) );
        }



        return resultList;
    }

    @Override
    public boolean isWayForRouting(Way way) {
        return true;
    }



}
