package com.mohistmc.silkard.mixin.vanilla.server.dedicated;

import com.google.common.base.MoreObjects;
import com.mohistmc.silkard.bukkit.MainHook;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Function;
import joptsimple.OptionSet;
import net.minecraft.server.dedicated.Settings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/11/1
 */
@Mixin(Settings.class)
public class MixinSettings {

    @Shadow @Final public Properties properties;

    @Shadow
    private String getStringRaw(String key) {
        throw new AssertionError();
    }

    // CraftBukkit - command line overrides the properties file
    @Redirect(method = "getStringRaw", at = @At(value = "INVOKE", target = "Ljava/util/Properties;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object silkard$getOverride(Properties properties, Object key) {
        String name = (String) key;
        OptionSet options = MainHook.options();
        if (options != null && options.has(name)) {
            return String.valueOf(options.valueOf(name));
        }
        return properties.getProperty(name);
    }

    // CraftBukkit - SPIGOT-7465, MC-264979: don't load if the file doesn't exist
    @Inject(method = "loadFromFile", at = @At("HEAD"), cancellable = true)
    private static void silkard$skipMissingFile(Path file, CallbackInfoReturnable<Properties> cir) {
        if (!file.toFile().exists()) {
            cir.setReturnValue(new Properties());
        }
    }

    // CraftBukkit - don't attempt writing to a read-only file
    @Inject(method = "store", at = @At("HEAD"), cancellable = true)
    private void silkard$skipReadOnlyFile(Path output, CallbackInfo ci) {
        if (output.toFile().exists() && !output.toFile().canWrite()) {
            ci.cancel();
        }
    }

    // CraftBukkit - report invalidly configured properties instead of silently failing
    @Inject(method = "get(Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Function;Ljava/lang/Object;)Ljava/lang/Object;", at = @At("HEAD"), cancellable = true)
    private void silkard$safeGet(String key, Function<String, Object> deserializer, Function<Object, String> serializer, Object defaultValue, CallbackInfoReturnable<Object> cir) {
        try {
            String value = this.getStringRaw(key);
            Object result = MoreObjects.firstNonNull(value != null ? deserializer.apply(value) : null, defaultValue);
            this.properties.put(key, serializer.apply(result));
            cir.setReturnValue(result);
        } catch (Exception ex) {
            throw new RuntimeException("Could not load invalidly configured property '" + key + "'", ex);
        }
    }
}
