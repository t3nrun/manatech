import com.vital.manatech.rune.*;
import com.vital.manatech.rune.hex.SpellElement;
import java.util.*;

public class ElementSchemesCheck {
    public static void main(String[] args) {
        Set<String> shapes = new HashSet<>();
        for (var element : SpellElement.values()) for (int layer=0;layer<12;layer++) {
            var paths=ElementSchemes.paths(element,layer);
            if (!shapes.add(paths.toString())) throw new AssertionError("Duplicate schema");
            for(var path:paths) for(var point:path.points())
                if(!Float.isFinite(point.x()) || !Float.isFinite(point.y()) || Math.hypot(point.x(),point.y())>1.001) throw new AssertionError("Out of bounds");
            var center=ElementSchemes.slot(element,layer,DiagramLayer.CENTER_SLOT);
            if(center.x()!=0 || center.y()!=0) throw new AssertionError("Center moved");
            for(int tier=1;tier<=7;tier++) if(!ElementSchemes.paths(element,layer).equals(paths)) throw new AssertionError("Tier changed schema");
        }
        int[] limits={2,3,5,7,9,10,12};
        for(int layer=1;layer<=12;layer++) {
            int circle=RuneTier.circleForLayer(layer);
            if(layer<=RuneTier.firstLayer(circle)||layer>RuneTier.layers(circle))throw new AssertionError("Layer outside circle");
        }
        for(int circle=1;circle<=7;circle++) {
            int count=RuneTier.layers(circle)-RuneTier.firstLayer(circle);
            if(count<1||count>2)throw new AssertionError("Uneven circle group");
        }
        for(int stylus=1;stylus<=7;stylus++) for(int table=1;table<=7;table++) {
            int tier=RuneTier.effectiveTier(stylus,table);
            if(RuneTier.layers(tier)!=limits[Math.min(stylus,table)-1]) throw new AssertionError("Pair ignores weaker tool");
            for(int layer=0;layer<12;layer++) if(RuneTier.allowsScheme(tier,layer)!=(layer<limits[Math.min(stylus,table)-1])) throw new AssertionError("Wrong layer gate");
        }
        var trace=List.of(new DiagramLayer.Point(-10,0),new DiagramLayer.Point(0,-10),new DiagramLayer.Point(10,0),new DiagramLayer.Point(0,10),new DiagramLayer.Point(-10,0),new DiagramLayer.Point(0,-10));
        var symbols=new ArrayList<DiagramLayer.Symbol>();
        symbols.add(new DiagramLayer.Symbol(0,0,8,trace));
        for(int i=0;i<8;i++) symbols.add(new DiagramLayer.Symbol(i+1,0,i,trace));
        var valid=new DiagramLayer(List.of(),symbols,0,11);
        int[] slotLimits={2,3,4,5,6,8,8};
        for(int tier=1;tier<=7;tier++) {
            if(RuneTier.slots(tier)!=slotLimits[tier-1]) throw new AssertionError("Slot limit");
            var visible=RuneTier.visibleSlots(tier);
            if(visible.size()!=slotLimits[tier-1] || new HashSet<>(visible).size()!=visible.size()) throw new AssertionError("Visible rings");
            for(int slot:visible) if(slot<0 || slot>=8) throw new AssertionError("Invalid ring");
            var limited=new ArrayList<DiagramLayer.Symbol>(); limited.add(symbols.get(0));
            for(int count=0;count<=visible.size();count++) {
                if(!RuneTier.allowsSymbols(tier,new DiagramLayer(List.of(),limited))) throw new AssertionError("Peripheral limit or central exception");
                if(count<visible.size()) limited.add(new DiagramLayer.Symbol(1+count,0,visible.get(count),trace));
            }
            if(tier<7) {
                int unavailable=java.util.stream.IntStream.range(0,8).filter(slot->!visible.contains(slot)).findFirst().orElse(-1);
                limited.add(new DiagramLayer.Symbol(8,0,unavailable,trace));
                if(RuneTier.allowsSymbols(tier,new DiagramLayer(List.of(),limited))) throw new AssertionError("Hidden slot was usable");
            }
        }
        if(!valid.hasCentralCreation() || !DiagramLayer.decode(valid.encode()).equals(valid)) throw new AssertionError("Nine symbols or metadata lost");
        if(new DiagramLayer(List.of(),List.of(new DiagramLayer.Symbol(0,0,0,trace))).hasCentralCreation()) throw new AssertionError("Peripheral Creation accepted");
        if(new DiagramLayer(List.of(),List.of(new DiagramLayer.Symbol(1,0,8,trace))).hasCentralCreation()) throw new AssertionError("Wrong central symbol accepted");
        if(new DiagramLayer(List.of(),List.of()).hasCentralCreation()) throw new AssertionError("Missing Creation accepted");
        if(!DiagramLayer.decode("v2;s,1,0,0,-10:0|0:-10|10:0").encode().startsWith("v2")) throw new AssertionError("Legacy lost");
        if(!new AssembledSpell(List.of(valid.encode())).hasCentralCreation() || new AssembledSpell(List.of("v2;s,1,0,0,0:0")).hasCentralCreation()) throw new AssertionError("Spell validation failed");
        System.out.println("72 unique bounded schemes; 7 tool tiers; 9-symbol round trip; required central Creation: passed.");
    }
}
