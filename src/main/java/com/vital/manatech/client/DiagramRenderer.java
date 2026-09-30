package com.vital.manatech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vital.manatech.rune.CircleEffectMesh;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/** Draws the GENERATED effect model. Rotation, translation, and scale belong to the caller's PoseStack. */
public final class DiagramRenderer {
    private DiagramRenderer() {}

    public static void render(CircleEffectMesh mesh, PoseStack pose, MultiBufferSource buffers, float radius, float phase) {
        VertexConsumer output = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = pose.last().pose();
        for (CircleEffectMesh.Segment segment : mesh.segments()) {
            float x1 = segment.x1()*radius, z1 = segment.y1()*radius;
            float x2 = segment.x2()*radius, z2 = segment.y2()*radius;
            float dx=x2-x1, dz=z2-z1, len=(float)Math.hypot(dx,dz);
            if (len < .0001f) continue;
            float y=segment.layer()*.002f;
            float pulse=(float)(.8+.2*Math.sin(phase-segment.layer()*.4));
            ribbon(output,matrix,x1,z1,x2,z2,y,.014f,segment.color(),(int)(20*pulse));
            ribbon(output,matrix,x1,z1,x2,z2,y+.0005f,.006f,segment.color(),(int)(140*pulse));
            ribbon(output,matrix,x1,z1,x2,z2,y+.001f,.0025f,
                    HologramStyle.core(segment.color()),(int)(245*pulse));
        }
    }

    private static void ribbon(VertexConsumer out, Matrix4f matrix, float x1, float z1,
                               float x2, float z2, float y, float width, int color, int alpha) {
        float dx=x2-x1, dz=z2-z1, len=(float)Math.hypot(dx,dz);
        if (len < .0001f) return;
        float cap = Math.min(width / 2, len / 2);
        x1 -= dx / len * cap; z1 -= dz / len * cap;
        x2 += dx / len * cap; z2 += dz / len * cap;
        float nx=-dz/len*width/2, nz=dx/len*width/2;
        vertex(out,matrix,x1+nx,y,z1+nz,color,alpha);
        vertex(out,matrix,x2+nx,y,z2+nz,color,alpha);
        vertex(out,matrix,x2-nx,y,z2-nz,color,alpha);
        vertex(out,matrix,x1-nx,y,z1-nz,color,alpha);
        vertex(out,matrix,x1-nx,y,z1-nz,color,alpha);
        vertex(out,matrix,x2-nx,y,z2-nz,color,alpha);
        vertex(out,matrix,x2+nx,y,z2+nz,color,alpha);
        vertex(out,matrix,x1+nx,y,z1+nz,color,alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4f m, float x,float y,float z,int color,int alpha) {
        out.addVertex(m,x,y,z).setColor((color>>16)&255,(color>>8)&255,color&255,alpha)
                .setUv(0,0).setOverlay(0).setLight(0xF000F0).setNormal(0,1,0);
    }
}
