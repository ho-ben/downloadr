from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1] / "icons"
SCALE = 4


def generate(size: int, maskable: bool) -> None:
    canvas_size = size * SCALE
    image = Image.new("RGB", (canvas_size, canvas_size), "#f6f2e9")
    draw = ImageDraw.Draw(image)
    inset = 0.18 if maskable else 0.09
    left = int(canvas_size * inset)
    top = int(canvas_size * (0.14 if maskable else 0.075))
    right = int(canvas_size * (1 - inset))
    bottom = int(canvas_size * (0.89 if maskable else 0.925))
    draw.rounded_rectangle(
        (left, top, right, bottom),
        radius=int(canvas_size * 0.14),
        fill="#fffdf8",
        outline="#d9d4c8",
        width=max(4, canvas_size // 80),
    )
    stroke = canvas_size // 13
    draw.line((canvas_size * 0.5, canvas_size * 0.26, canvas_size * 0.5, canvas_size * 0.61), fill="#e95c35", width=stroke)
    draw.line(
        (canvas_size * 0.36, canvas_size * 0.49, canvas_size * 0.5, canvas_size * 0.63, canvas_size * 0.64, canvas_size * 0.49),
        fill="#e95c35",
        width=stroke,
        joint="curve",
    )
    draw.line((canvas_size * 0.33, canvas_size * 0.74, canvas_size * 0.67, canvas_size * 0.74), fill="#315e49", width=canvas_size // 15)
    output = image.resize((size, size), Image.Resampling.LANCZOS)
    prefix = "maskable" if maskable else "icon"
    output.save(ROOT / f"{prefix}-{size}.png", optimize=True)


for icon_size in (192, 512):
    generate(icon_size, False)
    generate(icon_size, True)
