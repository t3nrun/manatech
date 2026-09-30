package com.vital.manatech.rune;

import java.util.ArrayList;
import java.util.List;
import com.vital.manatech.rune.hex.SpellElement;

/** Each layer retains its own authored ink; guides are never baked into the effect. */
public record CircleEffectMesh(List<Segment> segments) {
    public record Segment(float x1, float y1, float x2, float y2, int color, int layer) {}

    public static CircleEffectMesh generate(List<DiagramLayer> layers) {
        List<Segment> result = new ArrayList<>();
        for (int i = 0; i < layers.size(); i++) {
            DiagramLayer layer = layers.get(i);
            if (layer.isEmpty()) continue;
            float scale=LayerLayout.scale(i,layers.size(),layer),angle=LayerLayout.rotation(i,layers.size());
            for (DiagramLayer.Stroke stroke : layer.strokes()) trace(result, stroke.points(), stroke.element(), i, scale,angle);
            for (DiagramLayer.Symbol symbol : layer.symbols()) {
                if(layers.size()>1&&symbol.slot()==DiagramLayer.CENTER_SLOT) {
                    if(i==0)trace(result,symbol.trace(),symbol.element(),i,scale*.5f,angle);
                } else trace(result, symbol.trace(), symbol.element(), i, scale,angle);
            }
        }
        return new CircleEffectMesh(List.copyOf(result));
    }

    private static void trace(List<Segment> out, List<DiagramLayer.Point> points, int element, int layer, float scale,float angle) {
        SpellElement[] elements = SpellElement.values();
        int color = elements[Math.floorMod(element, elements.length)].color();
        for (int n=1;n<points.size();n++) {
            DiagramLayer.Point a=points.get(n-1), b=points.get(n);
            if (a.x()==1001 && a.y()==1001 || b.x()==1001 && b.y()==1001) continue;
            float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
            add(out,(a.x()*c-a.y()*s)*scale/1000f,(a.x()*s+a.y()*c)*scale/1000f,
                (b.x()*c-b.y()*s)*scale/1000f,(b.x()*s+b.y()*c)*scale/1000f,color,layer);
        }
    }

    private static void add(List<Segment> out,float x1,float y1,float x2,float y2,int color,int layer) {
        if (Math.hypot(x2-x1,y2-y1)>.0001f) out.add(new Segment(x1,y1,x2,y2,color,layer));
    }
}
