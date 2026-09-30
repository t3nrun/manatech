package com.vital.manatech.rune;
import java.util.ArrayList;
import java.util.List;
public final class SymbolGlyph {
    public record Point(float x,float y) {}
    public record Path(List<Point> vertices,boolean closed) {}
    private SymbolGlyph() {}
    private static Path ring(float x, float y, float r) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            double a = i * 2 * Math.PI / 96;
            points.add(new Point(x + (float)Math.cos(a) * r, y + (float)Math.sin(a) * r));
        }
        return new Path(points, true);
    }
    private static Path sigilLine(float x, float y, float r, float x1, float y1, float x2, float y2) {
        return new Path(List.of(new Point(x+r*x1,y+r*y1),new Point(x+r*x2,y+r*y2)),false);
    }
    private static Path sigilPolygon(float x, float y, float r, int sides, double angle) {
        List<Point> vertices = new ArrayList<>();
        for (int i = 0; i < sides; i++) {
            double a = angle + i * 2 * Math.PI / sides;
            vertices.add(new Point(x + (float)Math.cos(a)*r, y + (float)Math.sin(a)*r));
        }
        return new Path(vertices, true);
    }
    /** Editor-only sigil guides, shared by the palette and the tracing surface. */
    public static List<Path> paths(float x, float y, float r, int id) {
        List<Path> paths = new ArrayList<>();
        int kind = id == 8 ? 8 : Math.floorMod(id, 8);
        int variant = id <= 8 ? 0 : Math.floorMod(id / 8, 4);
        switch (kind) {
            case 0 -> { // interlaced seal
                paths.add(sigilPolygon(x,y,r*.9f,3,-Math.PI/2));
                paths.add(sigilPolygon(x,y,r*.9f,3,Math.PI/2));
                paths.add(ring(x,y,r*.38f));
            }
            case 1 -> { // eye and pupil
                paths.add(new Path(List.of(new Point(x-r,y),new Point(x-r*.45f,y-r*.48f),new Point(x+r*.45f,y-r*.48f),new Point(x+r,y)),false));
                paths.add(new Path(List.of(new Point(x-r,y),new Point(x-r*.45f,y+r*.48f),new Point(x+r*.45f,y+r*.48f),new Point(x+r,y)),false));
                paths.add(ring(x,y,r*.28f));
            }
            case 2 -> { // spiral of life
                List<Point> spiral = new ArrayList<>();
                for (int i = 0; i <= 48; i++) {
                    double a = i * Math.PI * 4 / 48, distance = r * (.08 + .85 * i / 48.0);
                    spiral.add(new Point(x+(float)(Math.cos(a)*distance),y+(float)(Math.sin(a)*distance)));
                }
                paths.add(new Path(spiral,false));
            }
            case 3 -> { // solar wheel
                paths.add(ring(x,y,r*.53f));
                paths.add(ring(x,y,r*.18f));
                for (int i=0;i<8;i++) {
                    double a=i*Math.PI/4;
                    paths.add(sigilLine(x,y,r,(float)Math.cos(a)*.62f,(float)Math.sin(a)*.62f,
                            (float)Math.cos(a), (float)Math.sin(a)));
                }
            }
            case 4 -> { // alchemical cross
                paths.add(sigilLine(x,y,r,0,-1,0,1));
                paths.add(sigilLine(x,y,r,-.72f,-.26f,.72f,-.26f));
                paths.add(sigilLine(x,y,r,-.55f,.45f,.55f,.45f));
                paths.add(ring(x,y-r*.64f,r*.2f));
            }
            case 5 -> { // three interlocked circles
                for(int i=0;i<3;i++) {
                    double a = i*2*Math.PI/3-Math.PI/2;
                    paths.add(ring(x+(float)Math.cos(a)*r*.35f,y+(float)Math.sin(a)*r*.35f,r*.46f));
                }
            }
            case 6 -> { // labyrinth and compass
                paths.add(sigilPolygon(x,y,r*.85f,4,Math.PI/4));
                paths.add(sigilPolygon(x,y,r*.55f,4,0));
                paths.add(sigilLine(x,y,r,0,-.8f,0,.8f));
                paths.add(sigilLine(x,y,r,-.8f,0,.8f,0));
            }
            case 7 -> { // crescent and star
                paths.add(sigilPolygon(x,y,r*.72f,5,-Math.PI/2));
                paths.add(ring(x-r*.2f,y,r*.55f));
                paths.add(ring(x-r*.03f,y-r*.12f,r*.47f));
            }
            default -> { // spatial portal, without an additional enclosing frame
                paths.add(new Path(List.of(new Point(x-r*.7f,y+r*.8f), new Point(x-r*.7f,y-r*.6f),
                        new Point(x,y-r),new Point(x+r*.7f,y-r*.6f),new Point(x+r*.7f,y+r*.8f)), false));
                paths.add(sigilLine(x,y,r,-.4f,.55f,.4f,-.2f));
                paths.add(sigilLine(x,y,r,.4f,-.2f,.05f,-.2f));
                paths.add(sigilLine(x,y,r,.4f,-.2f,.4f,.15f));
            }
        }
        if (variant >= 1) paths.add(ring(x,y,r*.98f));
        if (variant >= 2) paths.add(sigilPolygon(x,y,r*.84f,variant==2?6:8,-Math.PI/2));
        if (variant >= 3) {
            paths.add(sigilLine(x,y,r,-.65f,-.65f,.65f,.65f));
            paths.add(sigilLine(x,y,r,.65f,-.65f,-.65f,.65f));
        }
        return paths;
    }
}
