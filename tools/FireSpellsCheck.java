import com.vital.manatech.rune.*;
import com.vital.manatech.rune.hex.SpellElement;
import com.vital.manatech.magic.FireSpells;
import java.util.*;

public class FireSpellsCheck {
    private static final List<DiagramLayer.Point> TRACE=List.of(new DiagramLayer.Point(-12,0),new DiagramLayer.Point(0,-12),new DiagramLayer.Point(12,0),new DiagramLayer.Point(0,12),new DiagramLayer.Point(-12,0),new DiagramLayer.Point(0,-12));
    public static void main(String[] args) {
        if(FireSpells.Form.values().length!=21) throw new AssertionError("Expected 21 forms across circles 1–6");
        for(var form:FireSpells.Form.values()) {
            var recipe=com.vital.manatech.magic.SpellSchematics.ALL.stream().filter(r->r.element()==0&&r.id().equals(form.id())).findFirst().orElseThrow();
            var spell=new AssembledSpell(SchematicDiagrams.pages(recipe).stream().map(DiagramLayer::encode).toList());
            if(FireSpells.circle(spell)!=form.circle() || FireSpells.resolve(spell)!=form) throw new AssertionError("Recipe: "+form+" resolved "+FireSpells.resolve(spell));
            if(FireSpells.manaCost(spell,form)<8) throw new AssertionError("Mana cost: "+form);
        }
        var creation=new DiagramLayer(List.of(new DiagramLayer.Stroke(0,TRACE)),List.of(new DiagramLayer.Symbol(0,0,8,TRACE)),0,RuneTier.firstLayer(6));
        var high=new AssembledSpell(List.of(creation.encode()));
        if(FireSpells.circle(high)!=6 || FireSpells.resolve(high)!=null) throw new AssertionError("One high page must not cast");
        var seventh=new AssembledSpell(List.of(new DiagramLayer(creation.strokes(),creation.symbols(),0,11).encode()));
        if(FireSpells.circle(seventh)!=7 || FireSpells.resolve(seventh)!=null)throw new AssertionError("Seventh circle silently cast as sixth");
        var moved=DiagramLayer.decode("v3,0,0;s,0,0,8,-12:0|0:-12|12:0|0:12|-12:0|0:-12;s,1,0,0,-12:-267|0:-279|12:-267|0:-255|-12:-267|0:-279");
        if(!moved.hasCentralCreation() || !moved.encode().startsWith("v4,")) throw new AssertionError("Old layer migration failed");
        if(Math.abs(moved.symbols().get(1).trace().get(0).y())>200) throw new AssertionError("Old symbol still on contour");
        for(var element:SpellElement.values()) for(int layer=0;layer<12;layer++) {
            var paths=ElementSchemes.paths(element,layer);
            for(int slot=0;slot<8;slot++) {
                var p=ElementSchemes.slot(element,layer,slot);
                for(var guide:paths) if(guide.contour()) for(int i=0;i<guide.points().size();i++) {
                    var a=guide.points().get(i);
                    var b=guide.points().get((i+1)%guide.points().size());
                    double dx=b.x()-a.x(),dy=b.y()-a.y();
                    double t=Math.clamp(((p.x()-a.x())*dx+(p.y()-a.y())*dy)/(dx*dx+dy*dy),0,1);
                    if(Math.hypot(p.x()-a.x()-dx*t,p.y()-a.y()-dy*t)<=.07)
                        throw new AssertionError("Slot overlaps contour: "+element+"/"+layer+"/"+slot);
                }
            }
        }
        System.out.println("21 Fire forms across 6 circles; v3 glyph migration; 576 slot positions clear of contours: passed.");
    }
}
