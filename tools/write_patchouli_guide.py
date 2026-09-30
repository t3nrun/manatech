import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

write(ROOT / 'data/manatech/patchouli_books/occult_symbols/book.json', {
    'name': 'book.manatech.guide.name', 'landing_text': 'book.manatech.guide.landing',
    'version': 2, 'use_resource_pack': True, 'creative_tab': 'manatech:main', 'show_progress': False,
})
write(ROOT / 'data/manatech/recipe/manatech_guide.json', {
    'type': 'minecraft:crafting_shapeless', 'ingredients': [{'item': 'minecraft:book'}, {'item': 'minecraft:lapis_lazuli'}],
    'result': {'id': 'patchouli:guide_book', 'components': {'patchouli:book': 'manatech:occult_symbols'}},
})
for name, material in [('adamantite_furnace', 'minecraft:furnace'), ('adamantite_drain', 'minecraft:bucket')]:
    write(ROOT / f'data/manatech/recipe/{name}.json', {
        'type': 'minecraft:crafting_shaped', 'pattern': ['AAA', 'ACA', 'AAA'],
        'key': {'A': {'item': 'manatech:adamantite_ingot'}, 'C': {'item': material}},
        'result': {'id': 'manatech:' + name},
    })

def cell(x,y,z):
    borders = int(x in (0,3)) + int(y in (0,3)) + int(z in (0,3))
    return 'B' if borders >= 2 else ' ' if borders == 0 else 'F' if z == 0 else 'D' if z == 3 else 'A'
pattern = [[''.join('0' if (x,y,z)==(1,1,0) else cell(x,y,z) for z in range(4)) for x in range(4)] for y in range(3,-1,-1)]
multiblock = {'name': 'Адамантиевая печь', 'mapping': {'B': 'minecraft:polished_blackstone_bricks', 'A': 'manatech:adamantite_block', 'F': 'manatech:adamantite_furnace[facing=north]', '0': 'manatech:adamantite_furnace[facing=north]', 'D': 'manatech:adamantite_drain[facing=south]'}, 'pattern': pattern, 'symmetrical': False}

