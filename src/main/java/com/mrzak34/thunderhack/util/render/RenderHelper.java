package com.mrzak34.thunderhack.util.render;

import com.mrzak34.thunderhack.Thunderhack;
import com.mrzak34.thunderhack.gui.fontstuff.FontRender;
import com.mrzak34.thunderhack.gui.hud.elements.RadarRewrite;
import com.mrzak34.thunderhack.mixin.mixins.IRenderManager;
import com.mrzak34.thunderhack.util.Util;
import com.mrzak34.thunderhack.util.gaussianblur.GaussianFilter;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import static com.mrzak34.thunderhack.gui.hud.elements.RadarRewrite.astolfo;
import static com.mrzak34.thunderhack.modules.render.ItemESP.astolfo2;
import static com.mrzak34.thunderhack.util.Util.mc;
import static com.mrzak34.thunderhack.util.render.RenderUtil.TwoColoreffect;
import static org.lwjgl.opengl.GL11.*;

public class RenderHelper {

    public static Frustum frustum = new Frustum();

    private static final HashMap<Integer, Integer> shadowCache = new HashMap();


    public static void drawBlurredShadow(float x, float y, float width, float height, int blurRadius, Color color) {
        BufferedImage original = null;
        GaussianFilter op = null;
        GL11.glPushMatrix();
        GlStateManager.alphaFunc(516, 0.01f);
        float _X = (x -= (float) blurRadius) - 0.25f;
        float _Y = (y -= (float) blurRadius) + 0.25f;
        int identifier = String.valueOf((width += (float) (blurRadius * 2)) * (height += (float) (blurRadius * 2)) + width + (float) (1000000000 * blurRadius) + (float) blurRadius).hashCode();
        GL11.glEnable(3553);
        GL11.glDisable(2884);
        GL11.glEnable(3008);
        GlStateManager.enableBlend();
        int texId = -1;
        if (shadowCache.containsKey(identifier)) {
            texId = shadowCache.get(identifier);
            GlStateManager.bindTexture(texId);
        } else {
            if (width <= 0.0f) {
                width = 1.0f;
            }
            if (height <= 0.0f) {
                height = 1.0f;
            }
            if (original == null) {
                original = new BufferedImage((int) width, (int) height, 3);
            }
            Graphics g = original.getGraphics();
            g.setColor(Color.white);
            g.fillRect(blurRadius, blurRadius, (int) (width - (float) (blurRadius * 2)), (int) (height - (float) (blurRadius * 2)));
            g.dispose();
            if (op == null) {
                op = new GaussianFilter((float) blurRadius);
            }
            BufferedImage blurred = op.filter(original, null);
            texId = TextureUtil.uploadTextureImageAllocate(TextureUtil.glGenTextures(), blurred, true, false);
            shadowCache.put(identifier, texId);
        }
        GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        glBegin(7);
        GL11.glTexCoord2f(0.0f, 0.0f);
        glVertex2f(_X, _Y);
        GL11.glTexCoord2f(0.0f, 1.0f);
        glVertex2f(_X, _Y + height);
        GL11.glTexCoord2f(1.0f, 1.0f);
        glVertex2f(_X + width, _Y + height);
        GL11.glTexCoord2f(1.0f, 0.0f);
        glVertex2f(_X + width, _Y);
        glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GL11.glEnable(2884);
        GL11.glPopMatrix();
    }

    public static boolean isInViewFrustum(Entity entity) {
        return (isInViewFrustum(entity.getEntityBoundingBox()) || entity.ignoreFrustumCheck);
    }

    private static boolean isInViewFrustum(AxisAlignedBB bb) {
        Entity current = Util.mc.getRenderViewEntity();
        if (current != null) {
            frustum.setPosition(current.posX, current.posY, current.posZ);
        }
        return frustum.isBoundingBoxInFrustum(bb);
    }


    public static void setColor(int color) {
        GL11.glColor4ub((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color & 0xFF), (byte) (color >> 24 & 0xFF));
    }

    public static void setColor(Color color, float alpha) {
        float red = color.getRed() / 255F;
        float green = color.getGreen() / 255F;
        float blue = color.getBlue() / 255F;
        GlStateManager.color(red, green, blue, alpha);
    }

