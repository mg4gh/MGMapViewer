package mg.mgmap.activity.settings;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.DialogView;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadMaps {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final FragmentActivity activity;

    public DownloadMaps(FragmentActivity activity) {
        this.activity = activity;
    }

    public void downloadMenu(String baseUrl) {
        new Thread(() -> downloadMenuInternal(baseUrl)).start();
    }
    private void downloadMenuInternal(String baseUrl) {
        TableLayout table = null;
        try {
            mgLog.d("baseUrl="+baseUrl);

            OkHttpClient client = new OkHttpClient().newBuilder().build();
            Request.Builder requestBuilder = new Request.Builder().url(baseUrl);
            Request request = requestBuilder.build();

            Call call = client.newCall(request);
            String responseText;
            try (Response response = call.execute()){
                mgLog.i("rc=" + response.code());
                assert response.body() != null;
                responseText = response.body().string();
            }

            table = new TableLayout(activity);
            table.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            Pattern pattern = Pattern.compile("<a href=.*>(.*)</a>\\s*(\\d{2}-\\w{3}-\\d{4})\\s*(\\d{2}:\\d{2})\\s*(\\S*)");
            int lineCnt = 0;
            for (String line : responseText.split("\n")) {
                line = line.replaceFirst("\r*$", "");
                mgLog.v("line=\"" + line + "\"");
                Matcher m = pattern.matcher(line);
                if (!m.matches()) continue;
                StringBuilder sb = new StringBuilder("  cnt=").append(m.groupCount()).append(" ");
                for (int i = 1; i <= m.groupCount(); i++) {
                    sb.append(i).append("=").append(m.group(i)).append(" ");
                }
                mgLog.v(()->"res= " + sb);
                TableRow tr = new TableRow(activity);
                tr.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                table.addView(tr);
                if (lineCnt % 2 == 0) tr.setBackgroundColor(Color.rgb(240,240,240));

                tr.setPadding(ControlView.dp(5),0,ControlView.dp(5),ControlView.dp(0));
                tr.setGravity(Gravity.CENTER_VERTICAL);
                if (m.groupCount() == 4) {
                    String item = m.group(1);
                    assert (item != null);
                    boolean itemIsFile = item.endsWith(".zip");
                    {
                        TextView tv = new TextView(activity);
                        TableRow.LayoutParams params = new TableRow.LayoutParams(0, ControlView.dp(62));
                        params.weight = 60;
                        tv.setLayoutParams(params);
                        long size = -1;
                        try {
                            size = Long.parseLong(m.group(4)) / (1024 * 1024);
                        } catch (NumberFormatException e) {
                            // occurs regularly on directory entries
                        }
                        if (size >= 0){
                            tv.setText(Html.fromHtml(String.format(Locale.ENGLISH,"<b>%s</b><br/><small>%s %s<br/>%d MB</small>", item.replaceFirst(".zip$",""), m.group(2), m.group(3), size),0));
                        } else {
                            tv.setText(Html.fromHtml(String.format(Locale.ENGLISH,"<b>%s</b><br/><small>%s %s</small>", item.replaceFirst("/$",""), m.group(2), m.group(3)),0));
                        }
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                        tr.addView(tv);
                        tv.setGravity(Gravity.CENTER_VERTICAL);
                        tv.setPadding(ControlView.dp(5),0,ControlView.dp(5),ControlView.dp(0));
                    }

                    {
                        Button tv = new Button(activity);
                        TableRow.LayoutParams params = new TableRow.LayoutParams(0, ControlView.dp(50));
                        params.weight = 25;
                        tv.setLayoutParams(params);
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);

                        tv.setText(activity.getResources().getString(itemIsFile?R.string.button_dl:R.string.button_dl_open));
                        tv.setGravity(Gravity.CENTER_VERTICAL);
                        tr.addView(tv);
                        tv.setPadding(ControlView.dp(itemIsFile?15:35),0,0,0);
                        tv.setOnClickListener(v -> {
                            if (itemIsFile){
                                mgLog.d("do download "+baseUrl+item);
                                Intent intent = new Intent(activity.getApplicationContext(), MGMapActivity.class);
                                intent.setData(Uri.parse(baseUrl.replace("https:","mf-v4-map:")+item));
                                activity.startActivity(intent);
                            } else {
                                downloadMenu(baseUrl+item);
                            }
                        });
                    }

                }
                lineCnt++;
            }
        } catch (IOException e) {
            mgLog.e(e);
        }


        DialogView dialogView = activity.findViewById(R.id.dialog_parent);

        if (dialogView.isLocked()){
            activity.runOnUiThread(dialogView::cancel);
        }
        if (table != null){
            final ScrollView content = new ScrollView(activity);
            content.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            content.addView(table);
            dialogView.lock(() -> dialogView
                    .setTitle("Download map")
                    .setContentView(content)
                    .setLogPrefix("dps")
                    .setMaximize(true)
                    .show());
        }
        mgLog.d("downloadMenuInternal done");



    }


}
