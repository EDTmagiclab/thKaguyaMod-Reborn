package thKaguyaMod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;

/**
 * 霊夢のホーミングアミュレット / 拡散アミュレット
 *
 * 原版 ItemHomingAmulet は 1 つの Item を damage 値（0=homing / 1=diffusion）で
 * 使い分けていたが、1.20.1 は metadata 変体が廃止されたため 2 登録項に分割し、
 * このクラスを type 付きコンストラクタで共用する（`homing_amulet` / `diffusion_amulet`）。
 *
 * - type 0（ホーミング）：FORM_AMULET の追尾弾を扇状に発射
 *     潜行（低速）：speed 0.7 / 2 発 / 扇 20 度
 *     通常（高速）：speed 0.7 / 5 発 / 扇 100 度
 *   special = HOMING01（EntityTHShot.homing(4.0F) で追尾）
 * - type 1（拡散）：FORM_AMULET の弾を 2 発、special = DIFFUSION01
 *     消滅時に 20/16/8 発の扇状弾へ分裂する
 *
 * 1.20.1 迁移要点：
 * - player.getLookVec() → player.getLookAngle()
 * - world.playSoundAtEntity("random.bow") → level.playSound(SoundEvents.ARROW_SHOOT)
 * - setHasSubtypes(true)/getSubItems は不要（2 登録項に分割したため）
 */
public class ItemHomingAmulet extends Item
{
	/** ホーミングアミュレット */
	public static final int TYPE_HOMING = 0;
	/** 拡散アミュレット */
	public static final int TYPE_DIFFUSION = 1;

	private final int amuletType;

	public ItemHomingAmulet(int amuletType)
	{
		super(new Item.Properties().stacksTo(64));
		this.amuletType = amuletType;
	}

	public int getAmuletType()
	{
		return amuletType;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		if(amuletType == TYPE_HOMING)
		{
			if(player.isShiftKeyDown())
			{
				// 低速モード：2 発・狭い扇
				ShotData shot = ShotData.shot(DanmakuConstants.FORM_AMULET, DanmakuConstants.RED,
						1.0F, 8.0F, 0, 90, DanmakuConstants.HOMING01);
				THShotLib.createWideShot(player, player, THShotLib.pos_Living(player),
						player.getLookAngle(), 0.7D, 0.7D, 0.7D, THShotLib.gravity_Zero(),
						shot, 2, 20F, 0.5D, 0F);
			}
			else
			{
				// 高速モード：5 発・広い扇
				ShotData shot = ShotData.shot(DanmakuConstants.FORM_AMULET, DanmakuConstants.RED,
						0.4F, 5.0F, 0, 90, DanmakuConstants.HOMING01);
				THShotLib.createWideShot(player, player, THShotLib.pos_Living(player),
						player.getLookAngle(), 0.7D, 0.7D, 0.7D, THShotLib.gravity_Zero(),
						shot, 5, 100F, 0.5D, 0F);
			}
		}
		else
		{
			ShotData shot = ShotData.shot(DanmakuConstants.FORM_AMULET, DanmakuConstants.BLUE,
					1.0F, 8.0F, 0, 16, DanmakuConstants.DIFFUSION01);
			THShotLib.createWideShot(player, player, THShotLib.pos_Living(player),
					player.getLookAngle(), 0.5D, 0.0D, -0.03D, THShotLib.gravity_Zero(),
					shot, 2, 60F, 0.5D, 0F);
		}

		level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
				0.5F, 0.4F / (level.random.nextFloat() * 4F + 0.8F));

		if(!player.isCreative())
		{
			itemStack.shrink(1);
		}

		return InteractionResultHolder.success(itemStack);
	}
}