RU = {
 'first_spells': ('Первые заклинания', 'magic', 'manatech:rune_stylus', [
   'Поставьте стол начертания. ПКМ стилусом открывает холст. Выберите стихию и слой, ведите мышь по направляющей. После замыкания можно дорисовать любые контуры, линии и кружки своей схемы, а также поставить символы. Ctrl+Z отменяет штрих.',
   'Экспортируйте слой в страницу. ПКМ страницей по столу наложения загружает её в библиотеку и расходует предмет. ПКМ пустой рукой открывает стол: добавляйте копии, перемещайте, меняйте порядок, поворот и масштаб.',
   'ПКМ гримуаром по столу наложения сохраняет заклинание. Откройте книгу, выберите его; Shift+ПКМ в воздух — каст. ПКМ по мановой пластине показывает круг. Это снимок: изменение исходных страниц его не изменяет.']),
 'mana': ('Мана и уровни', 'magic', 'manatech:raw_manastone', [
   'При первом входе уровень выбирается от 1 до 11 и сохраняется. Синий сосуд слева снизу показывает запас. ПКМ магикамнем поглощает ману только до заполнения сосуда, остаток заряда остаётся в камне. Пустой камень расходуется.',
   '1/30 поглощённой маны дополнительно идёт в развитие. Восстановление — 0,5% максимума в секунду, оно не повышает уровень. Уровни: 1–10 человек; 11–20 подмастерье; 21–30 начинающий; 31–40 маг; 41–50 высший; 51–89 император; 90–100 падший.']),
 'elements': ('Шесть стихий', 'magic', 'manatech:rune_grimoire', [
   'Огонь — красный; Вода — синий; Энергия — зелёный; Воздух — золотистый; Пустота — фиолетовый; Земля — коричневый. В прототипе каста Вода лечит, остальные стихии наносят урон. Каталог форм и их особые эффекты ещё развивается.']),
 'modifiers': ('Девять символов', 'marks', 'manatech:rune_layer_page', [
   'Слева направо: печать — Создание; глаз — Движение; спираль — Плотность; солнечное колесо — Охлаждение; крест — Площадь; три кольца — Количество; компас — Защита; полумесяц — Связь; портал — Пространство.',
   'Названия и назначение видны при наведении на палитру. Символы одинаковы на всех слоях и уровнях столов/стилусов. Сейчас исследования ещё не закрывают символы. Вода с Охлаждением задумана как основа льда.']),
 'materials': ('Руды и магикамни', 'tech', 'manatech:deepslate_manastone_ore', [
   'Серебро встречается чаще остальных новых металлов; добывайте каменной киркой. Для орихалка, адамантия, мифрила и магикамня нужна железная кирка. Мифрил редок и встречается в горах. Новые руды генерируются в новых чанках.',
   'Серебро, орихалк и адамантий плавятся в обычной печи или плавильной печи. Мифрил — только в адамантиевой структуре. Девять слитков образуют блок; блок можно вернуть в девять слитков.',
   'Шесть зарядов камней: 128; 16.38k; 2.1m; 268.44m; 34.36b; 4.4t МП. Ручная обработка доступна на столе огранки. Автоматическая обработка и последующие рецепты улучшения пока не активны.']),
 'furnace': ('Адамантиевая печь', 'tech', 'manatech:adamantite_furnace', [
   'Соберите куб 4×4×4: 32 чернитных кирпичей на рёбрах, 16 блоков адамантия на полу, потолке и двух боковых стенах. Полость 2×2×2 оставьте пустой. Спереди — 4 печи 2×2, сзади — 4 стока 2×2.',
   'Все печи смотрят наружу в одну сторону. Стоки — наружу в противоположную. ПКМ пустой рукой по любой печи проверяет сборку; при ошибке показывает координаты и ожидаемый блок. Объёмная схема ниже помогает размещать блоки.',
   'Для нагрева выберите в гримуаре огненный слой с символами Создание, Плотность и Связь. Нужен ранг высшего мага (ур. 41+). ПКМ гримуаром по печи оплачивает 16.38k МП и нагревает все 4 печи на 60 секунд. Это текущий способ запуска «Столба пламени».',
   'ПКМ сырым мифрилом или его рудой по передней печи загружает вход. Одна единица плавится 10 секунд. ПКМ пустой рукой по соответствующему заднему стоку забирает слитки. Shift+ПКМ пустой рукой по печи возвращает сырьё.',
   'Воронка загружает сырьё в печь; воронка под стоком извлекает слитки. Каждый сток связан с печью напротив. Сломанный корпус или заполненный выход останавливают плавку. После остывания нужен новый нагрев.']),
}
EN = {
 'first_spells': ('First spells', ['Use a stylus on an inscription table. Trace a guide, export the layer, then use its page on the overlay table.', 'Open the overlay table with an empty hand. Add copies, move, rotate, resize and reorder them. Use a grimoire on the table to save a snapshot.', 'Select the saved spell in your grimoire. Sneak-use in the air to cast; use on a mana plate to display the circle.']),
 'mana': ('Mana and levels', ['Your initial level 1–11 is chosen once. The blue vessel shows mana. Use a manastone to fill it; unused charge stays in the stone.', '1/30 of absorbed mana also advances your level. Passive recovery restores 0.5% of maximum each second without granting experience.']),
 'elements': ('Six elements', ['Fire, Water, Energy, Air, Void and Earth have shared canvas/world colors. The current casting prototype heals with Water and damages with other elements.']),
 'modifiers': ('Nine symbols', ['From left to right: seal — Creation; eye — Movement; spiral — Density; wheel — Cooling; cross — Area; three rings — Quantity; compass — Defense; crescent — Link; portal — Space.', 'Hover a palette symbol to read its description. Symbols are shared across every layer and tool tier. Research restrictions are not active yet.']),
 'materials': ('Ores and manastones', ['Silver needs a stone pickaxe; other new ores need iron. Mithril is very rare in mountains. New ores appear in new chunks.', 'Silver, orichalcum and adamantite use ordinary smelting. Mithril requires the multiblock furnace. Manual manastone cutting is available; automation and later upgrades are not active yet.']),
 'furnace': ('Adamantite furnace', ['Build a 4×4×4 cube: 32 polished blackstone bricks along its edges, 16 adamantite blocks for the floor, ceiling and two side walls, and an empty 2×2×2 core. Four furnaces face outward at the front; four drains face outward at the rear.', 'Use an empty hand on a furnace to validate the structure. Errors report a position and the required block. Use the projection below for placement.', 'Select a Fire spell with Creation, Density and Link. At level 41+, use its grimoire on a furnace: 16.38k MP heats all four lanes for 60 seconds. Creative mode allows testing with any grimoire.', 'Use raw mithril or its ore on a front furnace. Smelting takes 10 seconds per item. Use the opposite drain to collect output. Sneak-use the furnace with an empty hand to retrieve input.', 'Hoppers insert at furnaces and extract at drains. A broken structure stops processing. Reheat after the flame expires.']),
}
RU['first_spells'][3].insert(1, 'У каждой стихии 12 слоёв, равномерно распределённых по 7 кругам. Слои: I — 1–2; II — 3; III — 4–5; IV — 6–7; V — 8–9; VI — 10; VII — 11–12. Инструменты I–VII открывают всего 2/3/5/7/9/10/12 слоёв. Нужны подходящие стилус и стол; слабый инструмент сохраняет старшие черновики.')
RU['first_spells'][3].insert(2, 'Стихию выбирайте до рисования: Ctrl+Backspace очищает слой для смены схемы. В режиме символов поставьте Создание в центр каждого слоя; остальные символы — в восемь внешних слотов. Без Создания экспорт и каст недоступны.')
EN['first_spells'][1].insert(1, 'Each element has twelve distinct schemes shared by all stylus and table tiers. Choose the element before drawing; Ctrl+Backspace clears a layer. In symbol mode place Creation in the center of every layer and other modifiers in the eight surrounding slots. Creation is required to export and cast.')
EN['first_spells'][1].insert(2, 'Twelve layers are evenly divided across seven circles: I — 1–2; II — 3; III — 4–5; IV — 6–7; V — 8–9; VI — 10; VII — 11–12. Tool tiers unlock 2/3/5/7/9/10/12 layers in total. The weaker stylus or table determines access; older inaccessible drafts are preserved.')
RU['first_spells'][3].append('Стилусы I–VII открывают 2/3/4/5/6/8/8 внешних кружков на слой. Создание в центре отдельно. Рисунки всех описанных заклинаний доступны по кнопке «Схемы» в гримуаре. Доступ к рисованию слоёв требует подходящего стилуса и стола.')
RU['first_spells'][3].append('Для каста нужны все разные слои до границы круга: 2/3/5/7/9/10/12 страниц. Расположите их по порядку и на каждой замкните контур вокруг Создания. Копии не заменяют пропущенный номер. «Выровнять» на столе сортирует и центрирует слои; неполная сборка сохраняется как черновик.')
EN['first_spells'][1].append('Stylus tiers I–VII reveal 2/3/4/5/6/8/8 peripheral rings per layer. Central Creation is separate. The grimoire Schematics button shows reference drawings for the spell catalogue. Drawing requires matching stylus and table tiers.')
EN['first_spells'][1].append('Casting requires every distinct layer through the circle boundary: 2/3/5/7/9/10/12 pages. Order them by number and close a contour around Creation on each. Copies cannot replace missing numbers. Align sorts and centers the overlay. Incomplete compositions can remain drafts.')
RU['processing']=('Огранка магикамня','tech','manatech:manastone_processing_table',[
    'ПКМ по столу обработки открывает огранку. Положите необработанный кластер в левый слот. Нажимайте «Срез», когда светлый маркер находится в зелёной зоне. Требуется шесть точных срезов подряд; промах сбрасывает попытку, сохраняя камень.',
    'Успешная огранка превращает кластер в обработанный магикамень с зарядом 16.38k МП. Ручная обработка не требует маны. Результат появляется сразу после шестого точного среза. При закрытии предметы возвращаются в инвентарь.'])
