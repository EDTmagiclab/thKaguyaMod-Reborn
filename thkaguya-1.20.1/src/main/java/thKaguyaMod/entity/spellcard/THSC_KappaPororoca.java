package thKaguyaMod.entity.spellcard;

import static thKaguyaMod.DanmakuConstants.*;
import static thKaguyaMod.THShotLib.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.entity.shot.ISpecialShot;
import thKaguyaMod.registry.SpecialShotRegistry;

public class THSC_KappaPororoca extends THSpellCard implements ISpecialShot
{
	//水符「河童のポロロッカ」
	
	public static final int SPECIAL_KAPPA_POROROCA01 = 1100;

	public THSC_KappaPororoca()
	{
		this.setNeedLevel(3);
		this.setRemoveTime(30);
		this.setEndTime(140);
		this.setOriginalUserName(NITORI);
		
		SpecialShotRegistry.registerSpecialShot(THSC_KappaPororoca.class, SPECIAL_KAPPA_POROROCA01);
	}
	
	@Override
	public void spellcard_main()
	{
		if(time < 5)return;
		
		if(time % 10 == 0)
		{
			
			THShotLib.createShot(user, user, pos_User(), getVecFromAngle(user.getYRot() + 90F, -60F), 0F, 0.3D, 0.3D, 0.0D, gravity_Default(), 
									shot(THShotLib.FORM_LIGHT, THShotLib.BLUE, 0.45F, 8.0F, 2, 60, SPECIAL_KAPPA_POROROCA01));
			THShotLib.createShot(user, user, pos_User(), getVecFromAngle(user.getYRot() - 90F, -60F), 0F, 0.3D, 0.3D, 0.0D, gravity_Default(), 
									shot(THShotLib.FORM_LIGHT, THShotLib.BLUE, 0.45F, 8.0F, 2, 60, SPECIAL_KAPPA_POROROCA01));
		}
	}

	@Override
	public void specialShot_move(Level world, int id, EntityTHShot shot) {
		switch(id)
		{
			case SPECIAL_KAPPA_POROROCA01:
				if(shot.tickCount % 4 == 3)
				{
					ShotData shotData = ShotData.shot(FORM_LIGHT, BLUE, 0.3F, 5.0F, 0, 100, BOUND01);
					THShotLib.createShot(shot.user, shot, pos_Entity(shot), angle(shot.user.getYRot() + rand.nextFloat() * 30F - 15F, -15F), 0F, 0.7D, 0.7D, 0.0D, gravity_Default(),
											shotData);
				}
				break;
			default:
				break;
		}
		
	}

	@Override
	public boolean specialShot_hitBlock(Level world, int id, EntityTHShot shot,
			HitResult movingObjectPosition) {
		switch(id)
		{
			case SPECIAL_KAPPA_POROROCA01:
				// 原版 THSC_KappaPororoca：撞墙反弹 4 次（0.9D 回弹率）
				shot.bound(movingObjectPosition, 0.9D, 4);
				return true;
			default:
				return false;
		}
	}

	@Override
	public boolean specialShot_hitEntity(Level world, int id,
			EntityTHShot shot, Entity entity_Hit) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}
}
