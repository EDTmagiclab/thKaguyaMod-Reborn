package thKaguyaMod.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * 小神霊ブロック（1.20.1 移植版）
 *
 * 原版: extract/sources/java/thKaguyaMod/block/BlockDivineSpirit.java（1.7.10、BlockContainer）
 *   - ワールドでは何も描かない（ISBRH が renderWorldBlock → false、BlockDivineSpirit は
 *     getRenderType() で独自レンダーID を返すだけ）
 *   - 実際の見た目は TileEntityDivineSpiritRenderer（TESR）が描く 3 枚重ねの板
 *     → 1.20.1 では {@code client/render/block/RenderDivineSpiritBlockEntity} が担当
 *   - 当たり判定なし（getCollisionBoundingBoxFromPool → null）
 *     → 1.20.1 は {@code Properties#noCollission()}
 *   - メタデータ 8 値 = 8 色（赤/青/緑/黄/紫/水/橙/白）→ 1.20.1 は BlockState プロパティで表現
 *   - 固さ 0.1 / 爆発耐性 2000 / 音 snow / 明るさ 8
 *
 * 色プロパティは原版メタデータの置き換え。ブロックアイテムは 1 種のままなので、
 * 通常は {@link DyeColor#BLUE}（原版の既定アイコン）で置かれ、
 * 他色は {@code /setblock ... thkaguya:divine_spirit[color=red]} で指定する。
 */
public class BlockDivineSpirit extends Block
{
    /** 原版メタデータ順: 赤 / 青 / 緑 / 黄 / 紫 / 水 / 橙 / 白 */
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class,
            DyeColor.RED, DyeColor.BLUE, DyeColor.GREEN, DyeColor.YELLOW,
            DyeColor.PURPLE, DyeColor.CYAN, DyeColor.ORANGE, DyeColor.WHITE);

    public BlockDivineSpirit(Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(COLOR, DyeColor.BLUE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(COLOR);
    }
}
