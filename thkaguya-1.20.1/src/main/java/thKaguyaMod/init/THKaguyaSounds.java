package thKaguyaMod.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import thKaguyaMod.THKaguyaCore;

/**
 * カスタム効果音イベント。
 *
 * <p>原版 1.7.10 は {@code world.playSoundAtEntity(entity, "thkaguyamod:masterspark", ...)} の
 * ように文字列で直接鳴らしていたが、1.20.1 では {@link SoundEvent} をレジストリに登録して渡す。
 * ここで登録する 4 種は {@code assets/thkaguya/sounds.json} と 1:1 で対応する。
 *
 * <p>注意：{@code assets/thkaguya/sounds/*.ogg} の実ファイルは上流（1.7.10 リポジトリ）に
 * 存在しないため、現状は無音。ファイルを置けば sounds.json 経由でそのまま鳴る。
 */
public final class THKaguyaSounds
{
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, THKaguyaCore.MODID);

    /** 魔砲（マスタースパーク）：EntityMiniHakkero / THSC_NonDirectionalLaser / EntityHisou */
    public static final RegistryObject<SoundEvent> MASTER_SPARK = register("masterspark");

    /** スペルカード宣言（THKaguyaLib.spellCardDeclaration） */
    public static final RegistryObject<SoundEvent> SPELL_CARD = register("spellcard");

    /** スキマ（隙間）の移動音 */
    public static final RegistryObject<SoundEvent> SUKIMA = register("sukima");

    /** プレイヤーが倒れたとき（エクステンドアイテム復活） */
    public static final RegistryObject<SoundEvent> DOWN = register("down");

    private THKaguyaSounds()
    {
    }

    private static RegistryObject<SoundEvent> register(String name)
    {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(THKaguyaCore.MODID, name)));
    }
}
