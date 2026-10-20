package thKaguyaMod;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
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
		return stack.isEmpty() ? null : stack.get(STACK_TAG.get());
	}

	public static boolean has(ItemStack stack)
	{
		return !stack.isEmpty() && stack.has(STACK_TAG.get());
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
