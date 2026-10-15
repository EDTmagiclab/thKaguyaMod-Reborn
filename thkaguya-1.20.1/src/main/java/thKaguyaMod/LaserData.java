package thKaguyaMod;

/**
 * レーザーの性質設定（1.20.1 移植版）
 * 激光属性数据类，纯数据无 Minecraft API 依赖，直接迁移。
 */
public class LaserData
{
	/** レーザーの形状 */
	public int form;
	/** レーザーの色。DanmakuConstants.RED 等 */
	public int color;
	/** レーザーのダメージ。2.0F でハート１つ分 */
	public float damage;
	/** レーザーの太さ（当たり判定） */
	public float width;
	/** レーザーの長さ */
	public float length;
	/** レーザーの遅延時間（1/20秒） */
	public int delay;
	/** レーザーの消滅時間（1/20秒） */
	public int end;
	/** 特殊動作 ID */
	public int special;

	public LaserData(int form, int color, float width, float length, float damage, int delay, int end, int special)
	{
		this.form = form;
		this.color = color;
		this.damage = damage;
		this.width = width;
		this.length = length;
		this.delay = delay;
		this.end = end;
		this.special = special;
	}

	public static LaserData laser(int form, int color, float width, float length, float damage, int delay, int end, int special)
	{
		return new LaserData(form, color, width, length, damage, delay, end, special);
	}

	public static LaserData laser(int form, int color, float width, float length, float damage, int delay, int end)
	{
		return laser(form, color, width, length, damage, delay, end, 0);
	}

	public static LaserData laser(int form, int color, float width, float length, float damage, int delay)
	{
		return laser(form, color, width, length, damage, delay, 80);
	}

	public static LaserData laser(int form, int color, float width, float length, float damage)
	{
		return laser(form, color, width, length, damage, 0, 80);
	}

	public static LaserData laser(int color, float width, float length, float damage, int delay, int end, int special)
	{
		return new LaserData(0, color, width, length, damage, delay, end, special);
	}

	public static LaserData laser(int color, float width, float length, float damage, int delay, int end)
	{
		return laser(color, width, length, damage, delay, end, 0);
	}

	public static LaserData laser(int color, float width, float length, float damage, int delay)
	{
		return laser(color, width, length, damage, delay, 80);
	}

	public static LaserData laser(int color, float width, float length, float damage)
	{
		return laser(color, width, length, damage, 0, 80);
	}
}
