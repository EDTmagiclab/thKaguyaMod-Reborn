package thKaguyaMod;

/**
 * 弾の性質設定（1.20.1 移植版）
 * 弹体属性数据类，纯数据无 Minecraft API 依赖，直接迁移。
 * 引用 DanmakuConstants.SIZE/DAMAGE（原 THShotLib 常量已上移）。
 */
public class ShotData
{
	/** 弾の形状。DanmakuConstants.FORM_SMALL 等 */
	public int form;
	/** 弾の色。DanmakuConstants.RED 等 */
	public int color;
	/** 弾のダメージ。2.0F でハート１つ分 */
	public float damage;
	/** 弾のサイズ（当たり判定）。1.0F で１メートル程度 */
	public float size;
	/** 弾の遅延時間（1/20秒） */
	public int delay;
	/** 弾の消滅時間（1/20秒，遅延時間は含まれない） */
	public int end;
	/** 特殊動作 ID */
	public int special;

	public ShotData(int form, int color, float size, float damage, int delay, int end, int special)
	{
		this.form = form;
		this.color = color;
		this.damage = damage;
		this.size = size;
		this.delay = delay;
		this.end = end;
		this.special = special;
	}

	public static ShotData shot(int form, int color, float size, float damage, int delay, int end, int special)
	{
		form = form % 32;
		color = color % 16;
		return new ShotData(form, color, size, damage, delay, end, special);
	}

	public static ShotData shot(int form, int color, float size, float damage, int delay, int end)
	{
		return shot(form, color, size, damage, delay, end, 0);
	}

	public static ShotData shot(int form, int color, float size, float damage)
	{
		return shot(form, color, size, damage, 0, 80);
	}

	public static ShotData shot(int form, int color)
	{
		return shot(form, color, 0);
	}

	public static ShotData shot(int form, int color, int delay)
	{
		return shot(form, color, delay, 80);
	}

	public static ShotData shot(int form, int color, int delay, int end)
	{
		return shot(form, color, delay, end, 0);
	}

	public static ShotData shot(int form, int color, int delay, int end, int special)
	{
		float size = DanmakuConstants.SIZE[form % 32];
		float damage = DanmakuConstants.DAMAGE[form % 32];
		return shot(form, color, size, damage, delay, end, special);
	}
}
