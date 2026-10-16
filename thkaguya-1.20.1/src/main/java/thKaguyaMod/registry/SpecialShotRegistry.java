package thKaguyaMod.registry;

import java.util.HashMap;
import java.util.Map;

import thKaguyaMod.entity.shot.ISpecialShot;

/**
 * 特殊弾の登録クラス（1.20.1 移植版）
 * 纯数据注册表，无 Minecraft API 依赖，直接迁移。
 */
public class SpecialShotRegistry
{
	public static Map<Integer, Class<? extends ISpecialShot>> specialShotClass = new HashMap<Integer, Class<? extends ISpecialShot>>();

	/**
	 * 特殊弾の「実体」を ID から引くための表（1.20.1 追加）。
	 *
	 * <p>原版 1.7.10 は毎回 `Class#newInstance()` で特殊弾インスタンスを作っていたが、
	 * 1.20.1 では **Item / Block のインスタンスを動的に new すると Forge の
	 * intrusive holder 検査（"Some intrusive holders were not registered"）に引っかかる**。
	 * 人魂灯（ItemSoulTorch）やエイジャの赤石（ItemAjaRedStone）のように
	 * `Item implements ISpecialShot` な実装はこれに該当するため、
	 * 登録済みの実体を保持して使い回す。
	 */
	public static Map<Integer, ISpecialShot> specialShotInstance = new HashMap<Integer, ISpecialShot>();

	/**
	 * 特殊弾を登録する。登録するクラスはISpecialShotをimplementsしたもの
	 * @param specialShot_class : ISpecialShotをimplementsしたクラス
	 * @param id              : 他の特殊弾と被らない数字
	 * @return 登録に成功したらtrue
	 */
	public static boolean registerSpecialShot(Class<? extends ISpecialShot> specialShot_class, int id)
	{
		specialShotClass.put(id, specialShot_class);

		return true;
	}

	/**
	 * 特殊弾の実体を登録する（Item など、動的に new できない実装用）
	 * @param instance : ISpecialShot の実体
	 * @param id       : 他の特殊弾と被らない数字
	 */
	public static void registerSpecialShotInstance(ISpecialShot instance, int id)
	{
		specialShotInstance.put(id, instance);
	}

	/** 登録済みの特殊弾実体を返す（無ければ null） */
	public static ISpecialShot getSpecialShotInstance(int id)
	{
		return specialShotInstance.get(id);
	}

	/**
	 * 特殊弾IDから特殊弾処理の書かれたクラスを返す
	 * @param id : 特殊弾ID
	 * @return 特殊弾定義の書かれたクラス
	 */
	public static Class<? extends ISpecialShot> getSpecialShotClass(int id)
	{
		return specialShotClass.get(id);
	}

	/**
	 * 登録されている特殊弾の合計数を返す
	 * @return
	 */
	public static int getNumberOfSpecialShot()
	{
		return specialShotClass.size();
	}
}
