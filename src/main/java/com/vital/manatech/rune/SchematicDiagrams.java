package com.vital.manatech.rune;

import com.vital.manatech.magic.SpellSchematics;
import com.vital.manatech.rune.hex.SpellElement;
import java.util.ArrayList;
import java.util.List;

/** Builds reference-only diagrams from the same guides and glyphs as the drawing table. */
public final class SchematicDiagrams {
    private SchematicDiagrams() {}
    public static List<DiagramLayer> pages(SpellSchematics.Recipe recipe) {
        var pages=new ArrayList<DiagramLayer>();
        int required=RuneTier.layers(recipe.circle());
        for(int index=0;index<required;index++) {
            boolean form=index==recipe.layer();
            int element=recipe.extraElement()>=0&&index==1?recipe.extraElement():recipe.element();
            pages.add(page(element,index,form?recipe.symbols():List.of(0),form?recipe.slots():List.of(),recipe.design(),form&&recipe.implemented()));
        }
        return List.copyOf(pages);
    }
    private static DiagramLayer page(int element,int layer,List<Integer> symbols,List<Integer> slots,int design,boolean implemented) {
        var type=SpellElement.values()[element];
        float scale=.62f+layer*.025f;
        var ink=new ArrayList<DiagramLayer.Stroke>();
        int decoration=0;
        for(var path:ElementSchemes.paths(type,layer)) {
            // Planned variants distinguish their shape with existing optional guide motifs.
            if(!path.contour() && !implemented && decoration++%3!=design%3)continue;
            var points=new ArrayList<DiagramLayer.Point>();
            for(var point:path.points()) points.add(point(point.x()*scale,point.y()*scale));
            if(path.closed())points.add(points.getFirst());
            ink.add(new DiagramLayer.Stroke(element,points));
        }
        var marks=new ArrayList<DiagramLayer.Symbol>();
        for(int n=0;n<symbols.size();n++) {
            int id=symbols.get(n),slot=id==0?DiagramLayer.CENTER_SLOT:slots.get(n-1);
            var center=ElementSchemes.slot(type,layer,slot);
            var trace=new ArrayList<DiagramLayer.Point>();
            for(var path:SymbolGlyph.paths(center.x(),center.y(),.07f*.78f,id)) {
                if(!trace.isEmpty())trace.add(new DiagramLayer.Point(1001,1001));
                for(var p:path.vertices())trace.add(point(p.x()*scale,p.y()*scale));
                if(path.closed()) { var p=path.vertices().getFirst();trace.add(point(p.x()*scale,p.y()*scale)); }
            }
            marks.add(new DiagramLayer.Symbol(id,element,slot,trace));
            if(slot!=DiagramLayer.CENTER_SLOT) {
                for(float radius:new float[]{.07f,.056f}) {
                    var points=new ArrayList<DiagramLayer.Point>();
                    for(int i=0;i<=96;i++) {
                        double a=i*Math.PI*2/96;
                        points.add(point((center.x()+(float)Math.cos(a)*radius)*scale,(center.y()+(float)Math.sin(a)*radius)*scale));
                    }
                    ink.add(new DiagramLayer.Stroke(element,points));
                }
            }
        }
        return new DiagramLayer(ink,marks,element,layer);
    }
    private static DiagramLayer.Point point(float x,float y) {
        return new DiagramLayer.Point(Math.round(x*1000),Math.round(y*1000));
    }
}