EN['processing']=('Manastone cutting',[
    'Use the processing table and place a raw cluster in the left slot. Press Cut while the pale marker is inside the green zone. Make six consecutive precise cuts. A miss restarts the attempt without consuming the cluster.',
    'The finished stone holds 16.38k MP. Manual cutting requires no mana. The result appears immediately after the sixth precise cut. Closing returns both slots to your inventory.'])
RU['fire_forms']=('Огонь: круги 1–6','magic','manatech:rune_layer_page',[
    'Схема огня определяется старшим нарисованным слоем стихии, а форма — символами огня на слоях. Круги 1–6 открываются по рангу игрока. Центральное Создание обязательно. Гримуар показывает распознанную форму; Shift+ПКМ в воздух кастует её. Сильные формы расходуют заметно больше маны.',
    'Круг 1: Огонёк — Создание; искры — Количество; нагрев — Плотность. Нагрев пока разжигает потухший костёр под прицелом.',
    'Круг 2: огненный шар — Движение; шквал — Количество + Движение; зона огня — Площадь. Шар летит как снаряд, зона действует несколько секунд.',
    'Круг 3: метеор — Плотность + Движение; огнемёт — Движение; усиленный шквал — Количество + Площадь + Движение.',
    'Круг 4: метеорит — Плотность + Движение; огненное торнадо — Площадь + Движение + Связь и слой Воздуха; длительный шквал — Количество + Площадь + Связь; удерживаемый огнемёт — Движение + Связь.',
    'Круг 5: метеоритный дождь — Количество + Площадь + Плотность + Движение; буря — Площадь + Движение + Связь; столб пламени — Плотность + Связь; пламенный луч — Плотность + Движение. Столб пламени зажигает адамантиевую печь.',
    'Круг 6: дезинтеграция — Плотность + Связь; осколок солнца — Плотность + Пространство; большой взрыв — Площадь + Плотность; святой луч — Плотность + Движение + Защита. Последний даёт краткую защиту игроку.'])
