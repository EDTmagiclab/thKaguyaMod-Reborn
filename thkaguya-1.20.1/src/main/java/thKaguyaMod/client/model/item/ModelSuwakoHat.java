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
 * 洩矢の帽子（suwako_hat）的盔甲模型
 *
 * 原版 extract/sources/java/thKaguyaMod/client/model/ModelSuwakoHat.java（63 行，extends ModelBiped，64x32）：
 * ・hatBase  texOffs(0,0)  addBox(-4,-9,-4, 8,4,8, scale)  rotationPoint(0,0,0)    挂在 bipedHead
 * ・eyeRight texOffs(0,0)  addBox(-1,-2,-1, 2,2,2, scale)  rotationPoint(-4,-9,-4) 挂在 hatBase
 * ・eyeLeft  texOffs(24,0) addBox(-1,-2,-1, 2,2,2, scale)  rotationPoint(4,-9,-4)  挂在 hatBase
 * ・brim     texOffs(0,16) addBox(-6,-1,-6,12,1,12, scale) rotationPoint(0,-5,0)   挂在 hatBase
 * ・scale = 0.4F（由 ItemKerobou 传入），对应 1.20.1 的 CubeDeformation(0.4F)
 * ・render() 只渲染 bipedHead，所以其余身体部位不画
 *
 * 1.20.1 用 HumanoidArmorLayer 自动渲染：Forge 会用 copyPropertiesTo 把玩家模型的头/身姿态复制到本模型，
 * 所以这里把帽子几何挂在 head 之下的自有部件上（head 本身留空），帽子就会跟着头转。
 */
public class ModelSuwakoHat extends HumanoidModel<LivingEntity>
{
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(
			new ResourceLocation(THKaguyaCore.MODID, "suwako_hat"), "main");

	public ModelSuwakoHat(ModelPart root)
	{
		super(root);
	}

	public static LayerDefinition createBodyLayer()
	{
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition head = mesh.getRoot().getChild("head");

		CubeDeformation deform = new CubeDeformation(0.4F);

		PartDefinition hatBase = head.addOrReplaceChild("hatBase",
				CubeListBuilder.create().texOffs(0, 0).mirror()
						.addBox(-4.0F, -9.0F, -4.0F, 8, 4, 8, deform),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		hatBase.addOrReplaceChild("eyeRight",
				CubeListBuilder.create().texOffs(0, 0).mirror()
						.addBox(-1.0F, -2.0F, -1.0F, 2, 2, 2, deform),
				PartPose.offset(-4.0F, -9.0F, -4.0F));

		hatBase.addOrReplaceChild("eyeLeft",
				CubeListBuilder.create().texOffs(24, 0).mirror()
						.addBox(-1.0F, -2.0F, -1.0F, 2, 2, 2, deform),
				PartPose.offset(4.0F, -9.0F, -4.0F));

		hatBase.addOrReplaceChild("brim",
				CubeListBuilder.create().texOffs(0, 16).mirror()
						.addBox(-6.0F, -1.0F, -6.0F, 12, 1, 12, deform),
				PartPose.offset(0.0F, -5.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 32);
	}
}
