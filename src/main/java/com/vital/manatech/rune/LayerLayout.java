package com.vital.manatech.rune;

/** Same automatic fit in the table, reference book and world; authored points stay intact. */
public final class LayerLayout {
    private LayerLayout() {}
    public static float scale(int index,int count,DiagramLayer page) {
        if(count<=1)return 1;
        float radius=1-.72f*index/Math.max(1,count-1);
        float authored=page.schemeLayer()<0?1:.62f+page.schemeLayer()*.025f;
        return radius/authored;
    }
    public static float rotation(int index,int count) { return count<=1?0:(float)(index*Math.PI*(3-Math.sqrt(5))); }
}
