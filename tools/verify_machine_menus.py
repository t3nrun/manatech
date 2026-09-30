import os
from pathlib import Path
from zipfile import ZipFile
import json
root=Path(__file__).resolve().parents[1]
version=next(s.split('=',1)[1].strip() for s in (root/'gradle.properties').read_text().splitlines() if s.startswith('mod_version='))
retired=['focus_rune','link_rune','amplify_rune','bind_rune','vent_rune','rune_schematic']
with ZipFile(Path(os.environ.get('MANATECH_BUILD_DIR', str(root/'build-verify-3')))/f'libs/manatech-{version}.jar') as jar:
    names=jar.namelist()
    for id in retired:
        assert not any(p.endswith('/'+id+'.json') or p.endswith('/'+id+'.png') for p in names), f'Retired asset still in JAR: {id}'
    for cls in ['menu/ModMenus','menu/ProcessingMenu','menu/AdamantiteMenu','client/ProcessingScreen','client/AdamantiteScreen','machine/CuttingGame']:
        assert f'com/vital/manatech/{cls}.class' in names
    assert b'RUNE_SCHEMATIC' not in jar.read('com/vital/manatech/item/ModItems.class')
    assert b'FOCUS_RUNE' not in jar.read('com/vital/manatech/item/ModItems.class')
    for locale in ['ru_ru','en_us']:
        lang=json.loads(jar.read(f'assets/manatech/lang/{locale}.json'))
        for key in ['screen.manatech.processing','screen.manatech.cut','screen.manatech.cut_hint','screen.manatech.furnace_heat','message.manatech.symbol_limit']:
            assert key in lang
        assert f'assets/manatech/patchouli_books/occult_symbols/{locale}/entries/processing.json' in names
    assert b'PlayerMana' not in jar.read('com/vital/manatech/menu/ProcessingMenu.class'), 'Manual cutting must not charge mana'
print('Machine menus, guide, translations and removal of six legacy items verified in JAR.')
