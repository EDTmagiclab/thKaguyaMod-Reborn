package thKaguyaMod.entity.shot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.init.THKaguyaEntities;

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

	/**
	 * 完全版コンストラクタ（自分の EntityType を明示できる）。
	 *
	 * <p>基底の便宜コンストラクタ（{@code EntityTHShot(Level, ...)}）は EntityType を
	 * {@code TH_SHOT} に固定してしまうため、必ずここを通して
	 * {@code THKaguyaEntities.SANAE_WIND} を渡すこと。でないとサーバーはスポーンパケットに
	 * TH_SHOT の ID を載せ、クライアントは汎用 {@code EntityTHShot} を生成して
	 * 汎用 {@code RenderTHShot}（＝赤い丸弾）で描画してしまい、専用レンダラー
	 * {@code RenderSanaeWind} が永久に選ばれない（御幣の陰陽玉と同じ罠）。
	 */
	public EntitySanaeWind(EntityType<?> type, Level world, LivingEntity user, Entity source,
			Vec3 pos, Vec3 angle, float slope,
			Vec3 rotate, float rotationSpeed, int rotationEnd,
			double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
			int kazetype, float size, float damage, int delay, int end, int special)
	{
		super(type, world, user, source, pos, angle, slope,
				rotate, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration,
				gravity, kazetype, size, damage, delay, end, special);

		lastPlayerPosX = user.getX();
		lastPlayerPosY = user.getY();
		lastPlayerPosZ = user.getZ();
	}

	/** 通常コンストラクタ（EntityType は必ず {@code THKaguyaEntities.SANAE_WIND}） */
    public EntitySanaeWind(Level world, LivingEntity user, Entity source,
    		Vec3 pos, Vec3 angle, float slope,
    		Vec3 rotate, float rotationSpeed, int rotationEnd,
    		double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
    		int kazetype, float size, float damage, int delay, int end, int special)
    {
    	this(THKaguyaEntities.SANAE_WIND.get(), world, user, source, pos, angle, slope,
    			rotate, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration,
    	    	gravity, kazetype, size, damage, delay, end, special);
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
