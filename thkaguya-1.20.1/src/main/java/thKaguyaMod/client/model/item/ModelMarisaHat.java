package thKaguyaMod.client.model.item;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import thKaguyaMod.THKaguyaCore;

/**
 * 魔理沙の帽子（marisa_hat）的盔甲模型
 *
 * 原版 extract/sources/java/thKaguyaMod/client/model/ModelMarisaHat.java（93 行，extends ModelBiped，64x32）：
 * ・hatBase    texOffs(0,0)   addBox(-4,-8,-4, 8,4,8, 0.4F)   rotationPoint(0,0,0)     rot(0,0,0)          挂在 bipedHead
 * ・hatBase2   texOffs(32,2)  addBox(-3,-3,-3, 6,3,6, 0.4F)   rotationPoint(0,-7,0)    rot(-0.2792527,0,0) 挂在 hatBase
 * ・hatBase3   texOffs(48,16) addBox(-2,-3,-2, 4,3,4, 0.4F)   rotationPoint(0,-9,0)    rot(-0.5410521,0,0) 挂在 hatBase
 * ・hatBase4   texOffs(48,24) addBox(-1,-3,-1, 2,3,2, 0.4F)   rotationPoint(0,-11,1)   rot(-0.8203047,0,0) 挂在 hatBase
 * ・brim      texOffs(0,16)  addBox(-6,0,-6, 12,1,12, 0.4F)   rotationPoint(0,-6,0)    rot(0,0,0)          挂在 hatBase
 * ・ribbonRight texOffs(44,12) addBox(0,-2,-1, 4,3,1, 0.4F)   rotationPoint(0.5,-6,-4) rot(0,0,-0.3490659) 挂在 hatBase
 * ・ribbonLeft  texOffs(32,12) addBox(-4,-2,-1, 4,3,1, 0.4F)  rotationPoint(-0.5,-6,-4) rot(0,0, 0.3490659) 挂在 hatBase
 * ・addBox 的最后一个参数（0.4F）在 1.7.10 是「盒子整体外扩」，对应 1.20.1 的 CubeDeformation(0.4F)。
 *
 * 1.20.1 关键点（踩过的坑）：HumanoidModel.createMesh() 会造出完整的头/身/手臂/腿盒子，
 * 而 HumanoidArmorLayer 会把整个模型都画出来——于是标准头盒子（texOffs 0,0 正好是帽子的黑色区域）
 * 把玩家整张脸糊成黑色，看起来就是「帽子下沿糊在脸上」。
 * 所以这里不用 createMesh()，只造「空 head + 帽子部件」，其余部件也留空，
 * 让 HumanoidArmorLayer 画出来只有帽子本身。
 */
public class ModelMarisaHat extends HumanoidModel<LivingEntity>
{
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(
			new ResourceLocation(THKaguyaCore.MODID, "marisa_hat"), "main");

	public ModelMarisaHat(ModelPart root)
	{
		super(root);
	}

	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// HumanoidModel 的构造器要求这些名字都存在；全部留空（不画任何盒子）
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
		root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
		root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));

		CubeDeformation deform = new CubeDeformation(0.4F);

		// 原版は hatBase 以降すべて hatBase の子（hatBase 自身は head の子）
		PartDefinition hatBase = head.addOrReplaceChild("hatBase",
				CubeListBuilder.create().texOffs(0, 0).mirror()
						.addBox(-4.0F, -8.0F, -4.0F, 8, 4, 8, deform),
				PartPose.ZERO);

		hatBase.addOrReplaceChild("hatBase2",
				CubeListBuilder.create().texOffs(32, 2).mirror()
						.addBox(-3.0F, -3.0F, -3.0F, 6, 3, 6, deform),
				PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.2792527F, 0.0F, 0.0F));

		hatBase.addOrReplaceChild("hatBase3",
				CubeListBuilder.create().texOffs(48, 16).mirror()
						.addBox(-2.0F, -3.0F, -2.0F, 4, 3, 4, deform),
				PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, -0.5410521F, 0.0F, 0.0F));

		hatBase.addOrReplaceChild("hatBase4",
				CubeListBuilder.create().texOffs(48, 24).mirror()
						.addBox(-1.0F, -3.0F, -1.0F, 2, 3, 2, deform),
				PartPose.offsetAndRotation(0.0F, -11.0F, 1.0F, -0.8203047F, 0.0F, 0.0F));

		hatBase.addOrReplaceChild("brim",
				CubeListBuilder.create().texOffs(0, 16).mirror()
						.addBox(-6.0F, 0.0F, -6.0F, 12, 1, 12, deform),
				PartPose.offset(0.0F, -6.0F, 0.0F));

		hatBase.addOrReplaceChild("ribbonRight",
				CubeListBuilder.create().texOffs(44, 12).mirror()
						.addBox(0.0F, -2.0F, -1.0F, 4, 3, 1, deform),
				PartPose.offsetAndRotation(0.5F, -6.0F, -4.0F, 0.0F, 0.0F, -0.3490659F));

		hatBase.addOrReplaceChild("ribbonLeft",
				CubeListBuilder.create().texOffs(32, 12).mirror()
						.addBox(-4.0F, -2.0F, -1.0F, 4, 3, 1, deform),
				PartPose.offsetAndRotation(-0.5F, -6.0F, -4.0F, 0.0F, 0.0F, 0.3490659F));

		return LayerDefinition.create(mesh, 64, 32);
	}
}
