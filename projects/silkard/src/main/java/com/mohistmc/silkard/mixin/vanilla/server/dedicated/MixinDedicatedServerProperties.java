package com.mohistmc.silkard.mixin.vanilla.server.dedicated;

import com.mohistmc.silkard.bukkit.MainHook;
import com.mohistmc.silkard.injected.server.dedicated.ContextDedicatedServerProperties;
import java.util.Properties;
import joptsimple.OptionSet;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(DedicatedServerProperties.class)
public class MixinDedicatedServerProperties implements ContextDedicatedServerProperties {

    @Unique
    public boolean silkard$debug;

    @Override
    public boolean silkard$debug() {
        return silkard$debug;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void silkard$readDebug(Properties properties, CallbackInfo ci) {
        OptionSet options = MainHook.options();
        String value = (options != null && options.has("debug")) ? String.valueOf(options.valueOf("debug")) : properties.getProperty("debug");
        this.silkard$debug = Boolean.parseBoolean(value);
    }
}
