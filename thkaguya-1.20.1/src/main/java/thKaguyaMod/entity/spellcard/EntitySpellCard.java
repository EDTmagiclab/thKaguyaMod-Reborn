package thKaguyaMod.entity.spellcard;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.entity.effect.EntitySpellCardCircle;
import thKaguyaMod.entity.living.EntityDanmakuMob;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.registry.SpellCardRegistry;

/**
 * スペルカードのEntity（1.20.1 移植版）
 *
 * 阶段 3d-2：符卡实体。管理符卡生命周期，每 tick 调用 THSpellCard.onUpdate()。
 * 裁剪：THShotLib.danmakuRemove（弹消，依赖未迁移）。
 */
public class EntitySpellCard extends Entity
{
    private static final EntityDataAccessor<Integer> SPELL_CARD_NUMBER =
            SynchedEntityData.defineId(EntitySpellCard.class, EntityDataSerializers.INT);

    public LivingEntity user;
    public LivingEntity tgEntity;
    public int count;
    private int spellCardNumber;
    protected Vec3 tgVec;
    private int spellCardUsedTime;
    private int lastTime;
    public int level;

    public EntitySpellCardCircle circle;

    public boolean canMoveInTimeStop;

    Class<?> spellcard;
    THSpellCard useSpellCard;

    public EntitySpellCard(EntityType<?> type, Level level)
    {
        super(type, level);
        lastTime = 0;
    }

    public EntitySpellCard(Level world, LivingEntity spUser, LivingEntity target, int num, int spLevel)
    {
        this(THKaguyaEntities.SPELL_CARD.get(), world);

        float yawRad = (spUser.getYRot() + 30F) / 180F * (float)Math.PI;
        float pitchRad = spUser.getXRot() / 180F * (float)Math.PI;
        double x = spUser.getX() - (double)Mth.sin(yawRad) * (double)Mth.cos(pitchRad);
        double y = spUser.getY() - (double)Mth.sin(pitchRad) + (double)spUser.getEyeHeight() - 0.1D;
        double z = spUser.getZ() + (double)Mth.cos(yawRad) * (double)Mth.cos(pitchRad);
        this.moveTo(x, y, z, spUser.getYRot(), spUser.getXRot());

        tgVec = spUser.getLookAngle();
        user = spUser;
        tgEntity = target;
        spellCardNumber = num;
        setSpellCardNumber(num);
        spellCardUsedTime = 0;
        count = 0;

        level = spLevel;

        if((spellcard = SpellCardRegistry.getSpellCardClass(num)) != null)
        {
            try {
                useSpellCard = (THSpellCard)spellcard.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }

            if(useSpellCard != null)
            {
                useSpellCard.init(world, this, user, tgEntity, level);
                canMoveInTimeStop = useSpellCard.canMoveInTimeStop();
                if(spUser instanceof Player)
                {
                    circle = new EntitySpellCardCircle(world, spUser, useSpellCard.getSpellCardCircleColor(), useSpellCard.getEndTime());
                    if(!world.isClientSide)
                    {
                        world.addFreshEntity(circle);
                    }
                }
            }
        }
        else
        {
            if(!level().isClientSide)
            {
                discard();
            }
        }
    }

    @Override
    protected void defineSynchedData()
    {
        this.entityData.define(SPELL_CARD_NUMBER, 0);
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    private void finish()
    {
        if(!this.level().isClientSide)
        {
            this.discard();
            if(circle != null)
            {
                circle.discard();
            }
        }
    }

    public void specialProcessInTimeStop()
    {
        if(useSpellCard != null)
        {
            useSpellCard.specialProcessInTimeStop();
        }
    }

    @Override
    public void tick()
    {
        super.tick();

        // 使用者がいないか、死んでいるなら消す
        if(!level().isClientSide && (user == null || !user.isAlive()))
        {
            finish();
            return;
        }

        // 使用者が弾幕MOBなら、スペルカードモードでなければ消す
        if(user != null && user instanceof EntityDanmakuMob)
        {
            EntityDanmakuMob danmakuMob = (EntityDanmakuMob)user;
            if(!danmakuMob.isSpellCardMode)
            {
                finish();
                return;
            }
        }

        if(tickCount > spellCardUsedTime)
        {
            if(spellcard == null || useSpellCard == null)
            {
                return;
            }

            useSpellCard.onUpdate();

            if(useSpellCard.time >= useSpellCard.getEndTime())
            {
                finish();
                return;
            }
        }

        if(spellCardUsedTime < tickCount)
        {
            spellCardUsedTime = tickCount;
        }

        if(tickCount >= 9999)
        {
            if(!level().isClientSide)
            {
                finish();
            }
        }

        lastTime = tickCount;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
    }

    public void setSpellCardNumber(int number)
    {
        this.entityData.set(SPELL_CARD_NUMBER, number);
    }

    public int getSpellCardNumber()
    {
        return this.entityData.get(SPELL_CARD_NUMBER);
    }
}
