"""MegaFlix opening (silent): the ribbon M assembles, a light sweep crosses it, then the camera dives
into the M and it bursts into orange light streaks. Frames only; brand.py encodes them with no audio."""
import math, os, sys
import numpy as np
from PIL import Image, ImageDraw

OUT = sys.argv[1]           # folder for the frames
W, H, FPS, DUR = 1280, 720, 30, 3.6
N = int(FPS * DUR)
os.makedirs(OUT, exist_ok=True)

# Ribbon M geometry (same as the app icon), logo space
LEFT  = [(110,1495),(308,1495),(522,903),(325,903)]
MID   = [(537,1315),(676,1315),(825,903),(688,903)]
FOLD  = [(387,903),(522,903),(672,1315),(537,1315)]
RIGHT = [(690,903),(885,903),(1100,1495),(905,1495)]
LCX, LCY = 605, 1199          # logo centre
ANCHOR = (606, 1180)          # zoom dives here (between the strips)
LOGO_W = 990

def ease_out(t): return 1 - (1 - t) ** 3
def ease_in(t): return t ** 3
def ease_io(t): return 3 * t * t - 2 * t * t * t
def seg(t, a, b): return min(1.0, max(0.0, (t - a) / (b - a)))

c = lambda h: np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], np.float32) / 255
FRONT_TOP, FRONT_BOT = c('FFA126'), c('E06400')
BACK_DARK, BACK = c('6A2A00'), c('B85200')

yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)

rng = np.random.default_rng(7)
STREAKS = []
palette = [c('FF8000'), c('FFB347'), c('FF5A00'), c('FFD08A'), c('E64A00'), c('FF9A1F')]
for _ in range(170):
    STREAKS.append((rng.uniform(-0.55, 0.55), rng.uniform(1, 9), palette[rng.integers(len(palette))],
                    rng.uniform(0.35, 1.0), rng.uniform(0, 1)))


def poly_mask(poly, tf, ss=2):
    m = Image.new('L', (W * ss, H * ss), 0)
    ImageDraw.Draw(m).polygon([(tf(x, y)[0] * ss, tf(x, y)[1] * ss) for x, y in poly], fill=255)
    return np.asarray(m.resize((W, H), Image.BILINEAR), np.float32) / 255


def frame(i):
    t = i / FPS
    img = np.zeros((H, W, 3), np.float32)

    # ---- camera: settle in, hold, then dive ----
    base = 560 / LOGO_W                         # logo 560 px wide on a 1280 frame
    settle = 0.9 + 0.1 * ease_out(seg(t, 0.2, 1.6))
    dive = ease_in(seg(t, 2.15, 3.05))
    scale = base * settle * (1 + 14 * dive)
    ax, ay = ANCHOR
    cx = W / 2 + (LCX - ax) * base * settle * (1 - dive) * 0  # keep centre stable before dive
    def tf(x, y):
        # before the dive the logo centre is at screen centre; during the dive we zoom on ANCHOR
        ox, oy = (LCX + (ax - LCX) * dive), (LCY + (ay - LCY) * dive)
        return (W / 2 + (x - ox) * scale, H / 2 + (y - oy) * scale)

    # ---- warm glow behind the logo ----
    g = 0.55 * ease_out(seg(t, 0.1, 1.0)) * (1 - seg(t, 2.6, 3.1))
    r2 = ((xx - W / 2) / (W * 0.42)) ** 2 + ((yy - H / 2) / (H * 0.5)) ** 2
    img += (np.exp(-r2 * 2.2) * g)[..., None] * c('7A3300')

    # ---- pieces, each revealed in turn (ribbon unrolling) ----
    def reveal_mask(poly, prog, from_bottom):
        if prog <= 0: return None
        pts = [tf(x, y) for x, y in poly]
        ys = [p[1] for p in pts]
        top, bot = min(ys), max(ys)
        lim = bot - (bot - top) * prog if from_bottom else top + (bot - top) * prog
        m = poly_mask(poly, tf)
        edge = 6.0
        if from_bottom: m *= np.clip((yy - lim) / edge + 1, 0, 1)
        else: m *= np.clip((lim - yy) / edge + 1, 0, 1)
        return m

    fade_logo = 1 - seg(t, 2.55, 2.95)
    pieces = [  # poly, start, end, from_bottom, kind
        (RIGHT, 1.00, 1.42, False, 'back'),
        (FOLD, 0.62, 1.00, False, 'back'),
        (LEFT, 0.30, 0.72, True, 'front'),
        (MID, 0.84, 1.22, True, 'front'),
    ]
    for poly, a, b, fb, kind in pieces:
        m = reveal_mask(poly, ease_out(seg(t, a, b)), fb)
        if m is None: continue
        pts = [tf(x, y) for x, y in poly]
        top = min(p[1] for p in pts); bot = max(p[1] for p in pts)
        ty = np.clip((yy - top) / max(1, bot - top), 0, 1)[..., None]
        if kind == 'front':
            col = FRONT_TOP * (1 - ty) + FRONT_BOT * ty
        else:
            # darker near the edge tucked under the front strip
            col = BACK_DARK * (1 - ty) * 0.6 + BACK * (0.4 + 0.6 * ty)
        a3 = (m * fade_logo)[..., None]
        img = img * (1 - a3) + col * a3

    # ---- light sweep across the finished M ----
    sw = seg(t, 1.45, 2.05)
    if 0 < sw < 1:
        band_x = -200 + sw * (W + 400)
        band = np.exp(-(((xx + (yy - H / 2) * 0.35) - band_x) / 60) ** 2)
        allm = np.zeros((H, W), np.float32)
        for poly in (LEFT, MID, FOLD, RIGHT): allm = np.maximum(allm, poly_mask(poly, tf))
        img += (band * allm * 0.45)[..., None]

    # ---- dive: the M bursts into streaks of light ----
    sk = seg(t, 2.3, 3.35)
    if sk > 0:
        spread = 1 + 2.6 * ease_out(sk)
        alpha_all = math.sin(math.pi * min(1, sk * 1.15)) * (1 - seg(t, 3.15, 3.5))
        vfade = np.clip(1 - np.abs(yy[:, :1] - H / 2) / (H * 0.62), 0, 1) ** 0.8
        for x0, wdt, col, inten, ph in STREAKS:
            x = W / 2 + x0 * W * spread * (0.6 + 0.4 * ph)
            w = wdt * (1 + 2.2 * sk)
            prof = np.exp(-((xx[:1, :] - x) / w) ** 2)
            flick = 0.75 + 0.25 * math.sin(12 * t + ph * 6.28)
            img += (vfade * prof)[..., None] * col * inten * alpha_all * flick * 0.55
        core = np.exp(-((xx - W / 2) / (W * 0.08 * spread)) ** 2) * alpha_all * 0.35
        img += (core * vfade)[..., None] * c('FFC27A')

    img = np.clip(img, 0, 1)
    Image.fromarray((img * 255).astype(np.uint8)).save(f'{OUT}/f{i:03d}.png')


if __name__ == '__main__':
    for i in range(N): frame(i)
    print('frames', N)
