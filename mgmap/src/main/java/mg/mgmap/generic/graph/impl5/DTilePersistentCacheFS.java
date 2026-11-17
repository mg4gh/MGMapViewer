package mg.mgmap.generic.graph.impl5;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.graph.WayAttributesManager;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;

/** Store main content of a tile in a persistent way, especially:
 * - MetaData (20byte):
 *   - tileY (short)
 *   - tileX (short)
 *   - initialNodeCnt (int)
 *   - maxNodeCnt (int)
 *   - initialNeighbourCnt (int)
 *   - maxNeighbourCnt (int)
 * - Mapping wayAttributesIdx (short) -> waValue (long)
 *   - numberOfWayAttributeMappings (int)
 *   - waValues (long) (size: numberOfWayAttributeMappings * 8)
 * - nodesBuffer (size: maxNodeCnt * NODE_SIZE)
 * - neighboursBuffer (size: maxNeighbourCnt * NEIGHBOUR_SIZE)
 */
public class DTilePersistentCacheFS implements DTilePersistentCache{

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    File cacheDir = null;
    Set<DGraphTile> persistencyCandidates;
    short headerSize = 32;


    @Override
    public void init(File cacheDir, Set<DGraphTile> persistencyCandidates) {
        this.persistencyCandidates = persistencyCandidates;
        this.cacheDir = cacheDir;


    }

    @Override
    public void service() {
        persistencyCandidates.forEach(this::put);
        persistencyCandidates.clear();
    }

    @Override
    public synchronized void put(DGraphTile tile) {
        if (cacheDir == null) return;
        int numWayAttributes = tile.wayAttributesList.size();
        int headerBufSize = headerSize + 8*numWayAttributes;
        ByteBuffer buf = ByteBuffer.allocate(headerSize + 8*numWayAttributes);
        buf.putShort((short)tile.tile.tileY);
        buf.putShort((short)tile.tile.tileX);
        buf.putInt(tile.initialNodeCnt);
        buf.putInt(tile.maxNodeCnt);
        buf.putInt(tile.initialNeighbourCnt);
        buf.putInt(tile.maxNeighbourCnt);
        buf.putInt(numWayAttributes);
        buf.putLong(tile.neighbourConnected); // use long to have proper alignment

        for (WayAttributs wa : tile.wayAttributesList){
            buf.putLong(wa.getWaValue());
        }
        buf.flip();

        File ydir = new File(cacheDir, String.valueOf(tile.tile.tileY));
        if (!ydir.exists()){
            if (!ydir.mkdirs()) return; // cannot create directory -> return
        }
        File fTile = new File(ydir, String.valueOf(tile.tile.tileX));


        try (FileChannel channel = FileChannel.open(
                fTile.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            int res = channel.write(buf);
            assert (res == headerBufSize);

            ByteBuffer nodesBuffer = tile.nodesBuffer.duplicate();
            nodesBuffer.position(0);
            nodesBuffer.limit(nodesBuffer.capacity());
            res = 0;
            while (nodesBuffer.hasRemaining()) {
                res += channel.write(nodesBuffer);
            }
            assert (res == tile.nodesBuffer.capacity());

            ByteBuffer neighboursBuffer = tile.neighboursBuffer.duplicate();
            neighboursBuffer.position(0);
            neighboursBuffer.limit(neighboursBuffer.capacity());
            res = 0;
            while (neighboursBuffer.hasRemaining()) {
                res += channel.write(neighboursBuffer);
            }
            assert (res == tile.neighboursBuffer.capacity()):" res="+res+" tile.neighboursBuffer.capacity()="+tile.neighboursBuffer.capacity();

        } catch (Exception e) {
            mgLog.e(e);
        }

    }

    @Override
    public synchronized boolean get(DGraphTile tile) {
        File ydir = new File(cacheDir, String.valueOf(tile.tile.tileY));
        if (!ydir.exists()) return false;

        File fTile = new File(ydir, String.valueOf(tile.tile.tileX));
        if (!fTile.exists()) return false;
        try (FileChannel channel = FileChannel.open(fTile.toPath(), StandardOpenOption.READ)) {
            ByteBuffer headerBuf = ByteBuffer.allocate(headerSize);
            int res = channel.read(headerBuf);
            assert (res == headerSize);

            headerBuf.flip();
            short tileY = headerBuf.getShort(); assert (tileY == tile.tile.tileY);
            short tileX = headerBuf.getShort(); assert (tileX == tile.tile.tileX);
            tile.initialNodeCnt = headerBuf.getInt();
            tile.maxNodeCnt = headerBuf.getInt();
            tile.initialNeighbourCnt = headerBuf.getInt();
            tile.maxNeighbourCnt = headerBuf.getInt();
            int numberOfWayAttributes = headerBuf.getInt();
            tile.neighbourConnected = (byte)headerBuf.getLong();


            ByteBuffer waValuesBuffer = ByteBuffer.allocate(numberOfWayAttributes * 8);
            res = channel.read(waValuesBuffer);
            assert (res == numberOfWayAttributes * 8);
            waValuesBuffer.flip();
            for (int i = 0; i < numberOfWayAttributes; i++) {
                tile.wayAttributesList.add(WayAttributesManager.getWayAttributs(waValuesBuffer.getLong()));
            };

            ByteBuffer nodesBuffer = ByteBuffer.allocateDirect(tile.maxNodeCnt * DGraphTile.NODE_SIZE);
            while (nodesBuffer.hasRemaining()) {
                int bytesRead = channel.read(nodesBuffer);
                if (bytesRead == -1) {
                    mgLog.e("Unexpected EOF: "+fTile.getAbsolutePath());
                }
            }
            nodesBuffer.flip();
            tile.nodesBuffer = nodesBuffer;

            ByteBuffer neighboursBuffer = ByteBuffer.allocateDirect(tile.maxNeighbourCnt * DGraphTile.NEIGHBOUR_SIZE);
            while (neighboursBuffer.hasRemaining()) {
                int bytesRead = channel.read(neighboursBuffer);
                if (bytesRead == -1) {
                    mgLog.e("Unexpected EOF: "+fTile.getAbsolutePath());
                    return false;
                }
            }
            neighboursBuffer.flip();
            tile.neighboursBuffer = neighboursBuffer;

            return true;
        } catch (Exception e){
            mgLog.e(e);
        }
        return false;
    }

    @Override
    public synchronized void clear() {
        PersistenceManager.deleteRecursivly(cacheDir);
    }
}
