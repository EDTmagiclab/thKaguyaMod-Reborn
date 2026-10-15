package thKaguyaMod.entity.shot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** 特殊弾を定義するインターフェース（1.20.1 移植版） */
public interface ISpecialShot
{
	/** 毎tick呼ばれる特殊弾の処理 */
	public void specialShot_move(Level world, int id, EntityTHShot shot);

	/** ブロックに当たったときの特殊弾の処理。ブロックに当って消滅するなら false */
	public boolean specialShot_hitBlock(Level world, int id, EntityTHShot shot, HitResult result);

	/** Entityに当たったときの特殊弾の処理。Entityに当って消滅するなら false */
	public boolean specialShot_hitEntity(Level world, int id, EntityTHShot shot, Entity entity_Hit);
}
