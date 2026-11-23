package com.nuvio.tv.megaflix;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * "MegaFlix Piloto": after MegaFlix hands a title to YouCine, this service presses the buttons a
 * person would press so the title starts playing full screen:
 *   home > Buscar > types the title > opens the match > Assistir > tela cheia
 * When the user backs out of the player, MegaFlix comes back to the front.
 * It acts only inside YouCine and only while a job started from MegaFlix is pending.
 */
public class PilotService extends AccessibilityService {

    public static final String YOUCINE_PKG = "com.world.youcinetv";
    public static final String YOUCINE_MOBILE = "com.world.youcinemobile";

    private static final String PREFS = "megaflix_pilot";
    private static final long JOB_TIMEOUT = 60000;

    // ---- YouCine view ids (from its resources) ----
    private static final String[] YC_SEARCH_ENTRY = {"mIvSearch", "mLlSearch", "mTvSearch",
            "mLlSearchFocus", "mHnbSearchNavBar", "tvSearch", "mSearchAndFilter", "search_button"};
    private static final String[] YC_SEARCH_INPUT = {"etSearchInput", "mEditSearch", "edit_query",
            "search_src_text"};
    private static final String[] YC_SEARCH_GO = {"mBtKeySearch", "search_go_btn"};
    private static final String[] YC_RESULTS = {"mRecyclerResult", "mRvtResult", "mVgvShowResult",
            "mVgvFilterResult", "searchGridview", "mVpSearchResult", "mAllSearchResultPanel", "mLayoutResult"};
    private static final String[] YC_NO_RESULT = {"mTvNoResult", "mVsNoResult", "mTvNoResultTitle"};
    private static final String[] YC_PLAY = {"btn_toPlay", "btn_play_continue"};
    private static final String[] YC_FULLSCREEN = {"mLayoutFullscreen", "mIvFullScreen", "mTvFullScreen"};

    private static final String[] YC_SPORTS = {"mLayoutMatchHome"};
    private static final String[] YC_LIVE = {"mLayoutLiveHome", "mLayoutLive"};
    private static final String[] PLAY_WORDS = {"Reproduzir", "Assistir", "Continuar", "Retomar", "Play", "Resume", "Watch"};

    private static final int STAGE_FIND_SEARCH = 0, STAGE_TYPED = 1, STAGE_OPENED = 2, STAGE_PLAYING = 4;

    private final Handler handler = new Handler(android.os.Looper.getMainLooper());
    private boolean ticking = false;
    private long lastClickAt = 0;
    private long currentJob = 0;
    private final HashSet<String> seenBeforePlay = new HashSet<String>();

    // ==================== API used by MegaFlix ====================

