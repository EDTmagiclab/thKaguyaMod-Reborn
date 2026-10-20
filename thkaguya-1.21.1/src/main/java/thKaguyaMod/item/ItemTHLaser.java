package thKaguyaMod.item;

import thKaguyaMod.THStackData;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.LaserData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaConfig;

/**
 * レーザーアイテム（1.20.1 移植版）
 *
 * 原版 ItemTHLaser は 1 個の Item + damage 値（0～2）で 3 種を表していた。
 * 1.20.1 では metadata が廃止されたため 3 種を独立 Item に分割し、このクラスを type 付きで共有する。
 *
 * - 0 = middle（中距離 / 長さ 5）
 * - 1 = short （短距離 / 長さ 2）
 * - 2 = long  （長距離 / 長さ 8）
 * 右键试射は原版 ItemTHLaser.onItemRightClick の移植。
 */
public class ItemTHLaser extends Item
{
	public static final int MIDDLE	= 0;
	public static final int SHORT	= 1;
	public static final int LONG	= 2;

	/** 原版 laserNames[]（登録名の suffix） */
	public static final String[] LASER_NAMES = { "middle", "short", "long" };

	/** 原版 ItemTHLaser.speed[] の先頭 3 要素（middle/short/long） */
	public static final float[] SPEED = { 0.6F, 0.6F, 0.5F };

	/** レーザーの長さ（原版 onItemRightClick 内の length） */
	public static final float[] LENGTH = { 5.0F, 2.0F, 8.0F };

	/** レーザーの当たり判定幅（原版 LaserData.laser(color, 0.1F, length, damage)） */
	public static final float WIDTH = 0.1F;

	/** 原版が LaserData に渡していたダメージ（表示は /2 で 2.5） */
	public static final float DAMAGE = 5.0F;

	/** 弹幕形状名（原版 danmakuForm[]、lang キー thKaguya.danmakuForm.N 用） */
	public static final String[] DANMAKU_FORM =
	{
		"Point", "Random", "Sector", "Around", "Sphere", "undefined"
	};

	private final int laserType;

	public ItemTHLaser(int laserType)
	{
		super(new Item.Properties().stacksTo(64));
		this.laserType = laserType;
	}

	public int getLaserType()
	{
		return laserType;
	}

	/** 原版のレーザー速度 */
	public float getLaserSpeed()
	{
		return SPEED[laserType];
	}

	/** 原版のレーザー長 */
	public float getLaserLength()
	{
		return LENGTH[laserType];
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		if(!level.isClientSide)
		{
			shootLaser(itemStack, player);

			if(!player.isCreative())
			{
				itemStack.shrink(1);
			}
		}

		return InteractionResultHolder.success(itemStack);
	}

	/**
	 * 1.20.1 の DyeColor（WHITE=0 … BLACK=15）→ 1.7.10 染料 metadata → 8 色レーザー色。
	 * 原版は「右隣のスロットに染料があればその色で撃つ」なので、同じ並びの表を用意する。
	 */
	private static final int[] DYE_COLOR_TO_LASER =
	{
		7,	// WHITE      -> meta 15 -> 7
		6,	// ORANGE     -> meta 14 -> 6
		4,	// MAGENTA    -> meta 13 -> 4
		5,	// LIGHT_BLUE -> meta 12 -> 5
		3,	// YELLOW     -> meta 11 -> 3
		2,	// LIME       -> meta 10 -> 2
		4,	// PINK       -> meta  9 -> 4
		7,	// GRAY       -> meta  8 -> 7
		7,	// LIGHT_GRAY -> meta  7 -> 7
		5,	// CYAN       -> meta  6 -> 5
		4,	// PURPLE     -> meta  5 -> 4
		1,	// BLUE       -> meta  4 -> 1
		0,	// BROWN      -> meta  3 -> 0
		2,	// GREEN      -> meta  2 -> 2
		0,	// RED        -> meta  1 -> 0
		7	// BLACK      -> meta  0 -> 7
	};

	/**
	 * 原版「右隣のスロットの染料で色を決める」処理。
	 *
	 * @return 8 色レーザー色。隣が染料でなければ -1（呼び出し側は乱数のまま）
	 */
	public static int adjacentDyeColor(Player player)
	{
		int selected = player.getInventory().selected;
		if(selected >= 8)
		{
			return -1;
		}
		ItemStack beside = player.getInventory().items.get(selected + 1);
		if(beside.getItem() instanceof DyeItem dye)
		{
			int id = dye.getDyeColor().getId();
			if(id >= 0 && id < DYE_COLOR_TO_LASER.length)
			{
				return DYE_COLOR_TO_LASER[id];
			}
		}
		return -1;
	}

	/**
	 * 原版 ItemTHLaser.onItemRightClick の移植。
	 * NBT の Number / DanmakuForm / Speed / Color を読み、形状ごとの THShotLib ヘルパーで発射する。
	 */
	public void shootLaser(ItemStack itemStack, Player player)
	{
		shootLaser(itemStack, player, laserType);
	}

