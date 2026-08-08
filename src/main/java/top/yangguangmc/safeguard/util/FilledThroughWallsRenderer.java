package top.yangguangmc.safeguard.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import top.yangguangmc.safeguard.protection.event.GameRendererCloseEvent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 1.20.6 adapted version: Uses Tessellator + BufferBuilder with POSITION_COLOR
 * format instead of RenderLayer.getTranslucent() (which requires texture/light/normal
 * elements that BoxRenderState doesn't provide).
 */
public class FilledThroughWallsRenderer {
    private final Map<String, List<BoxRenderState>> taggedStates = new ConcurrentHashMap<>();
    private boolean closed = false;

    public void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(this::draw);
        GameRendererCloseEvent.CALLBACK.register(this::close);
    }

    public void addBox(String tag, int x, int y, int z, int argb) {
        addBox(tag, x, y, z, x + 1, y + 1, z + 1, argb);
    }

    public void addBox(String tag, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int argb) {
        taggedStates.computeIfAbsent(tag, k -> new CopyOnWriteArrayList<>())
                .add(new BoxRenderState(minX, minY, minZ, maxX, maxY, maxZ, argb));
    }

    public void clearByTag(String tag) {
        taggedStates.remove(tag);
    }

    private void draw(WorldRenderContext context) {
        if (closed || taggedStates.isEmpty() || taggedStates.values().stream().allMatch(List::isEmpty)) return;

        MatrixStack matrices = Objects.requireNonNull(context.matrixStack());
        Vec3d camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        // 1.20.6: Use Tessellator with POSITION_COLOR format directly.
        // RenderLayer.getTranslucent() has a complex vertex format (position, color,
        // texture, normal, overlay, light) which we can't satisfy with just box data.
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (List<BoxRenderState> list : taggedStates.values()) {
            for (BoxRenderState state : list) {
                float r = ((state.argb() >>> 16) & 0xFF) / 255F;
                float g = ((state.argb() >>> 8) & 0xFF) / 255F;
                float b = (state.argb() & 0xFF) / 255F;
                float a = ((state.argb() >>> 24) & 0xFF) / 255F;
                renderFilledBox(positionMatrix, buffer, state.minX(), state.minY(), state.minZ(), state.maxX(), state.maxY(), state.maxZ(), r, g, b, a);
            }
        }

        tessellator.draw();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    // 8 corners of a box
    private static final float[][] FACES = {
            // Each face: 4 vertices in counter-clockwise winding order
            // Front face (+Z)
            {0, 0, 1,  1, 0, 1,  1, 1, 1,  0, 1, 1},
            // Back face (-Z)
            {1, 0, 0,  0, 0, 0,  0, 1, 0,  1, 1, 0},
            // Left face (-X)
            {0, 0, 0,  0, 0, 1,  0, 1, 1,  0, 1, 0},
            // Right face (+X)
            {1, 0, 1,  1, 0, 0,  1, 1, 0,  1, 1, 1},
            // Top face (+Y)
            {0, 1, 1,  1, 1, 1,  1, 1, 0,  0, 1, 0},
            // Bottom face (-Y)
            {0, 0, 0,  1, 0, 0,  1, 0, 1,  0, 0, 1},
    };

    // Map face corner indices (0=min, 1=max) to actual coordinates
    private float[] corner(int ix, int iy, int iz, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return new float[]{ix == 0 ? minX : maxX, iy == 0 ? minY : maxY, iz == 0 ? minZ : maxZ};
    }

    private void renderFilledBox(Matrix4f matrix, BufferBuilder buffer,
                                 float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                 float red, float green, float blue, float alpha) {
        for (float[] face : FACES) {
            for (int v = 0; v < 12; v += 3) {
                float[] c = corner((int) face[v], (int) face[v + 1], (int) face[v + 2],
                        minX, minY, minZ, maxX, maxY, maxZ);
                buffer.vertex(matrix, c[0], c[1], c[2]).color(red, green, blue, alpha).next();
            }
        }
    }

    public void close() {
        closed = true;
        taggedStates.clear();
    }

    public record BoxRenderState(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int argb) {
    }
}
