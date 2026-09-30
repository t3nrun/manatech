"""Build the reference catalogue from the agreed concept and current Fire recipes."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'src/main/java/com/vital/manatech'
RES=ROOT/'src/main/resources/assets/manatech'
concept=(ROOT/'MAGIC_CONCEPT.md').read_text(encoding='utf-8-sig')
modifiers=['Создание','Движение','Плотность','Охлаждение','Площадь','Количество','Защита','Связь','Пространство']
recipes=[]
def add(element,id,name,circle,symbols,extra=-1,ready=False):
    recipes.append(dict(element=element,id=id,name=name,circle=circle,symbols=sorted(set([0]+symbols)),extra=extra,ready=ready))
for element,heading in [(3,'Воздух'),(1,'Вода'),(5,'Земля —')]:
    table=concept.split('| Круг | '+heading,1)[1].split('\n\n',1)[0]
    for line in table.splitlines():
        m=re.match(r'\| (\d) \| (.*) \|',line)
        if not m: continue
        circle=int(m[1])
        for index,form in enumerate(m[2].split('; ')):
            match=re.search(r'\(([^()]*)\)\s*$',form)
            assert match,form
            symbols=[i for i,s in enumerate(modifiers) if s in match[1]]
            name=form[:match.start()].split(':',1)[0].split(', ледяной',1)[0].strip()
            add(element,f'c{circle}_{index}',name,circle,symbols)
            if ', ледяной вариант' in form:
                add(element,f'c{circle}_{index}_ice','Ледяной залп' if circle==4 else 'Ледяная крепость',circle,symbols+[3])
source=(JAVA/'magic/FireSpells.java').read_text(encoding='utf-8')
ru=json.loads((RES/'lang/ru_ru.json').read_text(encoding='utf-8-sig'))
for enum,circle,id,args in re.findall(r'(\w+)\((\d),"(\w+)",([^)]*)\)',source):
    add(0,id,ru['spell.manatech.fire.'+id],int(circle),[int(x.strip()) for x in args.split(',')],3 if id=='tornado' else -1,True)
for circle in (3,4): add(0,f'enhanced_ball_{circle}','Усиленный огненный шар',circle,[1,2])
add(0,'supernova','Сверхновая',7,[2,4])
add(0,'white_star','Белая звезда',7,[2,7,8])
add(0,'new_star','Основа новой звезды',7,[2,7,8])
for circle,id,name,symbols in [
    (1,'absorb','Поглощение',[]),(1,'mote','Сгусток Пустоты',[2]),(1,'push','Импульс Пустоты',[1]),
    (2,'decompose','Разложение',[2,1]),(2,'veil','Завеса Пустоты',[4]),(2,'fragments','Осколки Пустоты',[5]),
    (3,'storage','Хранилище',[2,7]),(3,'absorb_link','Удерживаемое поглощение',[7,2]),(3,'anchor','Пустотный якорь',[7,1]),
    (4,'beam','Луч Пустоты',[2,1]),(4,'beam_link','Удерживаемый луч Пустоты',[2,1,7]),(4,'screen','Пустотный заслон',[6,4]),
    (5,'storage_space','Расширенное хранилище',[2,7,8]),(5,'decompose_area','Разложение области',[4,7,2]),(5,'isolation','Изоляционная оболочка',[6,8,7]),
    (6,'vortex','Пустотный вихрь',[4,1,2,7]),(6,'prison','Пустотная тюрьма',[8,2,7]),(6,'phase_barrier','Фазовый барьер',[6,8,2]),
    (7,'abyss_core','Ядро бездны',[2,8,7]),(7,'unravel','Великое разложение',[4,2,8]),(7,'bastion','Бастион Пустоты',[6,8,7,2])]:
    add(4,id,name,circle,symbols)
for circle,id,name,symbols in [(3,'stabilize','Стабилизация',[]),(3,'rotate','Вращение',[1]),(3,'lighten','Облегчение',[2]),(4,'stabilize_link','Управляемая стабилизация',[7,6]),(4,'burst','Всплеск энергии',[4,2]),(4,'beam','Энергетический луч',[1,7]),(4,'load','Управление нагрузкой',[1,2]),(4,'hover','Парение',[1,2,7]),(4,'transfer','Передача энергии',[1,7])]:
    add(2,id,name,circle,symbols)
for circle,id,name,symbols in [
    (5,'network','Распределение энергии',[5,7,1]),(5,'containment','Оболочка стабилизации',[6,4,7]),(5,'focused_beam','Сфокусированный луч',[2,1,7]),
    (6,'reactor','Реакторное поле',[8,2,7]),(6,'levitation_field','Поле левитации',[1,4,2,7]),(6,'cascade','Энергетический каскад',[5,1,2,7]),
    (7,'core','Совершенное ядро',[8,2,6,7]),(7,'grid','Великая энергетическая сеть',[8,5,7,1]),(7,'resonance','Резонансный импульс',[2,4,1,7])]:
    add(2,id,name,circle,symbols)
for id,name,element,extra,symbols in [('magma','Магма',0,5,[2]),('steam','Пар',0,1,[]),('earth_projectile','Земляной снаряд с Воздухом',5,3,[1]),('mud','Грязь',5,1,[])]:
    add(element,id,name,3,symbols,extra)
recipes.sort(key=lambda r:(r['element'],r['circle'],r['id']))
counts={}
for r in recipes:
    pair=(r['element'],r['circle']);r['design']=counts.get(pair,0);counts[pair]=r['design']+1
(RES/'spell_schematics.json').write_text(json.dumps(recipes,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
entries=[]
for r in recipes:
    entries.append('        new Recipe("'+r['id']+'", '+str(r['element'])+', '+str(r['circle'])+', List.of('+','.join(map(str,r['symbols']))+'), '+str(r['extra'])+', '+str(r['ready']).lower()+', '+str(r['design'])+')')
catalog='''package com.vital.manatech.magic;

import java.util.List;
import com.vital.manatech.rune.RuneTier;

/** Reference recipes, not unlocks or free authored pages. Generated from the agreed catalogue. */
public final class SpellSchematics {
    public record Recipe(String id,int element,int circle,List<Integer> symbols,int extraElement,boolean implemented,int design) {
        public String key() { return "schematic.manatech."+element+"."+id; }
        public int layer() { int start=RuneTier.firstLayer(circle);return start+design%(RuneTier.layers(circle)-start); }
        public int toolTier() { return Math.max(1,Math.max(circle, symbols.size()<=3?1:symbols.size()==4?2:symbols.size()==5?3:4)); }
        public List<Integer> slots() {
            var visible=RuneTier.visibleSlots(toolTier());
            return java.util.stream.IntStream.range(0,symbols.size()-1).mapToObj(i -> visible.get((i+design)%visible.size())).toList();
        }
    }
    private SpellSchematics() {}
    public static final List<Recipe> ALL=List.of(
'''+',\n'.join(entries)+');\n}\n'
(JAVA/'magic/SpellSchematics.java').write_text(catalog,encoding='utf-8')
# Reuse the exact nine glyphs in the editor and reference previews.
canvas=JAVA/'client/RuneCanvasScreen.java'
text=canvas.read_text(encoding='utf-8')
if 'private GuidePath sigilLine' in text:
    helpers=text[text.index('    private GuidePath sigilLine'):text.index('    private double pathLength')]
    ring=text[text.index('    private GuidePath ring('):text.index('    private GuidePath polygonPath')]
    glyph='package com.vital.manatech.rune;\nimport java.util.ArrayList;\nimport java.util.List;\npublic final class SymbolGlyph {\n    public record Point(float x,float y) {}\n    public record Path(List<Point> vertices,boolean closed) {}\n    private SymbolGlyph() {}\n'+ring+helpers+'}\n'
    glyph=glyph.replace('GuidePath','Path').replace('private Path','private static Path').replace('private List<Path> occultPaths','public static List<Path> paths')
    glyph=glyph.replace('return linePath(x + r*x1, y + r*y1, x + r*x2, y + r*y2);','return new Path(List.of(new Point(x+r*x1,y+r*y1),new Point(x+r*x2,y+r*y2)),false);')
    (JAVA/'rune/SymbolGlyph.java').write_text(glyph,encoding='utf-8')
    start=text.index('    private GuidePath sigilLine');end=text.index('    private double pathLength')
    text=text[:start]+'''    private List<GuidePath> occultPaths(float x,float y,float r,int id) {
        return com.vital.manatech.rune.SymbolGlyph.paths(x,y,r,id).stream()
            .map(path -> new GuidePath(path.vertices().stream().map(p -> new Point(p.x(),p.y())).toList(),path.closed())).toList();
    }
'''+text[end:]
    canvas.write_text(text,encoding='utf-8')
translations={
 'screen.manatech.schematics':('Схемы заклинаний','Spell schematics'),
 'screen.manatech.schematic_circle':('Круг %s · стилус и стол %s+','Circle %s · stylus and table %s+'),
 'screen.manatech.schematic_unranked':('Круг ещё не назначен','Circle is not assigned yet'),
 'screen.manatech.schematic_layer':('Страница %s/%s · слой %s','Page %s/%s · authored layer %s'),
 'screen.manatech.schematic_ready':('Форма каста реализована','Casting form implemented'),
 'screen.manatech.schematic_planned':('Схема есть · особый эффект в плане','Reference scheme · specific effect planned'),
 'screen.manatech.schematic_future':('7-й круг: схема для планирования','Circle 7: planning reference'),
 'screen.manatech.schematic_hint':('Колесо: увеличение схемы. Страницы рисуйте вручную.','Scroll to zoom. Draw the reference pages by hand.'),
 'screen.manatech.schematic_back':('Назад','Back'),
}
english_rows={
3:['Wind stream','Gust','Air cushion','Air strike','Wind screen','Wind jump','Wind blade','Whirlwind','Air shackles','Air dome','Blade barrage','Air step','Hurricane','Thunderclap','Silence zone','Pressure prison','Sky corridor','Blade storm','Sky rupture','Great cyclone','Sky bastion'],
1:['Create water','Water jet','Cool a heated item','Water sphere','Water screen','Ice shard','Water cutter','Whirlpool','Water shackles','Water dome','Water volley','Cold veil','Tidal wave','Water storm','Deep spear','Deep prison','Great whirlpool','Water fortress','Ocean heart','Great tide','Abyss'],
5:['Lift stone','Earth rise','Compact soil','Stone spike','Stone screen','Stone throw','Stone volley','Earth shackles','Earth wave','Stone armor','Spike forest','Temporary bridge','Earthquake','Stone guardian','Rockfall','Earth fortress','Rift','Stone tomb','Mountain rise','Tectonic strike','Unbreakable bastion']}
for locale in ('ru_ru','en_us'):
    p=RES/f'lang/{locale}.json';lang=json.loads(p.read_text(encoding='utf-8-sig'))
    for r in recipes:
        name=r['name']
        if locale=='en_us':
            name=r['id'].replace('_',' ').title()
            if r['element'] in english_rows and re.fullmatch(r'c[1-7]_[0-2]',r['id']):
                name=english_rows[r['element']][(r['circle']-1)*3+int(r['id'][-1])]
            if r['id'].endswith('_ice'):name='Ice volley' if r['circle']==4 else 'Ice fortress'
            if r['element']==0:name=lang.get('spell.manatech.fire.'+r['id'],name)
        lang[f"schematic.manatech.{r['element']}.{r['id']}"]=name
    lang.update({k:v[locale=='en_us'] for k,v in translations.items()})
    p.write_text(json.dumps(lang,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    element_names=['Огонь','Вода','Энергия','Воздух','Пустота','Земля'] if locale=='ru_ru' else ['Fire','Water','Energy','Air','Void','Earth']
    for element in range(6):
        pages=[dict(type='patchouli:text',text=('Откройте гримуар → «Схемы». Заклинание требует все страницы 1–2/3/5/7/9/10/12 по кругам I–VII. Кнопка снизу переключает страницы и итоговое наложение. На каждой странице замкните контур вокруг Создания. Колесо увеличивает рисунок; на столе «Выровнять» упорядочивает и центрирует страницы.' if locale=='ru_ru' else 'Open Grimoire → Schematics. Circles I–VII require every page through 2/3/5/7/9/10/12. The bottom button cycles pages and the combined overlay. Each page needs a closed contour around Creation. Scroll to zoom. Align on the overlay table sorts and centers pages.'))]
        for r in recipes:
            if r['element']!=element:continue
            name=lang[f"schematic.manatech.{element}.{r['id']}"]
            symbols=' + '.join(lang['modifier.manatech.'+['creation','movement','density','cooling','area','quantity','defense','link','space'][s]] for s in r['symbols'])
            prefix=f"Круг {r['circle']}: " if locale=='ru_ru' else f"Circle {r['circle']}: "
            required=[2,3,5,7,9,10,12][r['circle']-1]
            text=prefix+name+'. '+symbols+'.'+(f' Нужны все страницы 1–{required}.' if locale=='ru_ru' else f' Requires all pages 1–{required}.')
            if r['extra']>=0:text+=(' Страница 2 — ' if locale=='ru_ru' else 'Page 2 uses ')+element_names[r['extra']]+'.'
            pages.append(dict(type='patchouli:text',text=text))
        pages.append(dict(type='patchouli:text',text=('Схемы не выдают страницы и не открывают круги. Создание ставится в центр каждого слоя. Полноценные специальные эффекты пока реализованы только для 21 формы Огня кругов 1–6; остальные формы и седьмой круг находятся в плане.' if locale=='ru_ru' else 'References do not grant pages or unlock circles. Creation belongs in the center of every page. Specific casting effects currently exist for 21 Fire forms of circles 1–6. Other forms and circle 7 are still planned.')))
        target=RES/f'patchouli_books/occult_symbols/{locale}/entries/schematics_{element}.json'
        target.write_text(json.dumps(dict(name=element_names[element]+(' — схемы' if locale=='ru_ru' else ' — schematics'),category='manatech:magic',icon='manatech:rune_layer_page',pages=pages),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
out=['# Схемы заклинаний всех стихий','', 'Каталог доступен через кнопку «Схемы» в гримуаре. Заклинания требуют полный набор разных страниц 1–2/3/5/7/9/10/12 по кругам I–VII, расположенных по возрастанию номера. На каждой странице обязательны Создание и замкнутый контур вокруг него. Дополнительная стихия занимает страницу №2, а не увеличивает число обязательных страниц. Каталог показывает каждую страницу и итоговое наложение; символы формы находятся на обозначенной странице. Пустота распределена по кругам 1–7, Энергия по кругам 3–7. Седьмой круг показан для планирования. Рецепты новых форм не означают реализацию их специальных эффектов.','', '| Стихия | Круг | Заклинание | Символы | Доп. стихия |','|---|---|---|---|---|']
names=['Огонь','Вода','Энергия','Воздух','Пустота','Земля']
for r in recipes: out.append(f"| {names[r['element']]} | {r['circle'] or 'не задан'} | {r['name']} | {' + '.join(modifiers[s] for s in r['symbols'])} | {names[r['extra']] if r['extra']>=0 else '—'} |")
(ROOT/'SPELL_SCHEMATICS.md').write_text('\n'.join(out)+'\n',encoding='utf-8')
print(f"Generated {len(recipes)} reference recipes for all six elements.")
