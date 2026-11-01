package com.mohistmc.silkard.mixin.vanilla.server.dedicated;

import com.mohistmc.silkard.injected.server.ContextDedicatedServer;
import com.mohistmc.silkard.injected.server.ContextMinecraftServer;
import java.io.IOException;
import net.minecraft.server.rcon.thread.QueryThreadGs4;
import net.minecraft.util.debugchart.RemoteSampleLogger;
import org.jspecify.annotations.Nullable;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Mgazul
 * @date 2026/10/24
 */
// 26.3 里 getPort/isStopped/loadLevel/... 全在 MinecraftServer，@Shadow 命中不了继承成员
// → extends MinecraftServer 后按 public/protected 继承直接取用。
// priority 900 < fabric 1000：让 fabric 的 @Redirect 能注入被 @Overwrite 合并的 initServer
@Mixin(value = DedicatedServer.class, priority = 900)
public abstract class MixinDedicatedServer extends MinecraftServer implements ContextDedicatedServer, com.mohistmc.silkard.injected.commands.ContextCommandSource {

    protected MixinDedicatedServer(Thread thread, net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess levelStorageSource,
                                   net.minecraft.server.packs.repository.PackRepository packRepository, net.minecraft.server.WorldStem worldStem,
                                   java.util.Optional<net.minecraft.world.level.gamerules.GameRules> gameRules, java.net.Proxy proxy,
                                   com.mojang.datafixers.DataFixer fixerUpper, net.minecraft.server.Services services,
                                   net.minecraft.server.level.progress.LevelLoadListener levelLoadListener, boolean propagatesCrashes,
                                   net.minecraft.server.notifications.NotificationManager notificationManager) {
        super(thread, levelStorageSource, packRepository, worldStem, gameRules, proxy, fixerUpper, services, levelLoadListener,
                propagatesCrashes, notificationManager);
    }

    private static final org.slf4j.Logger silkard$LOGGER = org.slf4j.LoggerFactory.getLogger(MixinDedicatedServer.class);

    // CraftBukkit - loadPlugins 钩子见 MixinMinecraftServer（26.3 的 loadLevel 只在 MinecraftServer）

    @Override
    public DedicatedServer self() {
        return (DedicatedServer) (Object) this;
    }

    // CraftBukkit start - initServer shadows
    @org.spongepowered.asm.mixin.Shadow
    public abstract void handleConsoleInput(String msg, net.minecraft.commands.CommandSourceStack source);

    @Shadow
    @Final
    private java.util.List<net.minecraft.server.ConsoleInput> consoleInput;

    @org.spongepowered.asm.mixin.Shadow
    protected abstract boolean convertOldUsers();

    @org.spongepowered.asm.mixin.Shadow
    public abstract net.minecraft.server.dedicated.DedicatedServerProperties getProperties();

    @org.spongepowered.asm.mixin.Shadow
    public abstract String getLevelIdName();

    @org.spongepowered.asm.mixin.Shadow
    @Nullable
    private QueryThreadGs4 queryThreadGs4;

    @org.spongepowered.asm.mixin.Unique
    private Thread silkard$consoleThreadVar;

    @org.spongepowered.asm.mixin.Shadow
    @Nullable
    private RemoteSampleLogger tickTimeLogger;


