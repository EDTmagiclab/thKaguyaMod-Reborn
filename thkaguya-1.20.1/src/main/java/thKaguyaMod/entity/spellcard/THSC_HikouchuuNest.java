package thKaguyaMod.entity.spellcard;

import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.entity.item.EntitySukima;

public class THSC_HikouchuuNest extends THSpellCard
{
	//幻巣「飛光虫ネスト」
	
	public THSC_HikouchuuNest()
	{
		this.setNeedLevel(4);
		this.setRemoveTime(50);
		this.setEndTime(160);
		this.setOriginalUserName(YUKARI);
	}
	
	@Override
	public void spellcard_main()
	{
		boolean flag = false;
		int count = 0;
		while(!flag && count < 4)
		{
			double length = rand.nextDouble() * 5.0D + 1.0D;
			float angleXZ = (card.getYRot() + 90F) / 180F * (float)Math.PI + rand.nextFloat() * (float)Math.PI;
			float angleY  = rand.nextFloat() * (float)Math.PI - (float)Math.PI / 2.0F;
			double xVector = Math.sin(angleXZ) * Math.cos(angleY) * length;
			double yVector = Math.sin(angleY) * length;
			double zVector = Math.cos(angleXZ) * Math.cos(angleY) * length;
			double xPos, yPos, zPos;
		    //始点（現在地）
	    	Vec3 vec3d = new Vec3(user.getX(), user.getY(), user.getZ());
	    	//終点（現在地に移動量を足した点）
	    	Vec3 vec3d1 = new Vec3(user.getX() + xVector, user.getY() + yVector, user.getZ() + zVector);
	        //始点と終点からブロックとの当たりを取得
	    	HitResult movingObjectPosition = world.clip(new net.minecraft.world.level.ClipContext(vec3d, vec3d1,
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, user));
	    	vec3d = new Vec3(user.getX(), user.getY(), user.getZ());
	    	vec3d1 = new Vec3(user.getX() + xVector, user.getY() + yVector, user.getZ() + zVector);
	    	//何らかのブロックに当たっているなら
	    	// 1.20.1 の clip() は miss でも非 null を返すため Type を見る（原版 1.7.10 は null を返した）
	        if (movingObjectPosition != null && movingObjectPosition.getType() != HitResult.Type.MISS)
	        {
	        	count ++;
	        }
			else
			{
				xPos = user.getX() + xVector;
				yPos = user.getY() + yVector;
				zPos = user.getZ() + zVector;
				EntitySukima entitySukima = new EntitySukima(world, user, xPos, yPos, zPos, user.getYRot(), 17);
				if(!world.isClientSide)
				{
					world.addFreshEntity(entitySukima);
				}
				flag = true;
			}
		}
	}
}
