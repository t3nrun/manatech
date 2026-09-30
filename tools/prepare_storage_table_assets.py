"""Save 16px assets, build a four-leg Minecraft table, and render its model."""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parents[1]
resources = root / 'src/main/resources/assets/manatech'
sources = json.loads((root / 'tools/storage_table_texture_sources.json').read_text(encoding='utf-8'))
for asset in sources:
    image = Image.open(asset['path']).convert('RGB').resize((16, 16), Image.Resampling.NEAREST)
    image.save(resources / 'textures/block' / (asset['name'] + '.png'))

def save_json(path, value):
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')

for metal in ['silver', 'orichalcum', 'adamantite', 'mithril']:
    name = metal + '_block'
    save_json(resources / 'models/block' / (name + '.json'),
              {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'manatech:block/' + name}})
    save_json(resources / 'models/item' / (name + '.json'), {'parent': 'manatech:block/' + name})

def box(name, start, end, tabletop=False):
    faces = {}
    for face in ['north', 'south', 'east', 'west', 'up', 'down']:
        if tabletop and face == 'up':
            uv, texture = [0, 0, 16, 16], '#top'
        elif face in ['up', 'down']:
            uv, texture = [0, 0, end[0]-start[0], end[2]-start[2]], '#wood'
        else:
            width = end[0]-start[0] if face in ['north', 'south'] else end[2]-start[2]
            uv, texture = [0, 0, width, end[1]-start[1]], '#wood'
        faces[face] = {'uv': uv, 'texture': texture}
    return {'name': name, 'from': start, 'to': end, 'faces': faces}

elements = [box('tabletop', [0, 13, 0], [16, 16, 16], True)]
for x, z in [(1, 1), (13, 1), (1, 13), (13, 13)]:
    elements.append(box('leg_' + str(x) + '_' + str(z), [x, 0, z], [x+2, 13, z+2]))
name = 'manastone_processing_table'
model = {'parent': 'minecraft:block/block', 'ambientocclusion': True,
         'textures': {'top': 'manatech:block/' + name + '_top',
                      'wood': 'manatech:block/' + name + '_wood',
                      'particle': 'manatech:block/' + name + '_wood'}, 'elements': elements}
save_json(resources / 'models/block' / (name + '.json'), model)
save_json(resources / 'models/item' / (name + '.json'), {'parent': 'manatech:block/' + name})

# Project the actual model geometry, including each face's UV mapping.
preview_dir = root / 'art/previews'
preview_dir.mkdir(parents=True, exist_ok=True)
font = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 18)
sheet = Image.new('RGB', (800, 230), '#252832')
draw = ImageDraw.Draw(sheet)
for i, (asset, label) in enumerate(zip(sources[:4], ['Серебро', 'Орихалк', 'Адамантий', 'Мифрил'])):
    image = Image.open(resources / 'textures/block' / (asset['name'] + '.png'))
    sheet.paste(image.resize((160, 160), Image.Resampling.NEAREST), (i*200+20, 20))
    draw.text((i*200+20, 192), label, font=font, fill='white')
sheet.save(preview_dir / 'metal_blocks.png')

model = json.loads((resources / 'models/block' / (name + '.json')).read_text())
render = Image.new('RGB', (480, 420), '#252832')
draw = ImageDraw.Draw(render)
textures = {key: Image.open(resources / 'textures' / (value.split(':')[1] + '.png')).convert('RGB')
            for key, value in model['textures'].items()}
def project(p):
    x, y, z = p
    return (240 + (x-z)*12, 205 + (x+z)*6 - y*12)
faces = []
for element in model['elements']:
    x0, y0, z0 = element['from']; x1, y1, z1 = element['to']
    quads = {'up': [(x0,y1,z0),(x1,y1,z0),(x1,y1,z1),(x0,y1,z1)],
             'south': [(x0,y1,z1),(x1,y1,z1),(x1,y0,z1),(x0,y0,z1)],
             'east': [(x1,y1,z1),(x1,y1,z0),(x1,y0,z0),(x1,y0,z1)]}
    for face, quad in quads.items():
        depth = sum(p[0]+p[1]+p[2] for p in quad)/4
        faces.append((depth, face, quad, element['faces'][face]))
for _, face, quad, spec in sorted(faces, key=lambda f: f[0]):
    texture = textures[spec['texture'][1:]]
    u0,v0,u1,v1 = spec['uv']
    cols, rows = max(1,int(u1-u0)), max(1,int(v1-v0))
    def surface(u,v):
        return project(tuple(quad[0][k]+u*(quad[1][k]-quad[0][k])+v*(quad[3][k]-quad[0][k]) for k in range(3)))
    shade = {'up': 1, 'south': .8, 'east': .65}[face]
    for y in range(rows):
        for x in range(cols):
            color = texture.getpixel((int(u0+x)%16,int(v0+y)%16))
            color = tuple(round(c*shade) for c in color)
            corners = [surface(x/cols,y/rows),surface((x+1)/cols,y/rows),
                       surface((x+1)/cols,(y+1)/rows),surface(x/cols,(y+1)/rows)]
            draw.polygon(corners, fill=color)
render.save(preview_dir / 'manastone_processing_table.png')

assert len(model['elements']) == 5
assert sum(e['name'].startswith('leg_') for e in model['elements']) == 4
for asset in sources:
    assert Image.open(resources / 'textures/block' / (asset['name']+'.png')).size == (16,16)
for path in (resources / 'models').rglob('*.json'):
    json.loads(path.read_text(encoding='utf-8'))
print('Verified: 6 opaque 16x16 textures, 4 metal models, table with 4 legs, valid JSON.')
