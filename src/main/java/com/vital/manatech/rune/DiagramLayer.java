package com.vital.manatech.rune;

import com.vital.manatech.rune.hex.SpellElement;
import java.util.ArrayList;
import java.util.List;

/** Normalised coordinates (-1000..1000) allow the same diagram to be drawn at any scale. */
public record DiagramLayer(List<Stroke> strokes, List<Symbol> symbols, int schemeElement, int schemeLayer) {
    public static final int CENTER_SLOT = 8;
    public static final int MAX_SYMBOLS = 9;
    public DiagramLayer(List<Stroke> strokes, List<Symbol> symbols) { this(strokes, symbols, -1, -1); }
    public record Point(int x, int y) {}
    public record Stroke(int element, List<Point> points) {}
    public record Symbol(int id, int element, int slot, List<Point> trace) {}

    public DiagramLayer {
        strokes = List.copyOf(strokes);
        symbols = List.copyOf(symbols);
    }

    public boolean isEmpty() { return strokes.isEmpty() && symbols.isEmpty(); }
    public boolean hasCentralCreation() {
        int creation = 0;
        java.util.Set<Integer> occupied = new java.util.HashSet<>();
        for (Symbol symbol : symbols) {
            if (symbol.id() < 0 || symbol.id() > 8 || symbol.element() < 0 || symbol.element() > 5) return false;
            if (!occupied.add(symbol.slot()) || symbol.slot() < 0 || symbol.slot() > CENTER_SLOT) return false;
            if (symbol.slot() == CENTER_SLOT && symbol.id() != 0) return false;
            if (symbol.id() == 0) {
                if (symbol.slot() != CENTER_SLOT || symbol.trace().stream().filter(p -> p.x() != 1001 && p.y() != 1001).count() < 6) return false;
                creation++;
            }
        }
        return creation == 1;
    }

    public String encode() {
        StringBuilder out = new StringBuilder(schemeElement >= 0 && schemeLayer >= 0 ? "v4," + schemeElement + "," + schemeLayer : "v2");
        for (Stroke stroke : strokes) out.append(";t,").append(stroke.element()).append(',').append(points(stroke.points()));
        for (Symbol symbol : symbols) out.append(";s,").append(symbol.id()).append(',').append(symbol.element())
                .append(',').append(symbol.slot()).append(',').append(points(symbol.trace()));
        return out.toString();
    }

    private static String points(List<Point> points) {
        StringBuilder out = new StringBuilder();
        for (Point point : points) {
            if (!out.isEmpty()) out.append('|');
            out.append(point.x()).append(':').append(point.y());
        }
        return out.toString();
    }

