package thKaguyaMod.entity.spellcard;

import net.minecraft.world.phys.Vec3;

/**
 * 符卡迁移辅助函数（1.20.1）
 * 原版 1.7.10 的 Vec3 是可变对象且带 rotateAroundX/Y/Z 方法；
 * 1.20.1 的 Vec3 不可变且无旋转方法，符卡代码中的分量赋值+旋转统一用本类重建。
 */
public final class THSC_Helper
{
    private THSC_Helper() {}

    /** 原版 Vec3.createVectorHelper 等价 */
    public static Vec3 vec3(double x, double y, double z)
    {
        return new Vec3(x, y, z);
    }

    /** 原版 Vec3.rotateAroundY 等价：绕 Y 轴旋转 angle 弧度 */
    public static Vec3 rotateY(Vec3 vec, float angle)
    {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vec.x * cos + vec.z * sin;
        double z = -vec.x * sin + vec.z * cos;
        return new Vec3(x, vec.y, z);
    }

    /** 原版 Vec3.rotateAroundX 等价：绕 X 轴旋转 angle 弧度 */
    public static Vec3 rotateX(Vec3 vec, float angle)
    {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double y = vec.y * cos + vec.z * sin;
        double z = -vec.y * sin + vec.z * cos;
        return new Vec3(vec.x, y, z);
    }

    /** 原版 Vec3.rotateAroundZ 等价：绕 Z 轴旋转 angle 弧度 */
    public static Vec3 rotateZ(Vec3 vec, float angle)
    {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vec.x * cos - vec.y * sin;
        double y = vec.x * sin + vec.y * cos;
        return new Vec3(x, y, vec.z);
    }
}
