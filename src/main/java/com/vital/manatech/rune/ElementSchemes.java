package com.vital.manatech.rune;

import com.vital.manatech.rune.hex.SpellElement;
import java.util.ArrayList;
import java.util.List;

/** Normalized authored guides. Tool/table tier never participates in their geometry. */
public final class ElementSchemes {
    public static final int LAYERS = 12;
    public record Point(float x, float y) {}
    public record Path(List<Point> points, boolean closed, boolean contour, int pair) {
        public Path { points = List.copyOf(points); }
    }
    private ElementSchemes() {}
    private static Point polar(double radius, double angle) { return new Point((float)(radius*Math.cos(angle)), (float)(radius*Math.sin(angle))); }
    private static Path ring(double r, int pair) {
        var points = new ArrayList<Point>();
        for (int i=0;i<96;i++) points.add(polar(r,i*Math.PI*2/96));
        return new Path(points,true,true,pair);
    }
    private static Path polygon(double r,int sides,double angle,boolean contour) {
        var points = new ArrayList<Point>();
        for(int i=0;i<sides;i++) points.add(polar(r,angle+i*Math.PI*2/sides));
        return new Path(points,true,contour,-1);
    }
    private static Path line(Point... points) { return new Path(List.of(points),false,false,-1); }
    public static List<Path> paths(SpellElement element,int layer) {
        if(layer<0 || layer>=LAYERS) throw new IllegalArgumentException("Layer " + layer);
        List<Path> paths=new ArrayList<>();
        paths.add(ring(1,0)); paths.add(ring(.95,0));
        double turn=-Math.PI/2 + layer*Math.PI/36;
        int order=3+layer/3;
        switch(element) {
            case FIRE -> {
                int sides=3+2*(layer%3);
                paths.add(polygon(.72,sides,turn,true)); paths.add(polygon(.68,sides,turn,true));
                var tips=new ArrayList<Point>();
                for(int i=0;i<order*2;i++) tips.add(polar(i%2==0?.92:.79,turn+i*Math.PI/order));
                paths.add(new Path(tips,true,false,-1));
                for(int i=0;i<order;i++) {
                    double a=turn+i*Math.PI*2/order;
                    paths.add(line(polar(.77,a-.09),polar(.91,a),polar(.77,a+.09)));
                }
            }
            case WATER -> {
                paths.add(ring(.72,1)); paths.add(ring(.68,1));
                var wave=new ArrayList<Point>();
                for(int i=0;i<128;i++) {
                    double a=i*Math.PI*2/128;
                    wave.add(polar(.84+.06*Math.sin(order*a+layer*Math.PI/6),a));
                }
                paths.add(new Path(wave,true,false,-1));
                for(int i=0;i<order;i++) {
                    double a=turn+i*Math.PI*2/order;
                    paths.add(line(polar(.78,a-.07),polar(.92,a),polar(.78,a+.07),polar(.75,a)));
                }
            }
            case ENERGY -> {
                int sides=6+2*(layer%3);
                paths.add(polygon(.72,sides,turn,true)); paths.add(polygon(.68,sides,turn,true));
                paths.add(polygon(.88,4,turn+Math.PI/4,false));
                paths.add(polygon(.84,4,turn,false));
                for(int i=0;i<order+1;i++) {
                    double a=turn+i*Math.PI*2/(order+1);
                    paths.add(line(polar(.76,a-.035),polar(.93,a-.035)));
                    paths.add(line(polar(.76,a+.035),polar(.93,a+.035)));
                }
            }
            case AIR -> {
                paths.add(ring(.72,1)); paths.add(ring(.68,1));
                for(int i=0;i<order;i++) {
                    double a=turn+i*Math.PI*2/order;
                    var sweep=new ArrayList<Point>();
                    for(int j=0;j<=24;j++) sweep.add(polar(.77+.15*j/24.,a+j*Math.PI/(order*30)));
                    paths.add(new Path(sweep,false,false,-1));
                    Point end=sweep.getLast();
                    paths.add(line(polar(.85,a+.63/order),end,polar(.92,a+.43/order)));
                }
                paths.add(polygon(.84,3+layer%4,turn+.18,false));
            }
            case VOID -> {
                int sides=5+layer%4;
                paths.add(polygon(.72,sides,turn,true)); paths.add(polygon(.68,sides,turn,true));
                paths.add(polygon(.90,sides,turn+Math.PI/sides,false));
                for(int i=0;i<order;i++) {
                    double a=turn+i*Math.PI*2/order;
                    paths.add(line(polar(.79,a-.13),polar(.88,a-.03),polar(.80,a+.13),polar(.92,a+.07)));
                }
            }
            case EARTH -> {
                paths.add(polygon(.72,4,turn,true)); paths.add(polygon(.68,4,turn,true));
                paths.add(polygon(.90,4,turn+Math.PI/4,false));
                for(int i=0;i<4;i++) {
                    double a=turn+i*Math.PI/2;
                    for(int j=0;j<1+layer/4;j++) {
                        double r=.77+j*.055;
                        paths.add(line(polar(r,a-.13),polar(r+.035,a-.13),polar(r+.035,a+.13),polar(r,a+.13)));
                    }
                }
            }
        }
        return List.copyOf(paths);
    }
    public static Point slot(SpellElement element,int layer,int slot) {
        if(slot==DiagramLayer.CENTER_SLOT) return new Point(0,0);
        double rotation=element.ordinal()*Math.PI/24 + layer*Math.PI/48;
        // The smallest inner contour is a triangle with inradius .34.
        // Reserve .235 + .07 for the entire slot, leaving a clear gap to it.
        double radius=.235;
        return polar(radius,-Math.PI/2+rotation+slot*Math.PI/4);
    }
}
