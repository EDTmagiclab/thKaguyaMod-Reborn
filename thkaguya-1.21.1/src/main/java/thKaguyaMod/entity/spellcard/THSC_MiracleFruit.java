package thKaguyaMod.entity.spellcard;

import static thKaguyaMod.DanmakuConstants.*;
import static thKaguyaMod.THShotLib.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.entity.shot.ISpecialShot;
import thKaguyaMod.registry.SpecialShotRegistry;

public class THSC_MiracleFruit extends THSpellCard implements ISpecialShot
{
	//奇跡「ミラクルフルーツ」
	
	public static final int SPECIAL_MIRACLE_FRUIT01 = 1400;
	
	public THSC_MiracleFruit()
	{
		this.setNeedLevel(1);
		this.setRemoveTime(30);
		this.setEndTime(60);
		this.setOriginalUserName(SANAE);
		
		SpecialShotRegistry.registerSpecialShot(THSC_MiracleFruit.class, SPECIAL_MIRACLE_FRUIT01);
	}
	
	@Override
	public void spellcard_main()
	{
		if(time == 1)
		{
			user.removeAllEffects();
		}
		if(time < 5)
		{
			return;
		}
		
		if(time % 30 == 6)
		{
			Vec3 angle = THShotLib.getRotationVectorFromAngle(user.getYRot(), user.getXRot(), 0F, 1.0D);
			THShotLib.playShotSound(user);
			THShotLib.createCircleShot(user, user, pos_User(), angle, 2.0D, 0.1D, -0.2D, gravity_Zero(), shot(FORM_OVAL, RED, THShotLib.SIZE[FORM_OVAL], 7.0F, 3, 20, SPECIAL_MIRACLE_FRUIT01), 8, 0.2D, rand.nextFloat() * 360F);
		}
	}

	@Override
	public void specialShot_move(Level world, int id, EntityTHShot shot) {
		switch(id)
		{
			case SPECIAL_MIRACLE_FRUIT01:
				if(shot.tickCount == shot.getDeadTime())
				{
					float angle2 = rand.nextFloat() * 360F;
					Vec3 look;
					for(int j = 0; j < 8; j++)
					{
						look = THShotLib.getVectorFromRotation(shot.rotate, shot.angle, angle2);
						for(int i = 0; i < 6; i++)
						{
							THShotLib.createShot(shot.user, shot, pos_Entity(shot), look, 0.0F, shot.rotate, 0.0F, 9999, 0.5D, 0.2D, 0.8D, gravity_Zero(), ShotData.shot(FORM_SMALL, RED, 0.3F, 6.0F, 3 + i * 2, 90));
		
						}
						angle2 += 45F;
					}
					
				}
				break;
			default:
				break;
		}
		
	}

	@Override
	public boolean specialShot_hitBlock(Level world, int id, EntityTHShot shot,
			HitResult movingObjectPosition) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}

	@Override
	public boolean specialShot_hitEntity(Level world, int id,
			EntityTHShot shot, Entity entity_Hit) {
		switch(id)
		{
			case SPECIAL_MIRACLE_FRUIT01:
				if(entity_Hit instanceof LivingEntity)
				{
					LivingEntity living = (LivingEntity)entity_Hit;
					living.removeAllEffects();
				}
				return false;
			default:
				return false;
		}
	}
}
