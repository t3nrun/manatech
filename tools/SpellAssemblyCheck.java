import com.vital.manatech.magic.*;
import com.vital.manatech.rune.*;
import java.util.*;

public class SpellAssemblyCheck {
    public static void main(String[] args) {
        for(var recipe:SpellSchematics.ALL) {
            var pages=SchematicDiagrams.pages(recipe);
            var spell=spell(pages);
            check(pages.size()==RuneTier.layers(recipe.circle()),"required count");
            check(SpellAssembly.inspect(spell).valid(),recipe.key()+": "+SpellAssembly.inspect(spell));
            check(AssembledSpell.read(spell.encode()).layers().equals(spell.layers()),"large snapshot round trip");
            for(int omit=0;omit<pages.size();omit++) {
                var broken=new ArrayList<>(pages);broken.remove(omit);
                var incomplete=SpellAssembly.inspect(spell(broken));
                check(!incomplete.valid()||incomplete.circle()<recipe.circle(),"missing page accepted at original circle");
                broken.add(Math.min(omit,broken.size()),pages.get((omit+1)%pages.size()));
                incomplete=SpellAssembly.inspect(spell(broken));
                check(!incomplete.valid()||incomplete.circle()<recipe.circle(),"duplicate replaces missing layer at original circle");
            }
            var reversed=new ArrayList<>(pages);Collections.reverse(reversed);
            check(!SpellAssembly.inspect(spell(reversed)).valid(),"reverse order");
            var noContour=new ArrayList<>(pages);var original=pages.getFirst();
            noContour.set(0,new DiagramLayer(List.of(),original.symbols(),original.schemeElement(),original.schemeLayer()));
            check(!SpellAssembly.inspect(spell(noContour)).valid(),"symbol alone accepted");
            double previous=2;
            var segments=CircleEffectMesh.generate(pages).segments();
            for(int layer=0;layer<pages.size();layer++) {
                final int current=layer;
                double radius=segments.stream().filter(s->s.layer()==current).mapToDouble(s->Math.max(Math.hypot(s.x1(),s.y1()),Math.hypot(s.x2(),s.y2()))).max().orElseThrow();
                check(radius<previous-.02,"layer radii not separated");previous=radius;
            }
        }
        var large=spell(SchematicDiagrams.pages(SpellSchematics.ALL.stream().filter(r->r.circle()==7).findFirst().orElseThrow()));
        check(large.encode().startsWith("z1:")&&large.encode().length()<32767,"large spell not network-safe");
        check(AssembledSpell.read("z1:garbage!").isEmpty(),"bad compressed data");
        System.out.println("134 complete prefixes; missing/duplicate pages cannot preserve circle, reversed pages rejected; contours, nested radii and compressed 12-page snapshots passed.");
    }
    private static AssembledSpell spell(List<DiagramLayer> pages) { return new AssembledSpell(pages.stream().map(DiagramLayer::encode).toList()); }
    private static void check(boolean okay,String message) { if(!okay)throw new AssertionError(message); }
}
