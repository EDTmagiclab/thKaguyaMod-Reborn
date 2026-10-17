package thKaguyaMod.client.render.shot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.client.render.THRenderTypes;
import thKaguyaMod.entity.shot.EntitySanaeWind;

/**
 * 早苗の風起こし（風弾）の描画 —— 原版 {@code RenderSanaeWind}（1.7.10, 83 行）の移植。
 *
 * <p>原版の要点：
 * <ul>
 *   <li>貼图 {@code thkaguyamod:textures/shot/WindShot.png}（移植版 {@code thkaguya:textures/shot/windshot.png}、64×32）</li>
 *   <li>{@code glScalef(0.5F, 0.5F, 0.5F)}（＝当たり判定 1.0 に対して見た目 0.5 倍）</li>
 *   <li>混合 {@code GL_SRC_ALPHA, GL_ONE}（加算＋alpha）、{@code glDisable(GL_CULL_FACE)} の両面描画</li>
 *   <li>8 枚のクワッドを Y 軸 45° ずつ回して「渦」に見せる</li>
 *   <li>アニメは {@code ticksExisted % 4} の 4 コマ（UV を 32×16 のグリッドで切る）</li>
 *   <li>幅を {@code rand1 = nextInt(50)/100F}、長さを {@code 4.0F + nextInt(100)/100F} で振る</li>
 * </ul>
 *
 * <p>1.7.10 は毎フレーム {@code new Random()} していたため幅がちらついた。
 * 1.20.1 では「弾 ID + tick」で種を決め、同じ揺らぎをちらつき無しで再現する。
 *
 * <p>このレンダラーが効くのは、{@link EntitySanaeWind} の通常コンストラクタが
 * {@code THKaguyaEntities.SANAE_WIND} を明示的に渡している場合だけ
 * （基底の便宜コンストラクタ経由だと EntityType が TH_SHOT になり、
 * 汎用 {@code RenderTHShot} で赤い丸弾として描画される）。
 */
public class RenderSanaeWind extends EntityRenderer<EntitySanaeWind>
{
	private static final ResourceLocation WIND_TEX =
			new ResourceLocation(THKaguyaCore.MODID, "textures/shot/windshot.png");

	public RenderSanaeWind(EntityRendererProvider.Context context)
	{
		super(context);
		this.shadowRadius = 0.0F;
	}

	@Override
	public ResourceLocation getTextureLocation(EntitySanaeWind entity)
	{
		return WIND_TEX;
	}

	@Override
	public void render(EntitySanaeWind entity, float yaw, float partialTicks, PoseStack poseStack,
			MultiBufferSource buffer, int packedLight)
	{
		// 原版 doRender: glScalef(f2 / 1.0F, f2 / 1.0F, f2 / 1.0F)（f2 = 0.5F）
		poseStack.pushPose();
		poseStack.scale(0.5F, 0.5F, 0.5F);

		// 原版: int pattern = sanaeWind.ticksExisted % 4;
		int pattern = entity.getWindRenderPattern();

		// 原版 UV（64×32 貼图を 32×16 の 4 コマに分割）
		float f3 = (float)((pattern % 2) * 32 + 0) / 64F;
		float f4 = (float)((pattern % 2) * 32 + 32) / 64F;
		float f5 = (float)((int)(pattern / 2) * 16 + 0) / 32F;
		float f6 = (float)((int)(pattern / 2) * 16 + 16) / 32F;

		RandomSource random = RandomSource.create(entity.getId() * 1000003L + entity.tickCount);
		float rand1 = (float)random.nextInt(50) / 100F;
		float rand2 = (float)random.nextInt(100) / 100F;

		float f7 = rand1;
		float f8 = 4.0F + rand2;
		float f9 = 0.5F;

		VertexConsumer consumer = buffer.getBuffer(THRenderTypes.additive(WIND_TEX));

		for(int i = 0; i < 8; i++)
		{
			// 原版: GL11.glRotatef(45F, 0.0F, 1.0F, 0.0F);
			poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));

			Matrix4f m = poseStack.last().pose();
			Matrix3f n = poseStack.last().normal();

			// 原版 quad: (-f7, -1-f9, 0) → (f7, -1-f9, 0) → (f8, 2-f9, 2) → (-f8, 2-f9, 2)
			// 色は原版 setColorRGBA_F(0.1F, 0.9F, 0.6F, 0.7F)
			vertex(consumer, m, n, -f7, -1.0F - f9, 0.0F, f3, f6);
			vertex(consumer, m, n,  f7, -1.0F - f9, 0.0F, f4, f6);
			vertex(consumer, m, n,  f8,  2.0F - f9, 2.0F, f4, f5);
			vertex(consumer, m, n, -f8,  2.0F - f9, 2.0F, f3, f5);
		}

		poseStack.popPose();

		super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
	}

	private static void vertex(VertexConsumer consumer, Matrix4f m, Matrix3f n,
			float x, float y, float z, float u, float v)
	{
		consumer.vertex(m, x, y, z)
				.color(0.1F, 0.9F, 0.6F, 0.7F)
				.uv(u, v)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(LightTexture.FULL_BRIGHT)
				.normal(n, 0.0F, 1.0F, 0.0F)
				.endVertex();
	}
}
