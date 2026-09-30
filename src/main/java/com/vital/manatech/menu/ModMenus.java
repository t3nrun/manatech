package com.vital.manatech.menu;

import com.vital.manatech.ManatechMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ManatechMod.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> PROCESSING = MENUS.register("processing", () -> IMenuTypeExtension.create((id, inv, buf) -> new ProcessingMenu(id, inv, buf.readBlockPos())));
    public static final DeferredHolder<MenuType<?>, MenuType<AdamantiteMenu>> FURNACE = MENUS.register("adamantite_furnace", () -> IMenuTypeExtension.create((id, inv, buf) -> new AdamantiteMenu(id, inv, buf.readBlockPos())));
    private ModMenus() {}
}
