package com.example.myxxx;

import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DynamicHandler {

    Map<String, String> map = new HashMap<>();
    InputStream is;
    OutputStream os;
    public DynamicHandler(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }

    public void run() throws  Exception{
        JsonReader jsonReader = Json.createReader(is);
        JsonArray cAll = jsonReader.readArray();

        Log.i("XXXX", "Hello");

        int cnt = 1;

        for (JsonValue cOneV : cAll) {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build();

            JsonObject cOne = cOneV.asJsonObject();
            Log.i("XXXX", " cOne=" +cOne);

            JsonObject init = cOne.getJsonObject("Init");
            if (init != null){
                for (Map.Entry<String,JsonValue> entry : init.entrySet()){
                    String k = entry.getKey();
                    if (entry.getValue().getValueType() == JsonValue.ValueType.STRING){
                        map.put(k, subst(init.getString(k)) );
                    } else {
                        JsonObject jo = entry.getValue().asJsonObject();
                        String jov = "";
                        for (Map.Entry<String, JsonValue> joEntry : jo.entrySet()) {
                            String jok = joEntry.getKey();
                            jov += subst(jo.getString(jok));
                        }
                        map.put(k, jov);
                    }
                }
            }

            Log.v("XXXX", "Map after init");
            for (String key : map.keySet()){
                Log.v("XXXX", "k="+key+" v="+map.get(key));
            }


            String type = cOne.getString("Type");
            Log.i("XXXX", "Type="+type);


            String url = cOne.getString("URL");
            Log.i("XXXX", "1 URL="+url);

            JsonObject params = cOne.getJsonObject("Params");
            if (params != null){
                for (Map.Entry<String,JsonValue> entry : params.entrySet()){
                    String k = entry.getKey();
                    url += "&"+ kv(k, params.getString(k));
                }
            }
            Log.i("XXXX", "2 URL="+url);

            Request.Builder requestBuilder = new Request.Builder()
                    .header("Content-Encoding", "gzip")
                    .url(url);

            if (type.equals("POST")){
                JsonObject bodyParams = cOne.getJsonObject("BodyReq");
                if (bodyParams != null){
                    FormBody.Builder fbb = new FormBody.Builder();
                    for (Map.Entry<String,JsonValue> entry : bodyParams.entrySet()){
                        String k = entry.getKey();
                        String v = subst( bodyParams.getString(k) );
                        Log.v("XXXX"," body params: "+k+": "+v);
                        fbb.add(k, v);
                    }
                    FormBody fb = fbb.build();
                    requestBuilder.post(fb);
                }
            }

            JsonObject header = cOne.getJsonObject("Header");
            if (header != null){
                for (Map.Entry<String,JsonValue> entry : header.entrySet()){
                    String k = entry.getKey();
                    String v = subst(header.getString(k));
                    Log.v("XXXX"," request header: "+k+": "+v);
                    requestBuilder.addHeader(k,v);
                }
            }
            Request request = requestBuilder.build();


            Call call = client.newCall(request);
            Response response = call.execute();

            Log.i("XXXX", " rc="+response.code());
            for (String s : response.headers().names()){
                for (String v : response.headers(s)){
                    Log.v("XXXX"," response header: "+s+": "+v);
                }
            }

            JsonObject headerResp = cOne.getJsonObject("HeaderResp");
            if (headerResp != null){
                for (Map.Entry<String,JsonValue> entry : headerResp.entrySet()){
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
                for (Map.Entry<String,JsonValue> entry : bodyResp.entrySet()){
                    String k = entry.getKey();
                    getFromResponse(response, k, bodyResp.getString(k));
                }
            }
            Log.v("XXXX", "Map after resp");
            for (String key : map.keySet()){
                Log.i("XXXX", "k="+key+" v="+map.get(key));
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
                    pw.println(sb.toString());
                    pw.close();
                }
            }

            Log.i("XXXX", "************************* "+(cnt++)+" finished *****************************");
        }
        Log.i("XXXX", "Goodbye");


    }

    public String subst(String value){
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
        if (value.startsWith("$KV{") && value.endsWith("}")){
            String varName = value.substring(4,value.length()-1);
//            Log.i("XXXX", " varName="+varName);
            value = varName+"="+map.get(varName);
        }
        if (value.startsWith("${") && value.endsWith("}")){
            String varName = value.substring(2,value.length()-1);
//            Log.i("XXXX", " varName="+varName);
            value = map.get(varName);
        }
        return value;
    }

    public String kv(String key, String value){
        return key+"="+subst(value);
    }
    public String kv(String key){
        return key+"="+subst("${"+key+"}");
    }








    private boolean getHeader(Response response, String group, String name) {
        String value = getFromHeader(response, group, name);
        if (value != null){
            map.put(name, value);
            return true;
        }
        return false;
    }

    private String getFromHeader(Response response, String group, String name){
        List<String> groupss = response.headers(group);
        for (String x : groupss){
            if (groupss != null){
                String[] groups = x.split(";");
                for (String grp : groups){
                    String[] part = grp.trim().split("=");
                    if ((part.length == 2) && (part[0].equals(name))){
                        return part[1];
                    }
                }
            }
        }
        return null;
    }

    private boolean getFromResponse(Response response, String name, String pattern) {
        String value = getFromResponseBody(response, pattern, 1);
        if (value != null){
            map.put(name, value);
            return true;
        }
        return false;
    }


    private String getFromResponseBody(Response response, String pattern, int groupIndex){
        try {
            Pattern p = Pattern.compile(pattern);

            BufferedReader in = new BufferedReader(response.body().charStream());
            String line = "";
            int cnt = 0;
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

    private void dumpResponseBody(Response response, int numLines){
        try {
            BufferedReader in = new BufferedReader(response.body().charStream());
            String line = "";
            int cnt = 0;
            while ((line = in.readLine()) != null){
                Log.i("XXXX", String.format("lc=%04d %s", cnt++,line));
                if (cnt > numLines) return;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
