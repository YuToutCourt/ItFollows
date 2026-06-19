# -*- coding: utf-8 -*-
"""Masque emissif (yeux + bouche) pour geometry.monstre, layout 128x128 identique
a gen_texture.py. Tout transparent sauf les zones qui doivent briller.
A utiliser avec RenderType.eyes (vanilla) ou comme *_glowmask (GeckoLib)."""
import math
from PIL import Image, ImageDraw

W = H = 128
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

EYE_GLOW   = (150, 245, 255, 255)   # cyan glacial
EYE_CORE   = (235, 255, 255, 255)   # coeur quasi blanc
MOUTH_GLOW = (255, 60, 40, 255)     # rouge incandescent
MOUTH_CORE = (255, 150, 60, 255)

def faces(u, v, sx, sy, sz):
    return {
        "up":    (u+sz,        v,      sx, sz),
        "down":  (u+sz+sx,     v,      sx, sz),
        "east":  (u,           v+sz,   sz, sy),
        "front": (u+sz,        v+sz,   sx, sy),
        "west":  (u+sz+sx,     v+sz,   sz, sy),
        "back":  (u+sz+sx+sz,  v+sz,   sx, sy),
    }

def front(u, v, sx, sy, sz):
    fx, fy, fw, fh = faces(u, v, sx, sy, sz)["front"]
    return int(round(fx)), int(round(fy)), int(math.ceil(fw)), int(math.ceil(fh))

# --- YEUX : sur la face avant de la tete (uv 0,0 ; 9x9x9) ---
hx, hy, hw, hh = front(0, 0, 9, 9, 9)
ey = hy + int(hh*0.30); eh = max(2, int(hh*0.22))
for ex in (hx+int(hw*0.18), hx+int(hw*0.58)):
    ew = max(2, int(hw*0.26))
    d.rectangle([ex, ey, ex+ew, ey+eh], fill=EYE_GLOW)
    d.rectangle([ex+1, ey+1, ex+ew-1, ey+eh-1], fill=EYE_CORE)

# yeux aussi sur la couche peau corrompue (uv 32,0 ; 8x8x8) pour couvrir l'inflate
hx, hy, hw, hh = front(32, 0, 8, 8, 8)
ey = hy + int(hh*0.30); eh = max(2, int(hh*0.22))
for ex in (hx+int(hw*0.18), hx+int(hw*0.58)):
    ew = max(2, int(hw*0.26))
    d.rectangle([ex, ey, ex+ew, ey+eh], fill=EYE_GLOW)

# arcades orbitaires (cubes uv 100,40 ; 2.5x2.5x1.5) — petite lueur
hx, hy, hw, hh = front(100, 40, 2.5, 2.5, 1.5)
d.rectangle([hx, hy, hx+hw, hy+hh], fill=EYE_GLOW)

# --- BOUCHE : interieur de la machoire (uv 0,32 ; 7.6x4.5x7) ---
jx, jy, jw, jh = front(0, 32, 7.6, 4.5, 7)
my = jy + int(jh*0.42)
d.rectangle([jx+1, my, jx+jw-1, jy+jh-1], fill=MOUTH_GLOW)
d.rectangle([jx+2, my+1, jx+jw-2, jy+jh-2], fill=MOUTH_CORE)
# gorge qui rougeoie sur la face avant tete (sous le nez/vide)
hx, hy, hw, hh = front(0, 0, 9, 9, 9)
d.rectangle([hx+int(hw*0.46), hy+int(hh*0.55), hx+int(hw*0.56), hy+int(hh*0.78)], fill=MOUTH_GLOW)

img.save("stalker_emissive.png")
print("stalker_emissive.png ecrit (128x128) — yeux cyan + bouche rouge")
