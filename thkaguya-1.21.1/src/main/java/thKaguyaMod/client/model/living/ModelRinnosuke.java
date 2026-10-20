package thKaguyaMod.client.model.living;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.living.EntityRinnosuke;

/**
 * 森近霖之助の模型（1.20.1 移植版）
 *
 * 原版 extract/sources/java/thKaguyaMod/client/model/living/ModelRinnosuke.java（222 行，extends ModelBase，64x64）：
 * ・bipedBody      texOffs(0,16)   addBox(-4,0,-2,    8,10,4)   rotationPoint( 0, 0,0)   ← root
 * ・box           texOffs(32,10)  addBox(-3,-2,0,    6, 4,2)   rotationPoint( 0, 8,-4)  ← bipedBody の子
 * ・bottom        texOffs(0,46)   addBox(-4.5,0,-2.5,9,13,5)   rotationPoint( 0, 8,0)   ← bipedBody の子
 * ・bipedRightArm texOffs(0,32)   addBox(-2,-1,-1.5, 3,10,3)   rotationPoint(-5, 1,0)   ← root
 * ・bipedLeftArm  texOffs(12,32)  addBox(-1,-1,-1.5, 3,10,3)   rotationPoint( 5, 1,0)   ← root
 * ・bipedRightLeg texOffs(48,16)  addBox(-1,0,-2,    4,14,4)   rotationPoint(-3,10,0)   ← root
 * ・bipedLeftLeg  texOffs(32,16)  addBox(-2,0,-2,    4,14,4)   rotationPoint( 2,10,0)   ← root
 * ・bipedHead     texOffs(0,0)    addBox(-4,-8,-4,   8, 8,8)   rotationPoint( 0, 0,0)   ← root
 * ・longHair      texOffs(24,0)   addBox(-4,0,-3,    8, 2,3)   rotationPoint( 0, 0,4)   ← bipedHead の子
 * ・Grass         texOffs(48,0)   addBox(-4,0,0,     8, 2,1)   rotationPoint( 0,-3,-4.2)← bipedHead の子
 * 全パーツ mirror = true、初期 rotateAngle は全て 0。
 *
 * 原版 render() は bipedBody / 両腕 / 両脚 / bipedHead を個別に render している（頭手足は body の子ではない）ので、
 * 1.20.1 でもそれらは root 直下の兄弟パーツとして持たせる。子は addChild されていた box/bottom と longHair/Grass のみ。
 *
 * 頭の視線追従は原版が setRotationAngles 内で直接 bipedHead を回しているので ModelWithHead は不要。
 */
