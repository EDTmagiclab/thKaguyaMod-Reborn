package thKaguyaMod.init;

import net.neoforged.neoforge.registries.DeferredHolder;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredRegister;
import thKaguyaMod.THKaguyaCore;

/**
 * 村人（1.20.1 的 Point of Interest + VillagerProfession）注册。
 *
 * <p>原版 1.7.10 用 {@code VillagerRegistry.instance().registerVillageTradeHandler(932, ...)}
 * 按固定 id 挂交易；1.20.1 改成「职业（VillagerProfession）+ 兴趣点（PoiType）」体系：
 * <ul>
 *   <li>兴趣点：弹幕工作台（原版没有指定工作方块，按用户要求用弹幕工作台当霖之助的工作站）；</li>
 *   <li>职业：{@code thkaguya:rinnosuke}，只认这一个兴趣点，交易由 {@link thKaguyaMod.event.THKaguyaVillagerTrades} 挂上。</li>
 * </ul>
 *
 * <p>注意：{@code PoiTypes.TYPE_BY_STATE} 在 Forge 里由 {@code GameData.getBlockStatePointOfInterestTypeMap()}
 * 提供（PoiTypes 静态块直接取这个 map 的 keySet），该 map 在注册事件后由 Forge 重建，
 * 所以用 DeferredRegister 注册的兴趣点会被 {@code ServerLevel#onBlockStateChange} 正常识别，
 * 不需要反射注入。
 */
public final class THKaguyaVillagers
{
    /** 兴趣点 / 职业的注册表 */
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, THKaguyaCore.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, THKaguyaCore.MODID);

    /** 兴趣点键（职业的判定谓词要比对 key，不能在注册期拿 RegistryObject#get） */
    public static final ResourceKey<PoiType> RINNOSUKE_POI_KEY =
            ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,
                    ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "rinnosuke"));

    /**
     * 霖之助的工作站兴趣点 = 弹幕工作台（含全部朝向/激光变体状态）。
     * maxTickets = 1（一个工作站只服务一个村民，与原版工作台一致），validRange = 1（紧贴方块）。
     */
    public static final DeferredHolder<PoiType, PoiType> RINNOSUKE_POI = POI_TYPES.register("rinnosuke",
            () -> new PoiType(
                    ImmutableSet.copyOf(THKaguyaBlocks.DANMAKU_CRAFTING_TABLE.get().getStateDefinition().getPossibleStates()),
                    1, 1));

    /**
     * 霖之助职业。requestedItems 是村民会主动捡起的东西（补货要给得出这些材料），
     * 因此把 8 条交易里作为「买方支付物」的物品都列进来：
     * 小 power、point、铁矿、下界之星、红石、青金石、金锭。
     */
    public static final DeferredHolder<VillagerProfession, VillagerProfession> RINNOSUKE = PROFESSIONS.register("rinnosuke",
            () -> new VillagerProfession(
                    "rinnosuke",
                    holder -> holder.is(RINNOSUKE_POI_KEY),   // heldJobSite
                    holder -> holder.is(RINNOSUKE_POI_KEY),   // acquirableJobSite
                    ImmutableSet.of(
                            THKaguyaItems.POWER_UP_ITEM_SMALL.get(),
                            THKaguyaItems.POINT_ITEM.get(),
                            Items.IRON_INGOT,
                            Items.NETHER_STAR,
                            Items.REDSTONE,
                            Items.LAPIS_LAZULI,
                            Items.GOLD_INGOT),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_FLETCHER));

    private THKaguyaVillagers()
    {
    }
}
