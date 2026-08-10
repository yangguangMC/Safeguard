package top.yangguangmc.safeguard.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import top.yangguangmc.safeguard.ModContext;
import top.yangguangmc.safeguard.protection.event.GameRendererCloseEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FilledThroughWallsRenderer {
    public static final RenderPipeline FILLED_THROUGH_WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(ModContext.MOD_ID, "pipeline/filled_through_walls"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthWrite(false)
            .withColorWrite(true)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );
    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);   // RenderType.SMALL_BUFFER_SIZE
    private BufferBuilder buffer;
    private final Map<String, List<BoxRenderState>> taggedStates = new ConcurrentHashMap<>();

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

    /**
     * 清除指定标签下的所有方块。
     *
     * @param tag 标签（通常为检测项或动作的 Identifier 路径）
     */
    public void clearByTag(String tag) {
        taggedStates.remove(tag);
    }

    private void extractAndDraw(WorldRenderContext context) {
        if (taggedStates.isEmpty() || taggedStates.values().stream().allMatch(List::isEmpty)) return;
        // 提取
        renderBox(context);
        // 绘制
        drawFilledThroughWalls(Minecraft.getInstance(), FILLED_THROUGH_WALLS);
    }

    // --------------- 提取阶段 ---------------

    private void renderBox(WorldRenderContext context) {
        PoseStack matrices = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;

        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        if (buffer == null) {
            buffer = new BufferBuilder(ALLOCATOR, FILLED_THROUGH_WALLS.getVertexFormatMode(), FILLED_THROUGH_WALLS.getVertexFormat());
        }

        for (List<BoxRenderState> list : taggedStates.values()) {
            for (BoxRenderState state : list) {
                renderFilledBox(matrices.last().pose(),
                        buffer,
                        state.minX(), state.minY(), state.minZ(),
                        state.maxX(), state.maxY(), state.maxZ(),
                        ((state.argb() >>> 16) & 0xFF) / 255F,
                        ((state.argb() >>> 8) & 0xFF) / 255F,
                        (state.argb() & 0xFF) / 255F,
                        ((state.argb() >>> 24) & 0xFF) / 255F);
            }
        }

        matrices.popPose();
    }

    @SuppressWarnings("SameParameterValue")
    private void renderFilledBox(Matrix4fc positionMatrix, BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue, float alpha) {
        // Front Face
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);

        // Back face
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);

        // Left face
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);

        // Right face
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);

        // Top face
        buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, maxY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, maxY, minZ).setColor(red, green, blue, alpha);

        // Bottom face
        buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(red, green, blue, alpha);
        buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(red, green, blue, alpha);
    }

    // --------------- 绘制阶段 ---------------
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private MappableRingBuffer vertexBuffer;

    private void drawFilledThroughWalls(Minecraft client, @SuppressWarnings("SameParameterValue") RenderPipeline pipeline) {
        // Build the buffer
        MeshData builtBuffer = buffer.buildOrThrow();
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = upload(drawParameters, format, builtBuffer);

        draw(client, pipeline, builtBuffer, drawParameters, vertices, format);

        // Rotate the vertex buffer so we are less likely to use buffers that the GPU is using
        vertexBuffer.rotate();
        buffer = null;
    }

    private GpuBuffer upload(MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {
        // Calculate the size needed for the vertex buffer
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        // Initialize or resize the vertex buffer as needed
        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            if (vertexBuffer != null) {
                vertexBuffer.close();
            }

            vertexBuffer = new MappableRingBuffer(() -> ModContext.MOD_ID + " example render pipeline", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
        }

        // Copy vertex data into the vertex buffer
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
        }

        return vertexBuffer.currentBuffer();
    }

    private static void draw(Minecraft client, RenderPipeline pipeline, MeshData builtBuffer, MeshData.DrawState drawParameters, GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            // Sort the quads if there is translucency
            builtBuffer.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
            // Upload the index buffer
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(Objects.requireNonNull(builtBuffer.indexBuffer()));
            indexType = builtBuffer.drawState().indexType();
        } else {
            // Use the general shape index buffer for non-quad draw modes
            RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.type();
        }

        // Actually execute the draw
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        assert client.getMainRenderTarget().getColorTextureView() != null;
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> ModContext.MOD_ID + " example render pipeline rendering", client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(), client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            // Bind texture if applicable:
            // Sampler0 is used for texture inputs in vertices
            // renderPass.bindTexture("Sampler0", textureSetup.texure0(), textureSetup.sampler0());

            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);

            // The base vertex is the starting index when we copied the data into the vertex buffer divided by vertex size
            //noinspection ConstantValue
            renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
        }

        builtBuffer.close();
    }

    // --------------- 清理 ---------------

    public void close() {
        ALLOCATOR.close();
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }

    public record BoxRenderState(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int argb) {
    }
}