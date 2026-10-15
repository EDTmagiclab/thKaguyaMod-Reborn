package thKaguyaMod.entity.spellcard;

import net.minecraft.world.phys.Vec3;
import thKaguyaMod.LaserData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.shot.EntityTHShot;

public class THSC_Kachoufuugetu extends THSpellCard
{
	//幻想「花鳥風月、嘯風弄月」

	public THSC_Kachoufuugetu()
	{
		this.setNeedLevel(5);
		this.setRemoveTime(40);
		this.setEndTime(90);
		this.setOriginalUserName(YUUKA);
	}
	
	@Override
	public void spellcard_main()
	{
		Vec3 look = THShotLib.getVecFromAngle(user.getYRot(), user.getXRot(), 1.0D);
		if(!target.isDeadOrDying())
		{
			look = THShotLib.angle_ToPos(this.pos_User(), this.pos_Target());
		}
		Vec3 rotate = THShotLib.getVecFromAngle(user.getYRot(), user.getXRot() + 90F);
		Vec3 look2, look3;
		EntityTHShot mother;
		float angle = time * 3F, angle2;
		//float rotationYawSpeed = 3F;
		if(time > 30)
		{
			angle = -time * 3F;
			//rotationYawSpeed = -3F;
		}
		
		if(time > 60)
		{
			
		}
		//else if(tickCount % 10 == 0)
		{
			rotate = THShotLib.getVecFromAngle(user.getYRot(), user.getXRot() + 5F + (float)Math.sin(time * 10F / 180F * 3.141593F) * 50F);
			//rotate = THShotLib.getVecFromAngle(user.getYRot(), user.getXRot() + 90F);
			for(int i = 0; i < 5; i++)
			{
				look2 = THShotLib.getVectorFromRotation(look.x, look.y, look.z, rotate.x, rotate.y, rotate.z, angle);
				look3 = THShotLib.getVectorFromRotation(look.x, look.y, look.z, rotate.x, rotate.y, rotate.z, -angle);
				if(time % 10 == 0)
				{
					mother = THShotLib.createShot(user, user, pos_User(look2, 2.0D), look2, 0F, rotate, 0F, 9999, 0.4D, 0.2D, 0.0D, gravity_Zero(), shot(THShotLib.FORM_MEDIUM, THShotLib.RED, THShotLib.SIZE[THShotLib.FORM_BIG], 14F, 3, 80));
					angle2 = 0F;
					Vec3 motherOver = THShotLib.getVecFromAngle(mother.getYRot(), mother.getXRot() + 90F);
					for(int j = 0; j < 5; j++)
					{
						Vec3 set = THShotLib.getVectorFromRotation(mother.angle.x, mother.angle.y, mother.angle.z, motherOver.x, motherOver.y, motherOver.z, angle2);
						//look3 = thShotLib.getVectorFromRotation(rotate.x, rotate.y, rotate.z, look2.x, look2.y, look2.z, angle2);
						THShotLib.createLaserB(user, mother, pos(0.0D, 0.0D, 0.0D), set, mother.angle, 4F, 9999, LaserData.laser(THShotLib.RED, 0.5F, 2.0F, 6.0F, 0, 120, 0), mother, 0.0D, 0.0D);
						angle2 += 72F;
					}
				}
				else if(time % 2 == 0)
				{
					THShotLib.createShot(user, user, pos_User(look2, 2.0D), look2, 0F, rotate, 0F, 9999, 0.4D, 0.2D, 0.0D, gravity_Zero(), shot(THShotLib.FORM_MEDIUM, THShotLib.YELLOW, THShotLib.SIZE[THShotLib.FORM_MEDIUM], 14F, 3, 80));
					THShotLib.createShot(user, user, pos_User(look3, 2.0D), look3, 0F, rotate, 0F, 9999, 0.4D, 0.2D, 0.0D, gravity_Zero(), shot(THShotLib.FORM_MEDIUM, THShotLib.YELLOW, THShotLib.SIZE[THShotLib.FORM_MEDIUM], 14F, 3, 80));
				}
				angle += 72F;
			}
		}
	}
}