    // CraftBukkit start - console thread: jline reader, ForwardLogHandler, writer thread, System redirect
    /**
     * @author Mgazul
     * @reason CraftBukkit - jline console + log redirection (CB DedicatedServer.initServer console hunk)
     */
    @Overwrite
    protected boolean initServer() throws IOException {
        // CraftBukkit start
        org.jline.terminal.Terminal terminal = ((com.mohistmc.silkard.injected.server.ContextMinecraftServer) this.self()).terminal();
        org.jline.reader.LineReader reader = org.jline.reader.LineReaderBuilder.builder()
                .terminal(terminal)
                .option(org.jline.reader.LineReader.Option.DISABLE_EVENT_EXPANSION, true)
                .completer(new org.bukkit.craftbukkit.command.ConsoleCommandCompleter())
                .build();
        // jline inserts a tab instead of completing when this option is set
        reader.unsetOpt(org.jline.reader.LineReader.Option.INSERT_TAB);
        // share the appender's reader so log output is drawn above the prompt
        final boolean appenderReader = net.minecrell.terminalconsole.TerminalConsoleAppender.getTerminal() == terminal;
        if (appenderReader) {
            net.minecrell.terminalconsole.TerminalConsoleAppender.setReader(reader);
        }
        // CraftBukkit end
        Thread thread = new Thread("Server console handler") {
            {
                Objects.requireNonNull(MixinDedicatedServer.this);
            }

            @Override
            public void run() {
                // CraftBukkit start
                if (!org.bukkit.craftbukkit.Main.useConsole) {
                    return;
                }

                // MC-33041, SPIGOT-5538: if System.in is not valid due to javaw, then return
                try {
                    System.in.available();
                } catch (IOException ex) {
                    return;
                }
                // CraftBukkit end

                String line;

                try {
                    // CraftBukkit start - JLine disabling compatibility
                    while (!MixinDedicatedServer.this.isStopped() && MixinDedicatedServer.this.isRunning()) {
                        try {
                            if (org.bukkit.craftbukkit.Main.useJline) {
                                line = reader.readLine(">", null);
                            } else {
                                line = reader.readLine();
                            }

                            if (line.trim().length() > 0) { // Trim to filter lines which are just spaces
                                MixinDedicatedServer.this.handleConsoleInput(line, MixinDedicatedServer.this.createCommandSourceStack());
                            }
                        } catch (org.jline.reader.EndOfFileException | org.jline.reader.UserInterruptException eof) {
                            // SPIGOT-5220: Throttle if EOF (ctrl^d) or stdin is /dev/null
                            try {
                                Thread.sleep(50L);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                } catch (Throwable ioexception) {
                    // CraftBukkit end
                    silkard$LOGGER.error("Exception handling console input", ioexception);
                } finally {
                    if (appenderReader) {
                        net.minecrell.terminalconsole.TerminalConsoleAppender.setReader(null);
                    }
                }

            }
        };

        // CraftBukkit start - TODO: handle command-line logging arguments
        java.util.logging.Logger global = java.util.logging.Logger.getLogger("");
        global.setUseParentHandlers(false);
        for (java.util.logging.Handler handler : global.getHandlers()) {
            global.removeHandler(handler);
        }
        global.addHandler(new org.bukkit.craftbukkit.util.ForwardLogHandler());

        final org.apache.logging.log4j.core.Logger logger = ((org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger());
        for (org.apache.logging.log4j.core.Appender appender : logger.getAppenders().values()) {
            if (appender instanceof org.apache.logging.log4j.core.appender.ConsoleAppender) {
                logger.removeAppender(appender);
            }
        }

        System.setOut(org.apache.logging.log4j.io.IoBuilder.forLogger(logger).setLevel(org.apache.logging.log4j.Level.INFO).buildPrintStream());
        System.setErr(org.apache.logging.log4j.io.IoBuilder.forLogger(logger).setLevel(org.apache.logging.log4j.Level.WARN).buildPrintStream());

        this.silkard$consoleThreadVar = thread;
        // CraftBukkit end

        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(new net.minecraft.DefaultUncaughtExceptionHandler(silkard$LOGGER));
        thread.start();
        silkard$LOGGER.info("Starting minecraft server version {}", net.minecraft.SharedConstants.getCurrentVersion().name());
        if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L) {
            silkard$LOGGER.warn("To start the server with more ram, launch it as \"java -Xmx1024M -jar minecraft_server.jar\"");
        }

        silkard$LOGGER.info("Loading properties");
        net.minecraft.server.dedicated.DedicatedServerProperties properties = this.getProperties();
        if (this.isSingleplayer()) {
            this.setLocalIp("127.0.0.1");
        } else {
            this.setUsesAuthentication(properties.onlineMode);
            this.setPreventProxyConnections(properties.preventProxyConnections);
            this.setLocalIp(properties.serverIp);
        }

        this.getWorldData().setGameType(properties.gameMode.get());
        silkard$LOGGER.info("Default game type: {}", properties.gameMode.get());
        InetAddress localAddress = null;
        if (!this.getLocalIp().isEmpty()) {
            localAddress = InetAddress.getByName(this.getLocalIp());
        }

        if (this.getPort() < 0) {
            this.setPort(properties.serverPort);
        }

        this.initializeKeyPair();
        silkard$LOGGER.info("Starting Minecraft server on {}:{}", this.getLocalIp().isEmpty() ? "*" : this.getLocalIp(), this.getPort());

        try {
            this.getConnection().startTcpServerListener(localAddress, this.getPort());
        } catch (IOException e) {
            silkard$LOGGER.warn("**** FAILED TO BIND TO PORT!");
            silkard$LOGGER.warn("The exception was: {}", e.toString());
            silkard$LOGGER.warn("Perhaps a server is already running on that port?");
            return false;
        }

        if (!this.usesAuthentication()) {
            silkard$LOGGER.warn("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
            silkard$LOGGER.warn("The server will make no attempt to authenticate usernames. Beware.");
            silkard$LOGGER.warn("While this makes the game possible to play without internet access, it also opens the ability for hackers to connect with any username they choose.");
            silkard$LOGGER.warn("To change this, set \"online-mode\" to \"true\" in the server.properties file.");
        }

        if (this.convertOldUsers()) {
            this.services().nameToIdCache().save();
        }

        if (!net.minecraft.server.players.OldUsersConverter.areOldUserlistsRemoved()) {
            return false;
        }

        this.setPlayerList(new net.minecraft.server.dedicated.DedicatedPlayerList(this.self(), this.registries(), this.playerDataStorage));
        // CraftBukkit start - loadPlugins/enablePlugins moved to MixinDedicatedServer.loadLevel HEAD
        this.tickTimeLogger = new net.minecraft.util.debugchart.RemoteSampleLogger(net.minecraft.util.debugchart.TpsDebugDimensions.values().length, this.debugSubscribers(), net.minecraft.util.debugchart.RemoteDebugSampleType.TICK_TIME);
        long levelNanoTime = net.minecraft.util.Util.getNanos();
        this.services().nameToIdCache().resolveOfflineUsers(!this.usesAuthentication());
        silkard$LOGGER.info("Preparing level \"{}\"", this.getLevelIdName());
        this.loadLevel();
        long elapsed = net.minecraft.util.Util.getNanos() - levelNanoTime;
        String time = String.format(Locale.ROOT, "%.3fs", elapsed / 1.0E9);
        silkard$LOGGER.info("Done ({})! For help, type \"help\"", time);
        if (properties.announcePlayerAchievements != null) {
            this.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SHOW_ADVANCEMENT_MESSAGES, properties.announcePlayerAchievements, this.self()); // CraftBukkit - per-world via overworld rules
        }

        if (properties.enableQuery) {
            silkard$LOGGER.info("Starting GS4 status responder");
            this.queryThreadGs4 = net.minecraft.server.rcon.thread.QueryThreadGs4.create(this.self());
        }
        // fabric-lifecycle-events 的 @Redirect 盯着这处 INVOKE，删了启动即崩
        this.notificationManager().serverStarted();
        return true;
    }

    /**
     * @author Mgazul
     * @reason CraftBukkit - fire ServerCommandEvent and drain the queue through the bukkit command map
     */
    @Overwrite
    public void handleConsoleInputs() {
        // CraftBukkit start
        ContextMinecraftServer contextServer = (ContextMinecraftServer) (Object) this;
        org.bukkit.craftbukkit.CraftServer craftServer = contextServer.silkard$server();
        org.bukkit.command.ConsoleCommandSender consoleSender = contextServer.silkard_console();

        while (!this.consoleInput.isEmpty()) {
            net.minecraft.server.ConsoleInput consoleinput = this.consoleInput.remove(0);

            if (craftServer == null || consoleSender == null) {
                this.getCommands().performPrefixedCommand(consoleinput.source, consoleinput.msg); // CraftBukkit - vanilla fallback before CraftServer is up
                continue;
            }

            org.bukkit.event.server.ServerCommandEvent event = new org.bukkit.event.server.ServerCommandEvent(consoleSender, consoleinput.msg);
            craftServer.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                continue;
            }
            consoleinput = new net.minecraft.server.ConsoleInput(event.getCommand(), consoleinput.source);

            craftServer.dispatchServerCommand(consoleSender, consoleinput);
        }
        // CraftBukkit end
    }

    @Override
    public String silkard$runCommand(net.minecraft.server.rcon.RconConsoleSource rconConsoleSource, String command) {
        // CraftBukkit start - fire RemoteServerCommandEvent, then dispatch through the bukkit command map
        rconConsoleSource.prepareForCommand();
        this.executeBlocking(() -> {
            net.minecraft.commands.CommandSourceStack wrapper = rconConsoleSource.createCommandSourceStack();
            org.bukkit.event.server.RemoteServerCommandEvent event = new org.bukkit.event.server.RemoteServerCommandEvent(((com.mohistmc.silkard.injected.server.rcon.ContextRconConsoleSource) rconConsoleSource).getBukkitSender(wrapper), command);
            org.bukkit.craftbukkit.CraftServer craftServer = ((ContextMinecraftServer) (Object) this).silkard$server();
            if (craftServer != null) {
                craftServer.getPluginManager().callEvent(event);
            }
            if (event.isCancelled()) {
                return;
            }
            if (craftServer != null) {
                craftServer.dispatchServerCommand(event.getSender(), new net.minecraft.server.ConsoleInput(event.getCommand(), wrapper));
            }
        });
        return rconConsoleSource.getCommandResponse();
        // CraftBukkit end
    }

    // CraftBukkit start - plugin list for query
    @Overwrite
    public String getPluginNames() {
        org.bukkit.craftbukkit.CraftServer craftServer = ((ContextMinecraftServer) this.self()).silkard$server();
        StringBuilder result = new StringBuilder();
        org.bukkit.plugin.Plugin[] plugins = craftServer.getPluginManager().getPlugins();

        result.append(craftServer.getName());
        result.append(" on Bukkit ");
        result.append(craftServer.getBukkitVersion());

        if (plugins.length > 0 && craftServer.getQueryPlugins()) {
            result.append(": ");

            for (int i = 0; i < plugins.length; i++) {
                if (i > 0) {
                    result.append("; ");
                }

                result.append(plugins[i].getDescription().getName());
                result.append(" ");
                result.append(plugins[i].getDescription().getVersion().replaceAll(";", ","));
            }
        }

        return result.toString();
    }

    // CraftBukkit - interrupt the console handler on shutdown
    @Inject(method = "stopServer", at = @At("TAIL"))
    private void silkard$stopConsoleThread(CallbackInfo ci) {
        Thread consoleThread = this.silkard$consoleThreadVar;
        if (consoleThread != null) {
            consoleThread.interrupt();
            try {
                consoleThread.join(50L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // CraftBukkit - code of conduct map must stay mutable
    @Inject(method = "readCodeOfConducts", at = @At("RETURN"), cancellable = true)
    private static void silkard$mutableCodeOfConducts(CallbackInfoReturnable<java.util.Map<String, String>> cir) {
        cir.setReturnValue(new java.util.LinkedHashMap<>(cir.getReturnValue()));
    }

    @Override
    public org.bukkit.command.CommandSender getBukkitSender(net.minecraft.commands.CommandSourceStack wrapper) {
        return ((ContextMinecraftServer) this.self()).silkard_console();
    }
    // CraftBukkit end

}
