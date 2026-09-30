"""Normalize generated ore assets and make a contact sheet for review."""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parents[1]
assets = json.loads((root / 'tools/ore_texture_sources.json').read_text(encoding='utf-8'))
preview_dir = root / 'art/previews'
preview_dir.mkdir(parents=True, exist_ok=True)
sheet = Image.new('RGB', (800, 870), '#252832')
draw = ImageDraw.Draw(sheet)
font_path = Path('C:/Windows/Fonts/arial.ttf')
font = ImageFont.truetype(str(font_path), 19)
labels = ['Серебро', 'Орихалк', 'Адамантий', 'Мифрил']
headers = ['Камень', 'Глубинный сланец', 'Сырая руда', 'Слиток']
for col, label in enumerate(headers):
    draw.text((col * 200 + 12, 10), label, font=font, fill='white')
for i, asset in enumerate(assets):
    image = Image.open(asset['path']).convert('RGBA').resize((16, 16), Image.Resampling.NEAREST)
    if asset['kind'] == 'block':
        image = image.convert('RGB')
    else:
        image.putalpha(image.getchannel('A').point(lambda a: 255 if a >= 128 else 0))
        assert image.getchannel('A').getextrema() == (0, 255), asset['name']
    destination = root / 'src/main/resources/assets/manatech/textures' / asset['kind'] / (asset['name'] + '.png')
    image.save(destination)
    reopened = Image.open(destination)
    assert reopened.size == (16, 16), asset['name']
    row, col = divmod(i, 4)
    x, y = col * 200 + 20, row * 205 + 62
    big = image.convert('RGBA').resize((160, 160), Image.Resampling.NEAREST)
    sheet.paste(big, (x, y), big)
    sheet.paste(image.convert('RGBA'), (x + 144, y + 169), image.convert('RGBA'))
    if col == 0:
        draw.text((x, y + 168), labels[row], font=font, fill='white')
    print(asset['name'], reopened.size, reopened.mode)
sheet.save(preview_dir / 'ores_contact_sheet.png')
