package thKaguyaMod.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.block.BlockDanmakuCraftingTable;
import thKaguyaMod.tileentity.TileEntityDivineSpirit;

import java.util.function.Supplier;

/**
 * ブロックの登録クラス（1.20.1 移植版）
 * 原版 2 个方块 + 1 个 TileEntity，改为 DeferredRegister 占位注册。
 * 阶段 1 仅完成注册；GUI（阶段 5）与渲染（阶段 4）后续接入。
 */
public class THKaguyaBlocks
{
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, THKaguyaCore.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, THKaguyaCore.MODID);

    /* ==================== 方块 ==================== */
    /** 弾幕作業台（弹） */
    public static final RegistryObject<Block> DANMAKU_CRAFTING_TABLE = BLOCKS.register("danmaku_crafting_table",
            () -> new BlockDanmakuCraftingTable(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5F, 1.0F)
                    .sound(SoundType.WOOD)
                    .lightLevel(s -> 15), false));

    /** 弾幕作業台（激光）— 原版 metadata 1 变体，1.20.1 拆分为独立方块 */
    public static final RegistryObject<Block> LASER_CRAFTING_TABLE = BLOCKS.register("laser_crafting_table",
            () -> new BlockDanmakuCraftingTable(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5F, 1.0F)
                    .sound(SoundType.WOOD)
                    .lightLevel(s -> 15), true));

    /** 小神霊 */
    public static final RegistryObject<Block> DIVINE_SPIRIT = BLOCKS.register("divine_spirit",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.1F, 2000.0F)
                    .sound(SoundType.SNOW)
                    .lightLevel(s -> 8)
                    .noOcclusion()));

    /* ==================== 方块实体 ==================== */
    /** 小神霊 TileEntity（原版方法体被注释，1.20.1 仅保留空实现占位） */
    public static final RegistryObject<BlockEntityType<TileEntityDivineSpirit>> DIVINE_SPIRIT_ENTITY =
            BLOCK_ENTITIES.register("divine_spirit",
                    () -> BlockEntityType.Builder.of(TileEntityDivineSpirit::new, DIVINE_SPIRIT.get()).build(null));

    /* ==================== 方块物品 ==================== */
    public static final RegistryObject<Item> DANMAKU_CRAFTING_TABLE_ITEM = registerBlockItem(
            "danmaku_crafting_table", DANMAKU_CRAFTING_TABLE);
    public static final RegistryObject<Item> LASER_CRAFTING_TABLE_ITEM = registerBlockItem(
            "laser_crafting_table", LASER_CRAFTING_TABLE);
    public static final RegistryObject<Item> DIVINE_SPIRIT_ITEM = registerBlockItem(
            "divine_spirit", DIVINE_SPIRIT);

    private static RegistryObject<Item> registerBlockItem(String name, Supplier<Block> block)
    {
        return THKaguyaItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
