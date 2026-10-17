"""Package design/app-icon-source.png for Android. Requires Pillow."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'android/app/src/main/res'
SOURCE = Image.open(ROOT / 'design/app-icon-source.png').convert('RGBA')
# Remove the exterior margin of the selected original tile, preserving its artwork.
ART = SOURCE.crop((44, 44, 1210, 1210))
BG = '#0B1022'

def tile(size, shape):
    canvas = Image.new('RGBA', (size, size), BG)
    canvas.alpha_composite(ART.resize((size, size), Image.Resampling.LANCZOS))
    if shape:
        mask = Image.new('L', (size, size))
        d = ImageDraw.Draw(mask)
        if shape == 'round':
            d.ellipse((0, 0, size-1, size-1), fill=255)
        else:
            d.rounded_rectangle((0, 0, size-1, size-1), radius=size*.23, fill=255)
        canvas.putalpha(mask)
    return canvas

for density, launcher, foreground in [('mdpi',48,108),('hdpi',72,162),('xhdpi',96,216),('xxhdpi',144,324),('xxxhdpi',192,432)]:
    folder = RES / f'drawable-{density}'
    folder.mkdir(exist_ok=True)
    # Protect the A from adaptive launcher masks with a small inset.
    image = Image.new('RGBA', (foreground, foreground), BG)
    inset = round(foreground * .08)
    image.alpha_composite(tile(foreground - inset*2, None), (inset, inset))
    image.save(folder / 'ic_launcher_foreground.png')
    for suffix, shape in [('', 'square'), ('_round', 'round')]:
        tile(launcher*4, shape).resize((launcher, launcher), Image.Resampling.LANCZOS).save(RES / f'mipmap-{density}/ic_launcher{suffix}.webp', lossless=True)

# Remove vector resources belonging to the rejected concept.
for name in ['ic_launcher_foreground.xml', 'ic_launcher_monochrome.xml']:
    (RES / 'drawable' / name).unlink(missing_ok=True)
for name in ['ic_launcher', 'ic_launcher_round']:
    (RES / f'mipmap-anydpi-v26/{name}.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
''', encoding='utf-8')
(RES / 'drawable/ic_launcher_background.xml').write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="{BG}" android:pathData="M0,0h108v108h-108z" />
</vector>
''', encoding='utf-8')
tile(512, 'square').save(ROOT / 'design/icon-preview.png')
print('Packaged selected artwork for all five densities and adaptive launchers.')
