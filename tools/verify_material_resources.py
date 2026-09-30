"""Check deployed material references and compare JSON layouts to vanilla 1.21.1."""
import json
import re
from pathlib import Path
from zipfile import ZipFile
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
assets = RES / 'assets/manatech'
block_source = (ROOT / 'src/main/java/com/vital/manatech/block/ModBlocks.java').read_text(encoding='utf-8')
item_source = (ROOT / 'src/main/java/com/vital/manatech/item/ModItems.java').read_text(encoding='utf-8')
names = set(re.findall(r'(?:material|BLOCKS\.register|table)\("([a-z_0-9]+)"', block_source))
items = set(re.findall(r'ITEMS\.register(?:SimpleItem)?\("([a-z_0-9]+)"', item_source)) | names
items |= {glyph.lower() + '_rune' for glyph in re.findall(r'rune\(RuneGlyph\.([A-Z_]+)\)', item_source)}
items |= set(re.findall(r'stylus\("([a-z_0-9]+)"', item_source))
new_blocks = [n for n in names if any(m in n for m in ['silver', 'orichalcum', 'adamantite', 'mithril', 'manastone'])]
for path in RES.rglob('*.json'):
    json.loads(path.read_text(encoding='utf-8-sig'))
for name in new_blocks:
    assert (assets / f'blockstates/{name}.json').is_file(), name
    assert (assets / f'models/block/{name}.json').is_file(), name
    assert (assets / f'models/item/{name}.json').is_file(), name
    assert (RES / f'data/manatech/loot_table/blocks/{name}.json').is_file(), name
for path in (assets / 'models').rglob('*.json'):
    model = json.loads(path.read_text(encoding='utf-8-sig'))
    for texture in model.get('textures', {}).values():
        if texture.startswith('manatech:'):
            assert (assets / 'textures' / (texture.split(':', 1)[1] + '.png')).is_file(), (path, texture)
for name in items:
    if name in new_blocks or 'manastone' in name or name == 'new_star' or name.startswith('raw_') or name.endswith('_ingot'):
        assert (assets / f'models/item/{name}.json').is_file(), name
for path in (RES / 'data/manatech/recipe').glob('*.json'):
    recipe = json.loads(path.read_text(encoding='utf-8-sig'))
    result = recipe.get('result')
    if isinstance(result, dict):
        assert 'item' not in result, (path, 'Obsolete recipe output key')
        if result.get('id', '').startswith('manatech:'):
            assert result['id'].split(':')[1] in items, path
for path in (RES / 'data/manatech/worldgen/configured_feature').glob('*.json'):
    feature = json.loads(path.read_text(encoding='utf-8'))
    for target in feature['config']['targets']:
        assert target['state']['Name'].split(':')[1] in names, path
for metal in ['silver', 'orichalcum', 'adamantite', 'mithril']:
    for texture in [metal + '_ore', 'deepslate_' + metal + '_ore', metal + '_block']:
        with Image.open(assets / f'textures/block/{texture}.png') as image:
            assert image.size == (16, 16), texture
    for texture in ['raw_' + metal, metal + '_ingot']:
        with Image.open(assets / f'textures/item/{texture}.png') as image:
            assert image.size == (16, 16), texture
            assert set(image.getchannel('A').get_flattened_data()) <= {0, 255}, texture
assert not list((RES / 'data/manatech/recipe').glob('mithril*smelting*'))
assert not list((RES / 'data/manatech/recipe').glob('mithril*blasting*'))
version = re.search(r'^mod_version=(.+)$', (ROOT/'gradle.properties').read_text(), re.M).group(1).strip()
with ZipFile(ROOT / f'build-verify-3/libs/manatech-{version}.jar') as jar:
    files = set(jar.namelist())
    for name in new_blocks:
        assert f'assets/manatech/blockstates/{name}.json' in files, name
        assert f'data/manatech/loot_table/blocks/{name}.json' in files, name
    assert 'com/vital/manatech/magic/ManaProgression.class' in files
    assert not any(n.startswith('data/manatech/recipes/') and n.endswith('.json') for n in files)
    assert not any(n.startswith('data/manatech/loot_tables/') and n.endswith('.json') for n in files)
print(f'Material resources: {len(new_blocks)} blocks, models, recipes, features and PNGs verified in source and JAR.')
