package com.mohistmc.silkard.injected.server;

import com.mohistmc.silkard.util.ContextStateException;
import java.util.Optional;
import joptsimple.OptionSet;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.jline.terminal.Terminal;

public interface ContextMinecraftServer {

    default void handleCustomClickAction(Identifier minecraftkey, Optional<Tag> optional, ServerPlayer player) {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$drainQueuedTasks() {
        throw new ContextStateException("Not implemented");
    }

    default Commands vanillaCommandDispatcher() {
        throw new ContextStateException("Not implemented");
    }

    default java.util.concurrent.ExecutorService silkard$chatExecutor() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$queuedProcess(Runnable runnable) {
        throw new ContextStateException("Not implemented");
    }

    default java.util.Queue<Runnable> silkard_processQueue() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setProcessQueue(java.util.Queue<Runnable> processQueue) {
        throw new ContextStateException("Not implemented");
    }

    default WorldLoader.DataLoadContext silkard_worldLoader() {
        throw new ContextStateException("Not implemented");
    }

    default CraftServer silkard$server() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setServer(CraftServer server) {
        throw new ContextStateException("Not implemented");
    }

    default OptionSet silkard_options() {
        throw new ContextStateException("Not implemented");
    }

    default ConsoleCommandSender silkard_console() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard_console(ConsoleCommandSender console) {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setTerminal(Terminal terminal) {
        throw new ContextStateException("Not implemented");
    }

    default Terminal terminal() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setRemoteConsole(RemoteConsoleCommandSender remoteConsole) {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$forceTicks(boolean forceTicks) {
        throw new ContextStateException("Not implemented");
    }

    default boolean silkard$forceTicks() {
        throw new ContextStateException("Not implemented");
    }

    default boolean isDebugging() {
        throw new ContextStateException("Not implemented");
    }

    default boolean hasStopped() {
        throw new ContextStateException("Not implemented");
    }

    default void initWorld(ServerLevel serverlevel, ServerLevelData serverleveldata, WorldData saveData, WorldOptions worldOptions) {
        throw new ContextStateException("Not implemented");
    }

    default void prepareLevels(ServerLevel serverlevel) {
        throw new ContextStateException("Not implemented");
    }

    default void addLevel(ServerLevel level) {
        throw new ContextStateException("Not implemented");
    }

    default void removeLevel(ServerLevel level) {
        throw new ContextStateException("Not implemented");
    }

    default void executeModerately() {
        throw new ContextStateException("Not implemented");
    }

    default double[] getTPS() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard$setRconConsoleSource(RconConsoleSource source) {
        throw new ContextStateException("Not implemented");
    }

    default void setServerLinks(ServerLinks serverLinks) {
        throw new ContextStateException("Not implemented");
    }

    default int silkard_autosavePeriod() {
        throw new ContextStateException("Not implemented");
    }

    default void silkard_autosavePeriod(int autosavePeriod) {
        throw new ContextStateException("Not implemented");
    }

    default int silkard$currentTick() {
        throw new ContextStateException("Not implemented");
    }
}
