# -*- coding: utf-8 -*-
"""Genere la texture du monstre 'stalker' (geometry.monstre) en 128x128.
Suit le depliage box-UV Bedrock de body.json : pour un cube uv=[u,v] taille [sx,sy,sz]
  up    : (u+sz,        v)      sx x sz
  down  : (u+sz+sx,     v)      sx x sz
  east  : (u,           v+sz)   sz x sy   (cote)
  front : (u+sz,        v+sz)   sx x sy
  west  : (u+sz+sx,     v+sz)   sz x sy   (cote)
  back  : (u+sz+sx+sz,  v+sz)   sx x sy
"""
import math, random
from PIL import Image, ImageDraw

random.seed(7)
W = H = 128
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()

def clamp(v): return max(0, min(255, int(v)))
def shade(c, f):
    return (clamp(c[0]*f), clamp(c[1]*f), clamp(c[2]*f))
def mix(a, b, t):
    return (a[0]+(b[0]-a[0])*t, a[1]+(b[1]-a[1])*t, a[2]+(b[2]-a[2])*t)

PAL = {
    "skin":     (172, 160, 148),
    "skin_dk":  (120, 108, 100),
    "cloth":    (50, 48, 56),
    "cloth_dk": (32, 30, 38),
    "bone":     (224, 216, 198),
    "blood":    (96, 22, 20),
    "blood_dk": (60, 12, 12),
    "void":     (6, 6, 9),
    "membrane": (56, 40, 46),
    "mem_dk":   (34, 22, 28),
    "vein":     (112, 36, 34),
    "claw":     (212, 202, 182),
}

FACE_SHADE = {"up":1.18, "down":0.58, "front":1.0, "back":0.82, "east":0.9, "west":0.94}

def fill_rect(x, y, w, h, base, face, noise=12, blotch=None):
    """Remplit un rectangle pixel avec couleur ombre + bruit + taches optionnelles."""
    x, y, w, h = int(round(x)), int(round(y)), int(math.ceil(w)), int(math.ceil(h))
    c = shade(base, FACE_SHADE[face])
    for j in range(y, y+h):
        for i in range(x, x+w):
            if 0 <= i < W and 0 <= j < H:
                n = random.randint(-noise, noise)
                col = (clamp(c[0]+n), clamp(c[1]+n), clamp(c[2]+n), 255)
                px[i, j] = col
    if blotch:
        d = ImageDraw.Draw(img)
        for _ in range(blotch[1]):
            bx = random.randint(x, x+w-1); by = random.randint(y, y+max(0,h-2))
            r = random.randint(1, 2)
            bc = shade(blotch[0], FACE_SHADE[face]*random.uniform(0.7,1.0))
            d.ellipse([bx-r, by-r, bx+r, by+r], fill=(bc[0], bc[1], bc[2], 255))

def faces(u, v, sx, sy, sz):
    return {
        "up":    (u+sz,        v,      sx, sz),
        "down":  (u+sz+sx,     v,      sx, sz),
        "east":  (u,           v+sz,   sz, sy),
        "front": (u+sz,        v+sz,   sx, sy),
        "west":  (u+sz+sx,     v+sz,   sz, sy),
        "back":  (u+sz+sx+sz,  v+sz,   sx, sy),
    }

def paint(u, v, sx, sy, sz, mat, detail=None):
    base = PAL[mat]
    for fname, (fx, fy, fw, fh) in faces(u, v, sx, sy, sz).items():
        if fw <= 0 or fh <= 0: continue
        blotch = None
        if mat == "skin":   blotch = (PAL["blood"], max(1, int(fw*fh/26)))
        if mat == "cloth":  blotch = (PAL["cloth_dk"], max(1, int(fw*fh/14)))
        if mat == "bone":   blotch = (PAL["blood_dk"], max(0, int(fw*fh/40)))
        fill_rect(fx, fy, fw, fh, base, fname, blotch=blotch)
        if detail:
            detail(fname, int(round(fx)), int(round(fy)), int(math.ceil(fw)), int(math.ceil(fh)))

d = ImageDraw.Draw(img)

