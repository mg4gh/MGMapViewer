package mg.mgmap.activity.mgmap.features.search;

import java.io.StringReader;
import java.lang.invoke.MethodHandles;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;

public class SearchPos extends PointModelImpl {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public SearchPos(double lat, double lon){
        super(lat,lon);
    }
    public SearchPos(PointModel pm){
        super(pm.getLat(),pm.getLon());
    }

    byte zoom = 0;
    String label = "";

    public byte getZoom() {
        return zoom;
    }

    public void setZoom(byte zoom) {
        this.zoom = zoom;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String toJsonString(){
        return Json.createObjectBuilder()
                .add("lat",Double.toString(getLat()))
                .add("lon",Double.toString(getLon()))
                .add("zoom",Byte.toString(zoom))
                .add("label",label)
                .build().toString();
    }

    public static SearchPos fromJsonString(String sSearchPos){
        try (JsonReader reader = Json.createReader(new StringReader(sSearchPos))){
            JsonObject joSearchPos = reader.readObject();
            double lat = Double.parseDouble(joSearchPos.getString("lat"));
            double lon = Double.parseDouble(joSearchPos.getString("lon"));
            byte zoom = Byte.parseByte(joSearchPos.getString("zoom"));
            String label = joSearchPos.getString("label");
            SearchPos searchPos = new SearchPos(lat, lon);
            searchPos.setZoom(zoom);
            searchPos.setLabel(label);
            return searchPos;
        } catch (Exception e){ mgLog.e(e); }
        return null;
    }
}
