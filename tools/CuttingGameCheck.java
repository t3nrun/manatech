import com.vital.manatech.machine.CuttingGame;

public class CuttingGameCheck {
    public static void main(String[] args) {
        for(int seed=0;seed<100;seed++) for(int cut=0;cut<6;cut++) {
            int target=CuttingGame.target(seed,cut);
            if(target<20 || target>80) throw new AssertionError("Target off bar");
            boolean reachable=false;
            for(int tick=0;tick<80;tick++) {
                int cursor=CuttingGame.cursor(tick);
                if(cursor<0 || cursor>100 || cursor!=CuttingGame.cursor(tick+80)) throw new AssertionError("Clock discontinuity");
                reachable|=CuttingGame.hits(cursor,target);
            }
            if(!reachable || !CuttingGame.hits(target+12,target) || CuttingGame.hits(target+13,target)) throw new AssertionError("Invalid hit zone");
        }
        System.out.println("Six cuts: 600 target configurations reachable, bounded cursor and hit boundaries passed.");
    }
}
