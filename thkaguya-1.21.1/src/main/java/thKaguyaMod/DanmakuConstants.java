package thKaguyaMod;

/**
 * 弾幕に関する定数定義（1.20.1 移植版）
 * 原版 1.7.10 的弹幕常量，全部为纯常量，无 Minecraft API 依赖，可直接迁移。
 * 阶段 3a：将原 THShotLib 的 SIZE/DAMAGE 数组上移到本类，消除 ShotData 对 THShotLib 的循环依赖。
 */
public class DanmakuConstants {

	/**
	 * 難易度（弾幕難易度）
	 */
	/**	弾幕難易度　EASY	*/
	public static final int EASY		= 1;
	/**	弾幕難易度　NORMAL	*/
	public static final int NORMAL		= 2;
	/**	弾幕難易度　HARD	*/
	public static final int HARD		= 3;
	/**	弾幕難易度　LUNATIC	*/
	public static final int LUNATIC		= 4;

	/**
	 * 弾の色		Shot Colors
	 */
	public static final int RED			= 0x0000;
	public static final int BLUE		= 0x0001;
	public static final int GREEN		= 0x0002;
	public static final int YELLOW		= 0x0003;
	public static final int PURPLE		= 0x0004;
	public static final int AQUA		= 0x0005;
	public static final int ORANGE		= 0x0006;
	public static final int WHITE		= 0x0007;

	public static final int RANDOM		= 0x0008;
	public static final int RAINBOW		= 0x0009;

	public static final int MASK_COLOR	= 0xF000;

	public static final int 赤弾			= RED;
	public static final int 青弾			= BLUE;
	public static final int 緑弾			= GREEN;
	public static final int 黄弾			= YELLOW;
	public static final int 紫弾			= PURPLE;
	public static final int 水弾			= AQUA;
	public static final int 橙弾			= ORANGE;
	public static final int 白弾			= WHITE;

	public static final int 乱弾			= RANDOM;
	public static final int 虹弾			= RAINBOW;

	/** 16 色染料 → 8 色弹幕色 的映射（原版 ItemTHShot.sellectbleColor，阶段 5 工作台复用） */
	public static final int[] SELECTABLE_COLOR = {7, 0, 2, 0, 1, 4, 5, 7, 7, 4, 2, 3, 5, 4, 6, 7};

	/**
	 * 弾の形状		Shot Forms
	 */
	public static final int MASK_FORM		= 0x0FC0;

	public static final int FORM_SMALL		=  0;
	public static final int FORM_TINY		=  1;
	public static final int FORM_MEDIUM	=  2;
	public static final int FORM_PEARL		=  3;
	public static final int FORM_CIRCLE	=  4;
	public static final int FORM_LIGHT		=  5;
	public static final int FORM_SCALE		=  6;
	public static final int FORM_BUTTERFLY	=  7;
	public static final int FORM_SMALLSTAR	=  8;
	public static final int FORM_STAR		=  9;
	public static final int FORM_RICE		= 10;
	public static final int FORM_CRYSTAL	= 11;
	public static final int FORM_HEART		= 12;
	public static final int FORM_KUNAI		= 13;
	public static final int FORM_TALISMAN	= 14;
	public static final int FORM_BIGLIGHT	= 15;
	public static final int FORM_OVAL		= 16;
	public static final int FORM_FAMILIAR	= 17;
	public static final int FORM_ARROW		= 18;
	public static final int FORM_PHANTOM	= 26;
	public static final int FORM_AMULET	= 27;
	public static final int FORM_KNIFE		= 28;
	public static final int FORM_WIND		= 29;
	public static final int FORM_BIG		= 30;
	public static final int FORM_KISHITU	= 31;

