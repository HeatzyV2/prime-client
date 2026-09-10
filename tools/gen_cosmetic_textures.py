from PIL import Image, ImageDraw
import math
import os

bases = [
    r"C:\Users\Zorat\Desktop\Plugins MC\Elysia Client\mc-1.21.11\src\main\resources\assets\primeclient\textures\cosmetics",
    r"C:\Users\Zorat\Desktop\Plugins MC\Elysia Client\mc-26.2\src\main\resources\assets\primeclient\textures\cosmetics",
]


def cape(path, bg, accent, highlight, motif="prime"):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for y in range(0, 32):
        for x in range(0, 22):
            t = y / 31.0
            r = int(bg[0] * (1 - t) + accent[0] * t)
            g = int(bg[1] * (1 - t) + accent[1] * t)
            b = int(bg[2] * (1 - t) + accent[2] * t)
            if (x + y) % 5 == 0:
                r = min(255, r + 18)
                g = min(255, g + 12)
                b = min(255, b + 10)
            img.putpixel((x, y), (r, g, b, 255))
    d.rectangle([0, 0, 21, 31], outline=highlight + (255,))
    if motif == "prime":
        d.polygon([(7, 6), (14, 10), (7, 14), (9, 10)], fill=highlight + (255,))
        d.rectangle([8, 14, 12, 24], fill=highlight + (230,))
    elif motif == "star":
        cx, cy = 11, 12
        pts = []
        for i in range(10):
            ang = -math.pi / 2 + i * math.pi / 5
            rad = 6 if i % 2 == 0 else 2.5
            pts.append((cx + rad * math.cos(ang), cy + rad * math.sin(ang)))
        d.polygon(pts, fill=highlight + (255,))
    elif motif == "crimson":
        d.ellipse([5, 8, 17, 20], outline=highlight + (255,), width=2)
        d.line([(11, 10), (11, 22)], fill=highlight + (255,), width=2)
    elif motif == "neon":
        d.rectangle([5, 6, 17, 26], outline=highlight + (255,), width=2)
        for py in range(8, 25, 3):
            d.line([(6, py), (16, py)], fill=highlight + (200,), width=1)
    elif motif == "shadow":
        d.polygon([(11, 6), (16, 16), (11, 26), (6, 16)], fill=highlight + (220,))
    elif motif == "founder":
        d.polygon([(11, 5), (14, 12), (21, 12), (15, 17), (17, 25), (11, 20), (5, 25), (7, 17), (1, 12), (8, 12)], fill=highlight + (255,))
    else:
        d.rectangle([6, 8, 16, 22], outline=highlight + (255,), width=2)
        for px, py in [(9, 11), (13, 14), (10, 17), (14, 19)]:
            img.putpixel((px, py), highlight + (255,))
    img.save(path)


def wings(path, c1, c2, vein):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(2, 8), (28, 4), (30, 28), (8, 34), (2, 22)], fill=c1 + (210,))
    d.polygon([(2, 8), (28, 4), (22, 16), (6, 18)], fill=c2 + (180,))
    d.polygon([(62, 8), (36, 4), (34, 28), (56, 34), (62, 22)], fill=c1 + (210,))
    d.polygon([(62, 8), (36, 4), (42, 16), (58, 18)], fill=c2 + (180,))
    d.line([(4, 12), (26, 10)], fill=vein + (255,), width=1)
    d.line([(6, 20), (28, 18)], fill=vein + (255,), width=1)
    d.line([(60, 12), (38, 10)], fill=vein + (255,), width=1)
    d.line([(58, 20), (36, 18)], fill=vein + (255,), width=1)
    d.ellipse([24, 2, 32, 10], fill=c2 + (160,))
    d.ellipse([32, 2, 40, 10], fill=c2 + (160,))
    img.save(path)


def hat(path, base, accent, tip):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([8, 28, 56, 48], fill=base + (240,))
    d.polygon([(20, 28), (32, 4), (44, 28)], fill=accent + (230,))
    d.ellipse([28, 2, 36, 10], fill=tip + (255,))
    img.save(path)


caps = {
    "cape_prime.png": ((20, 40, 120), (59, 130, 246), (147, 197, 253), "prime"),
    "cape_prime_classic.png": ((20, 40, 120), (59, 130, 246), (147, 197, 253), "prime"),
    "cape_prime_founder.png": ((80, 40, 0), (245, 158, 11), (254, 243, 199), "founder"),
    "cape_prime_neon.png": ((0, 40, 60), (34, 211, 238), (165, 243, 252), "neon"),
    "cape_prime_shadow.png": ((15, 15, 25), (71, 85, 105), (148, 163, 184), "shadow"),
    "cape_star.png": ((80, 55, 10), (255, 215, 0), (255, 248, 180), "star"),
    "cape_crimson.png": ((60, 10, 20), (225, 29, 72), (254, 202, 202), "crimson"),
    "cape_midnight.png": ((15, 15, 45), (99, 102, 241), (199, 210, 254), "midnight"),
}
wset = {
    "wings_ember.png": ((255, 80, 30), (255, 180, 60), (120, 20, 10)),
    "wings_aurora.png": ((34, 211, 238), (167, 139, 250), (8, 60, 80)),
    "wings_inferno.png": ((255, 69, 0), (255, 140, 0), (80, 10, 0)),
    "wings_shadow.png": ((55, 65, 81), (30, 30, 40), (10, 10, 15)),
    "wings_galaxy.png": ((88, 28, 135), (167, 139, 250), (30, 10, 60)),
    "wings_prime.png": ((37, 99, 235), (147, 197, 253), (15, 40, 100)),
}
hset = {
    "hat_crown.png": ((180, 120, 20), (250, 204, 21), (255, 255, 200)),
    "hat_horns.png": ((80, 20, 20), (185, 28, 28), (40, 10, 10)),
    "hat_wizard.png": ((60, 20, 100), (124, 58, 237), (250, 204, 21)),
    "hat_santa.png": ((180, 30, 30), (239, 68, 68), (255, 255, 255)),
    "hat_dev.png": ((20, 80, 40), (34, 197, 94), (190, 242, 100)),
}

for base in bases:
    os.makedirs(base, exist_ok=True)
    for name, (bg, ac, hi, motif) in caps.items():
        cape(os.path.join(base, name), bg, ac, hi, motif)
    for name, (c1, c2, vein) in wset.items():
        wings(os.path.join(base, name), c1, c2, vein)
    for name, (b, a, t) in hset.items():
        hat(os.path.join(base, name), b, a, t)
    print("wrote", base, len(os.listdir(base)), "files")
