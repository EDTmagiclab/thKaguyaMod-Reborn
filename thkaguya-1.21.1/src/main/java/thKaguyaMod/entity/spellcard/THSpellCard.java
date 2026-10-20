package thKaguyaMod.entity.spellcard;

import static thKaguyaMod.DanmakuConstants.*;

import java.util.Random;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;

/** スペルカード（1.20.1 移植版） */
public abstract class THSpellCard
{
	public Random rand = new Random();
	protected Level world;
	private int end;
	private int remove;
	private int number;
	/** スペルカードが発動してからの時間 （1/20秒）*/
	public int time;
	/** スペルカードのレベル EASY:1, NORMAL:2, HARD:3, LUNATIC:4 */
	public int level;
	/** Entityとしてのスペルカード */
	public EntitySpellCard card;
	/** スペルカードを宣言したEntityLiving */
	public LivingEntity user;
	/** スペルカードを宣言されたEntityLiving */
	public LivingEntity target;

	protected double cardPosX;
	protected double cardPosY;
	protected double cardPosZ;
	protected double userPosX;
	protected double userPosY;
	protected double userPosZ;
	protected double targetPosX;
	protected double targetPosY;
	protected double targetPosZ;

	private int needLevel;
	private int originalUserName;
	protected String spellcardName;
	protected String iconName;

	public static THSpellCard instance;

	public static final int OLD = -1;
	public static final int REIMU = 0;
	public static final int MARISA = 1;
	public static final int RUMIA = 2;
	public static final int DAIYOUSEI = 3;
	public static final int CIRNO = 4;
	public static final int MEIRIN = 5;
	public static final int KOAKUMA = 6;
	public static final int PATCHOULI = 7;
	public static final int SAKUYA = 8;
	public static final int REMILIA = 9;
	public static final int FLANDORE = 10;
	public static final int LETTY = 11;
	public static final int CHEN = 12;
	public static final int ALICE = 13;
	public static final int LILY = 14;
	public static final int LUNASA = 15;
	public static final int MERLIN = 16;
	public static final int LYRICA = 17;
	public static final int YOUMU = 18;
	public static final int YUYUKO = 19;
	public static final int RAN = 20;
	public static final int YUKARI = 21;
	public static final int SUIKA = 22;
	public static final int WRIGGLE = 23;
	public static final int MYSTIA = 24;
	public static final int KEINE = 25;
	public static final int TEI = 26;
	public static final int UDONGEIN = 27;
	public static final int EIRIN = 28;
	public static final int KAGUYA = 29;
	public static final int MOKOU = 30;
	public static final int AYA = 31;
	public static final int MEDICINE = 32;
	public static final int YUUKA = 33;
	public static final int KOMACHI = 34;
	public static final int SHIKIEIKI = 35;
	public static final int SHIZUHA = 36;
	public static final int MINORIKO = 37;
	public static final int HINA = 38;
	public static final int NITORI = 39;
	public static final int MOMIZI = 40;
	public static final int SANAE = 41;
	public static final int KANAKO = 42;
	public static final int SUWAKO = 43;
	public static final int IKU = 44;
	public static final int TENSHI = 45;
	public static final int KISUME = 46;
	public static final int YAMAME = 47;
	public static final int PARSEE = 48;
	public static final int YUUGI = 49;
	public static final int SATORI = 50;
	public static final int RIN = 51;
	public static final int UTUHO = 52;
	public static final int KOISHI = 53;
	public static final int NAZRIN = 54;
	public static final int KOGASA = 55;
	public static final int ICHIRIN = 56;
	public static final int MINAMITU = 57;
	public static final int SHOU = 58;
	public static final int BYAKUREN = 59;
	public static final int NUE = 60;
	public static final int HATATE = 61;
	public static final int KYOUKO = 62;
	public static final int YOSHIKA = 63;
	public static final int SEIGA = 64;
	public static final int TOZIKO = 65;
	public static final int FUTO = 66;
	public static final int MIKO = 67;
	public static final int MAMIZOU = 68;
	public static final int KOKORO = 69;
	public static final int WAKASAGIHIME = 70;
	public static final int SEKIBANKI = 71;
	public static final int KAGEROU = 72;
	public static final int BENBEN = 73;
	public static final int YATUHASHI = 74;
	public static final int SEIJA = 75;
	public static final int SHINMYOUMARU = 76;
	public static final int RAIKO = 77;
	public static final int RINNOSUKE = 128;
	public static final int TOKIKO = 129;
	public static final int SUNNY = 132;
	public static final int LUNAR = 133;
	public static final int STAR = 134;
	public static final int AKYUU = 138;
	public static final int TOYOHIME = 142;
	public static final int YORIHIME = 143;
	public static final int REISEN = 144;
	public static final int KASEN = 146;
	public static final int KOSUZU = 150;
	public static final int RENKO = 192;
	public static final int MAERIBERRY = 193;
	public static final int OTHER = 254;

