import com.vital.manatech.magic.*;
import com.vital.manatech.rune.*;
import java.util.*;
import java.nio.file.*;

public class SpellSchematicsCheck {
    public static void main(String[] args) throws Exception {
        Set<String> ids=new HashSet<>(),drawings=new HashSet<>();
        Map<Integer,Set<Integer>> circles=new HashMap<>();
        int ready=0;
        for(var recipe:SpellSchematics.ALL) {
            check(ids.add(recipe.key()),"duplicate id");
            check(recipe.circle()>=1 && recipe.circle()<=7,"circle");
            check(recipe.element()!=2 || recipe.circle()>=3,"early Energy");
            circles.computeIfAbsent(recipe.element(),e->new HashSet<>()).add(recipe.circle());
            var pages=SchematicDiagrams.pages(recipe);
            var spell=new AssembledSpell(pages.stream().map(DiagramLayer::encode).toList());
            check(spell.hasCentralCreation(),"central Creation: "+recipe.id());
            check(drawings.add(spell.encode()),"identical schematics: "+recipe.key());
            for(var page:pages) {
                check(RuneTier.allowsSymbols(recipe.toolTier(),page),"tool slots: "+recipe.id());
                check(DiagramLayer.decode(page.encode()).equals(page),"round trip");
                for(var symbol:page.symbols()) {
                    for(var p:symbol.trace()) if(p.x()!=1001)check(Math.abs(p.x())<=1000&&Math.abs(p.y())<=1000,"bounds");
                }
            }
            if(recipe.implemented()) {
                var form=FireSpells.resolve(spell);
                check(form!=null && form.id().equals(recipe.id()),"wrong Fire form: "+recipe.id()+" -> "+form);
                check(FireSpells.circle(spell)==recipe.circle(),"Fire circle");ready++;
            }
        }
        for(int element=0;element<6;element++) {
            var expected=element==2?Set.of(3,4,5,6,7):Set.of(1,2,3,4,5,6,7);
            check(circles.get(element).equals(expected),"missing circle: "+element);
        }
        check(ready==21,"implemented Fire count");
        // Standalone previews for checking the actual shared geometry.
        var out=new StringBuilder("[");
        for(var recipe:SpellSchematics.ALL) {
            if(out.length()>1)out.append(',');
            out.append("{\"key\":\"").append(recipe.key()).append("\",\"segments\":[");
            boolean first=true;
            for(var s:CircleEffectMesh.generate(SchematicDiagrams.pages(recipe)).segments()) {
                if(!first)out.append(',');first=false;
                out.append('[').append(s.x1()).append(',').append(s.y1()).append(',').append(s.x2()).append(',').append(s.y2()).append(',').append(s.color()).append(']');
            }
            out.append("]}");
        }
        out.append(']');
        Files.createDirectories(Path.of("build-verify-3/tmp/scheme-check"));
        Files.writeString(Path.of("build-verify-3/tmp/scheme-check/catalog_previews.json"),out);
        System.out.println(SpellSchematics.ALL.size()+" distinct schematics; all six elements and specified circles; central Creation, tool slots, serialization and 21 real Fire recipes passed.");
    }
    private static void check(boolean okay,String message) { if(!okay)throw new AssertionError(message); }
}
