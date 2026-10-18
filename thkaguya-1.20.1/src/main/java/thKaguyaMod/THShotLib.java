package thKaguyaMod;

import java.util.Random;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.entity.shot.EntityNuclearShot;
import thKaguyaMod.entity.shot.EntityTHLaser;
import thKaguyaMod.entity.shot.EntityTHShot;

/**
 * 弾幕関連の汎用関数群（1.20.1 移植版）
 *
 * 阶段 3a：先迁移纯数学/向量运算部分（无实体生成依赖）。
 * 弹体生成方法（createShot / createSphereShot / createWideShot / createCircleShot /
 * createRandomRingShot / createRingShot / createNuclearShot / playShotSound /
 * explosionEffect / danmakuRemove / banishExplosion）依赖 EntityTHShot 等实体类，
 * 在实体层（3c/3d）迁移完成后补回。
 *
 * 1.7.10 → 1.20.1 向量 API 映射：
 *   Vec3.createVectorHelper(x,y,z) -> new Vec3(x,y,z)
 *   vec.xCoord/yCoord/zCoord      -> vec.x/y/z
 *   vec.lengthVector()            -> vec.length()
 *   vec.addVector()               -> vec.add()
 *   MathHelper.sqrt_double        -> Math.sqrt / Mth
 */
public class THShotLib extends DanmakuConstants
{
    /* ============ 弹形状 ID 常量（原 THShotLib 头部） ============ */
    /** 小弾 */
    public static final int SMALL[]		= {  0,  1,  2,  3,  4,  5,  6,  7};
    /** 粒弾 */
    public static final int TINY[]		= {  8,  9, 10, 11, 12, 13, 14, 15};
    /** 中弾 */
    public static final int MEDIUM[]	= { 16, 17, 18, 19, 20, 21, 22, 23};
    /** 真珠弾 */
    public static final int PEARL[]		= { 24, 25, 26, 27, 28, 29, 30, 31};
    /** 輪弾 */
    public static final int CIRCLE[]	= { 32, 33, 34, 35, 36, 37, 38, 39};
    /** 光弾 */
    public static final int LIGHT[]		= { 40, 41, 42, 43, 44, 45, 46, 47};
    /** 鱗弾 */
    public static final int SCALE[]		= { 48, 49, 50, 51, 52, 53, 54, 55};
    /** 蝶弾 */
    public static final int BUTTERFLY[]	= { 56, 57, 58, 59, 60, 61, 62, 63};
    /** 小星弾 */
    public static final int SMALLSTAR[]	= { 64, 65, 66, 67, 68, 69, 70, 71};
    /** 星弾 */
    public static final int STAR[]		= { 72, 73, 74, 75, 76, 77, 78, 79};
    /** 米弾 */
    public static final int RICE[]		= { 80, 81, 82, 83, 84, 85, 86, 87};
    /** 結晶弾 */
    public static final int CRYSTAL[]	= { 88, 89, 90, 91, 92, 93, 94, 95};
    /** ハート弾 */
    public static final int HEART[]		= { 96, 97, 98, 99,100,101,102,103};
    /** クナイ弾 */
    public static final int KUNAI[]		= {104,105,106,107,108,109,110,111};
    /** 札弾 */
    public static final int TALISMAN[]	= {112,113,114,115,116,117,118,119};
    /** 大光弾 */
    public static final int BIGLIGHT[]	= {120,121,122,123,124,125,126,127};
    /** 楕円弾 */
    public static final int OVAL[]		= {128,129,130,131,132,133,134,135};
    /** 使い魔 */
    public static final int FAMILIAR[]	= {136,137,138,139,140,141,142,143};
    /** 矢弾 */
    public static final int ARROW[]		= {144,145,146,147,148,149,150,151};
    /** アミュレット */
    public static final int AMULET[]	= {216,217,218,219,220,221,222,223};
    /** ナイフ弾 */
    public static final int KNIFE[]		= {224,225,226,227,228,229,230,231};
    /** 風弾 */
    public static final int WIND[]		= {232,233,234,235,236,237,238,239};
    /** 大弾 */
    public static final int BIG[]		= {240,241,242,243,244,245,246,247};
    /** 気質弾 */
    public static final int KISHITU[]	= {248,249,250,251,252,253,254,255};
    /** レーザー */
    public static final int LASER[]		= {512,513,514,515,516,517,518,519};

    /* ============ 基础工具函数 ============ */

    /** 弾ID から形状 ID を返す */
    public static final int getFormID(int shotID)
    {
        return (shotID - (shotID % 8)) / 8;
    }

    /** 位置ベクトル */
    public static Vec3 pos(double x, double y, double z)
    {
        return new Vec3(x, y, z);
    }

    /** Entity の位置ベクトル */
    public static Vec3 pos_Entity(Entity entity)
    {
        return pos(entity.getX(), entity.getY(), entity.getZ());
    }

    /** Entity の位置ベクトル（Y 调整） */
    public static Vec3 pos_Entity(Entity entity, double height)
    {
        return pos(entity.getX(), entity.getY() + height, entity.getZ());
    }

    /** 生物の位置ベクトル（目の高さより少し下） */
    public static Vec3 pos_Living(LivingEntity living)
    {
        return pos(living.getX(), getPosYFromEye(living), living.getZ());
    }

    /** ある地点からある方向に一定距離離れた位置 */
    public static Vec3 pos_Distance(Vec3 pos, Vec3 angle, double distance)
    {
        return pos(pos.x + angle.x * distance, pos.y + angle.y * distance, pos.z + angle.z * distance);
    }

    /** ランダムな方向に一定距離離れた位置 */
    public static Vec3 pos_Random(Vec3 pos, double range)
    {
        Random rand = new Random();
        Vec3 rand_pos = angle(rand.nextFloat() * 360F, rand.nextFloat() * 180F - 90F);
        return pos.add(rand_pos.x, rand_pos.y, rand_pos.z);
    }

    /** 各成分から方向ベクトル */
    public static Vec3 angle(double x, double y, double z)
    {
        return new Vec3(x, y, z);
    }

    /** 水平角度と垂直角度から方向ベクトル */
    public static Vec3 angle(float yaw, float pitch)
    {
        return getVecFromAngle(yaw, pitch);
    }

    /** A地点からB地点への単位方向ベクトル */
    public static Vec3 angle_ToPos(Vec3 posA, Vec3 posB)
    {
        return getVectorNomalize(pos(posB.x - posA.x, posB.y - posA.y, posB.z - posA.z));
    }

    /** 全方向ランダム方向ベクトル */
    public static Vec3 angle_Random()
    {
        Random rand = new Random();
        return angle(rand.nextFloat() * 360F, rand.nextFloat() * 180F - 90F);
    }

    /** 基準方向から指定角度内のランダム方向 */
    public static Vec3 angle_LimitRandom(Vec3 angle, float limitAngle)
    {
        Vec3 rotate = rotate_Random();
        Random rand = new Random();
        return getVectorFromRotation(rotate, angle, rand.nextFloat() * limitAngle - (limitAngle / 2.0F));
    }

    /** 各成分から回転軸ベクトル */
    public static Vec3 rotate(double x, double y, double z)
    {
        return new Vec3(x, y, z);
    }

    /** 真上方向の回転軸ベクトル */
    public static Vec3 rotate_Default()
    {
        return new Vec3(0.0D, 1.0D, 0.0D);
    }

    /** ランダム回転軸ベクトル */
    public static Vec3 rotate_Random()
    {
        Random rand = new Random();
        return angle(rand.nextFloat() * 360F, rand.nextFloat() * 180F - 90F);
    }

    /** 各成分から重力ベクトル */
    public static Vec3 gravity(double x, double y, double z)
    {
        return new Vec3(x, y, z);
    }

    /** y軸方向のみの重力 */
    public static Vec3 gravity(double gravityY)
    {
        return gravity(0.0D, gravityY, 0.0D);
    }

    /** 重力なし */
    public static Vec3 gravity_Zero()
    {
        return gravity(0.0D, 0.0D, 0.0D);
    }

    /** 標準重力 */
    public static Vec3 gravity_Default()
    {
        return gravity(GRAVITY_DEFAULT);
    }

    /** 水平方向の単位ベクトル */
    public static Vec3 getVecOutY(Vec3 vec)
    {
        return getVectorNomalize(new Vec3(vec.x, 0.0D, vec.z));
    }

    /** 任意ベクトルの真上の方向ベクトル */
    public static Vec3 getOverVector(Vec3 vec)
    {
        float yaw, pitch;
        vec = getVecOutY(vec);
        yaw = getYawFromVector(vec.x, vec.z);
        pitch = getPitchFromVector(vec.x, vec.y, vec.z);
        return getVecFromAngle(yaw, pitch + 90F);
    }

