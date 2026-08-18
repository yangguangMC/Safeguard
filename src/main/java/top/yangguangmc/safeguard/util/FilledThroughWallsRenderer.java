package top.yangguangmc.safeguard.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import top.yangguangmc.safeguard.ModContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FilledThroughWallsRenderer {
    /**
     * 填充面管线：基于 {@code DEBUG_FILLED_SNIPPET}（{@code POSITION_COLOR} + {@code QUADS} 模式 + 半透明混合）。
     * 深度测试与深度写入均关闭（{@code Optional.empty()}），实现穿墙透视效果。
     */
    public static final RenderPipeline FILLED_THROUGH_WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(ModContext.MOD_ID, "pipeline/filled_through_walls"))
            .withCull(false)
            .withDepthStencilState(Optional.empty())
            .build()
    );

    /**
     * 穿墙填充面的渲染类型：复用 {@link #FILLED_THROUGH_WALLS} 管线，上传时排序以保证半透明混合正确。
     */
    private static final RenderType FILLED_THROUGH_WALLS_TYPE = RenderType.create(
            "filled_through_walls",
            RenderSetup.builder(FILLED_THROUGH_WALLS).sortOnUpload().createRenderSetup()
    );

    private final Map<String, List<BoxRenderState>> taggedStates = new ConcurrentHashMap<>();

    public void init() {
        LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(this::extractAndDraw);
    }

    public void addBox(String tag, int x, int y, int z, int argb) {
        addBox(tag, x, y, z, x + 1, y + 1, z + 1, argb);
    }

    public void addBox(String tag, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int argb) {
        taggedStates.computeIfAbsent(tag, k -> new CopyOnWriteArrayList<>())
                .add(new BoxRenderState(minX, minY, minZ, maxX, maxY, maxZ, argb));
    }

    /**
     * 清除指定标签下的所有方块。
     *
     * @param tag 标签（通常为检测项或动作的 Identifier 路径）
     */
    public void clearByTag(String tag) {
        taggedStates.remove(tag);
    }

    private void extractAndDraw(LevelRenderContext context) {
        if (taggedStates.isEmpty() || taggedStates.values().stream().allMatch(List::isEmpty)) {
            return;
        }

        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        context.submitNodeCollector().submitCustomGeometry(matrices, FILLED_THROUGH_WALLS_TYPE, this::renderBoxes);
        matrices.popPose();
    }

    private void renderBoxes(PoseStack.Pose pose, VertexConsumer buffer) {
        for (List<BoxRenderState> list : taggedStates.values()) {
            for (BoxRenderState state : list) {
                renderFilledBox(pose,
                        buffer,
                        state.minX(), state.minY(), state.minZ(),
                        state.maxX(), state.maxY(), state.maxZ(),
                        ((state.argb() >>> 16) & 0xFF) / 255F,
                        ((state.argb() >>> 8) & 0xFF) / 255F,
                        (state.argb() & 0xFF) / 255F,
                        ((state.argb() >>> 24) & 0xFF) / 255F);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void renderFilledBox(PoseStack.Pose pose, VertexConsumer buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue, float alpha) {
        // Front Face
        buffer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha);

        // Back face
        buffer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha);

        // Left face
        buffer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha);

        // Right face
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha);

        // Top face
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha);

        // Bottom face
        buffer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha);
    }

    public record BoxRenderState(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int argb) {
    }
}
