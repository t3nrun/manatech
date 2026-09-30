import os
import json
from collections import Counter
from pathlib import Path
from zipfile import ZipFile

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
book=json.loads((RES/'data/manatech/patchouli_books/occult_symbols/book.json').read_text(encoding='utf-8'))
assert book['creative_tab']=='manatech:main'
assert 'getBookStack' not in (ROOT/'src/main/java/com/vital/manatech/ModTabs.java').read_text(encoding='utf-8'), 'Guide must not be added manually when Patchouli adds it automatically'
for locale in ('ru_ru','en_us'):
    base=RES/f'assets/manatech/patchouli_books/occult_symbols/{locale}'
    entries=list((base/'entries').glob('*.json'))
    assert not any(p.stem.startswith('marks_') for p in entries)
    for path in entries:
        entry=json.loads(path.read_text(encoding='utf-8'))
        assert (base/f"categories/{entry['category'].split(':')[1]}.json").exists()
        for page in entry['pages']:
            if page['type']=='patchouli:text': assert len(page['text'])<=340
            if 'recipe' in page: assert (RES/f"data/manatech/recipe/{page['recipe'].split(':')[1]}.json").exists()
    furnace=json.loads((base/'entries/furnace.json').read_text(encoding='utf-8'))
    mb=next(p['multiblock'] for p in furnace['pages'] if p['type']=='patchouli:multiblock')
    pattern=mb['pattern']
    assert len(pattern)==4 and all(len(row)==4 and all(len(s)==4 for s in row) for row in pattern)
    counts=Counter(''.join(''.join(row) for row in pattern))
    assert counts==Counter({'B':32,'A':16,'F':3,'0':1,'D':4,' ':8}), counts
    for yi,row in enumerate(pattern):
        y=3-yi
        for x,s in enumerate(row):
            for z,ch in enumerate(s):
                borders=int(x in (0,3))+int(y in (0,3))+int(z in (0,3))
                expected='B' if borders>=2 else ' ' if borders==0 else 'F' if z==0 else 'D' if z==3 else 'A'
                assert (ch=='0' and expected=='F') or ch==expected
    assert mb['mapping']['F'].endswith('[facing=north]') and mb['mapping']['D'].endswith('[facing=south]')
recipe=json.loads((RES/'data/manatech/recipe/manatech_guide.json').read_text())
assert recipe['result']['id']=='patchouli:guide_book'
assert recipe['result']['components']['patchouli:book']=='manatech:occult_symbols'
version=next(line.split('=',1)[1].strip() for line in (ROOT/'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
with ZipFile(Path(os.environ.get('MANATECH_BUILD_DIR', str(ROOT/'build-verify-3')))/f'libs/manatech-{version}.jar') as jar:
    files=jar.namelist()
    assert b'getBookStack' not in jar.read('com/vital/manatech/ModTabs.class'), 'Packaged creative tab still adds the guide manually'
    assert 'com/vital/manatech/block/entity/AdamantiteFurnaceBlockEntity.class' in files
    assert not any('/entries/marks_' in s for s in files)
    assert json.loads(jar.read('data/manatech/recipe/manatech_guide.json'))==recipe
with ZipFile(Path(os.environ.get('MANATECH_BUILD_DIR', str(ROOT/'build-verify-3')))/'libs/Patchouli-1.21.1-93-NEOFORGE.jar') as jar:
    assert 'vazkii/patchouli/common/item/PatchouliDataComponents.class' in jar.namelist()
print('Guide: both locales, 4x4x4 projection, recipes, page lengths and both JARs passed.')
