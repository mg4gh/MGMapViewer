package mg.mgmap.activity.mgmap.features.tilestore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonWriter;

import mg.mgmap.generic.util.basic.MGLog;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DynamicHandler {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    Map<String, String> map = new HashMap<>();
    InputStream is;
    OutputStream os;
    Properties props;

    public DynamicHandler(InputStream is, OutputStream os, Properties props) {
        this.is = is;
        this.os = os;
        this.props = props;
    }

    public boolean run() throws Exception {
        JsonReader jsonReader = Json.createReader(is);
        JsonArray cAll = jsonReader.readArray();

        int cnt = 1;
        boolean res = false;
        for (JsonValue cOneV : cAll) {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build();

            JsonObject cOne = cOneV.asJsonObject();
            mgLog.i("cOne=" +cOne);

            JsonObject init = cOne.getJsonObject("Init");
            if (init != null){
                for (Map.Entry<String, JsonValue> entry : init.entrySet()){
                    String k = entry.getKey();
                    if (entry.getValue().getValueType() == JsonValue.ValueType.STRING){
                        map.put(k, subst(init.getString(k)) );
                    } else {
                        JsonObject jo = entry.getValue().asJsonObject();
                        StringBuilder jov = new StringBuilder();
                        for (Map.Entry<String, JsonValue> joEntry : jo.entrySet()) {
                            String jok = joEntry.getKey();
                            jov.append(subst(jo.getString(jok)));
                        }
                        map.put(k, jov.toString());
                    }
                }
            }

            mgLog.v("Map after init");
            for (String key : map.keySet()){
                mgLog.v("k="+key+" v="+map.get(key));
            }

            String type = cOne.getString("Type");
            mgLog.i("Type="+type);

            StringBuilder url = new StringBuilder(cOne.getString("URL"));
            mgLog.i("1 URL="+url);

            if (type.equals("GET")) {
                JsonObject params = cOne.getJsonObject("Params");
                if (params != null){
                    for (Map.Entry<String, JsonValue> entry : params.entrySet()){
                        String k = entry.getKey();
                        url.append("&").append(kv(k, params.getString(k)));
                    }
                }
            }
            mgLog.i("2 URL="+url);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(url.toString());

            if (type.equals("POST")){
                JsonObject params = cOne.getJsonObject("Params");
                if (params != null){
                    String sParams = subst(json2String(params));
                    mgLog.i("sParams="+sParams);
                    RequestBody body = RequestBody.create(sParams, MediaType.parse("application/json"));
                    requestBuilder.post(body);
                }


                JsonObject bodyParams = cOne.getJsonObject("BodyReq");
                if (bodyParams != null){
                    FormBody.Builder fbb = new FormBody.Builder();
                    for (Map.Entry<String, JsonValue> entry : bodyParams.entrySet()){
                        String k = entry.getKey();
                        String v = subst( bodyParams.getString(k) );
                        mgLog.v("body params: "+k+": "+v);
                        fbb.add(k, v);
                    }
                    FormBody fb = fbb.build();
                    requestBuilder.post(fb);
                }
            }

            JsonObject header = cOne.getJsonObject("Header");
            if (header != null){
                for (Map.Entry<String, JsonValue> entry : header.entrySet()){
                    String k = entry.getKey();
                    String v = subst(header.getString(k));
                    mgLog.v("request header: "+k+": "+v);
                    requestBuilder.addHeader(k,v);
                }
            }
            Request request = requestBuilder.build();


            Call call = client.newCall(request);
            Response response = call.execute();

            mgLog.i("rc="+response.code());
            for (String s : response.headers().names()){
                for (String v : response.headers(s)){
                    mgLog.v("response header: "+s+": "+v);
                }
            }

            JsonObject headerResp = cOne.getJsonObject("HeaderResp");
            if (headerResp != null){
                for (Map.Entry<String, JsonValue> entry : headerResp.entrySet()){
                    String k = entry.getKey();
                    JsonArray ja = entry.getValue().asJsonArray();
                    for (int i=0; i<ja.size(); i++){
                        String v = ja.getString(i);
                        getHeader(response, k, v);
                    }
                }
            }

            JsonObject bodyResp = cOne.getJsonObject("BodyResp");
            if (bodyResp != null){
                for (Map.Entry<String, JsonValue> entry : bodyResp.entrySet()){
                    String k = entry.getKey();
                    getFromResponse(response, k, bodyResp.getString(k));
                }
            }
            mgLog.i("Map after resp");
            for (String key : map.keySet()){
                mgLog.i("k="+key+" v="+map.get(key));
            }

            JsonString number = cOne.getJsonString("Number");
            if (number != null){
                if (number.getString().equals("6")){
                    dumpResponseBody(response,100);
                }
            }

            JsonArray export = cOne.getJsonArray("Export");
            if (export != null){
                String nl = System.lineSeparator();
                boolean checkOk = true;
                StringBuilder sb = new StringBuilder("[").append(nl);
                for (int i=0; i<export.size(); i++){
                    String k = export.getString(i);
                    String v = map.get(k);
                    if (v == null) {
                        checkOk = false;
                        break;
                    }

                    sb.append("{").append(nl);
                    sb.append("    \"Name raw\": \"").append(k).append("\",").append(nl);
                    sb.append("    \"Content raw\": \"").append(v).append("\"").append(nl);
                    sb.append("}");
                    if ( (i+1) < export.size() ) sb.append(","); // if not the last
                    sb.append(nl);
                }
                sb.append("]").append(nl);

                if (checkOk){
                    PrintWriter pw = new PrintWriter(os);
                    pw.println(sb);
                    pw.close();
                    mgLog.i("checkOk");
                    res = true;
                }
            }

            mgLog.i("************************* "+(cnt++)+" finished *****************************");
        }
        return res;
    }

    @SuppressWarnings("ConstantConditions")
    public String subst(String orig){
        int aidx;
        while ((aidx=orig.indexOf("$")) >= 0){
            String sub1 = orig.substring(0,aidx);
            int eidx = orig.indexOf("}", aidx);
            if (eidx == -1) break;  // unexpected behaviour
            String value = orig.substring(aidx, eidx+1);
            String sub3 = orig.substring(eidx+1);

            switch (value) {
                case "${UUID}":
                    value = UUID.randomUUID().toString();
                    break;
                case "${NOW}":
                    value = "" + System.currentTimeMillis();
                    break;
                case "${NOW_1}":
                    value = "" + (System.currentTimeMillis() + 1);
                    break;
                case "${NOW_T}":
                    value = "" + (System.currentTimeMillis() / 1000);
                    break;
            }
            if ((value!=null) && value.startsWith("$KV{") && value.endsWith("}")){
                String varName = value.substring(4,value.length()-1);
                value = varName+"="+map.get(varName);
            }
            if ((value!=null) && value.startsWith("${") && value.endsWith("}")){
                String varName = value.substring(2,value.length()-1);
                value = map.get(varName);
            }
            if ((value!=null) && value.startsWith("$P{") && value.endsWith("}")){
                String varName = value.substring(3,value.length()-1);
                value = props.getProperty(varName);
            }
            String orig2 = sub1 + value + sub3;
            if (orig.equals(orig2)) break;  // unexpected behaviour
            orig = orig2;
        }
        return orig;
    }

    public String kv(String key, String value){
        return key+"="+subst(value);
    }

    private void getHeader(Response response, String group, String name) {
        String value = getFromHeader(response, group, name);
        if (value != null){
            map.put(name, value);
        }
    }

    private String getFromHeader(Response response, String group, String name){
        List<String> groupss = response.headers(group);
        for (String x : groupss){
            String[] groups = x.split(";");
            for (String grp : groups){
                String[] part = grp.trim().split("=");
                if ((part.length == 2) && (part[0].equals(name))){
                    return part[1];
                }
            }
        }
        return null;
    }

    private void getFromResponse(Response response, String name, String pattern) {
        String value = getFromResponseBody(response, pattern, 1);
        if (value != null){
            map.put(name, value);
        }
    }


    @SuppressWarnings("SameParameterValue")
    private String getFromResponseBody(Response response, String pattern, int groupIndex){
        try {
            Pattern p = Pattern.compile(pattern);
            BufferedReader in = new BufferedReader(Objects.requireNonNull(response.body()).charStream());
            String line;
            while ((line = in.readLine()) != null){
                Matcher m = p.matcher(line);
                if (m.find()){
                    return m.group(groupIndex);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("SameParameterValue")
    private void dumpResponseBody(Response response, int numLines){
        try {
            BufferedReader in = new BufferedReader(Objects.requireNonNull(response.body()).charStream());
            String line;
            int cnt = 0;
            while ((line = in.readLine()) != null){
                mgLog.v(String.format(Locale.ENGLISH, "lc=%04d %s", cnt++,line));
                if (cnt > numLines) return;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String json2String(JsonObject jo){
        StringWriter sw = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(sw);
        jsonWriter.writeObject(jo);
        jsonWriter.close();
        return sw.toString();
    }
}
