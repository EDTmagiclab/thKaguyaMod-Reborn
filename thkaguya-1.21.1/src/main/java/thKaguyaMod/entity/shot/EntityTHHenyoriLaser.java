package thKaguyaMod.entity.shot;

import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.entity.item.EntitySakuyaWatch;
import thKaguyaMod.entity.living.EntityTHFairy;
import thKaguyaMod.init.THKaguyaItems;

/**
 * へにょりレーザー（蛇行レーザー）系の共通処理。
 *
 * 原版 `extract/sources/java/thKaguyaMod/entity/shot/EntityTHHenyoriLaser.java`（326 行）の移植。
 * 通常の {@link EntityTHLaser} との違いは以下の 3 点だけで、それ以外は同じレーザー処理を共有する。
 *
 * <ol>
 *   <li>当たり判定が直線ではなく <b>13 段の蛇行</b>（{@code sin/cos} で ±3 ブロック振れる）。</li>
 *   <li>消滅時に撒く弾の素が {@code EntityShotMaterial} ではなく <b>ItemEntity 直接</b>で、
 *       間隔も 1.5 ブロックではなく <b>0.3 ブロック</b>と非常に細かい。</li>
 *   <li>当たり判定用の体格が 15×1（原版 {@code setSize(15.0F, 1.0F)}）。</li>
 * </ol>
 *
 * 補足：原版ではこのエンティティの {@code registerModEntity}（CommonProxy.java:258）と
 * レンダラ登録（ClientProxy.java:159）がどちらもコメントアウトされており、
 * 実際に生成される経路が存在しない（＝原版でも死コード）。それでも移植版では
 * EntityType が登録済みで、将来ヘニョリ弾を出す符卡を足したときに素直に動くよう
 * 原版のロジックをそのまま再現しておく。
 */
public class EntityTHHenyoriLaser extends EntityTHLaser
{
    public EntityTHHenyoriLaser(EntityType<EntityTHHenyoriLaser> type, Level level)
    {
        super(type, level);
    }

    /** 原版コンストラクタの {@code setSize(15.0F, 1.0F)} 相当。 */
    @Override
    public EntityDimensions getDimensions(Pose pose)
    {
        return EntityDimensions.scalable(15.0F, 1.0F);
    }

    /**
     * レーザーを消す。同時にレーザーが弾の素になる（原版 :52-68）。
     *
     * 通常レーザー（{@link EntityTHLaser#shotFinishBonus()}）は根元から 1.5 ブロックおきに
     * {@code EntityShotMaterial} を出すが、こちらは 0.3 ブロックおきに {@code ItemEntity} を
     * 直接撒き、{@code age = 5700}（= 15 秒で消滅）にしておく。
     * 発生位置も原版どおり「根元から angle の逆方向」である点に注意。
     */
    @Override
    public void shotFinishBonus()
    {
        if(level().isClientSide)
        {
            return;
        }

        for(double zPos = 0.0D; zPos < getLaserLength(); zPos += 0.3D)
        {
            ItemEntity item = new ItemEntity(level(),
                    this.getX() - angle.x * zPos,
                    this.getY() - angle.y * zPos,
                    this.getZ() - angle.z * zPos,
                    new ItemStack(THKaguyaItems.SHOT_MATERIAL.get(), 1));
            level().addFreshEntity(item);
            // 原版は entityItem.age = 5700（寿命 6000 → 残り 300 tick = 15 秒で消滅）で
            // 撒いた弾の素を早く消していた。1.20.1 の ItemEntity#age は private なので、
            // 同じ意味になる lifespan を直接 300 に設定する。
            item.lifespan = 300;
        }

        discard();
    }

    /**
     * 衝突判定：13 段の蛇行レーザー（原版 :88-199）。
     *
     * 1 段ごとに「始点 → 次の節点」の線分を作り、ブロック判定とエンティティ判定を行う。
     * 節点は {@code angle * (i*length/13)} に {@code sin/cos(baseAngle)*3} の振れを足した位置で、
     * {@code baseAngle} は {@code getAnimationCount()}（＝弾齢）から作るので、時間とともに胴体がうねる。
     */
    @Override
    protected void hitCheck()
    {
        double width = getShotSize();
        float angleSpanY = (float)Math.PI / 30F;
        float baseAngle = (float)getAnimationCount() * angleSpanY;

        double nowX = this.getX();
        double nowY = this.getY();
        double nowZ = this.getZ();

        Vec3 motion = this.getDeltaMovement();
        int flontCount = 0;

        for(int i = 0; i < 13; i++)
        {
            baseAngle += angleSpanY;
            flontCount++;
            double moveLength = flontCount * getLaserLength() / 13.0D;

            double nextX = nowX + angle.x * moveLength - Math.sin(baseAngle) * 3.0D + motion.x;
            double nextY = nowY + angle.y * moveLength + motion.y;
            double nextZ = nowZ + angle.z * moveLength + Math.cos(baseAngle) * 3.0D + motion.z;

            Vec3 start = new Vec3(nowX, nowY, nowZ);
            Vec3 end = new Vec3(nextX, nextY, nextZ);

            // ブロック判定（原版 worldObj.rayTraceBlocks(vec3d, vec3d1, true)）
            HitResult blockHit = level().clip(new ClipContext(start, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if(blockHit.getType() == HitResult.Type.BLOCK)
            {
                blockHitSpecialProcess(blockHit);
                double hitDistance = this.position().distanceTo(blockHit.getLocation());
                if(hitDistance > 0.3D)
                {
                    setLaserLength(hitDistance);
                    setMaxLaserLength(hitDistance);
                }
                else
                {
                    discard();
                    return;
                }
                end = blockHit.getLocation();
            }

            // エンティティ判定：移動量ぶん肉付けした直方体で候補を拾う（原版 :136-144）
            double xLength = motion.x + Math.sin(getYRot() / 180F * 3.141593F)
                    * Math.cos(getXRot() / 180F * 3.141593F) * moveLength;
            double yLength = motion.y + Math.sin(getXRot() / 180F * 3.141593F) * moveLength;
            double zLength = motion.z + Math.cos(getYRot() / 180F * 3.141593F)
                    * Math.cos(getXRot() / 180F * 3.141593F) * moveLength;

            AABB searchBox = this.getBoundingBox()
                    .expandTowards(xLength, yLength, zLength)
                    .inflate(width, width, width);

            List<Entity> list = level().getEntities(this, searchBox);

            for(Entity entity1 : list)
            {
                // 原版 :150-159 のフィルタ（当たり判定を持つ / 使用者側 / 弾・時計・動物・村人は除外 /
                // 妖精同士は同士討ちしない）
                if(!entity1.isAlive()
                        || hitCheckEx(entity1)
                        || entity1 instanceof EntityTHShot
                        || entity1 instanceof EntitySakuyaWatch
                        || entity1 instanceof Animal
                        || entity1 instanceof Villager
                        || !(entity1 instanceof LivingEntity)
                        || (user instanceof EntityTHFairy && entity1 instanceof EntityTHFairy))
                {
                    continue;
                }

                AABB hitBox = entity1.getBoundingBox().inflate(width, width, width);
                if(hitBox.clip(start, end).isPresent())
                {
                    onImpact(new EntityTHShot.EntityHitResultHelper(entity1));
                }
            }

            nowX = nowX + angle.x * moveLength - Math.sin(baseAngle) * 3.0D;
            nowY = nowY + angle.y * moveLength;
            nowZ = nowZ + angle.z * moveLength + Math.cos(baseAngle) * 3.0D;
        }
    }
}