    public static DiagramLayer decode(String encoded) {
        if (encoded == null || encoded.length() > 32767) return new DiagramLayer(List.of(), List.of());
        boolean legacy = encoded.equals("v1") || encoded.startsWith("v1;");
        boolean oldGeometry=encoded.startsWith("v3,");
        boolean scheme = oldGeometry || encoded.startsWith("v4,");
        if (!legacy && !scheme && !(encoded.equals("v2") || encoded.startsWith("v2;")))
            return new DiagramLayer(List.of(), List.of());
        List<Stroke> strokes = new ArrayList<>();
        List<Symbol> symbols = new ArrayList<>();
        int schemeElement = -1, schemeLayer = -1;
        try {
            if (scheme) {
                String[] header = encoded.split(";",2)[0].split(",",-1);
                if (header.length != 3) throw new NumberFormatException();
                schemeElement = Integer.parseInt(header[1]); schemeLayer = Integer.parseInt(header[2]);
                if(schemeElement < 0 || schemeElement > 5 || schemeLayer < 0 || schemeLayer >= 12) throw new NumberFormatException();
            }
            for (String part : encoded.split(";", -1)) {
                String[] fields = part.split(",", -1);
                if (fields[0].equals("t") && fields.length == 3 && strokes.size() < 128)
                    strokes.add(new Stroke(Integer.parseInt(fields[1]), readPoints(fields[2], false)));
                if (fields[0].equals("s") && fields.length == 5 && symbols.size() < MAX_SYMBOLS) {
                    int id = Integer.parseInt(fields[1]);
                    List<Point> trace = readPoints(fields[4], true);
                    symbols.add(new Symbol(id, Integer.parseInt(fields[2]), Integer.parseInt(fields[3]),
                            legacy ? withoutLegacyFrame(trace, id) : trace));
                }
            }
        } catch (NumberFormatException exception) { return new DiagramLayer(List.of(), List.of()); }
        if(oldGeometry) return migrateV3(strokes,symbols,schemeElement,schemeLayer);
        return new DiagramLayer(strokes, symbols, schemeElement, schemeLayer);
    }
    private static DiagramLayer migrateV3(List<Stroke> strokes,List<Symbol> symbols,int element,int layer) {
        double scale=(.62+layer*.025)*1000;
        List<Stroke> movedStrokes=new ArrayList<>();
        for(Stroke stroke:strokes) {
            boolean moved=false;
            for(int slot=0;slot<8 && !moved;slot++) {
                Point old=oldSlot(element,layer,slot,scale);
                var points=stroke.points();
                if(points.size()<20) continue;
                double mean=0,min=Double.MAX_VALUE,max=0;
                for(Point p:points) {
                    double d=Math.hypot(p.x()-old.x(),p.y()-old.y());
                    mean+=d;min=Math.min(min,d);max=Math.max(max,d);
                }
                mean/=points.size();
                if(mean>15 && mean<105 && max-min<7) {
                    movedStrokes.add(new Stroke(stroke.element(),movePoints(points,old,newSlot(element,layer,slot,scale))));
                    moved=true;
                }
            }
            if(!moved)movedStrokes.add(stroke);
        }
        List<Symbol> movedSymbols=new ArrayList<>();
        for(Symbol symbol:symbols) {
            if(symbol.slot()<0 || symbol.slot()>CENTER_SLOT) { movedSymbols.add(symbol);continue; }
            Point old=symbol.slot()==CENTER_SLOT?new Point(0,0):oldSlot(element,layer,symbol.slot(),scale);
            Point target=symbol.slot()==CENTER_SLOT?new Point(0,0):newSlot(element,layer,symbol.slot(),scale);
            movedSymbols.add(new Symbol(symbol.id(),symbol.element(),symbol.slot(),movePoints(symbol.trace(),old,target)));
        }
        return new DiagramLayer(movedStrokes,movedSymbols,element,layer);
    }
    private static Point oldSlot(int element,int layer,int slot,double scale) {
        double angle=-Math.PI/2+element*Math.PI/24+layer*Math.PI/48+slot*Math.PI/4;
        double radius=.43+((slot+layer+element)%2)*.055;
        return new Point((int)Math.round(radius*scale*Math.cos(angle)),(int)Math.round(radius*scale*Math.sin(angle)));
    }
    private static Point newSlot(int element,int layer,int slot,double scale) {
        var p=ElementSchemes.slot(SpellElement.values()[element],layer,slot);
        return new Point((int)Math.round(p.x()*scale),(int)Math.round(p.y()*scale));
    }
    private static List<Point> movePoints(List<Point> points,Point old,Point target) {
        List<Point> moved=new ArrayList<>();
        for(Point p:points) moved.add(p.x()==1001 && p.y()==1001?p:new Point(target.x()+(int)Math.round((p.x()-old.x())*.5),target.y()+(int)Math.round((p.y()-old.y())*.5)));
        return moved;
    }

    /** Version 1 stamped a circle or polygon before every symbol; remove only that known first path. */
    private static List<Point> withoutLegacyFrame(List<Point> trace, int id) {
        int sides = switch (Math.floorMod(id / 32 + id % 32, 6)) {
            case 1 -> 3; case 2 -> 4; case 3 -> 5; case 4 -> 6; case 5 -> 8; default -> 48;
        };
        int separator = sides + 1;
        if (trace.size() > separator + 1 && trace.get(0).equals(trace.get(sides))
                && trace.get(separator).equals(new Point(1001, 1001)))
            return new ArrayList<>(trace.subList(separator + 1, trace.size()));
        return trace;
    }

    private static List<Point> readPoints(String raw, boolean symbol) {
        List<Point> result = new ArrayList<>();
        if (raw.isEmpty()) return result;
        for (String pair : raw.split("\\|")) {
            if (result.size() >= 2048) break;
            String[] xy = pair.split(":");
            if (xy.length != 2) throw new NumberFormatException();
            int x = Integer.parseInt(xy[0]), y = Integer.parseInt(xy[1]);
            if ((Math.abs(x) > 1000 || Math.abs(y) > 1000) && !(symbol && x == 1001 && y == 1001))
                throw new NumberFormatException();
            result.add(new Point(x, y));
        }
        return result;
    }

    public DiagramLayer overlay(DiagramLayer other) {
        List<Stroke> ink = new ArrayList<>(strokes);
        ink.addAll(other.strokes());
        List<Symbol> marks = new ArrayList<>(symbols);
        marks.addAll(other.symbols());
        return new DiagramLayer(ink, marks);
    }
}