    public static void drawCircle3D(Entity entity, double radius, float partialTicks, int points, float width, int color, boolean astolfo) {
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(width);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        glBegin(GL11.GL_LINE_STRIP);
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - ((IRenderManager)Util.mc.getRenderManager()).getRenderPosX();
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - ((IRenderManager)Util.mc.getRenderManager()).getRenderPosY();
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - ((IRenderManager)Util.mc.getRenderManager()).getRenderPosZ();
        if (!astolfo) {
            setColor(color);
            for (int i = 0; i <= points; i++) {
                GL11.glVertex3d(x + radius * Math.cos(i * 6.28 / points), y, z + radius * Math.sin(i * 6.28 / points));
            }
        } else {
            for (int i = 0; i <= points; i++) {
                setColor(astolfo2.getColor(i));
                GL11.glVertex3d(x + radius * Math.cos(i * 6.28 / points), y, z + radius * Math.sin(i * 6.28 / points));
            }
        }
        glEnd();
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }


    public static void drawEntityBox(Entity entity, Color color, Color color2, boolean fullBox, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GlStateManager.glLineWidth(2);
        GlStateManager.disableTexture2D();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.depthMask(false);
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.getRenderPartialTicks() - ((IRenderManager)Util.mc.getRenderManager()).getRenderPosX();
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.getRenderPartialTicks() - ((IRenderManager)Util.mc.getRenderManager()).getRenderPosY();
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.getRenderPartialTicks() - ((IRenderManager)Util.mc.getRenderManager()).getRenderPosZ();
        AxisAlignedBB axisAlignedBB = entity.getEntityBoundingBox();
        AxisAlignedBB axisAlignedBB2 = new AxisAlignedBB(axisAlignedBB.minX - entity.posX + x - 0.05, axisAlignedBB.minY - entity.posY + y, axisAlignedBB.minZ - entity.posZ + z - 0.05, axisAlignedBB.maxX - entity.posX + x + 0.05, axisAlignedBB.maxY - entity.posY + y + 0.15, axisAlignedBB.maxZ - entity.posZ + z + 0.05);
        GlStateManager.glLineWidth(2.0F);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GlStateManager.color(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, alpha);
        if (fullBox) {
            drawColorBox(axisAlignedBB2, color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, alpha);
            GlStateManager.color(color2.getRed() / 255F, color2.getGreen() / 255F, color2.getBlue() / 255F, alpha);
        }
        drawSelectionBoundingBox(axisAlignedBB2);
        GlStateManager.glLineWidth(2);
        GlStateManager.enableTexture2D();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }


    public static void drawSelectionBoundingBox(AxisAlignedBB boundingBox) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();

