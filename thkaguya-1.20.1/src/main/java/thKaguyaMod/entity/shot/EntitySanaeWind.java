package thKaguyaMod.entity.shot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 早苗の風起こし（1.20.1 移植版） */
public class EntitySanaeWind extends EntityTHShot
{
	public int type;
	public int kamikaze;

	private double lastPlayerPosX;
	private double lastPlayerPosY;
	private double lastPlayerPosZ;

    public EntitySanaeWind(EntityType<?> type, Level level)
    {
        super(type, level);
    }

    public EntitySanaeWind(Level world, LivingEntity user, Entity source,
    		Vec3 pos, Vec3 angle, float slope,
    		Vec3 rotate, float rotationSpeed, int rotationEnd,
    		double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
    		int kazetype, float size, float damage, int delay, int end, int special)
    {
    	super(world, user, source, pos, angle, slope,
    			rotate, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration,
    	    	gravity, kazetype, size, damage, delay, end, special);

    	lastPlayerPosX = user.getX();
    	lastPlayerPosY = user.getY();
    	lastPlayerPosZ = user.getZ();
    }

    @Override
    public void tick()
    {
    	if(user != null)
    	{
    		double userMotionX = user.getX() - lastPlayerPosX;
    		double userMotionY = user.getY() - lastPlayerPosY;
    		double userMotionZ = user.getZ() - lastPlayerPosZ;

    		if(getShotColor() == 1)
    		{
    			setYRot(getYRot() + 2F);
    			shotSpeed = Math.sqrt(userMotionX * userMotionX + userMotionY * userMotionY + userMotionZ * userMotionZ);
    			setVector();
    		}
    		else
    		{
	    		double gx = userMotionX * 0.2D;
	    		double gy = userMotionY * 0.2D;
	    		double gz = userMotionZ * 0.2D;
	    		if(gy < 0.0D)
	    		{
	    			gy = 0.0D;
	    		}
	    		if(user.isShiftKeyDown())
	    		{
	    			gy = -0.1D;
	    		}
	    		gravity = new Vec3(gx, gy, gz);
    		}
    	}
    	super.tick();

    	if(user != null)
    	{
    		lastPlayerPosX = user.getX();
    		lastPlayerPosY = user.getY();
    		lastPlayerPosZ = user.getZ();
    	}
    }

	public int getWindRenderPattern()
	{
		return tickCount % 4;
	}
}