public class ModelRinnosuke extends EntityModel<EntityRinnosuke>
{
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "rinnosuke"), "main");

    /** 180F / (float)Math.PI の逆数。原版の「度 → ラジアン」変換をそのまま表す */
    private static final float DEG_TO_RAD = (float) Math.PI / 180F;

    private final ModelPart root;
    private final ModelPart bipedBody;
    private final ModelPart bipedRightArm;
    private final ModelPart bipedLeftArm;
    private final ModelPart bipedRightLeg;
    private final ModelPart bipedLeftLeg;
    private final ModelPart box;
    private final ModelPart bottom;
    private final ModelPart bipedHead;
    private final ModelPart longHair;
    private final ModelPart Grass;

    public ModelRinnosuke(ModelPart root)
    {
        this.root = root;
        this.bipedBody = root.getChild("bipedBody");
        this.box = this.bipedBody.getChild("box");
        this.bottom = this.bipedBody.getChild("bottom");
        this.bipedRightArm = root.getChild("bipedRightArm");
        this.bipedLeftArm = root.getChild("bipedLeftArm");
        this.bipedRightLeg = root.getChild("bipedRightLeg");
        this.bipedLeftLeg = root.getChild("bipedLeftLeg");
        this.bipedHead = root.getChild("bipedHead");
        this.longHair = this.bipedHead.getChild("longHair");
        this.Grass = this.bipedHead.getChild("Grass");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // bipedBody texOffs(0,16) addBox(-4,0,-2, 8,10,4) rotationPoint(0,0,0)
        PartDefinition bipedBody = root.addOrReplaceChild("bipedBody",
            CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-4F, 0F, -2F, 8, 10, 4),
            PartPose.offset(0F, 0F, 0F));
            // box texOffs(32,10) addBox(-3,-2,0, 6,4,2) rotationPoint(0,8,-4)
            PartDefinition box = bipedBody.addOrReplaceChild("box",
                CubeListBuilder.create().texOffs(32, 10).mirror().addBox(-3F, -2F, 0F, 6, 4, 2),
                PartPose.offset(0F, 8F, -4F));
            // bottom texOffs(0,46) addBox(-4.5,0,-2.5, 9,13,5) rotationPoint(0,8,0)
            PartDefinition bottom = bipedBody.addOrReplaceChild("bottom",
                CubeListBuilder.create().texOffs(0, 46).mirror().addBox(-4.5F, 0F, -2.5F, 9, 13, 5),
                PartPose.offset(0F, 8F, 0F));
        // bipedRightArm texOffs(0,32) addBox(-2,-1,-1.5, 3,10,3) rotationPoint(-5,1,0)
        PartDefinition bipedRightArm = root.addOrReplaceChild("bipedRightArm",
            CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-2F, -1F, -1.5F, 3, 10, 3),
            PartPose.offset(-5F, 1F, 0F));
        // bipedLeftArm texOffs(12,32) addBox(-1,-1,-1.5, 3,10,3) rotationPoint(5,1,0)
        PartDefinition bipedLeftArm = root.addOrReplaceChild("bipedLeftArm",
            CubeListBuilder.create().texOffs(12, 32).mirror().addBox(-1F, -1F, -1.5F, 3, 10, 3),
            PartPose.offset(5F, 1F, 0F));
        // bipedRightLeg texOffs(48,16) addBox(-1,0,-2, 4,14,4) rotationPoint(-3,10,0)
        PartDefinition bipedRightLeg = root.addOrReplaceChild("bipedRightLeg",
            CubeListBuilder.create().texOffs(48, 16).mirror().addBox(-1F, 0F, -2F, 4, 14, 4),
            PartPose.offset(-3F, 10F, 0F));
        // bipedLeftLeg texOffs(32,16) addBox(-2,0,-2, 4,14,4) rotationPoint(2,10,0)
        PartDefinition bipedLeftLeg = root.addOrReplaceChild("bipedLeftLeg",
            CubeListBuilder.create().texOffs(32, 16).mirror().addBox(-2F, 0F, -2F, 4, 14, 4),
            PartPose.offset(2F, 10F, 0F));
        // bipedHead texOffs(0,0) addBox(-4,-8,-4, 8,8,8) rotationPoint(0,0,0)
        PartDefinition bipedHead = root.addOrReplaceChild("bipedHead",
            CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4F, -8F, -4F, 8, 8, 8),
            PartPose.offset(0F, 0F, 0F));
            // longHair texOffs(24,0) addBox(-4,0,-3, 8,2,3) rotationPoint(0,0,4)
            PartDefinition longHair = bipedHead.addOrReplaceChild("longHair",
                CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-4F, 0F, -3F, 8, 2, 3),
                PartPose.offset(0F, 0F, 4F));
            // Grass texOffs(48,0) addBox(-4,0,0, 8,2,1) rotationPoint(0,-3,-4.2)
            PartDefinition Grass = bipedHead.addOrReplaceChild("Grass",
                CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-4F, 0F, 0F, 8, 2, 1),
                PartPose.offset(0F, -3F, -4.2F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * 原版 setRotationAngles(float movement, float far, float f2, float f3, float f4, float f5, Entity entity)
     * の逐行移植。対応：movement→limbSwing / far→limbSwingAmount / f2→ageInTicks /
     * f3→netHeadYaw(度) / f4→headPitch(度)。1.20.1 の netHeadYaw・headPitch も「度」で渡ってくるので
     * 原版の f3 / (180F / PI) はそのまま netHeadYaw * (PI / 180F) と等価。
     *
     * 原版は全て「代入」で、rotationPointY/Z への += は丸ごとコメントアウトされた isSneak ブロックの中にしか
     * 存在しない（生きているコードには無い）。よって rotationPoint のドリフトは起きず、y/x/z のリセット処理は不要。
     */
    @Override
    public void setupAnim(EntityRinnosuke entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch)
    {
        // 原版162-163: this.bipedHead.rotateAngleY = f3 / (180F / PI); this.bipedHead.rotateAngleX = f4 / (180F / PI);
        this.bipedHead.yRot = netHeadYaw * DEG_TO_RAD;
        this.bipedHead.xRot = headPitch * DEG_TO_RAD;

        // 原版164: this.bipedBody.rotateAngleY = MathHelper.sin(MathHelper.sqrt_float(onGround) * PI * 2.0F) * 0.2F;
        // 1.7.10 の onGround は ModelBase の public フィールドで、RenderLiving からは一度も代入されない
        // （isRiding だけが設定される）。つまり常に 0.0F → sin(sqrt(0) * 2PI) = 0 なので結果は常に 0。
        // 1.20.1 に onGround 相当は無いため、原版の実効値 0 をそのまま代入する。
        this.bipedBody.yRot = 0.0F;

        // 原版166-167: 脚の前後振り
        this.bipedRightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 0.7F * limbSwingAmount;
        this.bipedLeftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.7F * limbSwingAmount;
        // 原版168-171: 脚の Y/Z 回転を明示的に 0（前フレームの値を持ち越さないため）
        this.bipedRightLeg.yRot = 0.0F;
        this.bipedLeftLeg.yRot = 0.0F;
        this.bipedRightLeg.zRot = 0.0F;
        this.bipedLeftLeg.zRot = 0.0F;

        // 原版173-174: 腕の前後振り（左右で位相が逆）
        this.bipedRightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.bipedLeftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        // 原版176/178: bipedRightArm.rotateAngleZ = 10F/180F*PI; bipedLeftArm.rotateAngleZ = -右腕
        this.bipedRightArm.zRot = 10F * DEG_TO_RAD;
        this.bipedLeftArm.zRot = -this.bipedRightArm.zRot;

        // 原版180-192: if (this.isRiding) → 1.20.1 は entity.isPassenger()
        if (entity != null && entity.isPassenger())
        {
            this.bipedRightArm.xRot = -((float) Math.PI / 5F);
            this.bipedLeftArm.xRot = -((float) Math.PI / 5F);
            this.bipedRightLeg.xRot = -((float) Math.PI * 2F / 5F);
            this.bipedLeftLeg.xRot = -((float) Math.PI * 2F / 5F);
            this.bipedRightLeg.yRot = ((float) Math.PI / 14F);
            this.bipedLeftLeg.yRot = -((float) Math.PI / 14F);
            this.bipedRightLeg.zRot = ((float) Math.PI / 14F);
            this.bipedLeftLeg.zRot = -((float) Math.PI / 14F);
        }

        // 原版 194-215 の isSneak ブロックは原文で丸ごとコメントアウトされているので移植しない。
        // あちらは rotationPointY/Z を 4.0F/9.0F/1.0F 等へ「代入」する形で、+= の累積では無い。

        // 原版217-220: 呼吸による微小な揺れ。直前で Z/X を代入済みなので += が毎フレーム累積しない
        this.bipedRightArm.zRot += Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.bipedLeftArm.zRot -= Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.bipedRightArm.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.bipedLeftArm.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F;

        // 原版 render() の末尾（setRotationAngles を呼んだ後）にあった腕の固定ポーズ。
        // 原版ではこの代入が上の腕アニメを全て上書きしていたので、同じ見た目にするため最後に適用する。
        // 腕を歩行に合わせて振らせたい場合は applyArmPoseFromOriginalRender() の呼び出しを消すだけでよい。
        applyArmPoseFromOriginalRender();
    }

    /**
     * 原版 render() の以下をそのまま移植（setRotationAngles の後に実行され、腕の値を上書きしていた）:
     *   bipedRightArm.rotateAngleX = -12F / 180F * PI;
     *   bipedRightArm.rotateAngleY = -10F / 180F * PI;
     *   bipedRightArm.rotateAngleZ =  30F / 180F * PI;
     *   bipedLeftArm.rotateAngleX =  bipedRightArm.rotateAngleX;
     *   bipedLeftArm.rotateAngleY = -bipedRightArm.rotateAngleY;
     *   bipedLeftArm.rotateAngleZ = -bipedRightArm.rotateAngleZ;
     */
    private void applyArmPoseFromOriginalRender()
    {
        this.bipedRightArm.xRot = -12F * DEG_TO_RAD;
        this.bipedRightArm.yRot = -10F * DEG_TO_RAD;
        this.bipedRightArm.zRot = 30F * DEG_TO_RAD;

        this.bipedLeftArm.xRot = this.bipedRightArm.xRot;
        this.bipedLeftArm.yRot = -this.bipedRightArm.yRot;
        this.bipedLeftArm.zRot = -this.bipedRightArm.zRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               int color)
    {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
