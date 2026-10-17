package thKaguyaMod.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
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
	 * 原版 ItemTHLaser.onItemRightClick の移植。
	 * NBT の Number / DanmakuForm / Speed / Color を読み、形状ごとの THShotLib ヘルパーで発射する。
	 */
	public void shootLaser(ItemStack itemStack, Player player)
	{
		net.minecraft.util.RandomSource rand = player.level().random;
		int color = rand.nextInt(7);
		float length = LENGTH[laserType];
		int way = 1;
		double shotSpeed = SPEED[laserType];
		int danmakuForm = 0;

		CompoundTag nbt = itemStack.getTag();
		if(nbt != null)
		{
			way = nbt.getShort("Number");
			danmakuForm = nbt.getByte("DanmakuForm");
			shotSpeed = SPEED[laserType] * (1.0D + (double)nbt.getByte("Speed") * 0.03D);

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
	public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
	{
		super.appendHoverText(itemStack, level, tooltip, flag);

		short shotNum = 1;
		byte form = 0;
		float shotSpeed = SPEED[laserType];
		int color = 8;

		CompoundTag nbt = itemStack.getTag();
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
