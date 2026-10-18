package thKaguyaMod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thKaguyaMod.THKaguyaAdvancements;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.gui.ContainerMiracleMallet;
import thKaguyaMod.init.THKaguyaConfig;
import org.jetbrains.annotations.Nullable;

/**
 * 打ち出の小槌（万宝槌，1.20.1 移植版）
 *
 * 原版 {@code thKaguyaMod.item.ItemUchide}（201 行）的可玩部分：
 *  - 右键：若饱食度 &gt; 0，把**自己发射的**、20 格内的所有 {@link EntityTHShot} 体积各 +0.5，
 *    播放「random.bow」音效（音量 0.5，音高 0.4/(rand*4+0.8)），挥臂，并给物品 +30 耐久损耗；
 *  - 物品栏 tick：每 tick 恢复 1 点耐久（原版 {@code onUpdate} 里 {@code setItemDamage(getItemDamage() - 1)}），
 *    所以它是「用一次扣 30、随时间慢慢自愈」的消耗型工具；
 *  - 最大堆叠 1、耐久 100。
 *
 * 原版的 GUI（{@code guiMiracleMalletID} → {@code ContainerMiracleMallet}）在原作者代码里
 * 是**被注释掉的死代码**，物品从不打开它；本移植版把它作为「实验的機能」加以复原：
 * 只有 {@link THKaguyaConfig#enableMalletGui} 打开时，潜行 + 右键才会打开界面
 * （默认关闭，可在设置里经由 {@code ConfigScreenHandler} 的万宝槌开关启用）。
 */
public class ItemMiracleMallet extends Item
{
    /** 原版 maxDamage = 100 */
    public static final int MAX_DAMAGE = 100;
    /** 每次使用增加 30 点损耗 */
    public static final int USE_COST = 30;
    /** 生效半径：原版 20 格 */
    private static final double RANGE = 20.0D;
    /** 每次使用让弹幕体积 +0.5 */
    private static final float SIZE_BONUS = 0.5F;

    public ItemMiracleMallet()
    {
        super(new Item.Properties().stacksTo(1).durability(MAX_DAMAGE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        // 実験的機能：潜行 + 右クリックで、原版では神隠しにされた万宝槌の界面を開く。
        // 設定が無効なら何もしない（耐久も消費しない）。
        if (player.isShiftKeyDown())
        {
            if (!THKaguyaConfig.enableMalletGui)
            {
                return InteractionResultHolder.pass(stack);
            }

            if (!level.isClientSide)
            {
                // 有効化済みセーブで実際に界面を開いた時点でも実績を付与する
                // （設定画面で後から有効化した場合、再ログインを待たずに取れるように）
                if (player instanceof ServerPlayer serverPlayer)
                {
                    THKaguyaAdvancements.awardMiracleMalletGui(serverPlayer);
                }

                player.openMenu(new SimpleMenuProvider(
                        (windowId, playerInventory, p) -> new ContainerMiracleMallet(windowId, playerInventory),
                        Component.translatable("gui.thkaguya.miracle_mallet.title")));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // 原版：空腹ゲージが 0 だと使えない
        if (player.getFoodData().getFoodLevel() <= 0)
        {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide)
        {
            AABB area = player.getBoundingBox().inflate(RANGE, RANGE, RANGE);
            for (EntityTHShot shot : level.getEntitiesOfClass(EntityTHShot.class, area))
            {
                if (shot.user == player)
                {
                    shot.setShotSize(shot.getShotSize() + SIZE_BONUS);
                }
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                    0.5F, 0.4F / (level.random.nextFloat() * 4.0F + 0.8F));

            // 原版是 setItemDamage(getItemDamage() + 30)，不触发「耐久耗尽消失」，这里保持同样语义
            stack.setDamageValue(Math.min(stack.getDamageValue() + USE_COST, MAX_DAMAGE));
        }

        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected)
    {
        // 原版 onUpdate：每 tick 自愈 1 点
        if (stack.getDamageValue() > 0)
        {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack)
    {
        return stack.getDamageValue() > 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("tooltip.thkaguya.miracle_mallet.use")
                .withStyle(ChatFormatting.GRAY));

        if (THKaguyaConfig.enableMalletGui)
        {
            tooltip.add(Component.translatable("tooltip.thkaguya.miracle_mallet.gui_on")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        else
        {
            tooltip.add(Component.translatable("tooltip.thkaguya.miracle_mallet.gui_off")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
