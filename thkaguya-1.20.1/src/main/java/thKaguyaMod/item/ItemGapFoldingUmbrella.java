package thKaguyaMod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thKaguyaMod.entity.item.EntitySukima;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 隙間の折りたたみ傘（隙间的折叠伞）
 *
 * 原版 ItemGapFoldingUmbrella：
 * - 右クリック：目の前 2 格の位置に隙間（EntitySukima）を開く。設置位置が「通常の立方体ブロック」なら失敗
 *     隙間の色番号 = 32 + NBT "GapColor"（0〜15）。同色の隙間同士でしか移動できない
 * - スニーク + 右クリック：NBT "GapColor" を 0→1→…→15→0 と巡回（色の切り替え）
 * - 耐久 30。使い切ると「隙間（gap）」アイテムに戻る（NBT "OriginalDamage" を引き継ぐ）
 * - 常に発光（hasEffect → isFoil）
 * - 開くときの音：useDefaultGapSE なら mob.endermen.portal、でなければ sukimaWarpSE
 *
 * 1.7.10 -> 1.20.1:
 *   onItemRightClick                       -> use
 *   player.isSneaking()                    -> player.isShiftKeyDown()
 *   world.isBlockNormalCubeDefault(x,y,z)  -> BlockState.isRedstoneConductor（ItemSukima 移植版と同じ扱い）
 *   NBTTagCompound.getByte / setByte       -> CompoundTag（putByte / getByte）
 *   damageItem(1, player)                  -> setDamageValue(damage + 1)
 *     ※hurtAndBreak は使わない：耐久切れの瞬間にスタックが消滅してしまい、
 *       原版の「gap アイテムに置き換わる」挙動を再現できないため。
 *       ItemCursedDecoyDoll 移植版と同じ手動減算方式に揃えている。
 *
 * 既知の移植差分（阶段 7 打磨送り）：
 *   原版は base レイヤー（無色）と ribbon レイヤー（GapColor で着色）の 2 層描画。
 *   1.20.1 資源には ribbon 相当の贴图が未収録で、モデルも layer0 一枚のみ
 *   （textures/item/gap_folding_umbrella.png は原版 GapFoldingUmbrella_base.png と同一）。
 *   色の見た目を再現するには ribbon 贴图 + layer1 モデル + ItemColor 登録が必要
 *   （remorse_rod の贴图切替と同じく客户端动态贴图类打磨项目）。
 */
public class ItemGapFoldingUmbrella extends Item
{
	public ItemGapFoldingUmbrella()
	{
		// 原版 setMaxDamage(30) / maxStackSize = 1
		super(new Item.Properties().stacksTo(1).durability(30));
	}

	/** 右クリックを押した瞬間の処理 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		ItemStack itemStack = player.getItemInHand(hand);

		double setX = player.getX() - Math.sin(player.getYRot() / 180F * (float)Math.PI) * Math.cos(player.getXRot() / 180F * (float)Math.PI) * 2.0D;
		double setY = player.getY() - Math.sin(player.getXRot() / 180F * (float)Math.PI) * 2.0D + player.getEyeHeight() / 2.0D;
		double setZ = player.getZ() + Math.cos(player.getYRot() / 180F * (float)Math.PI) * Math.cos(player.getXRot() / 180F * (float)Math.PI) * 2.0D;
		int color = 0;

		// スニーク時は色番号を１つ進める
		if(player.isShiftKeyDown())
		{
			CompoundTag nbt = itemStack.getTag();

			if(nbt != null)
			{
				color = nbt.getByte("GapColor");
				color++;

				if(color >= 16)
				{
					color = 0;
				}
			}
			else
			{
				color = 0;
				nbt = itemStack.getOrCreateTag();
			}
			nbt.putByte("GapColor", (byte)color);

			return InteractionResultHolder.success(itemStack);
		}

		// 通常の立方体ブロックが無い場所にだけ隙間を開く
		BlockPos setPos = new BlockPos((int)setX, (int)setY, (int)setZ);

		if(!level.getBlockState(setPos).isRedstoneConductor(level, setPos))
		{
			if(THKaguyaConfig.useDefaultGapSE)
			{
				// 原版 "mob.endermen.portal"
				level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
						0.5F, 0.4F / (player.getRandom().nextFloat() * 4F + 0.8F));
			}
			else
			{
				// 自定义音效：原版 jar 无 sukima.ogg（上游遗留），回退同款原版音效
				level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
			}

			CompoundTag nbt = itemStack.getTag();

			if(nbt != null)
			{
				color = nbt.getByte("GapColor");

				if(color >= 16)
				{
					color = 0;
				}
			}
			else
			{
				color = 0;
				itemStack.getOrCreateTag().putByte("GapColor", (byte)color);
			}

			// 隙間の色番号は 32 番台（32〜47）を使う
			EntitySukima entitySukima = new EntitySukima(level, player, setX, setY, setZ, 32 + color);

			if(!level.isClientSide)
			{
				level.addFreshEntity(entitySukima);
			}

			// 原版 damageItem(1, player) 相当
			itemStack.setDamageValue(itemStack.getDamageValue() + 1);

			// 耐久が尽きたら「隙間」アイテムに戻る
			if(itemStack.getDamageValue() >= itemStack.getMaxDamage())
			{
				int originalDamage = 0;
				CompoundTag tag = itemStack.getTag();

				if(tag != null)
				{
					originalDamage = tag.getInt("OriginalDamage");
				}

				ItemStack gapStack = new ItemStack(THKaguyaItems.GAP.get());
				gapStack.setDamageValue(originalDamage);
				player.setItemInHand(hand, gapStack);

				return InteractionResultHolder.success(gapStack);
			}
		}

		return InteractionResultHolder.success(itemStack);
	}

	/** 常に発光させる */
	@Override
	public boolean isFoil(ItemStack itemStack)
	{
		return true;
	}
}
