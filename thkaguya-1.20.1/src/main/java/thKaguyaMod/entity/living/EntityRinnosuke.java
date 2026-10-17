package thKaguyaMod.entity.living;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.Level;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaVillagers;

/**
 * 森近 霖之助（原版 1.7.10 的 EntityRinnosuke）。
 *
 * <p>原版：{@code extends EntityVillager}，固定职业 id 932，可以消失、每区块最多 1 只、
 * 不能繁殖，自然生成概率 2% 且避开「四周方块完全相同」的平地。
 *
 * <p>1.20.1：{@code extends Villager}，职业改为 {@code thkaguya:rinnosuke}（绑定弹幕工作台，
 * 见 {@link THKaguyaVillagers}）。职业被强制写回，避免 {@code Villager#finalizeSpawn} / 存档读取
 * / 村民脑行为把它改回 {@code none} 或随机职业。
 */
public class EntityRinnosuke extends Villager
{
    public EntityRinnosuke(EntityType<? extends EntityRinnosuke> type, Level level)
    {
        super(type, level);
        this.setVillagerData(this.getVillagerData().setProfession(THKaguyaVillagers.RINNOSUKE.get()).setLevel(1));
    }

    /** 方便 /summon 与刷怪蛋以外的地方直接按等级构造（对齐原版的单参构造） */
    public EntityRinnosuke(Level level)
    {
        this(THKaguyaEntities.RINNOSUKE.get(), level);
    }

    /**
     * 强制职业：原版这个实体永远是霖之助，而 1.20.1 的村民会因为「没工作站」、
     * 繁殖、读取存档等路径把职业改回 none。这里统一兜住。
     */
    @Override
    public void setVillagerData(VillagerData data)
    {
        VillagerProfession profession = THKaguyaVillagers.RINNOSUKE.get();
        if (data.getProfession() != profession)
        {
            data = data.setProfession(profession);
        }
        super.setVillagerData(data);
    }

    /** 原版 getMaxSpawnedInChunk() = 1 */
    @Override
    public int getMaxSpawnClusterSize()
    {
        return 1;
    }
}
