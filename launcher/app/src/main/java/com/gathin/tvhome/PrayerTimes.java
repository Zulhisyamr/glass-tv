package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class PrayerTimes {
    interface Callback { void onText(String text); }

    private static final String ZONE = "TRG01";

    private static String hm(String t) { return t.length() >= 5 ? t.substring(0, 5) : t; }

    /** Calls back from a background thread: post to the UI thread in the caller. */
    static void load(final Context ctx, final Callback cb) {
        final SharedPreferences sp = ctx.getSharedPreferences("prayer", Context.MODE_PRIVATE);
        final String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        if (today.equals(sp.getString("day", ""))) {
            cb.onText(sp.getString("text", ""));
            return;
        }
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    URL u = new URL("https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=today&zone=" + ZONE);
                    HttpURLConnection c = (HttpURLConnection) u.openConnection();
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(8000);
                    InputStream in = c.getInputStream();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] b = new byte[2048];
                    int n;
                    while ((n = in.read(b)) > 0) out.write(b, 0, n);
                    in.close();
                    JSONObject p = new JSONObject(out.toString("UTF-8"))
                            .getJSONArray("prayerTime").getJSONObject(0);
                    String text = "Subuh " + hm(p.getString("fajr"))
                            + "   Syuruk " + hm(p.getString("syuruk"))
                            + "   Zohor " + hm(p.getString("dhuhr"))
                            + "   Asar " + hm(p.getString("asr"))
                            + "   Maghrib " + hm(p.getString("maghrib"))
                            + "   Isyak " + hm(p.getString("isha"));
                    sp.edit().putString("day", today).putString("text", text).apply();
                    cb.onText(text);
                } catch (Exception e) {
                    cb.onText(sp.getString("text", ""));
                }
            }
        }).start();
    }
}
