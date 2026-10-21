package thKaguyaMod;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 1.20.1 时代 ItemStack 上的自由 NBT（{@code getOrCreateTag()}）在 1.21.1 已经没有等价物，
 * 一律改成自定义数据组件 {@code thkaguya:stack_tag}（CompoundTag）。
 *
 * <p>设计取舍：本项目旧代码把物品参数当成 stringly-typed 的 CompoundTag 到处读写
 * （弾幕工作台会动态写 Number / DanmakuForm / Speed / Color / Special / Gravity），
 * 逐键拆成 7 个强类型组件会牵连 40 多处读写点且容易漏同步。因此保留"一个组件装一个
 * CompoundTag"的形态，但**必须同时给 persistent 与 networkSynchronized**
 * （只给 persistent 的话单人正常、上服务器客户端读到的永远是默认值）。
 *
 * <p>使用约定：
 * <ul>
 *   <li>只读：{@code CompoundTag nbt = THStackData.get(stack);}（可能为 null，和旧 getTag() 一致）</li>
 *   <li>要改：{@code CompoundTag nbt = THStackData.getOrCreate(stack);} …改动… 最后 {@code THStackData.set(stack, nbt);}
 *       —— 组件是值语义，改完必须写回，否则不会同步也不会存档。</li>
 * </ul>
 */
public final class THStackData
{
	public static final DeferredRegister.DataComponents DATA_COMPONENTS =
			DeferredRegister.createDataComponents(THKaguyaCore.MODID);

	/** 1.20.1 ItemStack.tag 的等价物。 */
	public static final Supplier<DataComponentType<CompoundTag>> STACK_TAG = DATA_COMPONENTS.registerComponentType(
			"stack_tag",
			builder -> builder
					.persistent(CompoundTag.CODEC)
					.networkSynchronized(ByteBufCodecs.COMPOUND_TAG));

	private THStackData()
	{
	}

	/** @return 该堆叠上的 CompoundTag；没有就返回 null（与旧 {@code getTag()} 语义一致）。 */
	public static CompoundTag get(ItemStack stack)
	{
		if (stack.isEmpty())
		{
			return null;
		}
		CompoundTag tag = stack.get(STACK_TAG.get());
		if (tag == null)
		{
			// 1.20.1 旧存档的第一次读取：把旧 tag 搬进组件
			tag = migrateLegacyTag(stack);
		}
		return tag;
	}

	public static boolean has(ItemStack stack)
	{
		return get(stack) != null;
	}

	/** 供 {@code Item#verifyComponentsAfterLoad} 调用的入口：走到 get() 即触发一次性迁移。 */
	public static void migrate(ItemStack stack)
	{
		if (!stack.isEmpty())
		{
			get(stack);
		}
	}

	/**
	 * 1.20.1 旧存档迁移：那时的 {@code ItemStack.tag{...}} 在 1.21 被原版 DFU 拆成
	 * 两部分——「原版认识的键」（Damage、CustomModelData、display…）进各自组件，
	 * 「剩下的键」整体进 {@code minecraft:custom_data}。本 mod 的自由 NBT
	 * （Number / DanmakuForm / Speed / Color / Special / Gravity / Infinity /
	 * GapColor / Charge / mode / OriginalDamage / OriginalItemID）全在后一堆里，
	 * 所以这里把它原样搬进 {@code thkaguya:stack_tag}。
	 *
	 * <p>只对「本 mod 的物品 + 还没有 stack_tag + 确实带 custom_data」执行，读取一次即完成，
	 * 之后 {@code stack_tag} 存在就不会再进来；迁移后清掉 custom_data，避免两份数据打架。</p>
	 *
	 * <p>注意：{@code Damage}（符卡编号）不在这里——它被 DFU 搬去了 {@code minecraft:damage}
	 * 组件，由 {@code ItemTHSpellCard#getSpellCardID} 单独处理。</p>
	 */
	private static CompoundTag migrateLegacyTag(ItemStack stack)
	{
		CustomData legacy = stack.get(DataComponents.CUSTOM_DATA);
		if (legacy == null || legacy.isEmpty())
		{
			return null;
		}
		// 只动自己的物品，别碰别的 mod 写在 custom_data 里的东西
		if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(THKaguyaCore.MODID))
		{
			return null;
		}
		CompoundTag tag = legacy.copyTag();
		if (tag.isEmpty())
		{
			return null;
		}
		set(stack, tag);
		stack.remove(DataComponents.CUSTOM_DATA);
		return stack.get(STACK_TAG.get());
	}

	/** 取不到就返回一个**未挂到堆叠上**的空 CompoundTag，改完请调用 {@link #set}。 */
	public static CompoundTag getOrCreate(ItemStack stack)
	{
		CompoundTag tag = get(stack);
		return tag != null ? tag : new CompoundTag();
	}

	/** 取副本，适合"读了就改、不想污染原组件实例"的场合。 */
	public static CompoundTag copy(ItemStack stack)
	{
		CompoundTag tag = get(stack);
		return tag != null ? tag.copy() : new CompoundTag();
	}

	/** 写回。传空 tag 等于清空组件。 */
	public static void set(ItemStack stack, CompoundTag tag)
	{
		if (stack.isEmpty())
		{
			return;
		}
		if (tag == null || tag.isEmpty())
		{
			stack.remove(STACK_TAG.get());
		}
		else
		{
			stack.set(STACK_TAG.get(), tag);
		}
	}

	public static int getInt(ItemStack stack, String key, int fallback)
	{
		CompoundTag tag = get(stack);
		return tag != null && tag.contains(key, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
	}

	public static boolean getBoolean(ItemStack stack, String key)
	{
		CompoundTag tag = get(stack);
		return tag != null && tag.getBoolean(key);
	}
}