	public THSpellCard()
	{
		setNeedLevel(1);
	}

	public void init(Level worldObj, EntitySpellCard entitySpellCard, LivingEntity living_user, LivingEntity living_target, int spLevel)
	{
		world = worldObj;
		card = entitySpellCard;
		user = living_user;
		target = living_target;
		time = 0;
		level = spLevel;
	}

	/** スペルカード共通の毎tickの処理 */
	public boolean onUpdate()
	{
		cardPosX = card.getX();
		cardPosY = card.getY();
		cardPosZ = card.getZ();

		userPosX = user.getX();
		userPosY = THShotLib.getPosYFromEye(user);
		userPosZ = user.getZ();

		targetPosX = target.getX();
		targetPosY = target.getY() + target.getBbHeight() / 2.0D;
		targetPosZ = target.getZ();

		spellcard_main();

		time++;
		return true;
	}

	/** スペルカードの個別の処理 */
	public void spellcard_main()
	{
	}

	/** 時間停止中にのみ実行される */
	public void specialProcessInTimeStop()
	{
		if(canMoveInTimeStop())
		{
			setEndTime(getEndTime() + 1);
		}
	}

	/* ==================== 弾幕ライブラリへの中継 ==================== */

	public Vec3 pos(double x, double y, double z)
	{
		return new Vec3(x, y, z);
	}

	public Vec3 pos_User()
	{
		return pos(userPosX, userPosY, userPosZ);
	}

	public Vec3 pos_User(Vec3 angle, double distance)
	{
		angle = THShotLib.getVectorNomalize(angle);
		return pos(userPosX + angle.x * distance, userPosY + angle.y, userPosZ + angle.z * distance);
	}

	public Vec3 pos_Card()
	{
		return new Vec3(cardPosX, cardPosY, cardPosZ);
	}

	public Vec3 pos_Target()
	{
		return THShotLib.pos_Living(target);
	}

	public Vec3 angle(double x, double y, double z)
	{
		return THShotLib.angle(x, y, z);
	}

	public Vec3 angle(float yaw, float pitch)
	{
		return THShotLib.angle(yaw, pitch);
	}

	public Vec3 angle_UserToTarget()
	{
		return THShotLib.angle_ToPos(pos_User(), pos_Target());
	}

	public static Vec3 rotate_Default()
	{
		return THShotLib.rotate_Default();
	}

	public Vec3 gravity_Zero()
	{
		return THShotLib.gravity_Zero();
	}

	public Vec3 gravity_Default()
	{
		return THShotLib.gravity_Default();
	}

	public Vec3 getVecFromAngle(float yaw, float pitch)
	{
		return THShotLib.getVecFromAngle(yaw, pitch);
	}

	public static ShotData shot(int form, int color, float size, float damage, int delay, int end, int special)
	{
		return ShotData.shot(form, color, size, damage, delay, end, special);
	}

	public static ShotData shot(int form, int color, float size, float damage, int delay, int end)
	{
		return shot(form, color, size, damage, delay, end, 0);
	}

	public static ShotData shot(int form, int color, float size, float damage)
	{
		return shot(form, color, size, damage, 0, 80);
	}

	public static ShotData shot(int form, int color)
	{
		return shot(form, color, 0, 80, 0);
	}

	public static ShotData shot(int form, int color, int delay)
	{
		return shot(form, color, delay);
	}

	public static ShotData shot(int form, int color, int delay, int end)
	{
		return shot(form, color, delay, end, 0);
	}

	public static ShotData shot(int form, int color, int delay, int end, int special)
	{
		float size = SIZE[form];
		float damage = DAMAGE[form];
		return shot(form, color, size, damage, delay, end, special);
	}

	/* ==================== getter/setter ==================== */

	public int getSpellCardNumber()
	{
		return -1;
	}

	public String getSpellCardName()
	{
		return spellcardName;
	}

	public int getNeedLevel()
	{
		return needLevel;
	}

	public int getRemoveTime()
	{
		return remove;
	}

	public int getEndTime()
	{
		return end;
	}

	public int getOriginalUserName()
	{
		return originalUserName;
	}

	public int getSpellCardCircleColor()
	{
		return RED;
	}

	public void setSpellCardName(String name)
	{
		spellcardName = name;
	}

	public void setNeedLevel(int level)
	{
		needLevel = level;
	}

	public void setRemoveTime(int setTime)
	{
		remove = setTime;
	}

	public void setEndTime(int setTime)
	{
		end = setTime;
	}

	public void setOriginalUserName(int originalUserNameID)
	{
		originalUserName = originalUserNameID;
	}

	public void setIconName(String name)
	{
		iconName = name;
	}

	public String getIconName()
	{
		return iconName;
	}

	public boolean canMoveInTimeStop()
	{
		return false;
	}
}
