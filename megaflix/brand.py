#!/usr/bin/env python3
"""MegaFlix brand images for the Nuvio build (launcher icons, TV banners, logo, wordmark).
Usage: python3 megaflix/brand.py <res-dir> <font.ttf>   (needs pillow + numpy)"""
import numpy as np, os, sys
from PIL import Image, ImageDraw, ImageFont

# Geometry traced from the reference (x 110..1100, y 903..1495)
LEFT  = [(110,1495),(308,1495),(522,903),(325,903)]          # front strip
MID   = [(537,1315),(676,1315),(825,903),(688,903)]          # front strip
FOLD  = [(387,903),(522,903),(672,1315),(537,1315)]          # back (dark) strip
RIGHT = [(690,903),(885,903),(1100,1495),(905,1495)]         # back (dark) strip
X0, Y0, W, H = 110, 903, 990, 592

def lerp(a, b, t):
    return a[None,None,:]*(1-t[...,None]) + b[None,None,:]*t[...,None]

def render_m(width):
    s = 4                                     # supersample
    sc = width*s / W
    ww, hh = int(W*sc), int(H*sc)
    yy, xx = np.mgrid[0:hh, 0:ww].astype(np.float32)
    ty = yy / hh                               # 0 top -> 1 bottom
    img = np.zeros((hh, ww, 4), np.float32)

    def mask(poly):
        m = Image.new('L', (ww, hh), 0)
        ImageDraw.Draw(m).polygon([((x-X0)*sc, (y-Y0)*sc) for x,y in poly], fill=255)
        return np.asarray(m, np.float32)/255

    def paint(poly, rgb):
        a = mask(poly)[..., None]
        img[..., :3] = img[..., :3]*(1-a) + rgb*a
        img[..., 3:] = np.maximum(img[..., 3:], a*255)

    c = lambda h: np.array([int(h[i:i+2],16) for i in (0,2,4)], np.float32)
    # Back strips: darker orange, deepest shadow along the edge tucked under the front strip
    def back_shade(poly, edge_pt_top, edge_pt_bot):
        (x1,y1),(x2,y2) = edge_pt_top, edge_pt_bot
        px, py = xx/sc+X0, yy/sc+Y0
        d = np.abs((x2-x1)*(y1-py)-(x1-px)*(y2-y1))/np.hypot(x2-x1,y2-y1)
        t = np.clip(d/90, 0, 1)
        return lerp(c('6E2E00'), c('B85200'), t)
    paint(RIGHT, back_shade(RIGHT, (825,903),(676,1315)))
    paint(FOLD,  back_shade(FOLD,  (522,903),(308,1495)))
    # Front strips: bright orange at top fading slightly darker at the bottom
    front = lerp(c('FF9A1F'), c('E06800'), ty)
    paint(LEFT, front)
    paint(MID, lerp(c('FF9A1F'), c('D86200'), np.clip(ty/0.7,0,1)))

    out = Image.fromarray(np.clip(img,0,255).astype(np.uint8), 'RGBA')
    return out.resize((width, int(H*width/W)), Image.LANCZOS)

def on_black(size, frac=0.78, radius=0.0):
    bg = Image.new('RGBA', (size,size), (0,0,0,255))
    m = render_m(int(size*frac))
    bg.alpha_composite(m, ((size-m.width)//2, (size-m.height)//2 + int(size*0.01)))
    return bg


OUT = sys.argv[1]
FONT = sys.argv[2]
class _L: pass
logo = _L(); logo.render_m = render_m; logo.on_black = on_black

def font(size):
    f = ImageFont.truetype(FONT, size)
    try: f.set_variation_by_name("Bold")
    except Exception: pass
    return f
def save(img, rel, mode=None):
    p = os.path.join(OUT, rel); os.makedirs(os.path.dirname(p), exist_ok=True)
    (img.convert(mode) if mode else img).save(p, optimize=True)
# launcher icons (RGB, black square)
for d, s in dict(mdpi=48, hdpi=72, xhdpi=96, xxhdpi=144, xxxhdpi=192).items():
    save(logo.on_black(s, 0.80), f"mipmap-{d}/ic_launcher.png", "RGB")
# app mark 1080 (RGB)
save(logo.on_black(1080, 0.62), "drawable-nodpi/app_logo_mark.png", "RGB")
# wordmark 1085x344 RGBA: orange M + white MegaFlix
wm = Image.new("RGBA", (1085, 344), (0, 0, 0, 0))
m = logo.render_m(300); wm.alpha_composite(m, (12, (344 - m.height) // 2))
d = ImageDraw.Draw(wm)
size = 190
while d.textlength("MegaFlix", font=font(size)) > 1085 - 340 - 16: size -= 4
d.text((340, 172), "MegaFlix", font=font(size), fill=(255, 255, 255, 255), anchor="lm")
save(wm, "drawable-nodpi/app_logo_wordmark.png")
# banners
def banner(w, h):
    b = Image.new("RGBA", (w, h), (0, 0, 0, 255))
    mw = int(w * 0.30); m = logo.render_m(mw)
    f = font(int(h * 0.20)); tw = ImageDraw.Draw(b).textlength("MegaFlix", font=f)
    total = mw + int(w * 0.03) + tw; x0 = int((w - total) / 2)
    b.alpha_composite(m, (x0, (h - m.height) // 2))
    ImageDraw.Draw(b).text((x0 + mw + int(w * 0.03), h // 2), "MegaFlix", font=f, fill=(255, 255, 255, 255), anchor="lm")
    return b
save(banner(320, 180), "mipmap-xhdpi/banner.png", "RGB")
save(banner(1920, 1080), "drawable-nodpi/tv_banner.png", "RGB")
save(banner(1280, 720), "drawable/tv_banner.png", "RGB")
print("ok")
