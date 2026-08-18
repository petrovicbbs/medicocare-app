#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generator ilustrovanih pozadina za MediCare skinove.

Za svaki skin (kljuc npr. "skin_bg_lavanda") skripta prvo trazi gotovu sliku u
assets/skins/<kljuc>.{png,jpg,jpeg,webp}. Ako je nadje, samo je cover-crop-uje
(bez izoblicenja/rastezanja) na ciljnu velicinu i snima kao PNG. Ako slika ne
postoji, generise se SVG ilustracija (gradijent + ikonica, kao ranije) i
rasterizuje u PNG istim tim postupkom.

Rasterizacija SVG-a ide DIREKTNO preko librsvg + cairo (ctypes), a ne preko
ImageMagick-ovog `convert` — ImageMagick je na ovom sistemu tiho koristio svoj
interni, ogranicen MSVG kodek umesto pravog rsvg-convert delegata, sto je
lomilo gradijente i opacity. `rsvg-convert` kao CLI alat uopste nije
instaliran u sandboxu, samo deljene biblioteke (librsvg-2.so / libcairo.so),
pa se pozivaju direktno.

Pokretanje:
    python3 gen_skins.py

Ulaz:   <ovaj_folder>/assets/skins/   (opciono, gotove ilustracije)
Izlaz:  <ovaj_folder>/outputs/skins/  (finalni skin_bg_*.png + generisani .svg)
"""
import ctypes
import ctypes.util
import math
import random
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    Image = None

PROJECT_ROOT = Path(__file__).resolve().parent
ASSETS_DIR = PROJECT_ROOT / "assets" / "skins"
OUTPUT_DIR = PROJECT_ROOT / "outputs" / "skins"

W, H = 900, 1600
IMAGE_EXTENSIONS = [".png", ".jpg", ".jpeg", ".webp"]


# ---------------------------------------------------------------------------
# SVG helper elementi (za fallback ilustracije kad nema gotove slike)
# ---------------------------------------------------------------------------

def blob(cx, cy, r, color, opacity):
    return f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{color}" opacity="{opacity}"/>'


def glow(cx, cy, r, color, steps=4, max_op=0.22):
    parts = []
    for i in range(steps, 0, -1):
        rr = r * i / steps
        op = max_op * (1 - i / (steps + 1))
        parts.append(blob(cx, cy, rr, color, round(op, 3)))
    return "\n".join(parts)


def sparkle(cx, cy, s, color, opacity=0.8):
    d = f"M{cx},{cy-s} C{cx+s*0.15},{cy-s*0.15} {cx+s*0.85},{cy-s*0.1} {cx+s},{cy} " \
        f"C{cx+s*0.15},{cy+s*0.1} {cx+s*0.1},{cy+s*0.85} {cx},{cy+s} " \
        f"C{cx-s*0.1},{cy+s*0.15} {cx-s*0.85},{cy+s*0.1} {cx-s},{cy} " \
        f"C{cx-s*0.1},{cy-s*0.1} {cx-s*0.15},{cy-s*0.85} {cx},{cy-s} Z"
    return f'<path d="{d}" fill="{color}" opacity="{opacity}"/>'


def dots_field(cx, cy, spread, n, color, seed):
    r = random.Random(seed)
    parts = []
    for _ in range(n):
        x = cx + r.uniform(-spread, spread)
        y = cy + r.uniform(-spread * 1.4, spread * 1.4)
        rr = r.uniform(3, 7)
        op = r.uniform(0.25, 0.6)
        parts.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{rr:.1f}" fill="{color}" opacity="{op:.2f}"/>')
    return "\n".join(parts)


def icon_pill_heart(accent, light, leaf="#8BC9A6"):
    def leaf_shape(cx, cy, length, width, angle):
        hl = length / 2
        hw = width / 2
        return f'''<g transform="translate({cx},{cy}) rotate({angle})">
          <path d="M0,{-hl:.1f} C {hw:.1f},{-hl*0.42:.1f} {hw:.1f},{hl*0.55:.1f} 0,{hl:.1f} C {-hw:.1f},{hl*0.55:.1f} {-hw:.1f},{-hl*0.42:.1f} 0,{-hl:.1f} Z" fill="{leaf}"/>
          <line x1="0" y1="{-hl+9:.1f}" x2="0" y2="{hl-11:.1f}" stroke="#FFFFFF" stroke-width="2.5" opacity="0.35" stroke-linecap="round"/>
        </g>'''
    return f'''
    <g>
      <ellipse cx="0" cy="150" rx="120" ry="22" fill="#000000" opacity="0.10"/>
      {leaf_shape(-58, 46, 82, 38, -34)}
      {leaf_shape(58, 46, 82, 38, 34)}
      <rect x="-38" y="-140" width="76" height="42" rx="14" fill="{accent}"/>
      <rect x="-70" y="-100" width="140" height="230" rx="42" fill="{light}"/>
      <rect x="-70" y="-100" width="140" height="230" rx="42" fill="none" stroke="{accent}" stroke-width="4" opacity="0.35"/>
      <path d="M0,-8 C -14,-28 -48,-24 -48,4 C -48,28 -18,46 0,62 C 18,46 48,28 48,4 C 48,-24 14,-28 0,-8 Z" fill="{accent}"/>
    </g>'''


def icon_tulip(primary, secondary, white):
    def tulip(x, y, scale, color):
        return f'''
        <g transform="translate({x},{y}) scale({scale})">
          <line x1="0" y1="0" x2="0" y2="120" stroke="#7CB07C" stroke-width="10" stroke-linecap="round"/>
          <path d="M-14,10 C-30,-4 -22,-24 0,-30" stroke="#7CB07C" stroke-width="8" fill="none" stroke-linecap="round"/>
          <g transform="translate(0,-30)">
            <ellipse cx="-22" cy="6" rx="20" ry="34" fill="{color}" transform="rotate(-18 -22 6)"/>
            <ellipse cx="22" cy="6" rx="20" ry="34" fill="{color}" transform="rotate(18 22 6)"/>
            <ellipse cx="0" cy="-6" rx="22" ry="38" fill="{color}"/>
          </g>
        </g>'''
    return (
        tulip(-130, 40, 1.0, secondary) +
        tulip(0, -10, 1.15, white) +
        tulip(130, 40, 1.0, primary)
    )


def icon_cloud_sun(sun_color, cloud_white="#FFFFFF"):
    return f'''
    <g>
      {glow(70, -70, 110, sun_color, steps=4, max_op=0.35)}
      <circle cx="70" cy="-70" r="52" fill="{sun_color}"/>
      <g opacity="0.95">
        <ellipse cx="-40" cy="60" rx="95" ry="55" fill="{cloud_white}"/>
        <ellipse cx="-115" cy="80" rx="60" ry="42" fill="{cloud_white}"/>
        <ellipse cx="35" cy="85" rx="65" ry="42" fill="{cloud_white}"/>
        <ellipse cx="-45" cy="20" rx="55" ry="40" fill="{cloud_white}"/>
      </g>
    </g>'''


def icon_leaf_tree(primary, secondary):
    return f'''
    <g>
      <rect x="-16" y="60" width="32" height="90" rx="10" fill="#8D6E63"/>
      <circle cx="-55" cy="15" r="65" fill="{secondary}" opacity="0.85"/>
      <circle cx="55" cy="15" r="65" fill="{secondary}" opacity="0.85"/>
      <circle cx="0" cy="-55" r="78" fill="{primary}"/>
      <circle cx="-35" cy="-10" r="50" fill="{primary}" opacity="0.9"/>
      <circle cx="35" cy="-10" r="50" fill="{primary}" opacity="0.9"/>
    </g>'''


def icon_sun_wave(sun_color, wave_color="#FFFFFF"):
    waves = ""
    for i, dy in enumerate([70, 105, 138]):
        op = 0.85 - i * 0.22
        waves += f'<path d="M-150,{dy} C-100,{dy-24} -50,{dy+24} 0,{dy} C50,{dy-24} 100,{dy+24} 150,{dy}" stroke="{wave_color}" stroke-width="10" fill="none" stroke-linecap="round" opacity="{op:.2f}"/>'
    return f'''
    <g>
      {glow(0, -60, 100, sun_color, steps=4, max_op=0.32)}
      <circle cx="0" cy="-60" r="58" fill="{sun_color}"/>
      {waves}
    </g>'''


def icon_lavender(primary, secondary):
    def stalk(x, tilt, color, h=150):
        buds = ""
        for i in range(6):
            t = i / 5
            by = -h * t
            bx = 10 * (1 if i % 2 == 0 else -1)
            buds += f'<circle cx="{bx}" cy="{by-20:.1f}" r="9" fill="{color}"/>'
        return f'<g transform="translate({x},60) rotate({tilt})"><line x1="0" y1="0" x2="0" y2="-{h}" stroke="#7CB07C" stroke-width="7" stroke-linecap="round"/>{buds}</g>'
    return (
        stalk(-90, -8, secondary, 150) +
        stalk(-30, 3, primary, 175) +
        stalk(35, -5, secondary, 160) +
        stalk(95, 8, primary, 140)
    )


def icon_citrus(primary, light):
    segs = ""
    for i in range(8):
        a = i * math.pi / 4
        x1 = 14 * math.cos(a)
        y1 = 14 * math.sin(a)
        x2 = 92 * math.cos(a)
        y2 = 92 * math.sin(a)
        segs += f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{light}" stroke-width="5" opacity="0.75"/>'
    return f'''
    <g>
      <circle cx="0" cy="0" r="100" fill="{primary}"/>
      <circle cx="0" cy="0" r="92" fill="{primary}"/>
      {segs}
      <circle cx="0" cy="0" r="16" fill="{light}" opacity="0.9"/>
      <path d="M0,-100 C10,-130 40,-138 55,-118 C40,-108 20,-100 0,-100 Z" fill="#66BB6A"/>
    </g>'''


def icon_moon_stars(moon_color, star_color="#FFFFFF"):
    return f'''
    <g>
      {glow(20, 0, 130, moon_color, steps=4, max_op=0.28)}
      <path d="M 40,-95 A 95,95 0 1,0 40,95 A 68,95 0 1,1 40,-95 Z" fill="{moon_color}"/>
      {sparkle(-140, -110, 16, star_color, 0.9)}
      {sparkle(150, -40, 12, star_color, 0.8)}
      {sparkle(-120, 110, 10, star_color, 0.7)}
      {sparkle(160, 130, 14, star_color, 0.75)}
    </g>'''


def icon_wave_shell(primary, light):
    return f'''
    <g>
      {glow(0, -110, 90, "#FFF3B0", steps=4, max_op=0.3)}
      <circle cx="0" cy="-110" r="46" fill="#FFE58A"/>
      <path d="M-140,40 C-90,10 -40,60 0,30 C40,60 90,10 140,40 L140,90 L-140,90 Z" fill="{primary}" opacity="0.85"/>
      <path d="M-140,90 C-90,65 -40,110 0,85 C40,110 90,65 140,90 L140,150 L-140,150 Z" fill="{primary}"/>
      <path d="M-30,120 C-30,95 -8,80 10,90 C18,72 42,72 50,92 C68,88 82,104 74,122 C80,138 66,152 48,146 C40,158 18,158 12,144 C-6,150 -30,138 -30,120 Z" fill="{light}" opacity="0.95"/>
    </g>'''


def icon_frame_placeholder(accent, light):
    return f'''
    <g>
      <rect x="-110" y="-90" width="220" height="180" rx="18" fill="{light}" stroke="{accent}" stroke-width="6" opacity="0.95"/>
      <circle cx="-55" cy="-40" r="18" fill="{accent}" opacity="0.8"/>
      <path d="M-90,55 L-30,-5 L20,45 L60,5 L90,55 Z" fill="{accent}" opacity="0.55"/>
      <circle cx="80" cy="80" r="34" fill="{accent}"/>
      <path d="M80,66 L80,94 M66,80 L94,80" stroke="{light}" stroke-width="7" stroke-linecap="round"/>
    </g>'''


# ---------------------------------------------------------------------------
# Skin konfiguracija — kljuc = ime drawable resursa (bez ekstenzije).
# "stops"/"blob"/"icon"/"icon_pos" se koriste SAMO kao fallback kada nema
# gotove slike u assets/skins/<kljuc>.{png,jpg,jpeg,webp}.
# ---------------------------------------------------------------------------

SKINS = {
    "skin_bg_podrazumevana": {
        "stops": ["#F2F3F5", "#DDE1E6", "#C7CDD6"],
        "blob": "#FFFFFF",
        "icon": icon_pill_heart("#6B7280", "#FFFFFF", leaf="#9CA9B4"),
        "icon_pos": (450, 560),
    },
    "skin_bg_roze": {
        "stops": ["#FFE1EC", "#FFB8D2", "#F8749C"],
        "blob": "#FFFFFF",
        "icon": icon_tulip("#AD1457", "#F8BBD0", "#FFFFFF"),
        "icon_pos": (450, 620),
    },
    "skin_bg_plava": {
        "stops": ["#CDE7FF", "#9AD0FF", "#5AA9F0"],
        "blob": "#FFFFFF",
        "icon": icon_cloud_sun("#FFD54F"),
        "icon_pos": (450, 560),
    },
    "skin_bg_zelena": {
        "stops": ["#DFF3E6", "#AEE3C4", "#6FC498"],
        "blob": "#FFFFFF",
        "icon": icon_leaf_tree("#2E7D5B", "#8FD8B0"),
        "icon_pos": (450, 580),
    },
    "skin_bg_zuta": {
        "stops": ["#FFF3C4", "#FFD98A", "#FFB74D"],
        "blob": "#FFFFFF",
        "icon": icon_sun_wave("#F9A825"),
        "icon_pos": (450, 560),
    },
    "skin_bg_ljubicasta": {
        "stops": ["#F1E3FA", "#D9B8F0", "#B47DDD"],
        "blob": "#FFFFFF",
        "icon": icon_lavender("#6A1B9A", "#CE93D8"),
        "icon_pos": (450, 600),
    },
    "skin_bg_narandzasta": {
        "stops": ["#FFE3C7", "#FFC08A", "#FF9A4D"],
        "blob": "#FFFFFF",
        "icon": icon_citrus("#FF8F00", "#FFE0B2"),
        "icon_pos": (450, 560),
    },
    "skin_bg_crna": {
        "stops": ["#0D1333", "#1B2452", "#263159"],
        "blob": "#7C8FE0",
        "icon": icon_moon_stars("#FFD54F"),
        "icon_pos": (470, 520),
    },
    "skin_bg_custom_placeholder": {
        "stops": ["#ECEFF1", "#CFD8DC", "#B0BEC5"],
        "blob": "#FFFFFF",
        "icon": icon_frame_placeholder("#607D8B", "#FFFFFF"),
        "icon_pos": (450, 560),
    },
    "skin_bg_svitanje": {
        "stops": ["#8A6FE8", "#6E7FE0", "#3FB6C9"],
        "blob": "#FFFFFF",
        "icon": icon_pill_heart("#3FB6C9", "#FFFFFF", leaf="#8BE0C9"),
        "icon_pos": (450, 560),
        "extra_sparkles": True,
    },
    "skin_bg_more": {
        "stops": ["#BDEBF5", "#6FCBE0", "#1E8FB0"],
        "blob": "#FFFFFF",
        "icon": icon_wave_shell("#1E8FB0", "#FFFFFF"),
        "icon_pos": (450, 560),
    },
}


def build_svg(name, cfg):
    stops = cfg["stops"]
    n = len(stops)
    stop_tags = "\n".join(
        f'<stop offset="{i/(n-1)*100:.0f}%" stop-color="{c}"/>' for i, c in enumerate(stops)
    )
    ix, iy = cfg["icon_pos"]
    blob_color = cfg["blob"]

    extra = ""
    if cfg.get("extra_sparkles"):
        extra += sparkle(140, 260, 14, "#FFFFFF", 0.85)
        extra += sparkle(720, 340, 18, "#FFFFFF", 0.7)
        extra += sparkle(770, 700, 11, "#FFFFFF", 0.6)
        extra += sparkle(110, 780, 13, "#FFFFFF", 0.55)

    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      {stop_tags}
    </linearGradient>
  </defs>
  <rect width="{W}" height="{H}" fill="url(#bg)"/>
  {blob(120, 260, 220, blob_color, 0.10)}
  {blob(W-140, 520, 260, blob_color, 0.08)}
  {blob(80, H-320, 240, blob_color, 0.07)}
  {blob(W-100, H-160, 200, blob_color, 0.06)}
  {dots_field(W/2, H*0.62, 380, 26, blob_color, seed=abs(hash(name)) % 1000)}
  {extra}
  <g transform="translate({ix},{iy}) scale(1.55)">
    {cfg["icon"]}
  </g>
</svg>'''
    return svg


# ---------------------------------------------------------------------------
# Rasterizacija SVG -> PNG direktno preko librsvg + cairo (ctypes), bez
# ImageMagick-a i bez potrebe za rsvg-convert CLI alatom.
# ---------------------------------------------------------------------------

class _RsvgDimensionData(ctypes.Structure):
    _fields_ = [
        ("width", ctypes.c_int),
        ("height", ctypes.c_int),
        ("em", ctypes.c_double),
        ("ex", ctypes.c_double),
    ]


def _load_rsvg_cairo():
    cairo = ctypes.CDLL("libcairo.so.2")
    rsvg = ctypes.CDLL("librsvg-2.so.2")

    cairo.cairo_image_surface_create.restype = ctypes.c_void_p
    cairo.cairo_image_surface_create.argtypes = [ctypes.c_int, ctypes.c_int, ctypes.c_int]
    cairo.cairo_create.restype = ctypes.c_void_p
    cairo.cairo_create.argtypes = [ctypes.c_void_p]
    cairo.cairo_scale.argtypes = [ctypes.c_void_p, ctypes.c_double, ctypes.c_double]
    cairo.cairo_surface_write_to_png.argtypes = [ctypes.c_void_p, ctypes.c_char_p]
    cairo.cairo_surface_write_to_png.restype = ctypes.c_int
    cairo.cairo_destroy.argtypes = [ctypes.c_void_p]
    cairo.cairo_surface_destroy.argtypes = [ctypes.c_void_p]
    cairo.cairo_status.argtypes = [ctypes.c_void_p]
    cairo.cairo_status.restype = ctypes.c_int

    rsvg.rsvg_handle_new_from_file.restype = ctypes.c_void_p
    rsvg.rsvg_handle_new_from_file.argtypes = [ctypes.c_char_p, ctypes.POINTER(ctypes.c_void_p)]
    rsvg.rsvg_handle_render_cairo.restype = ctypes.c_int
    rsvg.rsvg_handle_render_cairo.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
    rsvg.rsvg_handle_get_dimensions.argtypes = [ctypes.c_void_p, ctypes.POINTER(_RsvgDimensionData)]
    rsvg.rsvg_handle_get_dimensions.restype = None

    return cairo, rsvg


def rasterize_svg(svg_path: Path, png_path: Path, out_w: int, out_h: int):
    cairo, rsvg = _load_rsvg_cairo()
    err = ctypes.c_void_p(None)
    handle = rsvg.rsvg_handle_new_from_file(str(svg_path).encode("utf-8"), ctypes.byref(err))
    if not handle:
        raise RuntimeError(f"Ne mogu da ucitam SVG {svg_path}")

    dims = _RsvgDimensionData()
    rsvg.rsvg_handle_get_dimensions(handle, ctypes.byref(dims))
    if dims.width <= 0 or dims.height <= 0:
        raise RuntimeError(f"Neispravne dimenzije SVG-a {svg_path}: {dims.width}x{dims.height}")

    surface = cairo.cairo_image_surface_create(0, out_w, out_h)  # ARGB32
    cr = cairo.cairo_create(surface)
    cairo.cairo_scale(cr, ctypes.c_double(out_w / dims.width), ctypes.c_double(out_h / dims.height))

    if not rsvg.rsvg_handle_render_cairo(handle, cr):
        raise RuntimeError(f"Rasterizacija nije uspela za {svg_path}")
    if cairo.cairo_status(cr) != 0:
        raise RuntimeError(f"Cairo status greska za {svg_path}")
    if cairo.cairo_surface_write_to_png(surface, str(png_path).encode("utf-8")) != 0:
        raise RuntimeError(f"Snimanje PNG-a nije uspelo za {png_path}")

    cairo.cairo_destroy(cr)
    cairo.cairo_surface_destroy(surface)


# ---------------------------------------------------------------------------
# Gotove slike: cover-crop (bez izoblicenja) na ciljnu velicinu
# ---------------------------------------------------------------------------

def find_source_image(key: str) -> Path | None:
    if not ASSETS_DIR.is_dir():
        return None
    for ext in IMAGE_EXTENSIONS:
        candidate = ASSETS_DIR / f"{key}{ext}"
        if candidate.exists():
            return candidate
    return None


def cover_crop_resize(src_path: Path, dst_path: Path, target_w: int, target_h: int):
    if Image is None:
        raise RuntimeError("Pillow (PIL) nije dostupan — potreban za obradu gotovih slika.")
    img = Image.open(src_path)
    if img.mode not in ("RGB", "RGBA"):
        img = img.convert("RGB")
    src_w, src_h = img.size
    target_ratio = target_w / target_h
    src_ratio = src_w / src_h

    if src_ratio > target_ratio:
        # izvor je relativno siri od cilja -> seci levo/desno
        new_h = src_h
        new_w = max(1, round(src_h * target_ratio))
    else:
        # izvor je relativno visi od cilja -> seci gore/dole
        new_w = src_w
        new_h = max(1, round(src_w / target_ratio))

    left = (src_w - new_w) // 2
    top = (src_h - new_h) // 2
    cropped = img.crop((left, top, left + new_w, top + new_h))
    resized = cropped.resize((target_w, target_h), Image.LANCZOS)
    if resized.mode == "RGBA":
        resized = resized.convert("RGB")
    resized.save(dst_path, "PNG")


# ---------------------------------------------------------------------------
# Glavni tok
# ---------------------------------------------------------------------------

def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for key, cfg in SKINS.items():
        dst_png = OUTPUT_DIR / f"{key}.png"
        src_image = find_source_image(key)
        if src_image is not None:
            cover_crop_resize(src_image, dst_png, W, H)
            print(f"[slika]  {key:<28} <- {src_image.name}")
        else:
            svg_content = build_svg(key, cfg)
            svg_path = OUTPUT_DIR / f"{key}.svg"
            svg_path.write_text(svg_content, encoding="utf-8")
            rasterize_svg(svg_path, dst_png, W, H)
            print(f"[svg]    {key:<28} <- generisano")
    print("\nGotovo. Fajlovi su u:", OUTPUT_DIR)


if __name__ == "__main__":
    main()