    /** Degree を Radian に変換 */
    public static final float radTrans(float deg)
    {
        return (float)((double)deg / 180.0D * Math.PI);
    }

    public static final float radTrans(double deg)
    {
        return (float)((double)deg / 180.0D * Math.PI);
    }

    /** 角度を -180～180 度に変換 */
    public static final float getAngleMax180(float angle)
    {
        angle %= 360F;
        if(angle > 180F)
        {
            angle -= 360F;
        }
        else if(angle < -180)
        {
            angle += 360F;
        }
        return angle;
    }

    /** 単位ベクトルを返す */
    public static final Vec3 getVectorNomalize(Vec3 vec)
    {
        double length = (double)Math.sqrt(vec.x * vec.x + vec.y * vec.y + vec.z * vec.z);
        if (length < 1.0E-4D)
        {
            return new Vec3(0, 0, 0);
        }
        length = 1.0D / length;
        return new Vec3(vec.x * length, vec.y * length, vec.z * length);
    }

    /** ベクトル加算 */
    public static final Vec3 getVectorAdd(Vec3 originalVec, Vec3 addVec)
    {
        return new Vec3(originalVec.x + addVec.x, originalVec.y + addVec.y, originalVec.z + addVec.z);
    }

    /** ベクトルスカラー乗算 */
    public static final Vec3 getVectorMultiply(Vec3 vec, double multiply)
    {
        return new Vec3(vec.x * multiply, vec.y * multiply, vec.z * multiply);
    }

    /** 内積（成分指定） */
    public static final double getInnerProduct(double vAX, double vAY, double vAZ, double vBX, double vBY, double vBZ)
    {
        return vAX * vBX + vAY * vBY + vAZ * vBZ;
    }

    /** 内積 */
    public static final double getInnerProduct(Vec3 a, Vec3 b)
    {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    /** ベクトル間の角度（-180～180度） */
    public static final float getVectorAndVectorAngle(Vec3 a, Vec3 b)
    {
        double inner = getInnerProduct(a, b);
        return getAngleMax180((float)(Math.acos(inner / (a.length() * b.length())) / Math.PI * 180.0D));
    }

    /** 外積（成分指定） */
    public static final Vec3 getOuterProduct(double aX, double aY, double aZ, double bX, double bY, double bZ)
    {
        return new Vec3(aY * bZ - aZ * bY, aZ * bX - aX * bZ, aX * bY - aY * bX);
    }

    /** 外積 */
    public static final Vec3 getOuterProduct(Vec3 a, Vec3 b)
    {
        return getOuterProduct(a.x, a.y, a.z, b.x, b.y, b.z);
    }

    /** 二つの角度の差（-180～180度） */
    public static final float getAngleAndAngleSpan(float angleA, float angleB)
    {
        angleA = getAngleMax180(angleA);
        angleB = getAngleMax180(angleB);

        if(angleA == 0.0F)
        {
            return angleB;
        }
        else if(angleA > 0.0F)
        {
            if(angleB > angleA - 180F)
            {
                return angleB - angleA;
            }
            else
            {
                return (angleB - angleA) + 360F;
            }
        }
        else
        {
            if(angleB < angleA + 180F)
            {
                return angleB - angleA;
            }
            else
            {
                return (angleB - angleA) - 360F;
            }
        }
    }

    /** Entity 間の水平方向の角度差 */
    public static final float getEntityAndEntityAngleSpanYaw(Entity a, Entity b)
    {
        float angleA = getAngleMax180(a.getYRot());
        float angleB = (float)Math.atan2(b.getX() - a.getX(), b.getZ() - a.getZ()) / 3.141593F * 180F;
        return getAngleAndAngleSpan(angleA, angleB);
    }

    /** Entity 間の垂直方向の角度差 */
    public static final float getEntityAndEntityAngleSpanPitch(Entity a, Entity b)
    {
        double distanceX = b.getX() - a.getX();
        double distanceY = b.getY() - a.getY();
        double distanceZ = b.getZ() - a.getZ();
        double distanceXZ = Math.sqrt(distanceX * distanceX + distanceZ * distanceZ);
        float angleA = getAngleMax180(a.getXRot());
        float angleB = (float)Math.atan2(distanceY, distanceXZ) / 3.141593F * 180F;
        return getAngleAndAngleSpan(angleA, angleB);
    }

    /** 特殊な sin（0度で最低、180度で最大） */
    public static final double halfAbsSin(float angle)
    {
        angle = angle * 0.5F;
        return Math.abs(Math.sin(angle));
    }

    /** 目の高さを含めた Y 座標 */
    public static final double getPosYFromEye(LivingEntity living, double yAdjustment)
    {
        return living.getY() + living.getEyeHeight() + Math.sin((double)living.getXRot() / 180.0D * Math.PI) * yAdjustment + yAdjustment;
    }

    public static final double getPosYFromEye(LivingEntity living)
    {
        return living.getY() + living.getEyeHeight() + Math.sin((double)living.getXRot() / 180.0D * Math.PI) * -0.5D + Math.cos((double)living.getXRot() / 180.0D * Math.PI) * -0.5D;
    }

    /** 角度（度数）と強さからベクトル */
    public static final Vec3 getVecFromAngle(float yaw, float pitch, double force)
    {
        double yaw_rad = (double)yaw / 180.0D * Math.PI;
        double pitch_rad = (double)pitch / 180.0D * Math.PI;
        double vectorX = -Math.sin(yaw_rad) * Math.cos(pitch_rad) * force;
        double vectorY = -Math.sin(pitch_rad) * force;
        double vectorZ =  Math.cos(yaw_rad) * Math.cos(pitch_rad) * force;
        return new Vec3(vectorX, vectorY, vectorZ);
    }

    /** 角度から単位ベクトル */
    public static final Vec3 getVecFromAngle(float yaw, float pitch)
    {
        return getVecFromAngle(yaw, pitch, 1.0D);
    }

    /** ベクトルから水平角度 */
    public static final float getYawFromVector(double vectorX, double vectorZ)
    {
        return (float)((Math.atan2(vectorX, vectorZ) * 180D) / 3.1415927410125732D);
    }

    /** ベクトルから垂直角度 */
    public static final float getPitchFromVector(double vectorX, double vectorY, double vectorZ)
    {
        double f = Math.sqrt(vectorX * vectorX + vectorZ * vectorZ);
        return (float)((Math.atan2(vectorY, f) * 180D) / 3.1415927410125732D);
    }

    /** 特定角度から水平方向に angle 度ずれたベクトル */
    public static final Vec3 getRotationVector(double vectorX, double vectorY, double vectorZ, float angle, double force)
    {
        float disXZ = (float)Math.sqrt(vectorX * vectorX + vectorZ * vectorZ);
        float angleYaw = (float)Math.atan2(-vectorX, vectorZ);
        float anglePitch = (float)Math.atan2(-vectorY, disXZ);
        float baseYaw = angleYaw;
        float basePitch = anglePitch;
        double X1, X2, Z1, Z2, vecX, vecY, vecZ;
        float anglePitch_rad;
        angle = angle / 180.0F * (float)Math.PI;

        angleYaw = angle;
        anglePitch = 0;
        anglePitch_rad = radTrans(anglePitch);

        X1 = Math.sin(angleYaw) * Math.cos(baseYaw);
        Z1 = Math.sin(angleYaw) * Math.sin(baseYaw);
        X2 = Math.cos(angleYaw) * Math.sin(anglePitch_rad) * Math.sin(basePitch) * Math.sin(baseYaw);
        Z2 = Math.cos(angleYaw) * Math.sin(anglePitch_rad) * Math.sin(basePitch) * Math.cos(baseYaw);

        vecY = -Math.cos(angleYaw) * Math.sin(basePitch - anglePitch_rad);
        vecX = Math.cos(angleYaw) * Math.cos(anglePitch_rad) * vectorX + X1 - X2;
        vecZ = Math.cos(angleYaw) * Math.cos(anglePitch_rad) * vectorZ + Z1 + Z2;

        return new Vec3(vecX * force, vecY * force, vecZ * force);
    }

    public static final Vec3 getRotationVectorFromAngle(float yaw, float pitch, float angle, double force)
    {
        Vec3 vec3 = getVecFromAngle(yaw, pitch, 1.0D);
        return getRotationVector(vec3.x, vec3.y, vec3.z, angle, force);
    }

    /** 任意の回転軸に対して任意のベクトルが angle 度回転したベクトル */
    public static Vec3 getVectorFromRotation(double rx, double ry, double rz,
                                             double ax, double ay, double az, float angle)
    {
        double angleRad = (double)angle / 180.0D * (double)Math.PI;
        double sinA = Math.sin(angleRad);
        double cosA = Math.cos(angleRad);
        double returnVectorX = (rx * rx * (1 - cosA) + cosA) * ax + (rx * ry * (1 - cosA) - rz * sinA) * ay + (rz * rx * (1 - cosA) + ry * sinA) * az;
        double returnVectorY = (rx * ry * (1 - cosA) + rz * sinA) * ax + (ry * ry * (1 - cosA) + cosA) * ay + (ry * rz * (1 - cosA) - rx * sinA) * az;
        double returnVectorZ = (rz * rx * (1 - cosA) - ry * sinA) * ax + (ry * rz * (1 - cosA) + rx * sinA) * ay + (rz * rz * (1 - cosA) + cosA) * az;

        return new Vec3(returnVectorX, returnVectorY, returnVectorZ);
    }

    public static Vec3 getVectorFromRotation(Vec3 rotate, Vec3 angleVector, float angle)
    {
        return getVectorFromRotation(rotate.x, rotate.y, rotate.z, angleVector.x, angleVector.y, angleVector.z, angle);
    }


private static int ways[][] ={	{ 0},
			
			{ 1},
			{ 1, 1},
			{ 1, 0, 2, 0},
			{ 1, 0, 3, 0},
			{ 1, 3, 1},
			{ 1, 4, 1},
			{ 1, 5, 1},
			{ 1, 3, 3, 1},
			{ 1, 5, 3, 0},
			{ 1, 4, 4, 1},
			
			{ 1, 6, 4, 0},
			{ 1, 5, 5, 1},
			{ 1, 3, 5, 3, 1},
			{ 1, 3, 6, 3, 1},
			{ 1, 4, 5, 4, 1},
			{ 1, 4, 6, 4, 1},
			{ 1, 4, 7, 4, 1},
			{ 1, 3, 5, 5, 3, 1},
			{ 1, 3, 6, 6, 3},
			{ 1, 3, 6, 6, 3, 1},
			
			{ 1, 5, 9, 5, 1},
			{ 1, 3, 7, 7, 3, 1},
			{ 1, 6, 9, 6, 1},
			{ 1, 4, 7, 7, 4, 1},
			{ 1, 3, 5, 7, 5, 3, 1},
			{ 1, 3, 5, 8, 5, 3, 1},
			{ 1, 3, 5, 9, 5, 3, 1},
			{ 1, 3, 6, 8, 6, 3, 1},
			{ 1, 3, 6, 9, 6, 3, 1},
			{ 1, 4, 6, 8, 6, 4, 1},
			
			{ 1, 3, 6, 11, 6, 3, 1},
			{ 1, 3, 6, 12, 6, 3, 1},
			{ 0},
			{ 0},
			{ 0},
			{ 0},
			{ 0},
			{ 0},
			{ 0},
			{ 0},
			
			{ 0},
			{ 0},
			{ 0},
			{ 1, 3, 6, 12, 12, 6, 3, 1}
			
};
	
////

public static final EntityTHShot createShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, float slope, 
											Vec3 rotate, float rotationSpeed, int rotationEnd,
											double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity, ShotData shot)
	{
		net.minecraft.world.level.Level world = source.level();

		EntityTHShot entityTHShot;
		ShotData shot2 = shot;
		boolean random = false;
		boolean rainbow = false;
		if(shot2 != null)
		{
			if(shot2.color == RAINBOW)
			{
				shot2.color = new Random().nextInt(7);
				rainbow = true;
			}
			else if(shot2.color == RANDOM)
			{
				shot2.color = new Random().nextInt(8);
				random = true;
			}
		}
		
		entityTHShot = new EntityTHShot(world, user, source, pos,
										angle, slope, 
										rotate, rotationSpeed, rotationEnd,
										firstSpeed, limitSpeed, acceleration,
										gravity, shot2);
		if(!world.isClientSide)
		{
			world.addFreshEntity(entityTHShot);
			
		}
		if(random)
		{
			shot.color = RANDOM;
		}
		else if(rainbow)
		{
			shot.color = RAINBOW;
		}
		return entityTHShot;
	}
	