	/**
	 * レーザーの種類を明示して発射する。激光の素（damage で種類が変わる）用。
	 *
	 * @param type {@link #MIDDLE} / {@link #SHORT} / {@link #LONG}
	 */
	public void shootLaser(ItemStack itemStack, Player player, int type)
	{
		net.minecraft.util.RandomSource rand = player.level().random;
		int color = rand.nextInt(7);
		float length = LENGTH[type];
		int way = 1;
		double shotSpeed = SPEED[type];
		int danmakuForm = 0;

		// 原版：右隣の染料 > NBT の Color の順で上書きされる（染料は NBT より先に読む）
		int dyeColor = adjacentDyeColor(player);
		if(dyeColor >= 0)
		{
			color = dyeColor;
		}

		CompoundTag nbt = THStackData.get(itemStack);
		if(nbt != null)
		{
			way = nbt.getShort("Number");
			danmakuForm = nbt.getByte("DanmakuForm");
			shotSpeed = SPEED[type] * (1.0D + (double)nbt.getByte("Speed") * 0.03D);

			int setColor = nbt.getByte("Color");
			if(setColor != 9)
			{
				color = setColor;
			}
		}

		if(way > THKaguyaConfig.laserMaxNumber)
		{
			way = THKaguyaConfig.laserMaxNumber;
		}

		float wide;
		Vec3 pos = THShotLib.pos_Living(player);
		Vec3 look = THShotLib.angle(player.getYRot(), player.getXRot());
		LaserData laser = LaserData.laser(color, WIDTH, length, DAMAGE);

		switch(danmakuForm)
		{
			case 0:// 一点
			{
				double shotSpeed2 = shotSpeed;

				for(int i = 1; i <= way; i++)
				{
					THShotLib.createLaserA(player, pos, look, shotSpeed2, laser);
					shotSpeed2 = shotSpeed / (double)way * (double)i;
				}
				break;
			}
			case 1:// 前方ランダム
				wide = 30F;
				if(player.isShiftKeyDown())
				{
					wide *= 0.5F;
				}
				THShotLib.createRandomRingLaser(player, player, pos, look,
						shotSpeed, shotSpeed, 0.0D, THShotLib.gravity_Zero(),
						laser, way, 0.2D, wide);
				break;
			case 2:// 前方 n-way
				wide = way * 5F;
				if(player.isShiftKeyDown())
				{
					wide *= 0.5F;
				}
				THShotLib.createWideLaserA(player, player, pos, look,
						shotSpeed, shotSpeed, 0.0D, THShotLib.gravity_Zero(),
						laser, way, wide, 0.1D);
				break;
			case 3:// 全方位
				THShotLib.createCircleLaserA(player, player, pos, look,
						shotSpeed, shotSpeed, 0.0D, THShotLib.gravity_Zero(),
						laser, way, 0.1D);
				break;
			case 4:// 球状
				THShotLib.createSphereLaserA(player, player, pos, look,
						shotSpeed, shotSpeed, 0.0D, THShotLib.gravity_Zero(),
						laser, way, 0.2D, 0F);
				break;
			case 5:// リング
				wide = 15F;
				if(player.isShiftKeyDown())
				{
					wide *= 0.5F;
				}
				THShotLib.createRingLaserA(player, player, pos, look,
						shotSpeed, shotSpeed, 0.0D, THShotLib.gravity_Zero(),
						laser, way, 0.1D, wide, rand.nextFloat() * 360F);
				break;
			default:
				break;
		}
	}

	@Override
	public void appendHoverText(ItemStack itemStack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		super.appendHoverText(itemStack, context, tooltip, flag);

		short shotNum = 1;
		byte form = 0;
		float shotSpeed = SPEED[laserType];
		int color = 8;

		CompoundTag nbt = THStackData.get(itemStack);
		if(nbt != null)
		{
			shotNum = nbt.getShort("Number");
			form = nbt.getByte("DanmakuForm");
			shotSpeed = (float)(SPEED[laserType] * (1.0F + (float)nbt.getByte("Speed") * 0.03F));
			color = nbt.getByte("Color");
		}

		if(form >= DANMAKU_FORM.length)
		{
			form = (byte)(DANMAKU_FORM.length - 1);
		}

		tooltip.add(Component.translatable("danmakuCrafting.damage").append(" : 2.5"));
		tooltip.add(Component.translatable("danmakuCrafting.number").append(" : ").append(String.valueOf(shotNum)));
		tooltip.add(Component.translatable("danmakuCrafting.form").append(" : ")
				.append(Component.translatable("thKaguya.danmakuForm." + form)));
		tooltip.add(Component.translatable("danmakuCrafting.speed").append(" : ")
				.append(String.format(java.util.Locale.ROOT, "%.2f", shotSpeed)));
		tooltip.add(Component.translatable("danmakuCrafting.color").append(" : ")
				.append(Component.translatable("thKaguya.color." + color)));
	}
}