	public static final int 小弾				= FORM_SMALL;
	public static final int 粒弾				= FORM_TINY;
	public static final int 中弾				= FORM_MEDIUM;
	public static final int 真珠弾			= FORM_PEARL;
	public static final int 輪弾				= FORM_CIRCLE;
	public static final int 光弾				= FORM_LIGHT;
	public static final int 鱗弾				= FORM_SCALE;
	public static final int 蝶弾				= FORM_BUTTERFLY;
	public static final int 小星弾			= FORM_SMALLSTAR;
	public static final int 星弾				= FORM_STAR;
	public static final int 米弾				= FORM_RICE;
	public static final int 結晶弾			= FORM_CRYSTAL;
	public static final int ハート弾			= FORM_HEART;
	public static final int クナイ弾			= FORM_KUNAI;
	public static final int 札弾				= FORM_TALISMAN;
	public static final int 大光弾			= FORM_BIGLIGHT;
	public static final int 楕円弾			= FORM_OVAL;
	public static final int 使い魔			= FORM_FAMILIAR;
	public static final int 矢弾				= FORM_ARROW;
	public static final int 幽霊弾			= FORM_PHANTOM;
	public static final int アミュレット弾	= FORM_AMULET;
	public static final int ナイフ弾			= FORM_KNIFE;
	public static final int 風弾				= FORM_WIND;
	public static final int 大弾				= FORM_BIG;
	public static final int 気質弾			= FORM_KISHITU;

	/**
	 * レーザーの形状
	 */
	public static final int LASER_NORMAL = 0;
	public static final int LASER_GUNGNIR = 1;
	public static final int LASER_LAEVATAIN = 2;

	/**
	 * 特殊弾の定義		Special Shot Definition
	 */
	public static final int HOMING01		=  10;
	public static final int HOMING02		=  11;
	public static final int DIFFUSION01	=  20;
	public static final int FALL01			=  30;
	public static final int BOUND01			=  40;
	public static final int BOUND02			=  41;
	public static final int BOUND03			=  42;
	public static final int BOUND04			=  43;
	public static final int BOUNDFALL01	=  50;
	public static final int FIRE			=  60;
	public static final int EXPLOSION01	=  70;
	public static final int EXPLOSION02	=  71;
	public static final int BRAKE01			= 150;

	public static final int WIND01			= 180;

	public static final int GUNGNIR			= 285;

	/** 気質弾特殊動作ID（原版 THSC_Zenzinrui_no_Hisouten.SPECIAL_HISOUTEN01，阶段 3d-4 上移避免循环依赖） */
	public static final int SPECIAL_HISOUTEN01 = 700;

	/** エイジャの赤石のレーザー特殊動作ID（原版 ItemAjaRedStone.SPECIAL_AJA_REDSTONE，阶段 3d-7 上移避免依赖未迁移物品类） */
	public static final int SPECIAL_AJA_REDSTONE = 351;

	/** 幽々子のパラソルの耐久値（原版 ItemYuukaParasol.PARASOL_MAX_DAMAGE，EntityYuukaParasol 用） */
	public static final int PARASOL_MAX_DAMAGE = 60;

	/**
	 * 標準の重力値		Default Gravity
	 */
	public static final double GRAVITY_DEFAULT = -0.03D;

	/**
	 * 形状ごとの弾のダメージを定義		Shot Damage by Form
	 * （原 THShotLib.DAMAGE，上移到本类消除循环依赖）
	 */
	public static final float DAMAGE[] = {
			2.4F, 1.0F, 2.8F, 5.0F, 2.0F, 3.4F, 1.6F, 2.8F,
			1.6F, 3.0F, 1.2F, 1.8F, 3.2F, 2.6F, 1.0F, 6.8F,
			3.0F, 1.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
			0.0F, 0.0F, 0.0F, 5.0F, 2.6F, 4.0F, 5.0F, 5.0F};

	/**
	 * 形状ごとの弾の大きさを定義		Shot Size by Form
	 * （原 THShotLib.SIZE，上移到本类消除循环依赖）
	 */
	public static final float SIZE[] = {
			0.30F, 0.15F, 0.50F, 0.30F, 0.30F, 0.30F, 0.15F, 0.30F,
			0.25F, 0.50F, 0.15F, 0.15F, 0.50F, 0.15F, 0.15F, 0.60F,
			0.50F, 1.00F, 0.15F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F,
			0.00F, 0.00F, 0.00F, 0.40F, 0.30F, 1.00F, 0.90F, 0.90F};
}
