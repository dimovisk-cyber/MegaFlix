package com.nuvio.tv.megaflix;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.VideoView;

/**
 * MegaFlix opening. Plays the silent logo animation full screen on top of the app while the
 * home loads underneath, then fades out into the home, like Netflix. Any key skips it.
 * Shown once per cold start, never for deep links (YouCine return, nuvio:// links).
 */
public final class MegaFlixIntro {

    private static boolean shown = false;

    private MegaFlixIntro() { }

    public static void attach(final Activity activity, Bundle savedInstanceState) {
        if (shown || savedInstanceState != null) return;
        if (activity.getIntent() != null && activity.getIntent().getData() != null) return;
        final int res = activity.getResources().getIdentifier("megaflix_intro", "raw", activity.getPackageName());
        if (res == 0) {
            activity.getWindow().getDecorView().postDelayed(() -> YouCineInstaller.offerOnce(activity), 1500);
            return;
        }
        shown = true;
        activity.getWindow().getDecorView().post(new Runnable() {
            @Override public void run() { show(activity, res); }
        });
    }

    private static void show(final Activity activity, int res) {
        if (activity.isFinishing()) return;
        final FrameLayout box = new FrameLayout(activity);
        box.setBackgroundColor(Color.BLACK);
        box.setClickable(true);
        box.setFocusable(true);
        box.setFocusableInTouchMode(true);

        final VideoView video = new VideoView(activity);
        box.addView(video, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        activity.addContentView(box, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        box.requestFocus();

        final boolean[] done = {false};
        final Runnable close = new Runnable() {
            @Override public void run() {
                if (done[0]) return;
                done[0] = true;
                box.animate().alpha(0f).setDuration(650).withEndAction(new Runnable() {
                    @Override public void run() {
                        try { video.stopPlayback(); } catch (Exception ignored) { }
                        ViewGroup parent = (ViewGroup) box.getParent();
                        if (parent != null) parent.removeView(box);
                        ViewGroup content = (ViewGroup) activity.findViewById(android.R.id.content);
                        if (content != null && content.getChildCount() > 0) content.getChildAt(0).requestFocus();
                        YouCineInstaller.offerOnce(activity);
                    }
                }).start();
            }
        };

        box.setOnKeyListener(new View.OnKeyListener() {
            @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP) close.run();
                return true;
            }
        });
        box.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { close.run(); }
        });
        video.setOnPreparedListener(mp -> mp.setVolume(0f, 0f));   // silent, even if a track sneaks in
        video.setOnCompletionListener(mp -> close.run());
        video.setOnErrorListener((mp, what, extra) -> { close.run(); return true; });
        video.setVideoURI(Uri.parse("android.resource://" + activity.getPackageName() + "/" + res));
        video.start();
        box.postDelayed(close, 6500);   // safety net
    }
}
