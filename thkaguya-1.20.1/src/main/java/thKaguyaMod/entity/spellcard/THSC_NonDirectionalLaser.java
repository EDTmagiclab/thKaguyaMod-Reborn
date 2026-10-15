package thKaguyaMod.entity.spellcard;

import net.minecraft.world.phys.Vec3;
import thKaguyaMod.LaserData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaConfig;

public class THSC_NonDirectionalLaser extends THSpellCard
{
	//恋符「ノンディレクショナルレーザー」

	public THSC_NonDirectionalLaser()
	{
		this.setNeedLevel(3);
		this.setRemoveTime(60);
		this.setEndTime(103);
		this.setOriginalUserName(MARISA);
	}
	
	@Override
	public void spellcard_main()
	{
		if(time == 3)
		{
			world.playSound(null, user.getX(), user.getY(), user.getZ(),
					net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
							new net.minecraft.resources.ResourceLocation(thKaguyaMod.THKaguyaCore.MODID, "masterspark")),
					net.minecraft.sounds.SoundSource.PLAYERS, (float)THKaguyaConfig.MasterSparkVol, 1.0F);
			for(int i = 0; i < 5; i++)
			{
				Vec3 look = user.getLookAngle();
				Vec3 rotate = THShotLib.getVecFromAngle(user.getYRot(), user.getXRot() - 90F);
				Vec3 move = THShotLib.getVectorFromRotation(rotate.x, rotate.y, rotate.z, look.x, look.y, look.z, i * 72F);
				THShotLib.createLaserB(user, user, pos(0.0D, THShotLib.getPosYFromEye(user) - user.getY(), 0.0D), move, rotate, -4F, 9999, LaserData.laser(THShotLib.RED + i, 0.7F, 20.8F, 7.0F, 25, 60, 0), user, 1.5D, 1.0F);
			}
		}
		if(time == 63)
		{
			world.playSound(null, user.getX(), user.getY(), user.getZ(),
					net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
							new net.minecraft.resources.ResourceLocation(thKaguyaMod.THKaguyaCore.MODID, "masterspark")),
					net.minecraft.sounds.SoundSource.PLAYERS, (float)THKaguyaConfig.MasterSparkVol, 1.0F);
			for(int i = 0; i < 5; i++)
			{
				Vec3 look = user.getLookAngle();
				Vec3 rotate = THShotLib.getVecFromAngle(user.getYRot(), user.getXRot() - 90F);
				Vec3 move = THShotLib.getVectorFromRotation(rotate.x, rotate.y, rotate.z, look.x, look.y, look.z, i * 72F);
				THShotLib.createLaserB(user, user, pos(0.0D, THShotLib.getPosYFromEye(user) - user.getY(), 0.0D), move, rotate,  4F, 9999, LaserData.laser(THShotLib.RED + i, 0.7F, 20.8F, 7.0F, 25, 60, 0), user, 1.5D, 1.0F);
			}
		}
	}
}
