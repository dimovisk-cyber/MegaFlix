package com.nuvio.tv.megaflix;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles megaflix://youcine/... links. The "YouCine" source in the MegaFlix source list points
 * here, so choosing it opens the exact title in YouCine, already playing full screen:
 *   megaflix://youcine/play?type=movie|series&id=tt123|tmdb:123&s=1&e=2
 *   megaflix://youcine/section?name=aovivo|esportes
 * The Piloto (accessibility service) does the searching and pressing inside YouCine.
 */
public class YouCineLinkActivity extends Activity {

    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent() != null ? getIntent().getData() : null;
        if (uri == null) { finish(); return; }
        if (findYouCine(this) == null) {
            Toast.makeText(this, "Instale o YouCine para usar esta opção.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (path.startsWith("/section")) {
            openSection(uri.getQueryParameter("name"));
        } else {
            play(uri.getQueryParameter("type"), uri.getQueryParameter("id"),
                    uri.getQueryParameter("s"), uri.getQueryParameter("e"), uri.getQueryParameter("title"));
        }
    }

    // ------------------------------------------------------------------ title

    private void play(final String type, final String id, final String s, final String e, final String hint) {
        final String episode = (s != null && e != null) ? "T" + s + " E" + e : null;
        withPilot(new Runnable() {
            @Override public void run() {
                Toast.makeText(YouCineLinkActivity.this, "Abrindo no YouCine…", Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override public void run() {
                        final List<String> queries = titles(type, id, hint);
                        main.post(new Runnable() {
                            @Override public void run() {
                                if (queries.isEmpty()) {
                                    Toast.makeText(YouCineLinkActivity.this,
                                            "Não encontrei o nome deste título.", Toast.LENGTH_LONG).show();
                                    finish();
                                    return;
                                }
                                PilotService.startJob(YouCineLinkActivity.this, "youcine", queries, queries.get(0), episode);
                                launchYouCineFresh();
                                finish();
                            }
                        });
                    }
                }).start();
            }
        }, new Runnable() {
            @Override public void run() {
                new Thread(new Runnable() {
                    @Override public void run() {
                        final List<String> queries = titles(type, id, hint);
                        main.post(new Runnable() {
                            @Override public void run() {
                                if (!queries.isEmpty()) copy(queries.get(0));
                                launchYouCineFresh();
                                finish();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    private void openSection(final String name) {
        final boolean sports = "esportes".equals(name);
        withPilot(new Runnable() {
            @Override public void run() {
                List<String> q = new ArrayList<String>();
                if (sports) {
                    q.add("Esportes"); q.add("Esporte"); q.add("Futebol"); q.add("Jogos"); q.add("Sports");
                } else {
                    q.add("TV ao vivo"); q.add("Ao vivo"); q.add("Canais"); q.add("Live TV"); q.add("TV");
                }
                PilotService.startJob(YouCineLinkActivity.this, "youcine_nav:" + (sports ? "esportes" : "aovivo"),
                        q, sports ? "Esportes" : "TV ao vivo", null);
                launchYouCineFresh();
                finish();
            }
        }, new Runnable() {
            @Override public void run() { launchYouCineFresh(); finish(); }
        });
    }

    /** Runs [withPilot] when the Piloto is on; otherwise explains how to turn it on (once). */
    private void withPilot(final Runnable withPilot, final Runnable without) {
        if (PilotService.isEnabled(this)) { withPilot.run(); return; }
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Ativar o Piloto do MegaFlix")
                .setMessage("Para abrir o título já reproduzindo em tela cheia no YouCine, ligue o "
                        + "\"MegaFlix Piloto\" em Acessibilidade (só uma vez).\n\n"
                        + "1. Toque em Ativar agora\n"
                        + "2. Abra \"MegaFlix Piloto\" e ligue\n"
                        + "3. Volte e escolha YouCine de novo\n\n"
                        + "Se o Android disser \"configuração restrita\": Configurações > Apps > MegaFlix > "
                        + "menu (3 pontos) > Permitir configurações restritas.\n\n"
                        + "O Piloto só age dentro do YouCine e só quando você escolhe um título no MegaFlix.")
                .setPositiveButton("Ativar agora", (d, w) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } catch (Exception ignored) { }
                    finish();
                })
                .setNegativeButton("Só abrir o YouCine", (d, w) -> without.run())
                .setOnCancelListener(d -> finish())
                .show();
    }

    // ------------------------------------------------------------------ YouCine

    public static String findYouCine(Context c) {
        PackageManager pm = c.getPackageManager();
        for (String p : new String[]{PilotService.YOUCINE_PKG, PilotService.YOUCINE_MOBILE}) {
            try { pm.getPackageInfo(p, 0); return p; } catch (Exception ignored) { }
        }
        return null;
    }

    /** Restart YouCine's task so the Piloto always starts from its home screen. */
    private void launchYouCineFresh() {
        String pkg = findYouCine(this);
        if (pkg == null) return;
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) return;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try { startActivity(i); } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir o YouCine", Toast.LENGTH_SHORT).show();
        }
    }

    private void copy(String text) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("MegaFlix", text));
            Toast.makeText(this, "\"" + text + "\" copiado. No YouCine, abra Buscar.", Toast.LENGTH_LONG).show();
        } catch (Exception ignored) { }
    }

    // ------------------------------------------------------------------ names (pt-BR first)

    /** Brazilian title first, then the original title, so YouCine's search finds it either way. */
    public static List<String> titles(String type, String id, String hint) {
        List<String> out = new ArrayList<String>();
        boolean tv = "series".equals(type) || "tv".equals(type);
        String key = com.nuvio.tv.BuildConfig.TMDB_API_KEY;
        try {
            if (id != null && key != null && key.length() > 0) {
                JSONObject r = null;
                if (id.startsWith("tt")) {
                    String imdb = id.contains(":") ? id.substring(0, id.indexOf(':')) : id;
                    JSONObject f = json("https://api.themoviedb.org/3/find/" + imdb + "?external_source=imdb_id&language=pt-BR&api_key=" + key);
                    JSONArray a = f.optJSONArray(tv ? "tv_results" : "movie_results");
                    if (a == null || a.length() == 0) a = f.optJSONArray(tv ? "movie_results" : "tv_results");
                    if (a != null && a.length() > 0) r = a.getJSONObject(0);
                } else if (id.startsWith("tmdb:")) {
                    String n = id.substring(5);
                    if (n.contains(":")) n = n.substring(0, n.indexOf(':'));
                    r = json("https://api.themoviedb.org/3/" + (tv ? "tv/" : "movie/") + n + "?language=pt-BR&api_key=" + key);
                }
                if (r != null) {
                    add(out, r.optString("title", r.optString("name", "")));
                    add(out, r.optString("original_title", r.optString("original_name", "")));
                }
            }
        } catch (Exception ignored) { }
        add(out, hint);
        if (out.isEmpty() && id != null && id.startsWith("tt")) {
            try {
                String imdb = id.contains(":") ? id.substring(0, id.indexOf(':')) : id;
                JSONObject m = json("https://v3-cinemeta.strem.io/meta/" + (tv ? "series" : "movie") + "/"
                        + URLEncoder.encode(imdb, "UTF-8") + ".json").optJSONObject("meta");
                if (m != null) add(out, m.optString("name", ""));
            } catch (Exception ignored) { }
        }
        return out;
    }

    private static void add(List<String> out, String s) {
        if (s == null || s.trim().isEmpty()) return;
        String n = PilotService.norm(s);
        for (String o : out) if (PilotService.norm(o).equals(n)) return;
        out.add(s.trim());
    }

    private static JSONObject json(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            return new JSONObject(b.toString("UTF-8"));
        } finally {
            c.disconnect();
        }
    }
}
