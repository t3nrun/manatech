"""Render the shared Java geometry, exported by SpellSchematicsCheck, for visual review."""
import json
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'build-verify-3/tmp/scheme-check/catalog_previews.json').read_text())
lang=json.loads((root/'src/main/resources/assets/manatech/lang/ru_ru.json').read_text(encoding='utf-8'))
sample=[]
for element in range(6):
    sample.append(next(x for x in data if x['key'].startswith(f'schematic.manatech.{element}.') and x['key'] in {'schematic.manatech.0.pillar','schematic.manatech.1.c4_0','schematic.manatech.2.reactor','schematic.manatech.3.c4_0','schematic.manatech.4.prison','schematic.manatech.5.c4_0'}))
factor=2;cell=360;rowheight=385
img=Image.new('RGB',(cell*2*factor,rowheight*3*factor),'#10121c');draw=ImageDraw.Draw(img)
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',18*factor)
for i,item in enumerate(sample):
    x=(i%2)*cell*factor;y=(i//2)*rowheight*factor
    draw.text((x+18*factor,y+15*factor),lang[item['key']],font=font,fill='#eee2ff')
    cx=x+cell*factor/2;cy=y+207*factor;r=160*factor
    for a,b,c,d,color in item['segments']:
        rgb=((color>>16)&255,(color>>8)&255,color&255)
        draw.line((cx+a*r,cy+b*r,cx+c*r,cy+d*r),fill=rgb,width=2)
out=root/'art/previews/spell_schematics_samples.png';out.parent.mkdir(parents=True,exist_ok=True)
img.resize((cell*2,rowheight*3),Image.Resampling.LANCZOS).save(out)
print(out)
