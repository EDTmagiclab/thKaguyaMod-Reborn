package thKaguyaMod;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 1.21.1 / NeoForge 21.1 骨架入口。
 * <p>此文件当前只用于验证开发环境（JDK21 + ModDevGradle + NeoForm + 注册 API）可用，
 * 不包含任何游戏内容；正式移植代码在 M1 之后按批次迁入。</p>
 */
@Mod(THKaguyaCore.MODID)
public class THKaguyaCore {

    public static final String MODID = "thkaguya";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 1.21.x 起构造器可直接注入 mod 事件总线与容器，不再用 FMLJavaModLoadingContext.get()。 */
    public THKaguyaCore(IEventBus modEventBus, ModContainer modContainer) {
        THKaguyaItems.ITEMS.register(modEventBus);
        LOGGER.info("[thkaguya] 1.21.1 / NeoForge bootstrap OK (smoke skeleton)");
    }
}
