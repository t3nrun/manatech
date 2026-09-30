"""Install the existing art and Minecraft 1.21.1 material data into the mod."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/com/vital/manatech'
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/manatech'
DATA = RES / 'data/manatech'
METALS = {'silver': 'Серебро', 'orichalcum': 'Орихалк', 'adamantite': 'Адамантий', 'mithril': 'Мифрил'}

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def inject(path, anchor, text):
    src = path.read_text(encoding='utf-8')
    if text.strip().splitlines()[0] not in src:
        src = src.replace(anchor, text + '\n' + anchor, 1)
        path.write_text(src, encoding='utf-8')

# The singular data directories are required by Minecraft 1.21.
for old, new in [('recipes', 'recipe'), ('loot_tables', 'loot_table')]:
    for path in (DATA / old).rglob('*.json'):
        data = json.loads(path.read_text(encoding='utf-8-sig'))
        if old == 'recipes' and isinstance(data.get('result'), dict) and 'item' in data['result']:
            data['result']['id'] = data['result'].pop('item')
        write(DATA / new / path.relative_to(DATA / old), data)
        path.unlink()
for path in (RES / 'data/minecraft/tags/blocks').rglob('*.json'):
    write(RES / 'data/minecraft/tags/block' / path.relative_to(RES / 'data/minecraft/tags/blocks'), json.loads(path.read_text(encoding='utf-8-sig')))
    path.unlink()

block_lines, item_lines, blocks, raw_items = [], [], [], []
for metal in METALS:
    for prefix in ('', 'deepslate_'):
        name = prefix + metal + '_ore'
        block_lines.append(f'    public static final DeferredBlock<Block> {name.upper()} = material("{name}", {4.5 if prefix else 3.0}F, false);')
        blocks.append(name)
    name = metal + '_block'
    block_lines.append(f'    public static final DeferredBlock<Block> {name.upper()} = material("{name}", 5.0F, true);')
    blocks.append(name)
    for name in ('raw_' + metal, metal + '_ingot'):
        item_lines.append(f'    public static final DeferredItem<Item> {name.upper()} = ITEMS.registerSimpleItem("{name}");')
        raw_items.append(name)

blocks += ['deepslate_manastone_ore', 'manastone_processing_table', 'adamantite_furnace', 'adamantite_drain']
block_lines += [
    '    public static final DeferredBlock<Block> DEEPSLATE_MANASTONE_ORE = material("deepslate_manastone_ore", 4.5F, false);',
    '    public static final DeferredBlock<Block> MANASTONE_PROCESSING_TABLE = BLOCKS.register("manastone_processing_table", () -> new ProcessingTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion()));',
    '    public static final DeferredBlock<Block> ADAMANTITE_FURNACE = BLOCKS.register("adamantite_furnace", () -> new HorizontalMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(8F, 30F).sound(SoundType.METAL).requiresCorrectToolForDrops()));',
    '    public static final DeferredBlock<Block> ADAMANTITE_DRAIN = BLOCKS.register("adamantite_drain", () -> new HorizontalMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(8F, 30F).sound(SoundType.METAL).requiresCorrectToolForDrops()));',
    '    private static DeferredBlock<Block> material(String id, float hardness, boolean metal) {',
    '        return BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(hardness, 6F).sound(metal ? SoundType.METAL : SoundType.STONE).requiresCorrectToolForDrops()));',
    '    }'
]
inject(JAVA / 'block/ModBlocks.java', '    private ModBlocks()', '\n'.join(block_lines) + '\n')
for name in blocks:
    item_lines.append(f'    public static final DeferredItem<BlockItem> {name.upper()} = ITEMS.registerSimpleBlockItem(ModBlocks.{name.upper()});')
STONES = ['raw_manastone', 'manastone', 'reprocessed_manastone', 'high_grade_manastone', 'dragon_manastone', 'new_star']
for stage, name in enumerate(STONES, 1):
    item_lines.append(f'    public static final DeferredItem<Item> {name.upper()} = ITEMS.register("{name}", () -> new ManastoneItem({stage}, new Item.Properties().stacksTo(1)));')
inject(JAVA / 'item/ModItems.java', '    private ModItems()', '\n'.join(item_lines) + '\n')
inject(JAVA / 'ModTabs.java', '                output.accept(ModItems.MANA_PLATE.get());', '\n'.join(f'                output.accept(ModItems.{name.upper()}.get());' for name in blocks + raw_items + STONES))

def self_loot(name):
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'manatech:' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}

def ore_loot(name, drop):
    return {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
        {'type': 'minecraft:item', 'name': 'manatech:' + name, 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {'predicates': {'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}]},
        {'type': 'minecraft:item', 'name': 'manatech:' + drop, 'functions': [{'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'}, {'function': 'minecraft:explosion_decay'}]}
    ]}]}]}

for name in blocks:
    if name.endswith('_ore'):
        metal = name.removeprefix('deepslate_').removesuffix('_ore')
        write(ASSETS / f'models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'manatech:block/' + name}})
        loot = ore_loot(name, 'raw_' + metal)
    else:
        loot = self_loot(name)
    if name in ('adamantite_furnace', 'adamantite_drain'):
        front = name + '_front'
        write(ASSETS / f'models/block/{name}.json', {'parent': 'minecraft:block/orientable', 'textures': {'top': 'manatech:block/adamantite_machine_top', 'front': 'manatech:block/' + front, 'side': 'manatech:block/adamantite_machine_side'}})
        variants = {f'facing={direction}': {'model': 'manatech:block/' + name, **({'y': angle} if angle else {})} for direction, angle in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}
    else:
        variants = {'': {'model': 'manatech:block/' + name}}
    write(ASSETS / f'blockstates/{name}.json', {'variants': variants})
    write(ASSETS / f'models/item/{name}.json', {'parent': 'manatech:block/' + name})
    write(DATA / f'loot_table/blocks/{name}.json', loot)
for name in raw_items + STONES:
    texture = name if name in raw_items else 'manastone'
    write(ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'manatech:item/' + texture}})

# Explicit starting balance; refine ore frequency after playing with new worlds.
for metal, count, size, bottom, top in [('silver', 8, 8, -32, 96), ('orichalcum', 2, 3, -64, 16), ('adamantite', 2, 3, -64, 16), ('mithril', 1, 2, 32, 256)]:
    targets = []
    for prefix, tag in [('', 'stone_ore_replaceables'), ('deepslate_', 'deepslate_ore_replaceables')]:
        targets.append({'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:' + tag}, 'state': {'Name': f'manatech:{prefix}{metal}_ore'}})
    write(DATA / f'worldgen/configured_feature/{metal}_ore.json', {'type': 'minecraft:ore', 'config': {'size': size, 'discard_chance_on_air_exposure': 0.5 if metal != 'silver' else 0.0, 'targets': targets}})
    placement = [{'type': 'minecraft:rarity_filter', 'chance': 24}] if metal == 'mithril' else [{'type': 'minecraft:count', 'count': count}]
    placement += [{'type': 'minecraft:in_square'}, {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': bottom}, 'max_inclusive': {'absolute': top}}}, {'type': 'minecraft:biome'}]
    write(DATA / f'worldgen/placed_feature/{metal}_ore.json', {'feature': 'manatech:' + metal + '_ore', 'placement': placement})
    write(DATA / f'neoforge/biome_modifier/{metal}_ore.json', {'type': 'neoforge:add_features', 'biomes': '#minecraft:is_mountain' if metal == 'mithril' else '#minecraft:is_overworld', 'features': 'manatech:' + metal + '_ore', 'step': 'underground_ores'})
    if metal != 'mithril':
        for kind, time in [('smelting', 200), ('blasting', 100)]:
            write(DATA / f'recipe/{metal}_ingot_{kind}.json', {'type': 'minecraft:' + kind, 'ingredient': [{'item': 'manatech:raw_' + metal}, {'item': 'manatech:' + metal + '_ore'}, {'item': 'manatech:deepslate_' + metal + '_ore'}], 'result': {'id': 'manatech:' + metal + '_ingot'}, 'experience': 0.7, 'cookingtime': time})
    write(DATA / f'recipe/{metal}_block.json', {'type': 'minecraft:crafting_shaped', 'pattern': ['III', 'III', 'III'], 'key': {'I': {'item': 'manatech:' + metal + '_ingot'}}, 'result': {'id': 'manatech:' + metal + '_block'}})
    write(DATA / f'recipe/{metal}_ingots_from_block.json', {'type': 'minecraft:crafting_shapeless', 'ingredients': [{'item': 'manatech:' + metal + '_block'}], 'result': {'id': 'manatech:' + metal + '_ingot', 'count': 9}})
    for kind, values in [('ores', [metal + '_ore', 'deepslate_' + metal + '_ore']), ('ingots', [metal + '_ingot']), ('raw_materials', ['raw_' + metal]), ('storage_blocks', [metal + '_block'])]:
        write(RES / f'data/c/tags/item/{kind}/{metal}.json', {'replace': False, 'values': ['manatech:' + v for v in values]})

for tag, values in [('mineable/pickaxe', [b for b in blocks if b != 'manastone_processing_table']), ('mineable/axe', ['manastone_processing_table']), ('needs_iron_tool', [b for b in blocks if b not in ['manastone_processing_table'] and 'silver' not in b]), ('needs_stone_tool', [b for b in blocks if 'silver' in b])]:
    path = RES / f'data/minecraft/tags/block/{tag}.json'
    old = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace': False, 'values': []}
    old['values'] = sorted(set(old['values'] + ['manatech:' + v for v in values]))
    write(path, old)
# A first cluster implementation; dedicated cave carving is a separate feature.
write(DATA / 'worldgen/configured_feature/manastone_cluster.json', {'type': 'minecraft:ore', 'config': {'size': 32, 'discard_chance_on_air_exposure': 0.0, 'targets': [{'target': {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'}, 'state': {'Name': 'manatech:deepslate_manastone_ore'}}]}})
write(DATA / 'worldgen/placed_feature/manastone_cluster.json', {'feature': 'manatech:manastone_cluster', 'placement': [{'type': 'minecraft:rarity_filter', 'chance': 32}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': -56}, 'max_inclusive': {'absolute': -8}}}, {'type': 'minecraft:biome'}]})
write(DATA / 'neoforge/biome_modifier/manastone_cluster.json', {'type': 'neoforge:add_features', 'biomes': '#minecraft:is_overworld', 'features': 'manatech:manastone_cluster', 'step': 'underground_ores'})
write(DATA / 'recipe/manastone_processing_table.json', {'type': 'minecraft:crafting_shaped', 'pattern': ['PPP', 'S S', 'S S'], 'key': {'P': {'tag': 'minecraft:planks'}, 'S': {'item': 'minecraft:stick'}}, 'result': {'id': 'manatech:manastone_processing_table'}})

ru = { 'raw_manastone': 'Необработанный кластер магикамня', 'manastone': 'Обработанный магикамень', 'reprocessed_manastone': 'Переработанный магикамень', 'high_grade_manastone': 'Магикамень высшего разряда', 'dragon_manastone': 'Сверхизбыточный магикамень', 'new_star': 'Новая звезда'}
for locale in ('ru_ru', 'en_us'):
    path = ASSETS / f'lang/{locale}.json'
    data = json.loads(path.read_text(encoding='utf-8-sig'))
    russian = locale == 'ru_ru'
    for name in blocks + raw_items + STONES:
        key = ('block' if name in blocks else 'item') + '.manatech.' + name
        label = name.replace('_', ' ').title()
        if russian:
            metal = next((k for k in METALS if k in name), None)
            if name in ru: label = ru[name]
            elif name == 'manastone_processing_table': label = 'Стол обработки магикамней'
            elif name == 'deepslate_manastone_ore': label = 'Магикаменная руда в глубинном сланце'
            elif name == 'adamantite_furnace': label = 'Адамантиевая печь'
            elif name == 'adamantite_drain': label = 'Адамантиевый сток'
            elif metal:
                base = METALS[metal]
                label = ('Руда: ' if name.endswith('_ore') else 'Необработанный металл: ' if name.startswith('raw_') else 'Слиток: ' if name.endswith('_ingot') else 'Блок: ') + base
                if name.startswith('deepslate_'): label += ' (глубинный сланец)'
        data[key] = label
    data.update({
        'element.manatech.earth': 'Земля' if russian else 'Earth',
        'hud.manatech.level': 'Ур. %s' if russian else 'Lv. %s',
        'message.manatech.mana_full': 'Сосуд маны заполнен' if russian else 'Mana vessel is full',
        'message.manatech.mana_absorbed': 'Поглощено %s МП' if russian else 'Absorbed %s MP',
        'message.manatech.level_up': 'Уровень %s — %s' if russian else 'Level %s — %s',
        'item.manatech.manastone.charge': 'Заряд: %s / %s МП' if russian else 'Charge: %s / %s MP',
        'item.manatech.manastone.absorb': 'ПКМ: поглотить ману; 1/30 идёт в развитие' if russian else 'Use: absorb mana; 1/30 also advances your level',
    })
    ranks = ['Обычный человек', 'Подмастерье', 'Начинающий волшебник', 'Маг', 'Высший маг', 'Маг-император', 'Падший'] if russian else ['Ordinary human', 'Apprentice', 'Novice wizard', 'Mage', 'High mage', 'Mage emperor', 'Fallen']
    data.update({f'rank.manatech.{i}': label for i, label in enumerate(ranks, 1)})
    modifiers = {
        'creation': ('Создание', 'Создаёт форму из маны', 'Creation', 'Creates a form from mana'),
        'movement': ('Движение', 'Движение формы или направление потока', 'Movement', 'Moves a form or directs a flow'),
        'density': ('Плотность', 'Концентрирует мощность в меньшем объёме', 'Density', 'Concentrates power into a smaller volume'),
        'cooling': ('Охлаждение', 'Понижает температуру; с Водой создаёт лёд', 'Cooling', 'Lowers temperature; Water can become ice'),
        'area': ('Площадь', 'Распределяет действие по большей области', 'Area', 'Spreads an effect over a larger area'),
        'quantity': ('Количество', 'Разделяет мощность между несколькими формами', 'Quantity', 'Divides power between several forms'),
        'defense': ('Защита', 'Защитная форма или заслон', 'Defense', 'Creates a defensive form or barrier'),
        'link': ('Связь', 'Удерживает форму или связь с целью', 'Link', 'Sustains a form or a link to a target'),
        'space': ('Пространство', 'Размещение и удержание пространственной формы', 'Space', 'Positions and sustains a spatial form'),
    }
    for name, (label, description, english, english_description) in modifiers.items():
        data[f'modifier.manatech.{name}'] = label if russian else english
        data[f'modifier.manatech.{name}.description'] = description if russian else english_description
    write(path, data)
print(f'Installed {len(blocks)} blocks, {len(raw_items) + len(STONES)} items and metal world generation.')
