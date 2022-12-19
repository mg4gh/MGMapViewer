package mg.mgmap.generic.util;

import android.content.Context;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.lang.invoke.MethodHandles;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.MGLog;

public class WiFiUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static boolean checkWLAN(MGMapApplication application, String wlanSSID){
        if (wlanSSID == null) return false;
        if (isEmulator()) return true;
        String ssid = "";
        try {
            WifiManager wifiManager = (WifiManager) application.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo;

            wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                ssid = wifiInfo.getSSID();
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
        mgLog.i("currentSsid=\""+ssid+"\""+" checkSsid=\""+wlanSSID+"\"" );
        return wlanSSID.equals(ssid);
    }


    private static boolean isEmulator() {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone64_arm64")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator");
    }

}
