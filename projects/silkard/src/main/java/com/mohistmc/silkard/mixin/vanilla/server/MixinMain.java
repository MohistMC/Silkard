package com.mohistmc.silkard.mixin.vanilla.server;

import com.llamalad7.mixinextras.sugar.Local;
import com.mohistmc.silkard.bukkit.MainHook;
import com.mojang.serialization.Dynamic;
import java.io.File;
import java.io.IOException;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SharedConstants;
import net.minecraft.server.Main;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CraftBukkit Main.patch 移植（silkard 变体）：静态槽位传递 OptionSet / DataLoadContext。
 * CB 把 main(String[]) 改为 main(OptionSet) 并在 WorldLoader.load 回调捕获 worldLoader；
 * silkard 走 Fabric 链无法改签名，改为：org.bukkit.craftbukkit.Main 预先 MainHook.configure(options)，
 * WorldLoader.load 回调 HEAD 注入捕获 DataLoadContext。
 *
 * @author Mgazul
 * @date 2026/10/24
 */
@Mixin(Main.class)
public class MixinMain {

    /**
     * vanilla Main.main HEAD：把 bukkit 选项解析结果交给 MainHook，
     * 让 MinecraftServer.silkard_options() 拿到带 bukkit 选项的 OptionSet
     * （CB Main 在 fabric 启动链上不会执行，只能从这里注入）。
     */
    @Inject(method = "main", at = @At("HEAD"))
    private static void silkard$bukkitOptions(String[] args, CallbackInfo ci) {
        MainHook.configure(org.bukkit.craftbukkit.Main.bukkitOptions(args)); // CraftBukkit
    }

    /**
     * WorldLoader.load 的 context 回调（Main.main 内 lambda）—— 捕获 DataLoadContext（CB worldLoader.set 等价）。
     */
    @Inject(method = "lambda$main$1", at = @At("HEAD"), require = 0)
    private static void silkard$captureWorldLoader(Dynamic levelDataTag, LevelStorageSource.LevelStorageAccess access, DedicatedServerSettings settings, OptionSet options, OptionSpec demo, OptionSpec bonusChest, WorldLoader.DataLoadContext context, CallbackInfoReturnable<WorldLoader.DataLoadOutput> cir) {
        MainHook.captureWorldLoader(context); // CraftBukkit
    }

    // CraftBukkit - create the bukkit datapack folder so plugins can ship data pack resources
    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/ServerPacksSource;createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;", shift = At.Shift.AFTER))
    private static void silkard$bukkitDataPack(String[] args, CallbackInfo ci, @Local LevelStorageSource.LevelStorageAccess access) {
        File bukkitDataPackFolder = new File(access.getLevelPath(LevelResource.DATAPACK_DIR).toFile(), "bukkit");
        if (!bukkitDataPackFolder.exists()) {
            bukkitDataPackFolder.mkdirs();
        }
        File mcMeta = new File(bukkitDataPackFolder, "pack.mcmeta");
        try {
            com.google.common.io.Files.write("{\n"
                    + "    \"pack\": {\n"
                    + "        \"description\": \"Data pack for resources provided by Bukkit plugins\",\n"
                    + "        \"min_format\": " + SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA) + ",\n"
                    + "        \"max_format\": " + SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA) + "\n"
                    + "    }\n"
                    + "}\n", mcMeta, Charsets.UTF_8);
        } catch (IOException exception) {
            throw new RuntimeException("Could not initialize Bukkit datapack", exception);
        }
    }

    // CraftBukkit - include the level name in the upgrade log
    @Redirect(method = "forceUpgrade", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;)V"))
    private static void silkard$forceUpgrade(Logger logger, String message, @Local(argsOnly = true) LevelStorageSource.LevelStorageAccess storageSource) {
        logger.info("Forcing world upgrade! {}", storageSource.getLevelId());
    }
}
