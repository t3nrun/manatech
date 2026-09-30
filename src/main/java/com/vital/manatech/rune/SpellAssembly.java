package com.vital.manatech.rune;

/** A circle requires every authored layer in its cumulative range, not copies of one page. */
public final class SpellAssembly {
    public record Result(String issue,int layer,int circle,int present,int required) {
        public boolean valid() { return issue.isEmpty(); }
    }
    private SpellAssembly() {}
    public static int primaryElement(AssembledSpell spell) {
        int[] counts=new int[6];int primary=0;
        var pages=spell.diagrams();
        if(!pages.isEmpty()&&pages.getFirst().schemeElement()>=0&&pages.getFirst().schemeElement()<6)primary=pages.getFirst().schemeElement();
        for(var page:pages) if(page.schemeElement()>=0&&page.schemeElement()<6)counts[page.schemeElement()]++;
        for(int i=0;i<6;i++)if(counts[i]>counts[primary])primary=i;
        return primary;
    }
    public static int circle(AssembledSpell spell) {
        return spell.diagrams().stream().filter(p->p.schemeLayer()>=0).mapToInt(p->RuneTier.circleForLayer(p.schemeLayer()+1)).max().orElse(1);
    }
    public static Result inspect(AssembledSpell spell) {
        var pages=spell.diagrams();int circle=circle(spell),required=RuneTier.layers(circle),present=0,last=-1;
        boolean[] seen=new boolean[12];
        for(var page:pages)if(page.schemeLayer()>=0&&page.schemeLayer()<12&&!seen[page.schemeLayer()]) { seen[page.schemeLayer()]=true;present++; }
        for(var page:pages) {
            int index=page.schemeLayer();
            if(index<0||index>=12||page.schemeElement()<0||page.schemeElement()>=6)return new Result("profile",0,circle,present,required);
            if(!page.hasCentralCreation())return new Result("creation",index+1,circle,present,required);
            if(!closedWorkingContour(page))return new Result("contour",index+1,circle,present,required);
            if(index<last)return new Result("order",index+1,circle,present,required);
            last=index;
        }
        for(int index=0;index<required;index++)if(!seen[index])return new Result("missing",index+1,circle,present,required);
        if(primaryElement(spell)==2&&circle<3)return new Result("energy",0,circle,present,required);
        if(circle<3&&pages.stream().map(DiagramLayer::schemeElement).distinct().count()>1)return new Result("mixed",0,circle,present,required);
        return new Result("",0,circle,present,required);
    }
    private static boolean closedWorkingContour(DiagramLayer page) {
        var center=page.symbols().stream().filter(s->s.slot()==8).findFirst().orElse(null);
        if(center==null)return false;
        var trace=center.trace().stream().filter(p->p.x()!=1001&&p.y()!=1001).toList();
        double cx=(trace.stream().mapToInt(DiagramLayer.Point::x).min().orElse(0)+trace.stream().mapToInt(DiagramLayer.Point::x).max().orElse(0))/2.;
        double cy=(trace.stream().mapToInt(DiagramLayer.Point::y).min().orElse(0)+trace.stream().mapToInt(DiagramLayer.Point::y).max().orElse(0))/2.;
        for(var stroke:page.strokes()) {
            var p=stroke.points();if(stroke.element()!=page.schemeElement()||p.size()<4)continue;
            if(Math.hypot(p.getFirst().x()-p.getLast().x(),p.getFirst().y()-p.getLast().y())>25)continue;
            boolean inside=false;
            for(int i=0,j=p.size()-1;i<p.size();j=i++) {
                var a=p.get(i);var b=p.get(j);
                if((a.y()>cy)!=(b.y()>cy)&&cx<(b.x()-a.x())*(cy-a.y())/(b.y()-a.y())+a.x())inside=!inside;
            }
            if(inside)return true;
        }
        return false;
    }
}
