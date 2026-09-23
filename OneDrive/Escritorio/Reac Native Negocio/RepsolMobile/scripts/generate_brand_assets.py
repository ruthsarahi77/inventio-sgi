from PIL import Image, ImageDraw

OUTPUTS = [
    ("assets/images/logo-brand.png", 1024),
    ("assets/images/favicon.png", 256),
    ("assets/images/splash-icon.png", 512),
]

ORANGE = (245, 130, 32, 255)

for rel_path, size in OUTPUTS:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    s = float(size)

    outer = [
        (s * 0.50, s * 0.11),
        (s * 0.82, s * 0.28),
        (s * 0.76, s * 0.84),
        (s * 0.50, s * 0.94),
        (s * 0.24, s * 0.84),
        (s * 0.18, s * 0.28),
    ]
    draw.polygon(outer, outline=ORANGE, width=max(18, int(s * 0.085)))

    inner = [
        (s * 0.50, s * 0.20),
        (s * 0.70, s * 0.31),
        (s * 0.66, s * 0.75),
        (s * 0.50, s * 0.83),
        (s * 0.34, s * 0.75),
        (s * 0.30, s * 0.31),
    ]
    draw.polygon(inner, outline=ORANGE, width=max(12, int(s * 0.055)))

    draw.rounded_rectangle(
        (s * 0.38, s * 0.30, s * 0.62, s * 0.78),
        radius=int(s * 0.08),
        outline=ORANGE,
        width=max(14, int(s * 0.04)),
    )
    draw.arc(
        (s * 0.42, s * 0.34, s * 0.76, s * 0.60),
        start=180,
        end=360,
        fill=ORANGE,
        width=max(12, int(s * 0.03)),
    )
    draw.line(
        (s * 0.52, s * 0.47, s * 0.72, s * 0.74),
        fill=ORANGE,
        width=max(12, int(s * 0.03)),
    )
    draw.line(
        (s * 0.52, s * 0.47, s * 0.66, s * 0.47),
        fill=ORANGE,
        width=max(12, int(s * 0.03)),
    )
    draw.polygon(
        [
            (s * 0.52, s * 0.42),
            (s * 0.61, s * 0.42),
            (s * 0.62, s * 0.48),
            (s * 0.52, s * 0.48),
        ],
        fill=(0, 0, 0, 0),
        outline=(0, 0, 0, 0),
    )

    img.save(rel_path)

print("Brand assets generated successfully.")