    public static boolean isEnabled(Context c) {
        String flat = new ComponentName(c, PilotService.class).flattenToString();
        String on = Settings.Secure.getString(c.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return on != null && on.toLowerCase().contains(flat.toLowerCase());
    }

    /** app: "youcine" | "youcine_nav:esportes" | "youcine_nav:aovivo". queries: search terms in order of preference. */
    public static void startJob(Context c, String app, List<String> queries, String title, String episode) {
        StringBuilder q = new StringBuilder();
        for (String s : queries) {
            if (s == null || s.trim().length() == 0) continue;
            if (q.length() > 0) q.append('\n');
            q.append(s.trim());
        }
        long now = System.currentTimeMillis();
        c.getSharedPreferences(PREFS, 0).edit()
                .putString("app", app)
                .putString("queries", q.length() > 0 ? q.toString() : title)
                .putString("title", title)
                .putString("episode", episode)
                .putLong("startedAt", now)
                .putInt("stage", STAGE_FIND_SEARCH)
                .putInt("queryIdx", 0)
                .putLong("stageAt", now)
                .apply();
    }

    private static void clearJob(Context c) {
        c.getSharedPreferences(PREFS, 0).edit().clear().apply();
    }

    // ==================== service plumbing ====================

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        kick();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && e.getClassName() != null) {
            String cls = e.getClassName().toString();
            if (cls.contains(".")) onWindowChanged(cls);
        }
        kick();
    }

    @Override
    public void onInterrupt() { }

    private SharedPreferences prefs() { return getSharedPreferences(PREFS, 0); }

    private void kick() {
        if (ticking) return;
        if (prefs().getString("queries", null) == null) return;
        ticking = true;
        handler.postDelayed(tick, 350);
    }

    private final Runnable tick = new Runnable() {
        public void run() {
            ticking = false;
            if (step()) {
                ticking = true;
                handler.postDelayed(this, 600);
            }
        }
    };

    /** YouCine: remember screens seen before play; landing on one again after play = user left the player. */
    private void onWindowChanged(String cls) {
        SharedPreferences p = prefs();
        if (p.getString("queries", null) == null) { seenBeforePlay.clear(); return; }
        long job = p.getLong("startedAt", 0);
        if (job != currentJob) { currentJob = job; seenBeforePlay.clear(); }
        int stage = p.getInt("stage", -1);
        long now = System.currentTimeMillis();
        if (stage != STAGE_PLAYING) { seenBeforePlay.add(cls); return; }
        if (now - p.getLong("stageAt", now) > 5000 && seenBeforePlay.contains(cls)) backToMegaFlix();
    }

    private void backToMegaFlix() {
        clearJob(this);
        seenBeforePlay.clear();
        // Same as tapping the MegaFlix icon: brings the existing task (and the title page) back.
        Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (i == null) i = new Intent(this, com.nuvio.tv.MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try { startActivity(i); } catch (Exception ignored) { }
    }

    // ==================== the pilot ====================

    /** @return true to keep polling */
    private boolean step() {
        SharedPreferences p = prefs();
        String raw = p.getString("queries", null);
        if (raw == null) return false;
        String app = p.getString("app", "youcine");
        String[] queries = raw.split("\n");
        int stage = p.getInt("stage", 0);
        int qi = p.getInt("queryIdx", 0);
        long now = System.currentTimeMillis();
        long stageAt = p.getLong("stageAt", now);
        String title = p.getString("title", "");

        AccessibilityNodeInfo root = getRootInActiveWindow();
        String pkg = root != null && root.getPackageName() != null ? root.getPackageName().toString() : "";
        if (app.startsWith("youcine_nav")) {
            boolean yc = pkg.equals(YOUCINE_PKG) || pkg.equals(YOUCINE_MOBILE);
            if (now - p.getLong("startedAt", now) > 25000) {
                fail("Abra a seção pelo menu do YouCine.");
                return false;
            }
            if (!yc || now - stageAt < 1500) return true;
            AccessibilityNodeInfo target = app.endsWith("esportes") ? first(root, pkg, YC_SPORTS) : first(root, pkg, YC_LIVE);
            if (target == null) target = byExact(root, queries);
            if (target != null && click(target)) { clearJob(this); return false; }
            return true;
        }
        boolean inApp = pkg.equals(YOUCINE_PKG) || pkg.equals(YOUCINE_MOBILE);

        if (stage == STAGE_PLAYING) {
            if (now - stageAt > 4L * 3600 * 1000) { clearJob(this); return false; }
            if (inApp && "youcine".equals(app) && now - stageAt < 6000) tryClick(root, pkg, YC_FULLSCREEN, 2500);
            // Back from the player shows the title page again (its play button) -> back to MegaFlix
            if (inApp && now - stageAt > 5000) {
                AccessibilityNodeInfo play = "youcine".equals(app) ? first(root, pkg, YC_PLAY) : null;
                if (play == null) play = byPrefix(root, PLAY_WORDS);
                if (play != null) { backToMegaFlix(); return false; }
            }
            return now - stageAt < 6000;
        }
        if (now - p.getLong("startedAt", now) > JOB_TIMEOUT) {
            fail("Não consegui abrir \"" + title + "\" sozinho. Continue pelo YouCine.");
            return false;
        }
        if (!inApp) return true;

        return stepYouCine(root, pkg, queries, qi, stage, stageAt, now, title);
    }

    private boolean stepYouCine(AccessibilityNodeInfo root, String pkg, String[] queries, int qi, int stage,
                                long stageAt, long now, String title) {
        String query = queries[Math.min(qi, queries.length - 1)];
        switch (stage) {
            case STAGE_FIND_SEARCH: {
                AccessibilityNodeInfo input = first(root, pkg, YC_SEARCH_INPUT);
                if (input != null) {
                    if (type(root, pkg, input, query)) setStage(STAGE_TYPED);
                    return true;
                }
                if (now - lastClickAt > 1500) {
                    AccessibilityNodeInfo entry = first(root, pkg, YC_SEARCH_ENTRY);
                    if (entry == null) entry = byExact(root, "Buscar", "Pesquisar", "Pesquisa", "Search");
                    if (entry != null && click(entry)) lastClickAt = now;
                }
                return true;
            }
            case STAGE_TYPED: {
                if (now - stageAt < 1500) return true;
                AccessibilityNodeInfo item = pickResult(root, pkg, query);
                if (item != null && click(item)) { setStage(STAGE_OPENED); return true; }
                boolean empty = first(root, pkg, YC_NO_RESULT) != null;
                if (empty || now - stageAt > 7000) {
                    if (qi + 1 < queries.length) {
                        prefs().edit().putInt("queryIdx", qi + 1).apply();
                        setStage(STAGE_FIND_SEARCH);
                    } else {
                        fail("\"" + title + "\" não foi encontrado no YouCine.");
                        return false;
                    }
                }
                return true;
            }
            case STAGE_OPENED: {
                AccessibilityNodeInfo play = first(root, pkg, YC_PLAY);
                if (play == null) play = byPrefix(root, PLAY_WORDS);
                if (play != null && now - stageAt > 800 && click(play)) {
                    lastClickAt = now;
                    setStage(STAGE_PLAYING);
                    return true;
                }
                if (now - stageAt > 12000) {
                    fail("Abri \"" + title + "\" no YouCine. Aperte Assistir para começar.");
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    private void tryClick(AccessibilityNodeInfo root, String pkg, String[] ids, long gapMs) {
        AccessibilityNodeInfo n = first(root, pkg, ids);
        long now = System.currentTimeMillis();
        if (n != null && now - lastClickAt > gapMs && click(n)) lastClickAt = now;
    }

    private void setStage(int s) {
        prefs().edit().putInt("stage", s).putLong("stageAt", System.currentTimeMillis()).apply();
    }

    private void fail(String msg) {
        clearJob(this);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    // ==================== node helpers ====================

    private AccessibilityNodeInfo first(AccessibilityNodeInfo root, String pkg, String[] ids) {
        if (root == null) return null;
        for (String id : ids) {
            List<AccessibilityNodeInfo> l = root.findAccessibilityNodeInfosByViewId(pkg + ":id/" + id);
            if (l == null) continue;
            for (AccessibilityNodeInfo n : l) if (n != null && n.isVisibleToUser()) return n;
        }
        return null;
    }

    private static CharSequence label(AccessibilityNodeInfo n) {
        return n.getText() != null ? n.getText() : n.getContentDescription();
    }

    private AccessibilityNodeInfo byExact(AccessibilityNodeInfo root, String... words) {
        if (root == null) return null;
        for (String w : words) {
            List<AccessibilityNodeInfo> l = root.findAccessibilityNodeInfosByText(w);
            if (l == null) continue;
            for (AccessibilityNodeInfo n : l) {
                if (n == null || !n.isVisibleToUser() || label(n) == null) continue;
                if (norm(label(n).toString()).equals(norm(w))) return n;
            }
        }
        return null;
    }

    /** Button whose label starts with one of the words ("Reproduzir", "Continuar T1 E2"...). */
    private AccessibilityNodeInfo byPrefix(AccessibilityNodeInfo root, String[] words) {
        if (root == null) return null;
        for (String w : words) {
            List<AccessibilityNodeInfo> l = root.findAccessibilityNodeInfosByText(w);
            if (l == null) continue;
            for (AccessibilityNodeInfo n : l) {
                if (n == null || !n.isVisibleToUser() || label(n) == null) continue;
                String t = norm(label(n).toString());
                if (t.startsWith(norm(w)) && t.length() <= norm(w).length() + 16 && isWide(n)) return n;
            }
        }
        return null;
    }

    /** The title page's play button is wide; player icons (play/pause) are small squares. */
    private boolean isWide(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo c = n;
        int depth = 0;
        while (c != null && !c.isClickable() && depth++ < 4) c = c.getParent();
        if (c == null) c = n;
        android.graphics.Rect r = new android.graphics.Rect();
        c.getBoundsInScreen(r);
        int sw = getResources().getDisplayMetrics().widthPixels;
        return r.width() >= sw * 0.12f && r.width() > r.height() * 1.8f;
    }

    private boolean click(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo c = n;
        int depth = 0;
        while (c != null && !c.isClickable() && depth++ < 6) c = c.getParent();
        if (c == null) c = n;
        c.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        return c.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    /** Types the query: directly when the field is editable, else via YouCine's on-screen keys. */
    private boolean type(AccessibilityNodeInfo root, String pkg, AccessibilityNodeInfo input, String q) {
        input.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        Bundle b = new Bundle();
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, q);
        boolean ok = input.isEditable() && input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
        if (!ok) {
            String keys = norm(q);
            int pressed = 0;
            for (int i = 0; i < keys.length(); i++) {
                String ch = String.valueOf(keys.charAt(i));
                AccessibilityNodeInfo k = ch.equals(" ")
                        ? byExact(root, "Espaco", "Space", "␣")
                        : byExact(root, ch, ch.toUpperCase());
                if (k != null && click(k)) pressed++;
            }
            ok = pressed > 0;
        }
        AccessibilityNodeInfo go = first(root, pkg, YC_SEARCH_GO);
        if (go != null) click(go);
        else input.performAction(0x01020054 /* ACTION_IME_ENTER, API 30+ */);
        return ok;
    }

    /** Result whose text best matches the query; otherwise the first result. */
    private AccessibilityNodeInfo pickResult(AccessibilityNodeInfo root, String pkg, String query) {
        AccessibilityNodeInfo box = first(root, pkg, YC_RESULTS);
        if (box == null) return null;
        List<AccessibilityNodeInfo> items = new ArrayList<AccessibilityNodeInfo>();
        collectClickable(box, items, 0);
        if (items.isEmpty()) return null;
        String nq = norm(query).replace(" ", "");
        for (AccessibilityNodeInfo it : items) {
            String t = norm(allText(it, 0)).replace(" ", "");
            if (t.length() > 0 && (t.contains(nq) || nq.contains(t))) return it;
        }
        return items.get(0);
    }

    private void collectClickable(AccessibilityNodeInfo n, List<AccessibilityNodeInfo> out, int d) {
        if (n == null || d > 8) return;
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo c = n.getChild(i);
            if (c == null || !c.isVisibleToUser()) continue;
            if (c.isClickable() || c.isFocusable()) out.add(c);
            else collectClickable(c, out, d + 1);
        }
    }

    private String allText(AccessibilityNodeInfo n, int d) {
        if (n == null || d > 5) return "";
        StringBuilder sb = new StringBuilder();
        if (n.getText() != null) sb.append(n.getText()).append(' ');
        if (n.getContentDescription() != null) sb.append(n.getContentDescription()).append(' ');
        for (int i = 0; i < n.getChildCount(); i++) sb.append(allText(n.getChild(i), d + 1));
        return sb.toString();
    }

    public static String norm(String s) {
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return t.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}
