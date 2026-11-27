package mg.mgmap.generic.graph.impl5;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import mg.mgmap.application.util.NativeBuffer;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.graph.WayAttributesManager;
import mg.mgmap.generic.graph.WayAttributs;
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
    short headerSize = 32;

    @Override
    public void init(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    @Override
    public void service() {
        // TODO Implement mechanism that prevents that the persistent cache becomes to large
        // - define meta Object per File with path,size,accessTime
        // - loop over all cache files and create a sorted tree of such meta objects.
        // - according to the targeted cache size define wich part of the tree shall be dropped
        // - remove files accordingly
        // - do this procedure only, if minimum time after last check is reached
    }

    @Override
    public synchronized void put(DGraphTile tile) {
        if (cacheDir == null) return;

        // keep files deterministic
        // TODO check, if a separate array of refs would make more sense - no need to persist this data
        tile.resetNodeRefs();
        // Here are multiple aspects relevant - first again, keep files deterministic
        // otherwise it would be necessary to keep track obout the used profile
        // Would it make sense to store cost separately (in a profile related File??)
        // At the end rather not, high effort and low performance gain
        tile.resetCosts();

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
            tile.graphModified = false;
        } catch (Exception e) {
            mgLog.e(e);
        }

    }

    @Override
    public synchronized boolean get(DGraphTile tile) {
        tile.state = DGraphTile.State.INIT_FROM_PERSISTENT_CACHE;
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
            }

            tile.naBuNodes = new NativeBuffer(tile+" nodes refilled", (long) tile.maxNodeCnt * DGraphTile.NODE_SIZE);
            tile.nodesBuffer = tile.naBuNodes.getBuffer();
//            tile.nodesBuffer = ByteBuffer.allocateDirect(tile.maxNodeCnt * DGraphTile.NODE_SIZE);
            while (tile.nodesBuffer.hasRemaining()) {
                int bytesRead = channel.read(tile.nodesBuffer);
                if (bytesRead == -1) {
                    mgLog.e("Unexpected EOF: "+fTile.getAbsolutePath());
                }
            }
            tile.nodesBuffer.flip();
            tile.nodesBufferClone = tile.nodesBuffer.duplicate();

            tile.naBuNeighbours = new NativeBuffer(tile+" neighbours refilled", (long) tile.maxNeighbourCnt * DGraphTile.NEIGHBOUR_SIZE);
            tile.neighboursBuffer = tile.naBuNeighbours.getBuffer();
//            tile.neighboursBuffer = ByteBuffer.allocateDirect(tile.maxNeighbourCnt * DGraphTile.NEIGHBOUR_SIZE);
            while (tile.neighboursBuffer.hasRemaining()) {
                int bytesRead = channel.read(tile.neighboursBuffer);
                if (bytesRead == -1) {
                    mgLog.e("Unexpected EOF: "+fTile.getAbsolutePath());
                    return false;
                }
            }
            tile.neighboursBuffer.flip();
            tile.neighboursBufferClone = tile.neighboursBuffer.duplicate();

            tile.state = DGraphTile.State.REFILLED;
            return true;
        } catch (Exception e){
            mgLog.e(e);
        }
        return false;
    }

    @Override
    public synchronized void clear() {
        if (cacheDir != null){
            PersistenceManager.deleteRecursivly(cacheDir);
        }
    }



//    private void createDump(DGraphTile origTile){
//        DGraphTile tile = new DGraphTile(origTile.tile, origTile.elevationProvider, origTile.statistics, origTile.smoothingDistance);
//        if (get(tile)){
//            File ydir = new File(cacheDir, String.valueOf(tile.tile.tileY));
//            File fTile = new File(ydir, String.valueOf(tile.tile.tileX)+".dump");
//            try (PrintWriter pw = new PrintWriter(fTile)) {
//                String header = String.format(Locale.ENGLISH, "Header %s maxNodes=%d initialNodes=%d ,maxNeighbours=%d initialNeighbours=%d, numWayAttributes=%d, neighbourConnected=%02X", tile.toString(), tile.maxNodeCnt, tile.initialNodeCnt, tile.maxNeighbourCnt, tile.initialNeighbourCnt, tile.wayAttributesList.size(), tile.neighbourConnected);
//                pw.println(header);
//                int cnt = 0;
//                for (WayAttributs wa : tile.wayAttributesList){
//                    pw.println(String.format(Locale.ENGLISH, "  %04d: %08X %08X", cnt++, wa.getWaValue()>>32, (wa.getWaValue() & 0xFFFFFFFFL)));
//                }
//                pw.println("Nodes:");
//                for (short i = 0; i < tile.maxNodeCnt; i++) {
//                    pw.println(tile.nodeToString(i));
//                }
//                pw.println("Neighbours:");
//                for (short i = 0; i < tile.maxNeighbourCnt; i++) {
//                    pw.println(tile.neighbourToString(i));
//                }
//            } catch (Exception e) {
//                mgLog.e(e);
//            }
//
//
//        } else {
//            mgLog.w("verify tile failed");
//
//        }

}