	/**
	 * 軌道変化しない弾の基礎となる生成処理
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param slope		 : 弾の進行方向を軸とした傾き。度数で指定（180にすると弾が上下ひっくり返る）
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @return 生成したEntityTHShotを返す
	 */
	public static final EntityTHShot createShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, float slope,
											double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											ShotData shot)
	{
		float yaw = getYawFromVector(angle.x, angle.z);
		float pitch = getPitchFromVector(angle.x, angle.y, angle.z);
		Vec3 rotate = getVecFromAngle(yaw, pitch + 90F);
		return createShot(user, source, pos, angle, slope, rotate, 0F, 9999,
										firstSpeed, limitSpeed, acceleration, gravity, shot);
	}
	
	/**
	 * 加減速を指定できる一般的な弾の生成
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param shot			 : 弾の動作以外のデータ
	 * @return 生成したEntityTHShotを返す
	 */
	public static final EntityTHShot createShot(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration, ShotData shot)
	{	
		return createShot(user, pos, angle, firstSpeed, limitSpeed, acceleration, gravity_Zero(), shot);
	}
	
	
	/**
	 * 加減速と重力加速度を指定できる弾の生成
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @return 生成したEntityTHShotを返す
	 */
	public static final EntityTHShot createShot(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity, ShotData shot)
	{

		return createShot(user, user, pos, angle, 0F,
						firstSpeed, limitSpeed, acceleration, gravity, shot);
	}

	
	/**
	 * 最も単純な弾の生成
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param speed    	 : 弾の速度
	 * @param shot			 : 弾の動作以外のデータ
	 * @return 生成したEntityTHShotを返す
	 */
	public static final EntityTHShot createShot(LivingEntity user, Vec3 pos, Vec3 angle, double speed, ShotData shot)
	{
		return createShot(user, pos, angle, speed, speed, 0.0D, shot);
	}
	
	
	/**
	 * すべての扇状弾の基礎となる処理	Create Wide Shots Base
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param wideAngle     : 扇状の成す角度
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param baseAngle	 : 弾の角度のズレ
	 */
	public static final void createWideShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, float rotationSpeed, int rotationEnd,
											double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											ShotData shot, int way, float wideAngle, double distance, float baseAngle)
	{	
		//弾の発射方向に対して上方に９０度回転させたベクトル（回転軸）を取得する
		float yaw = getYawFromVector(angle.x, angle.z);
		float pitch = getPitchFromVector(angle.x, angle.y, angle.z);
		//Vec3 rotateVec = getOverVector(angle);
		Vec3 rotateVec = THShotLib.getVecFromAngle(-yaw, - pitch + 90F, 1.0F);
		//扇の成す角度の半分、取得した回転軸を回転させる
		float angle2 = -wideAngle / 2F;
		float span = wideAngle / (way - 1);
		angle2 += baseAngle;
		
		//色関連
		int color = new Random().nextInt(7);
		int rainbowPattern[] = {RED, ORANGE, YELLOW, GREEN, AQUA, BLUE, PURPLE};
		boolean random = false;
		boolean rainbow = false;
		ShotData shot2 = shot;
		if(shot2.color == RANDOM)
		{
			random = true;
		}
		else if(shot2.color == RAINBOW)
		{
			rainbow = true;
		}
		
		if(random)
		{
			shot2.color = new Random().nextInt(8);
		}
		
		//扇の角度になるまで回転軸に沿って弾を配置する
		for(int i = 0; i < way; i++)
		{

			if(rainbow)
			{
				shot2.color = rainbowPattern[color % 7];
				color++;
			}
			Vec3 angleVec = getVectorFromRotation(rotateVec.x, rotateVec.y, rotateVec.z, angle.x, angle.y, angle.z, angle2);
			createShot(user, source, new Vec3(pos.x + angleVec.x * distance, pos.y + angleVec.y * distance, pos.z + angleVec.z * distance), angleVec,
					 pitch * (float)Math.sin(angle2 / 180F * 3.141593F), rotateVec, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration, gravity, shot);
			angle2 += span;
		}
		if(random)
		{
			shot.color = RANDOM;
		}
		else if(rainbow)
		{
			shot.color = RAINBOW;
		}
	}
	
	/**
	 * 旧すべての扇状弾の基礎となる処理
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param wideAngle     : 扇状の成す角度
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 */
	public static final void createWideShot(LivingEntity user, Entity source, Vec3 pos,
			Vec3 angle, float rotationSpeed, int rotationEnd,
			double firstSpeed, double limitSpeed, double acceleration,
			Vec3 gravity,
			ShotData shot, int way, float wideAngle, double distance)
	{
		createWideShot(user, source, pos, angle, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration,
				gravity, shot, way, wideAngle, distance, 0F);
	}
	
	/**
	 * 軌道変化しない扇状弾の基礎となる処理
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param wideAngle     : 扇状の成す角度
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param baseAngle	 : 弾の角度のズレ
	 */
	public static final void createWideShot(LivingEntity user, Entity source, Vec3 pos,
			Vec3 angle, 
			double firstSpeed, double limitSpeed, double acceleration,
			Vec3 gravity,
			ShotData shot, int way, float wideAngle, double distance, float baseAngle)
	{
		createWideShot(user, source, pos, angle, 0F, 9999, firstSpeed, limitSpeed, acceleration,
				gravity, shot, way, wideAngle, distance, baseAngle);
	}
	
	/**
	 * 旧軌道変化しない扇状弾の基礎となる処理
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param wideAngle     : 扇状の成す角度
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 */
	public static final void createWideShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											ShotData shot, int way, float wideAngle, double distance)
	{	
		createWideShot(user, source, pos, angle, 0F, 9999, firstSpeed, limitSpeed, acceleration, gravity, shot, way, wideAngle, distance);
	}
	
	/**
	 * 最も一般的な扇状弾に加減速が加わったもの
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param wideAngle     : 扇状の成す角度
	 */
	public static final void createWideShot(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration, ShotData shot, int way, float wideAngle)
	{
		createWideShot(user, user, pos, angle, firstSpeed, limitSpeed, acceleration, gravity_Zero(),
				shot, way, wideAngle, 0.0D);
	}
	
	/**
	 * 最も一般的な扇状弾
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param speed    	 : 弾の速度
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param wideAngle     : 扇状の成す角度
	 */
	public static final void createWideShot(LivingEntity user, Vec3 pos, Vec3 angle, double speed, ShotData shot, int way, float wideAngle)
	{
		createWideShot(user, pos, angle, speed, speed, 0.0D, shot, way, wideAngle);
	}
	
	/**
	 * すべての全方位弾の基礎となる処理	Create Circle Shots Base
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param baseAngle	 : 弾の角度のズレ
	 */
	public static final void createCircleShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, float rotationSpeed, int rotationEnd,
											double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											ShotData shot, int way, double distance, float baseAngle)
	{
		if(way % 2 == 0)
		{
			baseAngle += 360F / (way * 2F);
		}

		createWideShot(user, source, pos,
				angle, rotationSpeed, rotationEnd,
				firstSpeed, limitSpeed, acceleration,
				gravity,
				shot,  way, 360F - (360F / way), distance, baseAngle);
	}
	
	/**
	 * 旧すべての全方位弾の基礎となる処理
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 */
	public static final void createCircleShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, float rotationSpeed, int rotationEnd,
											double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											ShotData shot, int way, double distance)
	{
		createCircleShot(user, source, pos,
				angle, rotationSpeed, rotationEnd,
				firstSpeed, limitSpeed, acceleration,
				gravity,
				shot,  way, distance, 0F);
	}
	
	/**
	 * 角度連れのない全方位弾の基礎となる処理
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param baseAngle	 : 弾の角度のズレ
	 */
	public static final void createCircleShot(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											ShotData shot, int way, double distance, float baseAngle)
	{
		createCircleShot(user, source, pos,
				angle, 0F, 9999, firstSpeed, limitSpeed, acceleration,
				gravity,
				shot, way, distance, baseAngle);
	}
	
	/**
	 * 最も基本的な全方位弾を発射する
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param speed    	 : 弾の速度
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 */
	public static final void createCircleShot(LivingEntity user, Vec3 pos, Vec3 angle, double speed, ShotData shot, int way)
	{
		createCircleShot(user, pos, angle, speed, speed, 0.0D, shot, way);
	}
	
	/**
	 * 最も基本的な全方位弾を発射するに加速度遅延を指定できるもの
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 */
	public static final void createCircleShot(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration, ShotData shot, int way)
	{	
		createCircleShot(user, user, pos, angle, firstSpeed, limitSpeed, acceleration, gravity_Zero(), shot, way, shot.size, 0F);
	}
	
	
	/**
	 * 目の前にリング状の弾幕を発射する基礎処理
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param rinSpan       : リングの広がり。大きいほどリングが大きくなり中央ががら空きになる
	 * @param ringBaseAngle : リングの起点となる角度
	 */
	public static final void createRingShot(
		LivingEntity user, Entity source, Vec3 pos,
		Vec3 angle, float rotationSpeed, int rotationEnd,
		double firstSpeed, double limitSpeed, double acceleration,
		Vec3 gravity,
		ShotData shot, int way, double distance, float ringSpan, float ringBaseAngle)
	{
		ringSpan /= 2F;
		float yaw = getYawFromVector(-angle.x, angle.z);
		float pitch = getPitchFromVector(-angle.x, -angle.y, angle.z);
		Vec3 baseVec = getVecFromAngle(yaw, pitch + ringSpan, 1.0D);
		Vec3 baseOverVec = getVecFromAngle(yaw, pitch + ringSpan + 90F, 1.0D);
		Vec3 overVec = getVecFromAngle(yaw, pitch + 90F, 1.0D);
		Vec3 extendVec;
		float angle2 = ringBaseAngle;
		float span = 360F / way;
		
		Vec3 movingVec;
		
		int color = new Random().nextInt(7);
		int rainbowPattern[] = {RED, ORANGE, YELLOW, GREEN, AQUA, BLUE, PURPLE};
		boolean random = false;
		boolean rainbow = false;
		ShotData shot2 = shot;
		
		if(shot2 != null)
		{
			if(shot2.color == RANDOM)
			{
				random = true;
			}
			else if(shot2.color == RAINBOW)
			{
				rainbow = true;
			}
		}
		
		if(random)
		{
			shot2.color = new Random().nextInt(8);
		}
		
		//Vec3 sideVecBase = getVecFromAngle(yaw + 90F, 0F);//真横のベクトル
		//Vec3 sideVec = sideVecBase;
		
		
		
		for(int i = 0; i < way; i++)
		{
			if(rainbow)
			{
				shot2.color = rainbowPattern[color % 7];
				color++;
			}
			movingVec = getVectorFromRotation(angle.x, angle.y, angle.z, baseVec.x, baseVec.y, baseVec.z, angle2);
			extendVec = getVectorFromRotation(angle.x, angle.y, angle.z, overVec.x, overVec.y, overVec.z, angle2);
			Vec3 overVec2 = getVectorFromRotation(angle, baseOverVec, angle2);
			createShot(user, source, new Vec3(pos.x + extendVec.x * distance, pos.y + extendVec.y * distance, pos.z + extendVec.z * distance), new Vec3(movingVec.x, movingVec.y, movingVec.z),
					angle2, overVec2, rotationSpeed, rotationEnd,
					firstSpeed, limitSpeed, acceleration, gravity,
					shot2);
			angle2 += span;
		}
		if(random)
		{
			shot.color = RANDOM;
		}
		else if(rainbow)
		{
			shot.color = RAINBOW;
		}
	}
	
	/**
	 * 最も基本的な、目の前にリング状の弾幕を発射する
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param speed		 : 弾の速度
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param rinSpan       : リングの広がり。大きいほどリングが大きくなり中央ががら空きになる
	 * @param ringBaseAngle : リングの起点となる角度
	 */
	public static final void createRingShot(
			LivingEntity user, Vec3 pos,
			Vec3 angle, double speed, 
			ShotData shot, int way, double distance, float ringSpan, float ringBaseAngle)
	{
		ShotData shot2 = shot;
		createRingShot(user, user, pos, angle, 0F, 9999,
				speed, speed, 0.0D, gravity_Zero(), 
				shot2,
				way, distance, ringSpan, ringBaseAngle);
	}
	
	/**
	 * 目の前の輪状空間にランダムに弾を発射する
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param num           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param rinSpan       : 輪状空間の広がり。大きいほど輪状空間が大きくなりばらつく
	 */
	public static final void createRandomRingShot(
		LivingEntity user, Entity source, Vec3 pos,
		Vec3 angle, float rotationSpeed, int rotationEnd,
		double firstSpeed, double limitSpeed, double acceleration,
		Vec3 gravity,
		ShotData shot, int num, double distance, float ringSpan)
	{
		float ringMaxSpan = ringSpan / 2F;
		float yaw = getYawFromVector(-angle.x, angle.z);
		float pitch = getPitchFromVector(angle.x, angle.y, angle.z);
		Vec3 baseVec;
		Vec3 overVec = getVecFromAngle(-yaw, pitch + 90F, 1.0D);
		Vec3 extendVec;
		float angle2;
		Random rand = new Random();
		
		Vec3 movingVec;
		
		
		int color = new Random().nextInt(7);
		int rainbowPattern[] = {RED, ORANGE, YELLOW, GREEN, AQUA, BLUE, PURPLE};
		boolean random = false;
		boolean rainbow = false;
		if(shot != null)
		{
			if(shot.color == RANDOM)
			{
				random = true;
			}
			else if(shot.color == RAINBOW)
			{
				rainbow = true;
			}
		}
		
		if(random)
		{
			shot.color = new Random().nextInt(8);
		}
		
		
		for(int i = 0; i < num; i++)
		{
			if(rainbow)
			{
				shot.color = rainbowPattern[color % 7];
				color++;
			}

			angle2 = rand.nextFloat() * 360F;
			baseVec = getVecFromAngle(-yaw, pitch + rand.nextFloat() * ringMaxSpan, 1.0D);
			movingVec = getVectorFromRotation(-angle.x, -angle.y, angle.z, baseVec.x, baseVec.y, baseVec.z, angle2);
			extendVec = getVectorFromRotation(-angle.x, -angle.y, angle.z, overVec.x, overVec.y, overVec.z, angle2);
			createShot(user, source, new Vec3(pos.x - extendVec.x * distance, pos.y - extendVec.y * distance, pos.z + extendVec.z * distance), new Vec3(-movingVec.x, -movingVec.y, movingVec.z),
					angle2, angle, rotationSpeed, rotationEnd,
					firstSpeed, limitSpeed, acceleration, gravity,
					shot);
		}
		if(random)
		{
			shot.color = RANDOM;
		}
		else if(rainbow)
		{
			shot.color = RAINBOW;
		}
	}
	
	/**
	 * 目の前の輪状空間にランダムに弾を発射する
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param speed	     : 弾の速度
	 * @param shot			 : 弾の動作以外のデータ
	 * @param num           : 弾数
	 * @param rinSpan       : 輪状空間の広がり。大きいほど輪状空間が大きくなりばらつく
	 */
	public static final void createRandomRingShot(
			LivingEntity user, Vec3 pos,
			Vec3 angle, double speed, ShotData shot, int num, float ringSpan)
	{
		createRandomRingShot(user, pos, angle, 
				speed, speed, 0.0D, shot, num, 0.0D, ringSpan);
	}
	
	
	/**
	 * 目の前の輪状空間にランダムに弾を発射する
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param shot			 : 弾の動作以外のデータ
	 * @param num           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param rinSpan       : 輪状空間の広がり。大きいほど輪状空間が大きくなりばらつく
	 */
	public static final void createRandomRingShot(
			LivingEntity user, Vec3 pos,
			Vec3 angle,
			double firstSpeed, double limitSpeed, double acceleration,
			ShotData shot, int num, double distance, float ringSpan)
	{
		createRandomRingShot(user, user, pos, angle, 0F, 9999, firstSpeed, limitSpeed, acceleration,
				gravity_Zero(), shot, num, distance, ringSpan);
	}
	
	
	/**
	 * 球状の弾幕を発射する
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向（ここでは基準となる弾の方向）
	 * @param rotate		 : 弾が角速度を持つときの回転軸
	 * @param rotationSpeed : 弾が角度を変える速度（毎フレーム指定した角度（度数）で向きを変える）
	 * @param rotationEnd	 : 弾の角速度を0にする時間。特に終わらせないなら9999あたりを入れる
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param baseAngle     : 基準となる点からの角度のずれ
	 */
	public static final void createSphereShot(
			LivingEntity user, Entity source, Vec3 pos,
			Vec3 angle, float angleZ, 
			Vec3 rotate, float rotationSpeed, int rotationEnd,
			double firstSpeed, double limitSpeed, double acceleration,
			Vec3 gravity,
			ShotData shot, int way, double distance, float baseAngle)
	{
		//45発以上は未実装
		if(way >= ways.length)
		{
			return;
		}
		
		Vec3 frontPos = pos(pos.x + angle.x * distance, pos.y + angle.y * distance, pos.z + angle.z * distance);
		Vec3 backPos = pos(pos.x - angle.x * distance, pos.y - angle.y * distance, pos.z - angle.z * distance);
		

		
		float angleBase = 0F;
		float angleSpan = 0F;
		if(ways[way].length >= 2)
		{
			angleSpan = 360F / (float)(ways[way].length - 1);
		}
		boolean flagFB = false;
		int oldWay = 0;
		float slope = 0F;
		
		ShotData shot2 = shot;
		if(shot2.color == RANDOM)
		{
			shot2.color = new Random().nextInt(8);
		}
		
		for(int i = 0; i < ways[way].length; i++)
		{
			//slope = 0F;
			if(ways[way][i] == 1)
			{
				if(!flagFB)
				{
					createShot(user, source, frontPos, angle, angleZ, 
						rotate, rotationSpeed, rotationEnd,
						firstSpeed, limitSpeed, acceleration, gravity,
						shot2);
					flagFB = true;
				}
				else
				{
					createShot(user, source, backPos, angle(-angle.x, -angle.y, -angle.z), angleZ, 
							rotate, rotationSpeed, rotationEnd,
							firstSpeed, limitSpeed, acceleration, gravity,
							shot2);
				}
			}
			else
			{
				createRingShot(user, source, pos, angle, rotationSpeed, rotationEnd, firstSpeed, limitSpeed, acceleration, gravity, shot2, ways[way][i], distance, angleBase, baseAngle + slope);
				slope += 180F / ways[way].length;
			}
			
			oldWay = ways[way].length;
			angleBase += angleSpan;
		}

	}
	
	/**
	 * 球状の弾幕を発射する
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向（ここでは基準となる弾の方向）
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param baseAngle     : 基準となる点からの角度のずれ
	 */
	public static final void createSphereShot(
			LivingEntity user, Entity source, Vec3 pos,
			Vec3 angle, float angleZ, 
			double firstSpeed, double limitSpeed, double acceleration,
			Vec3 gravity,
			ShotData shot, int way, double distance, float baseAngle)
	{
		createSphereShot(user, source, pos, angle, angleZ, rotate_Default(), 0.0F, 9999, firstSpeed, limitSpeed, acceleration,
				gravity, shot, way, distance, baseAngle);
	}
	
	/**
	 * 球状の弾幕を発射する
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向（ここでは基準となる弾の方向）
	 * @param speed	     : 弾の速度
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param shot			 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param baseAngle     : 基準となる点からの角度のずれ
	 */
	public static final void createSphereShot(
			LivingEntity user, Vec3 pos, Vec3 angle, double speed, ShotData shot, int way, float baseAngle)
	{
		createSphereShot(user, user, pos, angle, 0F, rotate_Default(), 0.0F, 9999, speed, speed, 0.0D, gravity_Zero(),
				shot, way, 0.0D, baseAngle);
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

public static void playShotSound(Entity entity)
	{
		// 原版 "random.bow" -> 1.20.1 SoundEvents.ARROW_SHOOT
		entity.level().playSound(null, entity.blockPosition(), net.minecraft.sounds.SoundEvents.ARROW_SHOOT,
				net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 1.2F);//音を出す
	}

	public static final EntityTHLaser createLaserA(	LivingEntity user, Entity source, Vec3 pos, Vec3 angle,
												double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
												LaserData laser)
	{
		if(laser.color == RANDOM )
		{
			laser.color = new Random().nextInt(8);
		}
		else if(laser.color == RAINBOW)
		{
			laser.color = new Random().nextInt(7);
		}
		EntityTHLaser entityLaser = new EntityTHLaser(source.level(), user, source, pos, angle, 0F, rotate_Default(), 0F, 9999,
												firstSpeed, limitSpeed, acceleration, gravity,
												laser);
		if(!source.level().isClientSide)
		{
			//レーザーを生成する
			source.level().addFreshEntity(entityLaser);
		}
		return entityLaser;
	}

	/**
	 * 設置型レーザー（EntityTHSetLaser）を生成する（阶段 3d-4 追加，EntityMiniHakkero/EntitySukima 用）
	 * 原版 THShotLib.createLaserB
	 */
	public static final thKaguyaMod.entity.shot.EntityTHSetLaser createLaserB(LivingEntity user, Entity setting, Vec3 pos,
			Vec3 angle, Vec3 rotate, float rotateYawSpeed, int rotationEnd,
			LaserData laser, Entity set, double setLength, double setYOffset)
	{
		thKaguyaMod.entity.shot.EntityTHSetLaser entityLaser = new thKaguyaMod.entity.shot.EntityTHSetLaser(setting.level(), user, setting,
				pos, angle, 0F,
				rotate, rotateYawSpeed, rotationEnd, laser, set, setLength, setYOffset);

		if(!setting.level().isClientSide)
		{
			//レーザーを生成する
			setting.level().addFreshEntity(entityLaser);
		}
		return entityLaser;
	}

	/*最も一般的な移動レーザーを生成
	 * user : レーザーの使用者
	 * posX : レーザーの発射地点のX座標
	 * posY :
	 * posZ :
	 * angleYaw : レーザーの水平方向の角度
	 * anglePitch  : レーザーの水平方向の角度
	 * speed   : レーザーの速度
	 * damage  : レーザーのダメージ
	 * color   : レーザーの色
	 * width   : レーザーの太さ
	 * length  : レーザーの長さ
	 */
	/**
	 * 最も単純な移動レーザー発射を出す処理
	 * @param user          : レーザーの持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : レーザーの発射地点
	 * @param angle         : レーザーの進行方向（ここでは基準となる弾の方向）
	 * @param speed         : レーザーの速度
	 * @param gravity       : 未実装　gravity_Zero()とお願いします
	 * @param laser         : レーザーの動作以外のデータ
	 * @return
	 */
	public static final EntityTHLaser createLaserA(LivingEntity user, Vec3 pos, Vec3 angle, double speed, LaserData laser)
	{
		return createLaserA(	user, pos, angle, speed, speed, 0.0D, laser);
	}


	/**
	 * 移動レーザーを出す処理。最も一般的なもの
	 * @param user          : レーザーの持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : レーザーの発射地点
	 * @param angle         : レーザーの進行方向（ここでは基準となる弾の方向）
	 * @param firstSpeed    : レーザーの初速度
	 * @param limitSpeed    : レーザーの限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : レーザーの加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 未実装　gravity_Zero()とお願いします
	 * @param laser         : レーザーの動作以外のデータ
	 * @return
	 */
	public static final EntityTHLaser createLaserA(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration, LaserData laser)
	{
		return createLaserA(	user, user, pos, angle, firstSpeed, limitSpeed, acceleration, gravity_Zero(), laser);
	}


	/**すべてのn-wayレーザーの基礎となる処理
	 * @param user          : レーザーの持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : レーザーの発射元。あまり意味は無い
	 * @param pos           : レーザーの発射地点
	 * @param angle         : レーザーの進行方向（ここでは基準となる弾の方向）
	 * @param firstSpeed    : レーザーの初速度
	 * @param limitSpeed    : レーザーの限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : レーザーの加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 未実装　gravity_Zero()とお願いします
	 * @param laser         : レーザーの動作以外のデータ
	 * @param way           : 弾数
	 * @param wideAngle     : 扇状の成す角度
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 */
	public static final void createWideLaserA(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											LaserData laser, int way, float wideAngle, double distance)
	{
		//弾の発射方向に対して上方に９０度回転させたベクトル（回転軸）を取得する
		float yaw = getYawFromVector(angle.x, angle.z);
		float pitch = getPitchFromVector(angle.x, angle.y, angle.z);
		Vec3 rotateVec = THShotLib.getVecFromAngle(-yaw, - pitch + 90F, 1.0F);
		//扇の成す角度の半分、取得した回転軸を回転させる
		float angle2 = -wideAngle / 2F;
		float span = wideAngle / (way - 1);

		boolean rainbow = false;
		int color = new Random().nextInt(7);
		int rainbowPattern[] = {RED, ORANGE, YELLOW, GREEN, AQUA, BLUE, PURPLE};
		boolean random = false;
		if(laser.color == RANDOM)
		{
			random = true;
		}
		else if(laser.color == RAINBOW)
		{
			rainbow = true;
		}
		//扇の角度になるまで回転軸に沿って弾を配置する
		for(int i = 0; i < way; i++)
		{
			if(random)
			{
				laser.color = new Random().nextInt(8);
			}
			else if(rainbow)
			{
				laser.color = rainbowPattern[color % 7];
				color++;
			}
			Vec3 angleVec = getVectorFromRotation(rotateVec.x, rotateVec.y, rotateVec.z, angle.x, angle.y, angle.z, angle2);
			createLaserA(user, source, pos(pos.x + angleVec.x * distance, pos.y + angleVec.y * distance, pos.z + angleVec.z * distance), angleVec,
					firstSpeed, limitSpeed, acceleration, gravity, laser);
			angle2 += span;
		}
	}

	/**最も単純なn-wayレーザーを出す処理
	 * @param user          : レーザーの持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : レーザーの発射地点
	 * @param angle         : レーザーの進行方向（ここでは基準となる弾の方向）
	 * @param speed         : レーザーの初速度
	 * @param laser         : レーザーの動作以外のデータ
	 * @param way           : 弾数
	 * @param wideAngle     : 扇状の成す角度
	 */
	public static final void createWideLaserA(LivingEntity user, Vec3 pos, Vec3 angle, double speed,
			LaserData laser, int way, float wideAngle)
	{
		createWideLaserA(user, pos, angle, speed, speed, 0.0D, laser, way, wideAngle);
	}

	/**一般的なn-wayレーザーを出す処理
	 * @param user          : レーザーの持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : レーザーの発射地点
	 * @param angle         : レーザーの進行方向（ここでは基準となる弾の方向）
	 * @param firstSpeed    : レーザーの初速度
	 * @param limitSpeed    : レーザーの限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : レーザーの加速度。毎フレームこの数値分速く（遅く）なる
	 * @param laser         : レーザーの動作以外のデータ
	 * @param way           : 弾数
	 * @param wideAngle     : 扇状の成す角度
	 */
	public static final void createWideLaserA(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration,
			LaserData laser, int way, float wideAngle)
	{
		createWideLaserA(user, user, pos, angle, firstSpeed, limitSpeed, acceleration,
				gravity_Zero(), laser, way, wideAngle, 0.0D);
	}

	/**すべての全方位レーザーの基礎となる処理
	 * @param user          : レーザーの持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : レーザーの発射元。あまり意味は無い
	 * @param pos           : レーザーの発射地点
	 * @param angle         : レーザーの進行方向（ここでは基準となる弾の方向）
	 * @param firstSpeed    : レーザーの初速度
	 * @param limitSpeed    : レーザーの限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : レーザーの加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 未実装　gravity_Zero()とお願いします
	 * @param laser         : レーザーの動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 */
	public static final void createCircleLaserA(LivingEntity user, Entity source, Vec3 pos,
											Vec3 angle, double firstSpeed, double limitSpeed, double acceleration,
											Vec3 gravity,
											LaserData laser, int way, double distance)
	{
		createWideLaserA(user, source, pos,
				angle, firstSpeed, limitSpeed, acceleration,
				gravity,
				laser, way + 1, 360F, distance);
	}

	/**最も単純な全方位レーザーを出す処理
	 * @param user          : レーザーの持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : レーザーの発射地点
	 * @param angle         : レーザーの進行方向（ここでは基準となる弾の方向）
	 * @param speed         : レーザーの速度
	 * @param laser         : レーザーの動作以外のデータ
	 * @param way           : 弾数
	 */
	public static final void createCircleLaserA(LivingEntity user, Vec3 pos, Vec3 angle, double speed,
			LaserData laser, int way)
	{
		createCircleLaserA(user, pos, angle, speed, speed, 0.0D, laser, way);
	}

	/**一般的な全方位レーザーを出す処理
	 * @param user          : レーザーの持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : レーザーの発射地点
	 * @param angle         : レーザーの進行方向（ここでは基準となる弾の方向）
	 * @param firstSpeed    : レーザーの初速度
	 * @param limitSpeed    : レーザーの限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : レーザーの加速度。毎フレームこの数値分速く（遅く）なる
	 * @param laser         : レーザーの動作以外のデータ
	 * @param way           : 弾数
	 */
	public static final void createCircleLaserA(LivingEntity user, Vec3 pos, Vec3 angle, double firstSpeed, double limitSpeed, double acceleration,
			LaserData laser, int way)
	{
		createCircleLaserA(user, user, pos, angle, firstSpeed, limitSpeed, acceleration,
				gravity_Zero(), laser, way, 0.0D);
	}

	/**
	 * 目の前にリング状のレーザーを発射する基礎処理
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param laser		 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param rinSpan       : リングの広がり。大きいほどリングが大きくなり中央ががら空きになる
	 * @param ringBaseAngle : リングの起点となる角度
	 */
	public static final void createRingLaserA(
		LivingEntity user, Entity source, Vec3 pos,
		Vec3 angle,
		double firstSpeed, double limitSpeed, double acceleration,
		Vec3 gravity,
		LaserData laser, int way, double distance, float ringSpan, float ringBaseAngle)
	{
		ringSpan /= 2F;
		float yaw = getYawFromVector(-angle.x, angle.z);
		float pitch = getPitchFromVector(angle.x, angle.y, angle.z);
		Vec3 baseVec = getVecFromAngle(-yaw, pitch + ringSpan, 1.0D);
		Vec3 overVec = getVecFromAngle(-yaw, pitch + 90F, 1.0D);
		Vec3 extendVec;
		float angle2 = ringBaseAngle;
		float span = 360F / way;

		Vec3 movingVec;

		int color = new Random().nextInt(7);
		int rainbowPattern[] = {RED, ORANGE, YELLOW, GREEN, AQUA, BLUE, PURPLE};
		boolean random = false;
		boolean rainbow = false;
		LaserData laser2 = laser;
		if(laser2.color == RANDOM)
		{
			random = true;
		}
		else if(laser2.color == RAINBOW)
		{
			rainbow = true;
		}

		if(random)
		{
			laser2.color = new Random().nextInt(8);
		}


		for(int i = 0; i < way; i++)
		{
			if(rainbow)
			{
				laser2.color = rainbowPattern[color % 7];
				color++;
			}

			movingVec = getVectorFromRotation(-angle.x, -angle.y, angle.z, baseVec.x, baseVec.y, baseVec.z, angle2);
			extendVec = getVectorFromRotation(-angle.x, -angle.y, angle.z, overVec.x, overVec.y, overVec.z, angle2);
			createLaserA(user, source, pos(pos.x - extendVec.x * distance, pos.y - extendVec.y * distance, pos.z + extendVec.z * distance), angle(-movingVec.x, -movingVec.y, movingVec.z),
					firstSpeed, limitSpeed, acceleration, gravity,
					laser2);
			angle2 += span;
		}
		if(random)
		{
			laser.color = RANDOM;
		}
		else if(rainbow)
		{
			laser.color = RAINBOW;
		}
	}

	/**
	 * 目の前にリング状のレーザーを発射する基礎処理
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param pos           : 弾の発射地点
	 * @param angle         : 弾の進行方向
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param laser		 : 弾の動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param rinSpan       : リングの広がり。大きいほどリングが大きくなり中央ががら空きになる
	 * @param ringBaseAngle : リングの起点となる角度
	 */
	public static final void createRingLaserA(
		LivingEntity user, Vec3 pos,
		Vec3 angle,
		double firstSpeed, double limitSpeed, double acceleration,
		LaserData laser, int way, double distance, float ringSpan, float ringBaseAngle)
	{
		createRingLaserA(user, user, pos, angle, firstSpeed, limitSpeed, acceleration, gravity_Zero(), laser,
				way, distance, ringSpan, ringBaseAngle);
	}


	/**
	 * 目の前の輪状空間にランダムにレーザーを発射する
	 * @param user          : 弾の持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : 弾の発射元。あまり意味は無い
	 * @param posX          : 弾のX座標の発射地点。
	 * @param posY          : 弾のY座標の発射地点。THShotLib.getPosYFromEye(living)を使えば、プレイヤーの目より少し低い位置に生成される。
	 * @param posZ          :
	 * @param vectorX       : 弾のX方向のベクトル値
	 * @param vectorY       :
	 * @param vectorZ       :
	 * @param firstSpeed    : 弾の初速度
	 * @param limitSpeed    : 弾の限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : 弾の加速度。毎フレームこの数値分速く（遅く）なる
	 * @param vectorX_G     : 弾のX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param vectorY_G     :
	 * @param vectorZ_G     :
	 * @param damage        : 弾のダメージ
	 * @param color         : レーザーの色
	 * 							ex: THShotLib.YELLOW　で黄色のレーザーというように指定できる
	 * @param width         : レーザーの太さ。当たり判定、描画サイズに関わる
	 * @param endTime       : 弾の消滅時間。この時間になったら強制的に消滅する。懐中時計で止められている場合は時間は進まず停滞する
	 * @param delay         : 弾の遅延時間。この時間が経過後に実体化して動作を起こす。それまでは光のエフェクトだけで何もしない
	 * @param special       : 弾の特殊な処理を指定する。普通の弾は0。THShotLib.BOUND01とすれば１回跳ね返る弾になる
	 * @param length        : レーザーの長さ
	 * @param num           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param rinSpan       : 輪状空間の広がり。大きいほど輪状空間が大きくなりばらつく
	 */
	public static final void createRandomRingLaser(
		LivingEntity user, Entity source, Vec3 pos,
		Vec3 angle,
		double firstSpeed, double limitSpeed, double acceleration,
		Vec3 gravity,
		LaserData laser, int num, double distance, float ringSpan)
	{
		float ringMaxSpan = ringSpan / 2F;
		float yaw = getYawFromVector(-angle.x, angle.z);
		float pitch = getPitchFromVector(angle.x, angle.y, angle.z);
		Vec3 baseVec;
		Vec3 overVec = getVecFromAngle(-yaw, pitch + 90F, 1.0D);
		Vec3 extendVec;
		float angle2;
		Random rand = new Random();

		Vec3 movingVec;
		for(int i = 0; i < num; i++)
		{
			angle2 = rand.nextFloat() * 360F;
			baseVec = getVecFromAngle(-yaw, pitch + rand.nextFloat() * ringMaxSpan, 1.0D);
			movingVec = getVectorFromRotation(-angle.x, -angle.y, angle.z, baseVec.x, baseVec.y, baseVec.z, angle2);
			extendVec = getVectorFromRotation(-angle.x, -angle.y, angle.z, overVec.x, overVec.y, overVec.z, angle2);

			createLaserA(user, source, pos(pos.x - extendVec.x * distance, pos.y - extendVec.y * distance, pos.z + extendVec.z * distance),angle( -movingVec.x, -movingVec.y, movingVec.z),
					firstSpeed, limitSpeed, acceleration, gravity,
					laser);
		}
	}

	/**
	 * 球状のレーザーを発射する
	 * @param user          : レーザーの持ち主、当たった場合持ち主が当てたことになる。また、持ち主が死ぬと消滅したり、アイテム化する
	 * @param source        : レーザーの発射元。あまり意味は無い
	 * @param pos           : レーザーの発射地点
	 * @param angle         : レーザーの進行方向（ここでは基準となる弾の方向）
	 * @param firstSpeed    : レーザーの初速度
	 * @param limitSpeed    : レーザーの限界速度。これ以上は速く（遅く）ならない
	 * @param acceleration  : レーザーの加速度。毎フレームこの数値分速く（遅く）なる
	 * @param gravity       : レーザーのX軸方向の重力値。0.0Dで重力の影響を受けない
	 * @param laser		 : レーザーの動作以外のデータ
	 * @param way           : 弾数
	 * @param distance      : 発射地点からこの分だけ離して発射する
	 * @param baseAngle     : 基準となる点からの角度のずれ
	 */
	public static final void createSphereLaserA(
			LivingEntity user, Entity source, Vec3 pos,
			Vec3 angle,
			double firstSpeed, double limitSpeed, double acceleration,
			Vec3 gravity,
			LaserData laser, int way, double distance, float baseAngle)
	{
		//45発以上は未実装
		if(way >= ways.length)
		{
			return;
		}

		Vec3 frontPos = pos(pos.x + angle.x * distance, pos.y + angle.y * distance, pos.z + angle.z * distance);
		Vec3 backPos = pos(pos.x - angle.x * distance, pos.y - angle.y * distance, pos.z - angle.z * distance);



		float angleBase = 0F;
		float angleSpan = 0F;
		if(ways[way].length >= 2)
		{
			angleSpan = 360F / (float)(ways[way].length - 1);
		}
		boolean flagFB = false;
		int oldWay = 0;
		float slope = 0F;

		LaserData laser2 = laser;
		if(laser2.color == RANDOM)
		{
			laser2.color = new Random().nextInt(8);
		}

		for(int i = 0; i < ways[way].length; i++)
		{
			//slope = 0F;
			if(ways[way][i] == 1)
			{
				if(!flagFB)
				{
					createLaserA(user, source, frontPos, angle,
						firstSpeed, limitSpeed, acceleration, gravity,
						laser2);
					flagFB = true;
				}
				else
				{
					createLaserA(user, source, frontPos, angle(-angle.x, -angle.y, -angle.z),
							firstSpeed, limitSpeed, acceleration, gravity,
							laser2);
				}
			}
			else
			{
				createRingLaserA(user, source, pos, angle, firstSpeed, limitSpeed, acceleration, gravity, laser2, ways[way][i], distance, angleBase, baseAngle + slope);
				slope += 180F / ways[way].length;
			}

			oldWay = ways[way].length;
			angleBase += angleSpan;
		}
	}

	/**
	 * すべての設置レーザーの基礎となる生成処理
	 * 仕様変更がある可能性が大であり、不具合があるので設置レーザーを使うのは非推奨
	 * @param user
	 * @param setting
	 * @param pos
	 * @param angle
	 * @param rotate
	 * @param rotateYawSpeed
	 * @param rotationEnd
	 * @param laser
	 * @param set
	 * @param setLength
	 * @param setYOffset
	 * @return
	 */

	/* ==================== 弾消し（原版 THShotLib.danmakuRemove 移植 / 阶段 7-③）==================== */

	/**
	 * 周囲の弾幕を消去する（プレイヤーのスペルカード宣言時に呼ばれる）
	 *
	 * @param centerEntity 中心となるEntity
	 * @param range        有効範囲
	 * @param target       消去対象 "ALL" / "Enemy" / "Player" / "Other"
	 * @param isDropBonus  原版 true なら shotFinishBonus() で EntityShotMaterial を出現させていた。
	 *                     1.20.1 では EntityShotMaterial 未移植のため true でも discard に降格
	 *                     （ドロップ報酬は同エンティティ移植時に復元）。
	 * @return 消去した弾数
	 *
	 * 1.7.10 -> 1.20.1:
	 *   worldObj.getEntitiesWithinAABBExcludingEntity(e, box.expand(r,r,r))
	 *       -> level().getEntities(e, e.getBoundingBox().inflate(r,r,r))
	 *   entity.setDead()                          -> entity.discard()
	 *   entityTHShot.user instanceof EntityPlayer -> user instanceof Player
	 */
	/**
	 * 核弾を生成する基礎処理（原版 THShotLib.createNuclearShot）
	 *
	 * <p>通常の弾と違い EntityNuclearShot は使用者がアイテムを使い続けている間
	 * チャージされ、離した瞬間に飛ぶ（EntityNuclearShot.tick が player.isUsingItem() を見る）。
	 *
	 * <p>注意：原版は加速度の位置に firstSpeed を渡している（原版のコピペミス）。
	 * 呼び出し側（制御棒）は firstSpeed = acceleration = 0.0D なので実害はないが、
	 * 忠実移植のため原版どおり firstSpeed を渡す。
	 */
	public static final EntityNuclearShot createNuclearShot(LivingEntity user, Entity source,
			Vec3 pos, Vec3 angle, float slope,
			Vec3 rotate, float rotationSpeed, int rotationEnd,
			double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
			ShotData shot)
	{
		EntityNuclearShot nuclearShot = new EntityNuclearShot(source.level(), user, source,
				pos, angle, slope, rotate, rotationSpeed, rotationEnd,
				firstSpeed, limitSpeed, firstSpeed, gravity, shot);
		if(!source.level().isClientSide)
		{
			source.level().addFreshEntity(nuclearShot);
		}
		return nuclearShot;
	}

	public static int danmakuRemove(Entity centerEntity, double range, String target, boolean isDropBonus)
	{
		int count = 0;
		java.util.List<Entity> list = centerEntity.level().getEntities(centerEntity,
				centerEntity.getBoundingBox().inflate(range, range, range));

		for (Entity entity : list)
		{
			if (!(entity instanceof EntityTHShot))
			{
				continue;
			}

			EntityTHShot shot = (EntityTHShot)entity;
			boolean match;

			if ("ALL".equals(target))
			{
				match = true;
			}
			else if ("Enemy".equals(target))
			{
				match = !(shot.user instanceof Player);
			}
			else if ("Player".equals(target))
			{
				match = shot.user instanceof Player;
			}
			else if ("Other".equals(target))
			{
				match = shot.user != centerEntity;
			}
			else
			{
				match = false;
			}

			if (match)
			{
				// 原版 danmakuRemove :1680 付近: isDropBounus が true なら
				// shotFinishBonus()（弾素材を落とす）、false なら単に消す。
				if (isDropBonus)
				{
					shot.shotFinishBonus();
				}
				else
				{
					shot.discard();
				}
				count++;
			}
		}

		return count;
	}

	/* ================================================================
	 * 原版 THShotLib の爆炸/消弹系（:1581 explosionEffect / :1603 explosionEffect2 /
	 * :1711 banishExplosion / :2379 弾生成）。移植版曾整段缺失，
	 * 导致 boss 死亡演出与死亡时对周围妖精的伤害都没有发生。
	 * ================================================================ */

	/**
	 * 原版 THShotLib.explosionEffect（:1581）。
	 * 1.7.10 の {@code world.spawnParticle("hugeexplosion"/"largeexplode")} は
	 * 1.20.1 の {@code ParticleTypes.EXPLOSION_EMITTER}/{@code EXPLOSION} に対応。
	 */
	public static void explosionEffect(net.minecraft.world.level.Level level, double posX, double posY, double posZ, float explosionSize)
	{
		level.playSound(null, posX, posY, posZ, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
				net.minecraft.sounds.SoundSource.NEUTRAL, 2.0F, 3.0F);
		spawnExplosionParticle(level, posX, posY, posZ, explosionSize);
	}

	/**
	 * 原版 THShotLib.explosionEffect2（:1603）。音だけ花火の爆発音（fireworks.blast）になる。
	 */
	public static void explosionEffect2(net.minecraft.world.level.Level level, double posX, double posY, double posZ, float explosionSize)
	{
		level.playSound(null, posX, posY, posZ, net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
				net.minecraft.sounds.SoundSource.NEUTRAL, 2.0F, 3.0F);
		spawnExplosionParticle(level, posX, posY, posZ, explosionSize);
	}

	/** explosionEffect 系のパーティクル部分（サーバーなら周囲へ送信、クライアントなら直接追加）。 */
	private static void spawnExplosionParticle(net.minecraft.world.level.Level level, double posX, double posY, double posZ, float explosionSize)
	{
		boolean huge = explosionSize >= 2.0F;
		if(level instanceof net.minecraft.server.level.ServerLevel serverLevel)
		{
			serverLevel.sendParticles(huge ? net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER
					: net.minecraft.core.particles.ParticleTypes.EXPLOSION, posX, posY, posZ, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
		else if(level.isClientSide)
		{
			level.addParticle(huge ? net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER
					: net.minecraft.core.particles.ParticleTypes.EXPLOSION, posX, posY, posZ, 1.0D, 0.0D, 0.0D);
		}
	}

	/**
	 * 原版 THShotLib.banishExplosion（:1711）：倒れた Entity の周囲の妖精（使い魔を除く）に
	 * 距離減衰ダメージ（最低 2.0F）を与える。原版 EntityDanmakuMob.onDeathUpdate の deathTime==7 で呼ばれる。
	 */
	public static void banishExplosion(Entity deadEntity, double range, float maxDamage)
	{
		java.util.List<Entity> list = deadEntity.level().getEntities(deadEntity,
				deadEntity.getBoundingBox().inflate(range, range, range));

		for(Entity entity : list)
		{
			if(entity instanceof thKaguyaMod.entity.living.EntityTHFairy
					&& !(entity instanceof thKaguyaMod.entity.living.EntityFamiliar))
			{
				double distance = entity.distanceTo(deadEntity);
				if(distance <= range)
				{
					float damage = maxDamage * (1.0F - (float)(distance / range));
					if(damage < 2.0F)
					{
						damage = 2.0F;
					}
					entity.hurt(deadEntity.damageSources().indirectMagic(entity, deadEntity), damage);
				}
			}
		}
	}

	/**
	 * 原版 THShotLib.弾生成（:2379）：createShot の完全な引数版への別名（原版でも呼び出し箇所なし）。
	 */
	public static final EntityTHShot danmakuSpawn(LivingEntity user, Entity source, Vec3 pos,
			Vec3 angle, float slope,
			Vec3 rotate, float rotationSpeed, int rotationEnd,
			double firstSpeed, double limitSpeed, double acceleration,
			Vec3 gravity, ShotData shot)
	{
		return createShot(user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
				firstSpeed, limitSpeed, acceleration, gravity, shot);
	}

	/* ============ 弾の色名（原版 THShotLib.java:88-95） ============ */

	/** 弾の色の名前（日本語） */
	public static final String COLOR_NAME_JP[] = {
		"赤", "青", "緑", "黄", "紫", "水色", "橙", "白", "虹色", "ランダム", "暖色", "寒色", "", "", "", ""};

	/** 弾の色の名前（英語） */
	public static final String COLOR_NAME[] = {
		"Red", "Blue", "Green", "Yellow", "Purple", "Aqua", "Orange", "White", "Rainbow", "Random", "Hot", "Cold", "", "", "", ""};

}