        builder.begin(3, DefaultVertexFormats.POSITION);
        builder.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        builder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        builder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        builder.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        builder.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        tessellator.draw();
        builder.begin(3, DefaultVertexFormats.POSITION);
        builder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        builder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        builder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        builder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        builder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        tessellator.draw();
        builder.begin(1, DefaultVertexFormats.POSITION);
        builder.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex();
        builder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex();
        builder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex();
        builder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex();
        builder.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex();
        builder.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        builder.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex();
        builder.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex();
        tessellator.draw();
    }


    public static void drawCircle(float x, float y, float start, float end, float radius, boolean filled, Color color) {

        float sin;
        float cos;
        float i;
        GlStateManager.color(0, 0, 0, 0);

        float endOffset;
        if (start > end) {
            endOffset = end;
            end = start;
            start = endOffset;
        }

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        if (color != null)
            setColor(color.getRGB());


        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2);
        if (filled) {
            glBegin(GL11.GL_TRIANGLE_FAN);
        } else {
            glBegin(GL11.GL_LINE_STRIP);
        }
        for (i = end; i >= start; i -= 5) {

            if (color == null) {
                double stage = (i + 90) / 360.;
                int clr = astolfo.getColor(stage);
                int red = ((clr >> 16) & 255);
                int green = ((clr >> 8) & 255);
                int blue = ((clr & 255));

                GL11.glColor4f(red / 255f, green / 255f, blue / 255f, 1);
            }
            cos = (float) Math.cos(i * Math.PI / 180) * radius;
            sin = (float) Math.sin(i * Math.PI / 180) * radius;
            glVertex2f(x + cos, y + sin);
        }


        glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();

    }


    public static void drawElipse(float x, float y, float rx, float ry, float start, float end, float radius, Color color, int stage1, RadarRewrite.mode2 cmode) {
        float sin;
        float cos;
        float i;
        GlStateManager.color(0, 0, 0, 0);
        float endOffset;
        if (start > end) {
            endOffset = end;
            end = start;
            start = endOffset;
        }
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2);
        glBegin(GL11.GL_LINE_STRIP);
        for (i = start; i <= end; i += 2) {

                double stage = (i - start) / 360;
                Color clr = null;

                if(cmode == RadarRewrite.mode2.Astolfo) {
                   clr = new Color(astolfo.getColor(stage));
                }else if(cmode == RadarRewrite.mode2.Rainbow){
                    clr = color;
                }  else if(cmode == RadarRewrite.mode2.Custom){
                    clr = color;
                } else {
                    clr = TwoColoreffect(color, Thunderhack.moduleManager.getModuleByClass(RadarRewrite.class).cColor2.getValue().getColorObject(), Math.abs(System.currentTimeMillis() / 10) / 100.0 + i * ((20f - Thunderhack.moduleManager.getModuleByClass(RadarRewrite.class).colorOffset1.getValue()) / 200) );
                }

            int clr2 = clr.getRGB();
            int red = ((clr2 >> 16) & 255);
            int green = ((clr2 >> 8) & 255);
            int blue = ((clr2 & 255));

            GL11.glColor4f(red / 255f, green / 255f, blue / 255f, 1);



            cos = (float) Math.cos(i * Math.PI / 180) * (radius / ry);
            sin = (float) Math.sin(i * Math.PI / 180) * (radius / rx);
            glVertex2f((x + cos), (y + sin));
        }
        glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        if (stage1 != -1) {
            cos = (float) Math.cos((start - 15) * Math.PI / 180) * (radius / ry);
            sin = (float) Math.sin((start - 15) * Math.PI / 180) * (radius / rx);

            switch (stage1) {
                case 0: {
                    FontRender.drawCentString3("W", (x + cos), (y + sin), -1);
                    break;
                }
                case 1: {
                    FontRender.drawCentString3("N", (x + cos), (y + sin), -1);
                    break;
                }
                case 2: {
                    FontRender.drawCentString3("E", (x + cos), (y + sin), -1);

                    break;
                }
                case 3: {
                    FontRender.drawCentString3("S", (x + cos), (y + sin), -1);
                    break;
                }
            }
        }
    }


    public static void drawCircle(float x, float y, float radius, boolean filled, Color color) {
        drawCircle(x, y, 0, 360, radius, filled, color);
    }

    public static void drawEllipsCompas(int yaw, float x, float y, float x2, float y2, float radius, Color color, boolean Dir, RadarRewrite.mode2 mode) {
        if (Dir) {
            drawElipse(x, y, x2, y2, 15 + yaw, 75 + yaw, radius, color, 0,mode);
            drawElipse(x, y, x2, y2, 105 + yaw, 165 + yaw, radius, color, 1,mode);
            drawElipse(x, y, x2, y2, 195 + yaw, 255 + yaw, radius, color, 2,mode);
            drawElipse(x, y, x2, y2, 285 + yaw, 345 + yaw, radius, color, 3,mode);
        } else {
            drawElipse(x, y, x2, y2, 15 + yaw, 75 + yaw, radius, color, -1,mode);
            drawElipse(x, y, x2, y2, 105 + yaw, 165 + yaw, radius, color, -1,mode);
            drawElipse(x, y, x2, y2, 195 + yaw, 255 + yaw, radius, color, -1,mode);
            drawElipse(x, y, x2, y2, 285 + yaw, 345 + yaw, radius, color, -1,mode);
        }
    }


    public static void drawColorBox(AxisAlignedBB axisalignedbb, float red, float green, float blue, float alpha) {

        Tessellator ts = Tessellator.getInstance();
        BufferBuilder buffer = ts.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);

        buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        ts.draw();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        ts.draw();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        ts.draw();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        ts.draw();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        ts.draw();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.maxY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.maxY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ).color(red, green, blue, alpha).endVertex();
        ts.draw();
    }


}
