package thKaguyaMod.entity.spellcard;

import static thKaguyaMod.DanmakuConstants.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.entity.shot.ISpecialShot;
import thKaguyaMod.registry.SpecialShotRegistry;

public class THSC_YouryokuSpoiler extends THSpellCard implements ISpecialShot
{
	//妖怪退治「妖力スポイラー」
	
	public static final int SPECIAL_SPOILER01 = 1600;
	public static final int SPECIAL_SPOILER02 = 1601;
	
	public THSC_YouryokuSpoiler()
	{
		this.setNeedLevel(2);
		this.setRemoveTime(10);
		this.setEndTime(80);
		this.setOriginalUserName(SANAE);
		
		SpecialShotRegistry.registerSpecialShot(THSC_YouryokuSpoiler.class, SPECIAL_SPOILER01);
		SpecialShotRegistry.registerSpecialShot(THSC_YouryokuSpoiler.class, SPECIAL_SPOILER02);
	}
	
	@Override
	public void spellcard_main()
	{
		if(time < 20)
		{
			if(target != null)
			{
				world.playSound(null, user.getX(), user.getY(), user.getZ(),
            net.minecraft.sounds.SoundEvents.PORTAL_TRAVEL, net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.9F);//音を出す
				THShotLib.createCircleShot(user, user, pos(targetPosX, targetPosY, targetPosZ), getVecFromAngle(card.getYRot(), card.getXRot()), 2.0D, 0.2D, -0.1D, gravity_Zero(), shot(FORM_SCALE, rand.nextInt(7), 0.3F, 3.0F, 6, 70, SPECIAL_SPOILER01), 7, 1.5D, time * 3F);
		
			}
		}
		if(time > 40 && time < 60)
		{
			if(target != null)
			{
				world.playSound(null, user.getX(), user.getY(), user.getZ(),
            net.minecraft.sounds.SoundEvents.PORTAL_TRAVEL, net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.9F);//音を出す
				THShotLib.createCircleShot(user, user, pos(targetPosX, targetPosY, targetPosZ), getVecFromAngle(card.getYRot(), card.getXRot()), 2.0D, 0.2D, -0.1D, gravity_Zero(), shot(FORM_SCALE, rand.nextInt(7), 0.3F, 3.0F, 6, 70, SPECIAL_SPOILER01), 7, 1.5D,  -time * 3F);
		
			}
		}
	}

	@Override
	public void specialShot_move(Level world, int id, EntityTHShot shot) {
		switch(id)
		{
			case SPECIAL_SPOILER01:
				if(shot.tickCount >= 20)
				{
					if(shot.user != null)
					{
						shot.shotLimitSpeed = 1.0D;
						shot.shotAcceleration = 0.05D;
					
						double lengthToUserX = shot.user.getX() - shot.getX();
						double lengthToUserZ = shot.user.getZ() - shot.getZ();
						shot.setYRot((float)Math.atan2(lengthToUserX, lengthToUserZ) / 3.141593F * 180F);
						shot.setXRot((float)Math.atan2(shot.user.getY() + 1.0D - shot.getY(), Math.sqrt(lengthToUserX * lengthToUserX + lengthToUserZ * lengthToUserZ)) / 3.141593F * 180F);
						
						shot.setVector();
						
						if(shot.distanceTo(shot.user) < 1.5F)
						{
							if(!world.isClientSide)
							{
								shot.discard();
							}
						}
					}
				}
				break;
			case SPECIAL_SPOILER02:
				if(shot.tickCount >= 20)
				{
					if(shot.user != null)
					{
						shot.shotLimitSpeed = 1.0D;
						shot.shotAcceleration = 0.05D;
					
						double lengthToUserX = shot.user.getX() - shot.getX();
						double lengthToUserZ = shot.user.getZ() - shot.getZ();
						shot.setYRot((float)Math.atan2(lengthToUserX, lengthToUserZ) / 3.141593F * 180F);
						shot.setXRot((float)Math.atan2(shot.user.getY() + 1.0D - shot.getY(), Math.sqrt(lengthToUserX * lengthToUserX + lengthToUserZ * lengthToUserZ)) / 3.141593F * 180F);
						
						shot.setVector();
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
			case SPECIAL_SPOILER01:
				if(entity_Hit instanceof LivingEntity)
				{
					LivingEntity living = (LivingEntity)entity_Hit;
					if(!living.is(shot.user))
					{
						living.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 20, 1));//ウィザー
						living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 20, 1));//攻撃低下
						shot.shotSpecial = SPECIAL_SPOILER02;
						return true;
					}
					else
					{

						return false;
					}
				}
			case SPECIAL_SPOILER02:
				if(shot.user != null && entity_Hit.is(shot.user))
				{
					LivingEntity living = (LivingEntity)entity_Hit;
					living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1));//回復
					living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 20, 1));//攻撃上昇
					return false;
				}
				else
				{
					return true;
				}
			default:
				return false;
		}
	}
}