EN['fire_forms']=('Fire: circles 1–6',[
    'The highest authored Fire layer determines the circle. Fire symbols select the form. Circles 1–6 require matching player ranks and central Creation. The grimoire names the resolved form. Sneak-use it to cast. Higher circles cost much more mana.',
    'Circle 1: flame with Creation; sparks with Quantity; heating with Density. Heating currently relights a targeted campfire.',
    'Circle 2: fireball with Movement; barrage with Quantity and Movement; fire zone with Area. Fireballs are projectiles; the zone lasts several seconds.',
    'Circle 3: meteor with Density and Movement; flamethrower with Movement; greater barrage with Quantity, Area and Movement.',
    'Circle 4: meteorite with Density and Movement; tornado with Area, Movement, Link and an Air layer; long barrage with Quantity, Area and Link; held flamethrower with Movement and Link.',
    'Circle 5: meteor rain with Quantity, Area, Density and Movement; storm with Area, Movement and Link; flame pillar with Density and Link; flame beam with Density and Movement. The pillar ignites the adamantite furnace.',
    'Circle 6: heat disintegration with Density and Link; sun shard with Density and Space; large explosion with Area and Density; holy beam with Density, Movement and Defense. The holy beam grants short resistance.'])
RU['furnace'][3].append('ПКМ по передней печи открывает её канал: сырьё слева, слитки справа. Интерфейс показывает готовность структуры, оставшееся время нагрева и прогресс плавки. Предметы остаются в печи после закрытия. Задние стоки и воронки по-прежнему забирают результат.')
EN['furnace'][1].append('Use a front furnace to open its lane: ore on the left, ingots on the right. The menu shows structure validity, remaining heat and smelting progress. Closing preserves items. Rear drains and hoppers still extract output.')
RU['mana'][3].append('Большие числа сокращены: k — тысяча, m — миллион, b — миллиард, t — триллион, qa — квадриллион, qi — квинтиллион. Например, 16.38k МП. Подсказки, сосуд и сообщения используют округлённое отображение; заряд рассчитывается точно.')
EN['mana'][1].append('Large values use k for thousand, m for million, b for billion, t for trillion, qa for quadrillion and qi for quintillion. Example: 16.38k MP. Tooltips, the vessel and messages round displayed values; charge calculations retain full precision.')
for locale in ('ru_ru', 'en_us'):
    base = ROOT / f'assets/manatech/patchouli_books/occult_symbols/{locale}'
    for name in ('marks_01_08', 'marks_09_16', 'marks_17_24'):
        (base / f'entries/{name}.json').unlink(missing_ok=True)
    for category, ru, en, icon in [('magic','Магия и мана','Magic and mana','manatech:rune_grimoire'), ('marks','Символы','Symbols','manatech:rune_layer_page'), ('tech','Материалы и печь','Materials and furnace','manatech:adamantite_furnace')]:
        write(base / f'categories/{category}.json', {'name': ru if locale=='ru_ru' else en, 'description': ru if locale=='ru_ru' else en, 'icon': icon})
    for name,(title,category,icon,texts) in RU.items():
        if locale=='en_us': title,texts=EN[name]
        pages=[]
        for text in texts:
            part=''
            for word in text.split():
                if len(part)+len(word)>330:
                    pages.append({'type':'patchouli:text','text':part}); part=''
                part=(part+' '+word).strip()
            if part: pages.append({'type':'patchouli:text','text':part})
        if name=='furnace':
            mb=dict(multiblock); mb['name']=title
            pages.append({'type':'patchouli:multiblock','name':title,'multiblock':mb,'enable_visualize':True})
            pages += [{'type':'patchouli:crafting','recipe':'manatech:adamantite_furnace'}, {'type':'patchouli:crafting','recipe':'manatech:adamantite_drain'}]
        if name=='first_spells': pages.append({'type':'patchouli:crafting','recipe':'manatech:manatech_guide'})
        write(base / f'entries/{name}.json', {'name':title,'category':'manatech:'+category,'icon':icon,'pages':pages})
    path=ROOT / f'assets/manatech/lang/{locale}.json'
    lang=json.loads(path.read_text(encoding='utf-8-sig'))
    ru=locale=='ru_ru'
    values={
      'screen.manatech.schematic_combined':('Итоговое наложение · нажмите для страниц','Combined overlay · click for pages'),
      'screen.manatech.overlay_align':('Выровнять','Align'),
      'screen.manatech.assembly_count':('Слои: %s / %s','Layers: %s / %s'),
      'message.manatech.circle_rank':('Для заклинания нужен ранг %s','This spell requires rank %s'),
      'message.manatech.assembly_missing':('Не хватает слоя %s; нужны все %s','Missing layer %s; all %s are required'),
      'message.manatech.assembly_order':('Слой %s расположен неверно; порядок 1–%s','Layer %s is out of order; use 1–%s'),
      'message.manatech.assembly_contour':('На слое %s замкните контур вокруг Создания','Close a contour around Creation on layer %s'),
      'message.manatech.assembly_creation':('На слое %s нет центрального Создания','Layer %s is missing central Creation'),
      'message.manatech.assembly_profile':('Нужны страницы со схемой стихии и номером слоя','Pages need an element scheme and layer number'),
      'message.manatech.assembly_energy':('Самостоятельная Энергия начинается с III круга','Standalone Energy starts at circle III'),
      'message.manatech.assembly_mixed':('Смешанные стихии доступны с III круга','Mixed elements start at circle III'),
      'message.manatech.fire_seventh_planned':('Особые формы Огня VII круга ещё не реализованы','Special Fire circle VII forms are not implemented yet'),
      'message.manatech.fire_rank':('Нужен %s-й круг огня','Requires Fire circle %s'),
      'message.manatech.need_stylus':('Откройте стол стилусом','Use a stylus on this table'),
      'message.manatech.symbol_limit':('Этот стилус допускает %s внешних символов + Создание','This stylus allows %s peripheral symbols + Creation'),
      'screen.manatech.processing':('Огранка магикамня','Manastone cutting'),
      'screen.manatech.adamantite_furnace':('Адамантиевая печь','Adamantite furnace'),
      'screen.manatech.cut':('Срез','Cut'),
      'screen.manatech.cut_step':('Грани: %s / %s','Facets: %s / %s'),
      'screen.manatech.cut_hint':('Попадите в зелёную зону','Hit the green zone'),
      'screen.manatech.cut_miss':('Промах — начните заново','Miss — start again'),
      'screen.manatech.cut_done':('Камень обработан','Stone finished'),
      'screen.manatech.furnace_formed':('Структура готова','Structure ready'),
      'screen.manatech.furnace_broken':('Ошибка структуры','Invalid structure'),
      'screen.manatech.furnace_heat':('Нагрев: %s с','Heat: %s s'),
      'screen.manatech.furnace_progress':('Плавка: %s%%','Smelting: %s%%'),
      'screen.manatech.furnace_heat_hint':('Нагрев: ПКМ гримуаром','Heat: use grimoire'),
      'message.manatech.creation_required':('Поместите Создание в центр каждого слоя','Place Creation in the center of every layer'),
      'message.manatech.scheme_in_use':('Сначала очистите слой: Ctrl+Backspace','Clear the layer first: Ctrl+Backspace'),
      'book.manatech.guide.name':('МанаТех: краткий гайд','ManaTech: Quick Guide'),
      'book.manatech.guide.landing':('Магия, мана, материалы и сборка адамантиевой печи.','Magic, mana, materials and the adamantite furnace.'),
      'message.manatech.furnace_error':('Ошибка сборки (%s, %s, %s): требуется %s','Structure error (%s, %s, %s): expected %s'),
      'message.manatech.furnace_status':('Сборка готова | Сырьё: %s | Нагрев: %s с | Плавка: %s%%','Structure ready | Input: %s | Heat: %s s | Progress: %s%%'),
      'message.manatech.furnace_spell':('Нужен ур. 41+ и Огонь с Созданием, Плотностью и Связью','Requires level 41+ and Fire with Creation, Density and Link'),
      'message.manatech.furnace_heated':('Столб пламени: 4 печи нагреты на 60 секунд','Flame pillar: four furnaces heated for 60 seconds'),
      'structure.manatech.brick':('чернитные кирпичи','polished blackstone bricks'),
      'structure.manatech.adamantite':('блок адамантия','adamantite block'),
      'structure.manatech.furnace':('печь, направленная наружу спереди','outward-facing front furnace'),
      'structure.manatech.drain':('сток, направленный наружу сзади','outward-facing rear drain'),
      'structure.manatech.air':('пустое пространство','air'),
      'structure.manatech.loaded':('загруженный чанк','loaded chunk'),
    }
    fire_names={
      'flame':('Огонёк','Flame'), 'sparks':('Искры','Sparks'), 'heat':('Нагрев','Heating'),
      'fireball':('Огненный шар','Fireball'), 'barrage':('Огненный шквал','Fire barrage'), 'zone':('Зона огня','Fire zone'),
      'meteor':('Метеор','Meteor'), 'flamethrower':('Огнемёт','Flamethrower'), 'greater_barrage':('Усиленный шквал','Greater barrage'),
      'meteorite':('Метеорит','Meteorite'), 'tornado':('Огненное торнадо','Fire tornado'), 'long_barrage':('Длительный шквал','Long barrage'), 'held_flame':('Удерживаемый огнемёт','Held flamethrower'),
      'meteor_rain':('Метеоритный дождь','Meteor rain'), 'storm':('Огненная буря','Fire storm'), 'pillar':('Столб пламени','Flame pillar'), 'flame_beam':('Пламенный луч','Flame beam'),
      'disintegration':('Дезинтеграция','Disintegration'), 'sun_shard':('Осколок солнца','Sun shard'), 'big_explosion':('Большой взрыв','Large explosion'), 'holy_beam':('Святой луч','Holy beam'),
    }
    lang.update({'spell.manatech.fire.'+key:name[0 if ru else 1] for key,name in fire_names.items()})
    for key in ('screen.manatech.finish_cut','screen.manatech.cut_cost','screen.manatech.cut_mana'): lang.pop(key,None)
    lang.update({k:v[0 if ru else 1] for k,v in values.items()})
    write(path,lang)
print('Patchouli quick guide, furnace projection and recipes written.')
