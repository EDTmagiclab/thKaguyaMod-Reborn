package thKaguyaMod.entity.spellcard;

import static thKaguyaMod.DanmakuConstants.*;
import static thKaguyaMod.THShotLib.*;

import java.util.Calendar;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.living.EntityDanmakuCreeper;
import thKaguyaMod.entity.shot.EntityNuclearShot;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.entity.shot.ISpecialShot;
import thKaguyaMod.registry.SpecialShotRegistry;

/** 奇跡「ファフロッキーズの奇跡」（1.20.1 移植版）
 *  12/25 分支按原版生成 EntityDanmakuCreeper（弹幕苦力怕，阶段 3d-6 已迁移）。 */
public class THSC_Fafurotskies_no_Kiseki extends THSpellCard implements ISpecialShot
{

	public static final int SPECIAL_FAFUROTSKIES01 = 1500;

	public THSC_Fafurotskies_no_Kiseki()
	{
		this.setNeedLevel(1);
		this.setRemoveTime(20);
		this.setEndTime(40);
		this.setOriginalUserName(SANAE);

		SpecialShotRegistry.registerSpecialShot(THSC_Fafurotskies_no_Kiseki.class, SPECIAL_FAFUROTSKIES01);
	}

	@Override
	public void spellcard_main()
	{
		if(time == 1)
		{
			int pattern = rand.nextInt(100);
			for(int i = 0; i < 10; i++)
			{
				Vec3 angle = getVecFromAngle(user.getYRot() + rand.nextFloat() * 50F - 25F, rand.nextFloat() * 20F - 69F);
				Vec3 angle2 = angle(user.getYRot(), 0.0F);

				THShotLib.createShot(user, user, pos_Distance(pos_User(), angle2, 0.5D), angle, 0F, 1.2D, pattern, 0.0D, gravity_Default(), shot(THShotLib.FORM_CIRCLE, THShotLib.AQUA, 0.4F, 6.0F, i, 20, SPECIAL_FAFUROTSKIES01));
			}
			THShotLib.playShotSound(user);
		}
	}

	@Override
	public void specialShot_move(Level world, int id, EntityTHShot shot) {
		switch(id)
		{
		case SPECIAL_FAFUROTSKIES01:
			if(shot.tickCount == shot.getDeadTime())
			{
				int pattern = (int)shot.shotLimitSpeed;

				Calendar calendar = Calendar.getInstance();

				if(calendar.get(Calendar.MONTH) + 1 == 12 && calendar.get(Calendar.DATE) == 25)
				{
					// 原版：EntityDanmakuCreeper（弾幕を撒き散らす苦力怕）
					EntityDanmakuCreeper entityCreeper = new EntityDanmakuCreeper(world);
					entityCreeper.moveTo(shot.getX(), shot.getY(), shot.getZ(), rand.nextFloat() * 360F, 0.0F);
					if(!world.isClientSide)
					{
						world.addFreshEntity(entityCreeper);
						shot.discard();
					}
					return;
				}

				if(calendar.get(Calendar.MONTH) + 1 == 1 && (calendar.get(Calendar.DATE) >= 1 && calendar.get(Calendar.DATE) <= 3))
				{
					if(!world.isClientSide)
					{
						EntityNuclearShot nuclearShot = new EntityNuclearShot(world, shot.user, shot.user, pos(shot.getX(), shot.getY(), shot.getZ()), angle_Random(), 0.0F, rotate_Default(), 0.0F, 9999,
								0.2D, 0.8D, 0.00D, gravity_Default(), ShotData.shot(0, rand.nextInt(7), 3.2F, 10.0F, 0, 70, 0));
						world.addFreshEntity(nuclearShot);//核弾を出現させる
						nuclearShot.shootingFlag = true;
						shot.discard();
					}
					return;
				}

				if(pattern <= 70)
				{
					for(int i = 0; i < 8; i++)
					{
						ShotData shotData = ShotData.shot(FORM_KNIFE, RANDOM, 0.6F, 10.0F, 3, 120);
						THShotLib.createShot(shot.user, shot, pos_Entity(shot), angle(rand.nextFloat() * 360F, rand.nextFloat() * 180 - 90F), 0F, 0.6D, 0.6D, 0.0D, gravity_Default(), shotData);
					}
					if(!world.isClientSide)
					{
						shot.discard();
					}
				}
				else if(pattern <= 80)
				{
					if(!world.isClientSide)
					{
						world.addFreshEntity(new ItemEntity(world, shot.getX(), shot.getY(), shot.getZ(), new ItemStack(Items.COD)));
						shot.discard();
					}
				}
				else if(pattern <= 90)
				{
					Creeper entityCreeper = new Creeper(EntityType.CREEPER, world);
					entityCreeper.moveTo(shot.getX(), shot.getY(), shot.getZ(), rand.nextFloat() * 360F, 0.0F);
					if(!world.isClientSide)
					{
						world.addFreshEntity(entityCreeper);
						shot.discard();
					}
				}
				else
				{
					Chicken entitychicken = new Chicken(EntityType.CHICKEN, world);
					entitychicken.moveTo(shot.getX(), shot.getY(), shot.getZ(), rand.nextFloat() * 360F, 0.0F);
					if(!world.isClientSide)
					{
						world.addFreshEntity(entitychicken);
						shot.discard();
					}
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
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}
}
