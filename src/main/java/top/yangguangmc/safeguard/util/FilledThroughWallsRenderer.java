package top.yangguangmc.safeguard.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.event.GameRendererCloseEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 1.20.6 adapted version: Uses the pre-1.21 rendering system
 * (Tessellator, BufferBuilder, VertexConsumerProvider) instead of
 * the new RenderPipeline/CommandEncoder/RenderPass API.
 */
public class FilledThroughWallsRenderer {
    private static final RenderLayer LAYER = RenderLayer.getTranslucent();
    private final Map<String, List<BoxRenderState>> taggedStates = new ConcurrentHashMap<>();
    private boolean closed = false;

    public void init() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(this::extractAndDraw);
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

    private void extractAndDraw(WorldRenderContext context) {
        if (closed || taggedStates.isEmpty() || taggedStates.values().stream().allMatch(List::isEmpty)) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();

        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
        VertexConsumerProvider.Immediate immediate = context.consumers();
        // Use a translucent vertex consumer that supports translucency and no depth test
        VertexConsumer vertexConsumer = context.consumers().getBuffer(RenderLayer.getTranslucent());

        for (List<BoxRenderState> list : taggedStates.values()) {
            for (BoxRenderState state : list) {
                float r = ((state.argb() >>> 16) & 0xFF) / 255F;
                float g = ((state.argb() >>> 8) & 0xFF) / 255F;
                float b = (state.argb() & 0xFF) / 255F;
                float a = ((state.argb() >>> 24) & 0xFF) / 255F;
                renderFilledBox(positionMatrix, vertexConsumer, state.minX(), state.minY(), state.minZ(), state.maxX(), state.maxY(), state.maxZ(), r, g, b, a);
            }
        }

        matrices.pop();
    }

    private static final int[] VERTEX_ORDER = {
            // Each quad: 4 vertices. We use QUADS draw mode.
            // Front (+Z)
            0, 1, 2, 3,
            // Back (-Z)
            1, 0, 4, 5,
            // Left (-X)
            0, 3, 7, 4,
            // Right (+X)
            1, 5, 6, 2,
            // Top (+Y)
            3, 2, 6, 7,
            // Bottom (-Y)
            0, 4, 5, 1
    };

    private void renderFilledBox(Matrix4f matrix, VertexConsumer vertexConsumer,
                                  float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                  float red, float green, float blue, float alpha) {
        // 8 corners
        float[][] corners = {
                {minX, minY, minZ}, // 0
                {maxX, minY, minZ}, // 1
                {maxX, maxY, minZ}, // 2
                {minX, maxY, minZ}, // 3
                {minX, minY, maxZ}, // 4
                {maxX, minY, maxZ}, // 5
                {maxX, maxY, maxZ}, // 6
                {minX, maxY, maxZ}, // 7
        };

        // 6 faces × 4 vertices = 24 vertices
        for (int i = 0; i < VERTEX_ORDER.length; i++) {
            float[] corner = corners[VERTEX_ORDER[i]];
            vertexConsumer.vertex(matrix, corner[0], corner[1], corner[2]).color(red, green, blue, alpha).next();
        }
    }

    public void close() {
        closed = true;
        taggedStates.clear();
    }

    public record BoxRenderState(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int argb) {
    }
}
