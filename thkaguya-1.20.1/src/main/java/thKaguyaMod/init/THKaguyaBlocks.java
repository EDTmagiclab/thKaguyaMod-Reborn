package thKaguyaMod.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
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
import thKaguyaMod.block.BlockDivineSpirit;
import thKaguyaMod.item.ItemBlockDivineSpirit;
import thKaguyaMod.tileentity.TileEntityDivineSpirit;

import java.util.List;
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

    /**
     * 小神霊（原版 metadata 8 色 → BlockState プロパティ、当たり判定なし、ワールドでは TESR だけが描く）
     * 原版: BlockDivineSpirit（BlockContainer + ISBRH は何も描かない + TileEntityDivineSpiritRenderer）
     */
    public static final RegistryObject<Block> DIVINE_SPIRIT = BLOCKS.register("divine_spirit",
            () -> new BlockDivineSpirit(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.1F, 2000.0F)
                    .sound(SoundType.SNOW)
                    .lightLevel(s -> 8)
                    .noOcclusion()
                    .noCollission()));

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

    /**
     * 小神霊のアイテム（原版 metadata 8 色 → 8 個の独立アイテム。銀ナイフ 4 色と同じ方針）
     * 並びは原版 metadata と同じ 赤 / 青 / 緑 / 黄 / 紫 / 水 / 橙 / 白。
     * 青だけは既存 ID {@code divine_spirit} をそのまま使う（旧ワールドのアイテムを消さないため）。
     */
    public static final RegistryObject<Item> DIVINE_SPIRIT_RED =
            registerDivineSpiritItem("divine_spirit_red", DyeColor.RED);
    public static final RegistryObject<Item> DIVINE_SPIRIT_ITEM =
            registerDivineSpiritItem("divine_spirit", DyeColor.BLUE);
    public static final RegistryObject<Item> DIVINE_SPIRIT_GREEN =
            registerDivineSpiritItem("divine_spirit_green", DyeColor.GREEN);
    public static final RegistryObject<Item> DIVINE_SPIRIT_YELLOW =
            registerDivineSpiritItem("divine_spirit_yellow", DyeColor.YELLOW);
    public static final RegistryObject<Item> DIVINE_SPIRIT_PURPLE =
            registerDivineSpiritItem("divine_spirit_purple", DyeColor.PURPLE);
    public static final RegistryObject<Item> DIVINE_SPIRIT_CYAN =
            registerDivineSpiritItem("divine_spirit_cyan", DyeColor.CYAN);
    public static final RegistryObject<Item> DIVINE_SPIRIT_ORANGE =
            registerDivineSpiritItem("divine_spirit_orange", DyeColor.ORANGE);
    public static final RegistryObject<Item> DIVINE_SPIRIT_WHITE =
            registerDivineSpiritItem("divine_spirit_white", DyeColor.WHITE);

    /**
     * 原版 metadata（0..7）→ 小神霊アイテムの唯一の対応表。
     * ドロップ（{@link BlockDivineSpirit#getDrops}）と小神霊エンティティの受け渡しは必ずここを通す。
     */
    private static final List<RegistryObject<Item>> DIVINE_SPIRIT_ITEMS = List.of(
            DIVINE_SPIRIT_RED, DIVINE_SPIRIT_ITEM, DIVINE_SPIRIT_GREEN, DIVINE_SPIRIT_YELLOW,
            DIVINE_SPIRIT_PURPLE, DIVINE_SPIRIT_CYAN, DIVINE_SPIRIT_ORANGE, DIVINE_SPIRIT_WHITE);

    /** 色番号（0 赤 / 1 青 / 2 緑 / 3 黄 / 4 紫 / 5 水 / 6 橙 / 7 白）→ アイテム */
    public static Item divineSpiritItem(int colorIndex)
    {
        return DIVINE_SPIRIT_ITEMS.get(Math.floorMod(colorIndex, DIVINE_SPIRIT_ITEMS.size())).get();
    }

    private static RegistryObject<Item> registerBlockItem(String name, Supplier<Block> block)
    {
        return THKaguyaItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<Item> registerDivineSpiritItem(String name, DyeColor color)
    {
        return THKaguyaItems.ITEMS.register(name,
                () -> new ItemBlockDivineSpirit(DIVINE_SPIRIT.get(), color, new Item.Properties()));
    }
}
