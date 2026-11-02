package com.mohistmc.silkard.commands;

import com.mohistmc.silkard.api.ColorAPI;
import com.mohistmc.silkard.api.ItemAPI;
import com.mohistmc.silkard.api.WorldAPI;
import com.mohistmc.silkard.api.gui.DemoGUI;
import com.mohistmc.silkard.api.gui.GUIItem;
import com.mohistmc.silkard.api.gui.ItemStackFactory;
import com.mohistmc.silkard.util.I18n;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ShowsCommand extends Command {

    private final List<String> params = List.of("sound", "entitys", "blockentitys");

    public ShowsCommand(String name) {
        super(name);
        this.description = "Silkard shows commands";
        this.usageMessage = "/shows [sound|entitys|blockentitys]";
        this.setPermission("silkard.command.shows");
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1 && (sender.isOp() || testPermission(sender))) {
            for (String param : params) {
                if (param.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(param);
                }
            }
        }

        return list;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(I18n.as("commands.usage", usageMessage));
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(I18n.as("error.notplayer"));
            return false;
        }

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "sound" -> {
                DemoGUI wh = new DemoGUI(I18n.as("shows.sound"));

                Map<String, List<Sound>> soundsByNamespace = new HashMap<>();
                for (Sound s : Sound.values()) {
                    NamespacedKey key = s.getKey();
                    soundsByNamespace.computeIfAbsent(key.getNamespace(), k -> new ArrayList<>()).add(s);
                }

                wh.setItem(47, new GUIItem(new ItemStackFactory(Material.REDSTONE)
                    .setDisplayName(I18n.as("shows.sound.stopall"))
                    .build()) {
                    @Override
                    public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                        u.stopAllSounds();
                    }
                });
                for (Map.Entry<String, List<Sound>> entry : soundsByNamespace.entrySet()) {
                    String namespace = entry.getKey();
                    List<Sound> sounds = entry.getValue();

                    wh.addItem(new GUIItem(new ItemStackFactory(Material.CHEST)
                        .setDisplayName("§b" + namespace + " §7(" + sounds.size() + ")")
                        .build()) {
                        @Override
                        public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                            openSoundCategoryGUI(u, namespace, sounds);
                        }
                    });
                }
                wh.openGUI(player);
                return true;
            }
            case "entitys" -> {

                Map<net.minecraft.world.entity.EntityType<?>, Integer> collect =
                    StreamSupport.stream(WorldAPI.getServerLevel(player.getWorld()).getAllEntities().spliterator(), false)
                        .collect(Collectors.toMap(
                            net.minecraft.world.entity.Entity::getType,
                            entity -> 1,
                            Integer::sum
                        ));

                Map<net.minecraft.world.entity.EntityType<?>, Map<String, Integer>> entityChunkCount = new HashMap<>();

                StreamSupport.stream(WorldAPI.getServerLevel(player.getWorld()).getAllEntities().spliterator(), false)
                    .forEach(entity -> {
                        net.minecraft.world.entity.EntityType<?> type = entity.getType();
                        var v = ((com.mohistmc.silkard.injected.world.entity.ContextEntity) entity).getBukkitEntity().getLocation();
                        long chunkX = (long) v.getX() >> 4;
                        long chunkZ = (long) v.getZ() >> 4;
                        String chunkKey = chunkX + "," + chunkZ;

                        entityChunkCount.computeIfAbsent(type, k -> new HashMap<>())
                            .merge(chunkKey, 1, Integer::sum);
                    });

                List<Map.Entry<net.minecraft.world.entity.EntityType<?>, Integer>> infoIds = new ArrayList<>(collect.entrySet());
                infoIds.sort((o1, o2) -> o2.getValue() - o1.getValue());

                LinkedHashMap<net.minecraft.world.entity.EntityType<?>, Integer> newMap = new LinkedHashMap<>();
                AtomicInteger allSize = new AtomicInteger(0);
                for (Map.Entry<net.minecraft.world.entity.EntityType<?>, Integer> entity : infoIds) {
                    newMap.put(entity.getKey(), entity.getValue());
                    allSize.addAndGet(entity.getValue());
                }

                DemoGUI wh = new DemoGUI(I18n.as("shows.entitys.title", allSize.getAndSet(0)));
                for (Map.Entry<net.minecraft.world.entity.EntityType<?>, Integer> s : newMap.entrySet()) {

                    String topChunk = "";
                    int maxCount = 0;
                    if (entityChunkCount.containsKey(s.getKey())) {
                        for (Map.Entry<String, Integer> chunkEntry : entityChunkCount.get(s.getKey()).entrySet()) {
                            if (chunkEntry.getValue() > maxCount) {
                                maxCount = chunkEntry.getValue();
                                topChunk = chunkEntry.getKey();
                            }
                        }
                    }

                    String finalTopChunk = topChunk;
                    int finalMaxCount = maxCount;
                    wh.addItem(new GUIItem(new ItemStackFactory(ItemAPI.getEggMaterial(s.getKey()))
                                   .setLore(List.of(
                                       "§7====================",
                                       I18n.as("shows.entitys.item.name", s.getValue()),
                                       I18n.as("shows.entitys.item.entity", net.minecraft.world.entity.EntityType.getKey(s.getKey())),
                                       I18n.as("shows.entitys.item.chunk", finalTopChunk, finalMaxCount),
                                       "",
                                       I18n.as("shows.entitys.item.click"),
                                       "§7===================="
                                   ))
                                   .build()) {
                                   @Override
                                   public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                       teleportToChunk(u, finalTopChunk, finalMaxCount);
                                   }
                               }
                    );
                }
                wh.openGUI(player);
                return true;
            }
            case "blockentitys" -> {

                Map<Material, Map<String, Integer>> blockEntityChunkCount = new HashMap<>();
                Map<Material, Integer> collect = new HashMap<>();
                for (org.bukkit.Chunk chunk : player.getWorld().getLoadedChunks()) {
                    for (BlockState state : chunk.getTileEntities()) {
                        Material material = state.getType();
                        if (material.isAir()) continue;
                        collect.merge(material, 1, Integer::sum);
                        String chunkKey = (state.getX() >> 4) + "," + (state.getZ() >> 4);
                        blockEntityChunkCount.computeIfAbsent(material, k -> new HashMap<>())
                            .merge(chunkKey, 1, Integer::sum);
                    }
                }

                List<Map.Entry<Material, Integer>> infoIds = new ArrayList<>(collect.entrySet());
                infoIds.sort((o1, o2) -> o2.getValue() - o1.getValue());

                LinkedHashMap<Material, Integer> newMap = new LinkedHashMap<>();
                AtomicInteger allSize = new AtomicInteger(0);
                for (Map.Entry<Material, Integer> entity : infoIds) {
                    newMap.put(entity.getKey(), entity.getValue());
                    allSize.addAndGet(entity.getValue());
                }

                DemoGUI wh = new DemoGUI(I18n.as("shows.blockentitys.title", allSize.getAndSet(0)));
                for (Map.Entry<Material, Integer> s : newMap.entrySet()) {

                    String topChunk = "";
                    int maxCount = 0;
                    if (blockEntityChunkCount.containsKey(s.getKey())) {
                        for (Map.Entry<String, Integer> chunkEntry : blockEntityChunkCount.get(s.getKey()).entrySet()) {
                            if (chunkEntry.getValue() > maxCount) {
                                maxCount = chunkEntry.getValue();
                                topChunk = chunkEntry.getKey();
                            }
                        }
                    }

                    String finalTopChunk = topChunk;
                    int finalMaxCount = maxCount;

                    wh.addItem(new GUIItem(new ItemStackFactory(s.getKey())
                                   .setLore(List.of(
                                       "§7====================",
                                       I18n.as("shows.entitys.item.name", s.getValue()),
                                       I18n.as("shows.blockentitys.item.entity", s.getKey()),
                                       I18n.as("shows.entitys.item.chunk", finalTopChunk, finalMaxCount),
                                       "",
                                       I18n.as("shows.entitys.item.click"),
                                       "§7===================="
                                   ))
                                   .build()) {
                                   @Override
                                   public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                                       teleportToChunk(u, finalTopChunk, finalMaxCount);
                                   }
                               }
                    );
                }
                wh.openGUI(player);
                return true;
            }
            default -> {
                sender.sendMessage(I18n.as("commands.usage", usageMessage));
                return false;
            }
        }
    }

    private void teleportToChunk(Player u, String topChunk, int maxCount) {
        if (topChunk.isEmpty()) {
            u.sendMessage(I18n.as("shows.entitys.chunk.notfound"));
            return;
        }
        String[] coords = topChunk.split(",");
        if (coords.length != 2) {
            return;
        }
        try {
            int chunkX = Integer.parseInt(coords[0]);
            int chunkZ = Integer.parseInt(coords[1]);
            u.teleport(u.getWorld().getHighestBlockAt(chunkX * 16 + 8, chunkZ * 16 + 8).getLocation().add(0.5, 1, 0.5));
            u.sendMessage(I18n.as("shows.entitys.teleport.success", chunkX, chunkZ, maxCount));
        } catch (NumberFormatException e) {
            u.sendMessage(I18n.as("shows.entitys.teleport.error"));
        }
    }

    private void openSoundCategoryGUI(Player player, String namespace, List<Sound> sounds) {
        DemoGUI categoryGUI = new DemoGUI(I18n.as("shows.sound.category.title", namespace));

        categoryGUI.setItem(47, new GUIItem(new ItemStackFactory(Material.ARROW)
            .setDisplayName(I18n.as("shows.sound.back"))
            .build()) {
            @Override
            public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                execute(u, "shows", new String[]{"sound"});
            }
        });

        for (Sound s : sounds) {
            categoryGUI.addItem(new GUIItem(new ItemStackFactory(Material.NOTE_BLOCK)
                .setDisplayName(s.name())
                .build()) {
                @Override
                public void ClickAction(ClickType type, Player u, ItemStack itemStack) {
                    if (type == ClickType.LEFT) {
                        player.playSound(player.getLocation(), s, 1f, 1.0f);
                    } else if (type == ClickType.RIGHT) {
                        u.sendMessage(ColorAPI.string(I18n.as("shows.sound.copy", s.name())));
                    }
                }
            });
        }

        categoryGUI.openGUI(player);
    }
}
