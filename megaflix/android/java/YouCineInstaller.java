package com.nuvio.tv.megaflix;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Installs YouCine, MegaFlix's second server. The APK ships inside MegaFlix (assets/youcine.apk);
 * if a build has no copy, it is downloaded from the fork's "youcine" release.
 * Installation goes through the system PackageInstaller, so Android shows its own confirm screen.
 */
public final class YouCineInstaller {

    private static final String ASSET = "youcine.apk";
    private static final String RELEASE_API =
            "https://api.github.com/repos/dimovisk-cyber/MegaFlix/releases/tags/youcine";
    private static final String PREFS = "megaflix_youcine";
    private static volatile boolean busy = false;

    private YouCineInstaller() { }

    public static boolean isInstalled(Context c) {
        return YouCineLinkActivity.findYouCine(c) != null;
    }

    /** First launch: YouCine account step, once (after the intro). Later: Configurações > Conta. */
    public static void offerOnce(final Activity a) {
        if (a.isFinishing()) return;
        final SharedPreferences p = a.getSharedPreferences(PREFS, 0);
        if (p.getBoolean("offered", false)) return;
        p.edit().putBoolean("offered", true).apply();
        final boolean installed = isInstalled(a);
        new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Conta YouCine")
                .setMessage("O YouCine é a segunda opção de servidor do MegaFlix (dublado e legendado, "
                        + "canais e esportes) e já vem junto com o app. "
                        + (installed ? "Entre na sua conta no YouCine para usá-lo." : "Instale e entre na sua conta para usá-lo.")
                        + "\n\nVocê pode fazer isso depois em Configurações > Conta.")
                .setPositiveButton(installed ? "Entrar no YouCine" : "Instalar YouCine", (d, w) -> {
                    if (installed) {
                        Intent i = a.getPackageManager().getLaunchIntentForPackage(YouCineLinkActivity.findYouCine(a));
                        if (i != null) try { a.startActivity(i); } catch (Exception ignored) { }
                    } else {
                        install(a);
                    }
                })
                .setNegativeButton("Agora não", null)
                .show();
    }

    /** Installs YouCine now (from the bundled copy, or downloading it). */
    public static void install(final Activity a) {
        if (busy) {
            Toast.makeText(a, "Aguarde, instalando o YouCine…", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canInstall(a)) {
            Toast.makeText(a, "Ative \"Permitir desta fonte\" para o MegaFlix e escolha YouCine de novo.",
                    Toast.LENGTH_LONG).show();
            try {
                a.startActivity(new Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES",
                        Uri.parse("package:" + a.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                try { a.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)); } catch (Exception ignored) { }
            }
            return;
        }
        busy = true;
        final Context app = a.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        Toast.makeText(a, "Preparando o YouCine…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            PackageInstaller.Session session = null;
            InputStream in = null;
            HttpURLConnection http = null;
            try {
                try {
                    in = app.getAssets().open(ASSET);
                } catch (Exception notBundled) {
                    String url = downloadUrl();
                    if (url == null) throw new IllegalStateException("YouCine não está disponível para download");
                    http = (HttpURLConnection) new URL(url).openConnection();
                    http.setConnectTimeout(15000);
                    http.setReadTimeout(30000);
                    in = http.getInputStream();
                }
                PackageInstaller pi = app.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params =
                        new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                int id = pi.createSession(params);
                session = pi.openSession(id);
                try (OutputStream out = session.openWrite("base.apk", 0, -1)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    session.fsync(out);
                }
                Intent cb = new Intent(app, Result.class);
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= 31) flags |= 0x02000000; // FLAG_MUTABLE: the system fills in the result
                session.commit(PendingIntent.getBroadcast(app, id, cb, flags).getIntentSender());
                session.close();
                session = null;
            } catch (final Exception e) {
                if (session != null) try { session.abandon(); } catch (Exception ignored) { }
                main.post(() -> Toast.makeText(app, "Não foi possível instalar o YouCine: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            } finally {
                try { if (in != null) in.close(); } catch (Exception ignored) { }
                if (http != null) http.disconnect();
                busy = false;
            }
        }).start();
    }

    private static boolean canInstall(Context c) {
        if (Build.VERSION.SDK_INT < 26) return true;
        try { return c.getPackageManager().canRequestPackageInstalls(); } catch (Throwable t) { return true; }
    }

    /** APK attached to the fork's "youcine" release. */
    private static String downloadUrl() {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(RELEASE_API).openConnection();
            c.setRequestProperty("Accept", "application/vnd.github+json");
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            InputStream in = c.getInputStream();
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            in.close();
            JSONArray assets = new JSONObject(b.toString("UTF-8")).optJSONArray("assets");
            for (int i = 0; assets != null && i < assets.length(); i++) {
                JSONObject a = assets.getJSONObject(i);
                if (a.optString("name").toLowerCase().endsWith(".apk")) return a.optString("browser_download_url");
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.disconnect();
        }
        return null;
    }

    /** PackageInstaller result: shows Android's confirm screen, then the outcome. */
    public static class Result extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
            switch (status) {
                case PackageInstaller.STATUS_PENDING_USER_ACTION: {
                    Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                    if (confirm != null) {
                        confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try { context.startActivity(confirm); } catch (Exception ignored) { }
                    }
                    break;
                }
                case PackageInstaller.STATUS_SUCCESS:
                    Toast.makeText(context, "YouCine instalado! Já pode escolher YouCine em Reproduzir.",
                            Toast.LENGTH_LONG).show();
                    break;
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                    Toast.makeText(context, "Instalação do YouCine cancelada", Toast.LENGTH_SHORT).show();
                    break;
                case PackageInstaller.STATUS_FAILURE_CONFLICT:
                    Toast.makeText(context, "Já existe um YouCine com outra assinatura. Desinstale-o e tente de novo.",
                            Toast.LENGTH_LONG).show();
                    break;
                default: {
                    String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                    Toast.makeText(context, "Falha ao instalar o YouCine" + (msg != null ? ": " + msg : ""),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
