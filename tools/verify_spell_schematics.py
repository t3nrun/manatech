import os
import json
from pathlib import Path
from zipfile import ZipFile

root=Path(__file__).resolve().parents[1]
version=next(s.split('=',1)[1].strip() for s in (root/'gradle.properties').read_text().splitlines() if s.startswith('mod_version='))
path='assets/manatech/spell_schematics.json'
source=json.loads((root/'src/main/resources'/path).read_text(encoding='utf-8'))
assert len(source)==134 and len({(r['element'],r['id']) for r in source})==134
with ZipFile(Path(os.environ.get('MANATECH_BUILD_DIR', str(root/'build-verify-3')))/f'libs/manatech-{version}.jar') as jar:
    assert json.loads(jar.read(path))==source
    for cls in ('magic/SpellSchematics','rune/SchematicDiagrams','rune/SymbolGlyph','rune/SpellAssembly','rune/LayerLayout','client/SpellSchematicsScreen'):
        assert f'com/vital/manatech/{cls}.class' in jar.namelist()
    for locale in ('ru_ru','en_us'):
        lang=json.loads(jar.read(f'assets/manatech/lang/{locale}.json'))
        for key in ('screen.manatech.schematic_combined','screen.manatech.overlay_align','screen.manatech.assembly_count','message.manatech.assembly_missing'):
            assert key in lang, key
        for recipe in source:
            assert f"schematic.manatech.{recipe['element']}.{recipe['id']}" in lang
        for element in range(6):
            entry=json.loads(jar.read(f'assets/manatech/patchouli_books/occult_symbols/{locale}/entries/schematics_{element}.json'))
            assert len(entry['pages'])==sum(r['element']==element for r in source)+2
            assert all(len(p['text'])<=340 for p in entry['pages'])
    assert b'SpellSchematicsScreen' in jar.read('com/vital/manatech/client/GrimoireScreen.class')
print('134 packaged schematics, grimoire entry, six Patchouli chapters and both translations verified.')
