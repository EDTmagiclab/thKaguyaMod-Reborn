package thKaguyaMod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import thKaguyaMod.init.THKaguyaBlocks;
import thKaguyaMod.tileentity.TileEntityDivineSpirit;

import javax.annotation.Nullable;
import java.util.List;

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
 * 色プロパティは原版メタデータの置き換え。アイテムも色ごとに独立しているので
 * （{@link thKaguyaMod.item.ItemBlockDivineSpirit}）、置いた色と割ったときのドロップ色が一致する。
 *
 * ※ 重要: 原版の BlockContainer と同じく「ブロックエンティティを持つ」ことが必須。
 *    {@link EntityBlock#newBlockEntity} が無いと BlockEntity が生成されず、
 *    BlockEntityRenderer が一度も呼ばれない（= 空モデル + BER 構成では完全に不可視になる。
 *    段二十三の初回実装はこれを落として「置いても見えない」状態になった）。
 */
public class BlockDivineSpirit extends Block implements EntityBlock
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

    /** 原版 BlockContainer#createNewTileEntity 相当。null でも復刻は可能だが BER が描けない。 */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new TileEntityDivineSpirit(pos, state);
    }

    /** 原版メタデータ順（赤/青/緑/黄/紫/水/橙/白）→ 色番号 0..7 */
    public static int colorIndex(DyeColor dyeColor)
    {
        switch (dyeColor)
        {
            case RED:    return 0;
            case BLUE:   return 1;
            case GREEN:  return 2;
            case YELLOW: return 3;
            case PURPLE: return 4;
            case CYAN:   return 5;
            case ORANGE: return 6;
            default:     return 7;   // WHITE
        }
    }

    /**
     * 原版は metadata 付き ItemStack を返していた（new ItemStack(blockDivineSpirit, 1, color)）。
     * 1.20.1 は色ごとの独立アイテムなので、割ったときも置いたときと同じ色が出るようにする。
     *
     * mod 全体に loot_tables ディレクトリが無いため、ここで明示的に返さないと
     * どのブロックもドロップ 0 個になる（= 置いた小神霊を回収できない）。
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params)
    {
        return List.of(new ItemStack(THKaguyaBlocks.divineSpiritItem(colorIndex(state.getValue(COLOR)))));
    }
}
