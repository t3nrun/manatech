package com.vital.manatech.magic;

import com.vital.manatech.rune.AssembledSpell;
import com.vital.manatech.rune.DiagramLayer;
import com.vital.manatech.rune.RuneTier;
import java.util.*;

/** A form is resolved from the authored circle and its fire modifiers. Copies do not raise its circle. */
public final class FireSpells {
    public enum Form {
        FLAME(1,"flame",0), SPARKS(1,"sparks",5), HEAT(1,"heat",2),
        FIREBALL(2,"fireball",1), BARRAGE(2,"barrage",5,1), ZONE(2,"zone",4),
        METEOR(3,"meteor",2,1), FLAMETHROWER(3,"flamethrower",1), GREATER_BARRAGE(3,"greater_barrage",5,4,1),
        METEORITE(4,"meteorite",2,1), TORNADO(4,"tornado",4,1,7), LONG_BARRAGE(4,"long_barrage",5,4,7), HELD_FLAME(4,"held_flame",1,7),
        METEOR_RAIN(5,"meteor_rain",5,4,2,1), STORM(5,"storm",4,1,7), PILLAR(5,"pillar",2,7), FLAME_BEAM(5,"flame_beam",2,1),
        DISINTEGRATION(6,"disintegration",2,7), SUN_SHARD(6,"sun_shard",2,8), BIG_EXPLOSION(6,"big_explosion",4,2), HOLY_BEAM(6,"holy_beam",2,1,6);
        private final int circle,mask;
        private final String id;
        Form(int circle,String id,int...symbols) { this.circle=circle;this.id=id;int m=1;for(int s:symbols)m|=1<<s;mask=m; }
        public int circle() { return circle; }
        public String id() { return id; }
        public int mask() { return mask; }
    }
    private FireSpells() {}
    public static int dominant(AssembledSpell spell) {
        return com.vital.manatech.rune.SpellAssembly.primaryElement(spell);
    }
    public static int circle(AssembledSpell spell) {
        return com.vital.manatech.rune.SpellAssembly.circle(spell);
    }
    public static int modifiers(AssembledSpell spell) {
        int mask=0;
        for(var layer:spell.diagrams()) for(var symbol:layer.symbols())
            if(symbol.element()==0 && symbol.id()>=0 && symbol.id()<9) mask|=1<<symbol.id();
        return mask;
    }
    public static boolean hasAir(AssembledSpell spell) {
        return spell.diagrams().stream().anyMatch(l->l.schemeElement()==3 || l.strokes().stream().anyMatch(s->s.element()==3));
    }
    public static Form resolve(AssembledSpell spell) {
        if(!com.vital.manatech.rune.SpellAssembly.inspect(spell).valid() || dominant(spell)!=0) return null;
        int circle=circle(spell),mask=modifiers(spell);
        if(circle>6)return null;
        Form best=Form.FLAME;
        for(Form form:Form.values()) {
            if(form.circle()>circle || (mask&form.mask())!=form.mask() || form==Form.TORNADO && !hasAir(spell)) continue;
            if(form.circle()>best.circle() || form.circle()==best.circle() && Integer.bitCount(form.mask())>Integer.bitCount(best.mask())) best=form;
        }
        return best;
    }
    public static long manaCost(AssembledSpell spell,Form form) {
        return Math.max(8,Math.round(ManaProgression.power(circle(spell))/8.0*(1+.2*(Integer.bitCount(form.mask())-1))));
    }
}