# ---- detail callbacks ----
def head_detail(face, x, y, w, h):
    if face == "front":
        # orbites vides profondes + arcades + ombrage gaunt
        ey = y + int(h*0.30)
        for ex in (x+int(w*0.18), x+int(w*0.58)):
            ew = int(w*0.26); eh = int(h*0.24)
            d.rectangle([ex, ey, ex+ew, ey+eh], fill=PAL["void"]+(255,))
            d.rectangle([ex, ey, ex+ew, ey+1], fill=(2,2,3,255))
        # nez creuse / vide
        nx = x+int(w*0.46)
        d.rectangle([nx, y+int(h*0.55), nx+int(w*0.10), y+int(h*0.78)], fill=PAL["void"]+(255,))
        # joues creusees
        for _ in range(6):
            sx_ = random.randint(x, x+w-1); sy_ = random.randint(y+int(h*0.6), y+h-1)
            d.point((sx_, sy_), fill=PAL["skin_dk"]+(255,))
    if face == "up":
        for _ in range(10):
            d.point((random.randint(x,x+w-1), random.randint(y,y+h-1)), fill=PAL["blood_dk"]+(255,))

def jaw_detail(face, x, y, w, h):
    if face == "front":
        # interieur sombre + rangee de dents en haut
        d.rectangle([x, y+int(h*0.45), x+w, y+h], fill=(14,8,8,255))
        tw = max(1, w//8)
        for k in range(0, w, tw*2):
            d.rectangle([x+k, y+int(h*0.35), x+k+tw-1, y+int(h*0.62)], fill=(206,198,180,255))

def membrane_detail(face, x, y, w, h):
    if face in ("front", "back"):
        # veines rayonnantes depuis le coin epaule (haut interieur)
        ox, oy = x+w-1, y
        for _ in range(5):
            tx = random.randint(x, x+w-1); ty = y+h-1
            d.line([ox, oy, tx, ty], fill=PAL["vein"]+(255,), width=1)
        for _ in range(int(w*h/30)):
            d.point((random.randint(x,x+w-1), random.randint(y,y+h-1)), fill=PAL["mem_dk"]+(255,))

def claw_detail(face, x, y, w, h):
    # pointes noircies en bas
    d.rectangle([x, y+int(h*0.6), x+w, y+h], fill=(30,26,24,255))

# ---- BODY ----
paint(16,16, 9,13,7, "skin")          # torse (chair)
paint(10,80, 9,13,7, "cloth")         # surcouche : vetements dechires
# cote / sang qui coule sur le torse
for k in range(3):
    d.line([20+k*8, 26, 21+k*8, 44], fill=PAL["blood"]+(255,), width=1)
# cage thoracique (cubes ribs)
for (ru,rv) in [(90,60),(90,63),(90,66)]:
    paint(ru,rv, 7,1,0.6, "bone")
paint(90,69, 1.2,8,0.6, "bone")       # sternum

# ---- HEAD ----
paint(0,0, 9,9,9, "skin", head_detail)
paint(32,0, 8,8,8, "skin", head_detail)   # couche peau corrompue
paint(100,40, 2.5,2.5,1.5, "void")        # arcade orbitaire G
paint(100,40, 2.5,2.5,1.5, "void")        # arcade orbitaire D (meme uv, mirror)

# ---- JAW ----
paint(0,32, 7.6,4.5,7, "skin", jaw_detail)

# ---- ARMS ----
paint(40,16, 4.5,14,5, "skin")        # bras (D allonge)
paint(40,34, 4.5,14,5, "cloth")       # manche dechiree
# (le bras gauche partage les memes UV via mirror)

# ---- CLAWS ----
paint(56,48, 2.5,4.5,2, "claw", claw_detail)
paint(56,56, 2,4,1.6, "claw", claw_detail)

# ---- LEGS ----
paint(0,16, 4.2,12.5,5, "skin")
paint(0,33.5, 4.2,12.5,5, "cloth")

# ---- WINGS ----
paint(64,0, 9.5,2.2,2, "bone")            # humerus (os de l'aile)
paint(80,0, 9.5,2,2, "bone")              # avant-bras
paint(0,96, 9.5,7.5,0.5, "membrane", membrane_detail)   # membrane interne
paint(40,96, 9.5,6.5,0.5, "membrane", membrane_detail)  # membrane externe
paint(96,0, 2,3.2,1.6, "claw", claw_detail)             # griffe de l'aile

# ---- taches de sang / vide globales ----
for _ in range(40):
    bx, by = random.randint(0,W-1), random.randint(0,H-1)
    if px[bx,by][3] == 0: continue
    r = random.choice([0,0,1])
    col = random.choice([PAL["blood_dk"], PAL["void"]])
    d.ellipse([bx-r,by-r,bx+r,by+r], fill=col+(255,))

img.save("stalker_texture.png")
print("stalker_texture.png ecrit (128x128)")
