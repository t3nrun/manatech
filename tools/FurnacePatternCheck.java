import com.vital.manatech.machine.FurnacePattern;
import java.util.HashMap;

public class FurnacePatternCheck {
    static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) {
        var counts = new HashMap<Character, Integer>();
        for (int x=0;x<4;x++) for(int y=0;y<4;y++) for(int z=0;z<4;z++) counts.merge(FurnacePattern.cell(x,y,z),1,Integer::sum);
        require(counts.get('B')==32 && counts.get('A')==16 && counts.get('F')==4 && counts.get('D')==4 && counts.get(' ')==8, "Bill of materials and hollow volume");
        require(FurnacePattern.inspect((x,y,z,c)->true).valid(), "Complete structure");
        for(int x=0;x<4;x++) for(int y=0;y<4;y++) for(int z=0;z<4;z++) {
            final int ex=x,ey=y,ez=z;
            var missing=FurnacePattern.inspect((a,b,c,expected)->a!=ex || b!=ey || c!=ez);
            require(!missing.valid() && missing.matched()==63 && missing.x()==x && missing.y()==y && missing.z()==z, "Every single missing block, wrong facing or filled core must invalidate");
        }
        for(int x=1;x<=2;x++) for(int y=1;y<=2;y++) require(FurnacePattern.cell(x,y,0)=='F' && FurnacePattern.cell(x,y,3)=='D', "Aligned inputs and drains");
        System.out.println("Furnace layout: 64 failure positions, hollow core, bill of materials and four lanes passed.");
    }
}
