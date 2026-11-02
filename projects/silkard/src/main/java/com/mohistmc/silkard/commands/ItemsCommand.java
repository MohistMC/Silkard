package com.mohistmc.silkard.commands;

import com.mohistmc.silkard.api.ItemAPI;
import com.mohistmc.silkard.api.PlayerAPI;
import com.mohistmc.silkard.api.gui.DemoGUI;
import com.mohistmc.silkard.api.gui.GUIItem;
import com.mohistmc.silkard.feature.item.ItemsConfig;
import com.mohistmc.silkard.util.I18n;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class ItemsCommand extends Command {

    private final List<String> params = Arrays.asList("name", "save", "remove", "list", "get", "modeldata", "lore", "attribute", "unattribute", "rarity", "give");

    public ItemsCommand(String name) {
        super(name);
        this.description = I18n.as("itemscmd.description");
        this.usageMessage = "/items [name|save|list|get|give|remove|lore|attribute|unattribute|rarity|modeldata]";
        this.setPermission("silkard.command.items");
    }

    public static void info(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        ItemMeta meta = itemStack.getItemMeta();

        PlayerAPI.sendMessageByCopy(player, I18n.as("items.info.type", itemStack.getType().name()), itemStack.getType().name());
        PlayerAPI.sendMessageByCopy(player, I18n.as("items.info.key", itemStack.getType().getKey().toString()), itemStack.getType().getKey().toString());
        PlayerAPI.sendMessageByCopy(player, I18n.as("items.info.name", nmsItem.getHoverName().getString()), nmsItem.getHoverName().getString());
        player.sendMessage(I18n.as("items.info.moditem", !"minecraft".equals(itemStack.getType().getKey().getNamespace())));
        player.sendMessage(I18n.as("items.info.modblock", !"minecraft".equals(itemStack.getType().getKey().getNamespace())));
        if (meta != null && meta.hasCustomModelData()) {
            player.sendMessage(I18n.as("items.info.custommodeldata", meta.getCustomModelData()));
        }
        if (meta != null) {
            player.sendMessage(I18n.as("items.info.itemflags", meta.getItemFlags()));
        }
        player.sendMessage(I18n.as("items.info.component_click"));

        player.sendMessage(I18n.as("items.info.amount", itemStack.getAmount()));
        player.sendMessage(I18n.as("items.info.maxstacksize", itemStack.getMaxStackSize()));

        if (itemStack.hasItemMeta() && meta != null) {
            player.sendMessage(I18n.as("items.info.hasitemmeta", true));

            if (meta.hasDisplayName()) {
                PlayerAPI.sendMessageByCopy(player, I18n.as("items.info.displayname", meta.getDisplayName()), meta.getDisplayName());
            }

            if (meta.hasLore()) {
                player.sendMessage(I18n.as("items.info.lore"));
                meta.getLore().forEach(lore -> player.sendMessage("  " + lore));
            }

            if (meta.hasEnchants()) {
                player.sendMessage(I18n.as("items.info.enchants"));
                meta.getEnchants().forEach((enchant, level) ->
                    player.sendMessage("  " + enchant.getKey().toString() + " - " + level));
            }
        } else {
            player.sendMessage(I18n.as("items.info.hasitemmeta", false));
        }

        if (itemStack.getType().getMaxDurability() > 0) {
            player.sendMessage(I18n.as("items.info.durability",
                itemStack.getType().getMaxDurability() - itemStack.getDurability(),
                itemStack.getType().getMaxDurability()));
        }
        var base64 = ItemAPI.base64(itemStack);
        if (base64 != null) {
            PlayerAPI.sendMessageByCopy(player, "#灰色" + "Base64 - " + base64.substring(0, 20) + "......", base64);
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (!sender.isOp() && !testPermission(sender)) {
            return new ArrayList<>();
        }

        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            for (String param : params) {
                if (param.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(param);
                }
            }
        } else if (args.length == 2) {
            switch (args[0]) {
                case "get", "remove" -> {
                    return ItemsConfig.INSTANCE.getItemStrings();
                }
                case "rarity" -> {
                    return Arrays.stream(ItemRarity.values()).map(rarity -> rarity.name().toLowerCase(Locale.ROOT)).collect(Collectors.toList());
                }
                case "lore" -> {
                    return Arrays.asList("add", "set", "remove");
                }
                case "attribute", "unattribute" -> {
                    String partialAttribute = args[1].toLowerCase();
                    for (org.bukkit.attribute.Attribute attr : org.bukkit.attribute.Attribute.values()) {
                        String name = attr.name().toLowerCase();
                        if (name.startsWith(partialAttribute)) {
                            list.add(name.replace("generic_", ""));
                        }
                    }
                }
                case "give" -> {
                    if (args[1].isEmpty()) {
                        return org.bukkit.Bukkit.getOnlinePlayers().stream()
                            .map(org.bukkit.entity.Player::getName)
                            .collect(Collectors.toList());
                    } else {
                        return org.bukkit.Bukkit.getOnlinePlayers().stream()
                            .map(org.bukkit.entity.Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                    }
                }
                default -> {
                }
            }
        } else if (args.length == 3 && args[0].equals("attribute")) {
            list.addAll(Arrays.asList("1.0", "2.0", "5.0", "10.0", "-1.0"));
        } else if (args.length == 3 && args[0].equals("give")) {
            return ItemsConfig.INSTANCE.getItemStrings();
        } else if (args.length == 4 && args[0].equals("attribute")) {
            String partialSlot = args[3].toLowerCase();
            List<String> slots = new ArrayList<>();
            slots.add("ANY");
            for (org.bukkit.inventory.EquipmentSlot slot : org.bukkit.inventory.EquipmentSlot.values()) {
                slots.add(slot.name());
            }
            for (String slot : slots) {
                if (slot.toLowerCase().startsWith(partialSlot)) {
                    list.add(slot);
                }
            }
        } else if (args.length == 3 && args[0].equals("lore") &&
            (args[1].equals("set") || args[1].equals("remove")) &&
            sender instanceof Player player) {
            ItemStack itemStack = player.getInventory().getItemInMainHand();
            if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()) {
                List<String> loreLines = itemStack.getItemMeta().getLore();
                if (loreLines != null) {
                    for (int i = 0; i < loreLines.size(); i++) {
                        list.add(String.valueOf(i));
                    }
                }
            }
        } else if (args.length == 4 && args[0].equals("lore") &&
            args[1].equals("set") && sender instanceof Player player) {
            try {
                int line = Integer.parseInt(args[2]);
                ItemStack itemStack = player.getInventory().getItemInMainHand();
                if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()) {
                    List<String> loreLines = itemStack.getItemMeta().getLore();
                    if (loreLines != null && line >= 0 && line < loreLines.size()) {
                        list.add(loreLines.get(line).replace('§', '&'));
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return list;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!testPermission(sender)) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(I18n.as("error.notplayer"));
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(I18n.as("itemscmd.usage", usageMessage));
            return false;
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "name" -> {
                if (itemStack.getType().isAir()) {
                    player.sendMessage(I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }
                if (args.length != 2) {
                    sender.sendMessage(I18n.as("itemscmd.usage", "/items name <string>"));
                    return false;
                }
                ItemAPI.name(itemStack, args[1]);
                sender.sendMessage(I18n.as("itemscmd.completeSet"));
                return true;
            }
            case "rarity" -> {
                if (itemStack.getType().isAir()) {
                    player.sendMessage(I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }
                if (args.length != 2) {
                    sender.sendMessage(I18n.as("itemscmd.usage", "/items rarity <rarity>"));
                    return false;
                }
                ItemAPI.rarity(itemStack, ItemRarity.valueOf(args[1]));
                sender.sendMessage(I18n.as("itemscmd.completeRarity"));
                return true;
            }
            case "save" -> {
                if (itemStack.getType().isAir()) {
                    player.sendMessage(I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }
                if (args.length != 2) {
                    sender.sendMessage(I18n.as("itemscmd.usage", "/items save <name>"));
                    return false;
                }
                ItemsConfig.INSTANCE.put("items." + args[1], itemStack);
                sender.sendMessage(I18n.as("itemscmd.itemSavedSuccessfully"));
                return true;
            }
            case "lore" -> {
                if (itemStack.getType().isAir()) {
                    player.sendMessage(I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }

                if (args.length < 2) {
                    sender.sendMessage(I18n.as("itemscmd.usage", "/items lore <add|set|remove> [line] [text...]"));
                    return false;
                }

                switch (args[1].toLowerCase()) {
                    case "add" -> {
                        if (args.length < 3) {
                            sender.sendMessage(I18n.as("itemscmd.usage", "/items lore add [line] <text...>"));
                            return false;
                        }

                        List<String> currentLore = itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()
                            ? new ArrayList<>(itemStack.getItemMeta().getLore())
                            : new ArrayList<>();

                        // An all-digit second argument means an insert position, otherwise it is text
                        int insertLine = -1;
                        String loreText;
                        try {
                            insertLine = Integer.parseInt(args[2]);
                            if (args.length < 4) {
                                sender.sendMessage(I18n.as("itemscmd.usage", "/items lore add [line] <text...>"));
                                return false;
                            }
                            loreText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                        } catch (NumberFormatException e) {
                            loreText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                        }

                        String[] lines = loreText.split("\\\\n");

                        if (insertLine >= 0) {
                            if (insertLine > currentLore.size()) {
                                sender.sendMessage(I18n.as("itemscmd.lineNumberOutOfRange"));
                                return false;
                            }
                            for (int i = lines.length - 1; i >= 0; i--) {
                                currentLore.add(insertLine, lines[i]);
                            }
                        } else {
                            Collections.addAll(currentLore, lines);
                        }

                        ItemAPI.lore(itemStack, currentLore);
                        sender.sendMessage(I18n.as("itemscmd.completeLore"));
                        return true;
                    }

                    case "set" -> {
                        if (args.length < 4) {
                            sender.sendMessage(I18n.as("itemscmd.usage", "/items lore set <line> <text...>"));
                            return false;
                        }

                        try {
                            int line = Integer.parseInt(args[2]);
                            if (line < 0) {
                                sender.sendMessage(I18n.as("itemscmd.lineNumberMustBePositive"));
                                return false;
                            }

                            String loreText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                            List<String> currentLore = itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()
                                ? new ArrayList<>(itemStack.getItemMeta().getLore())
                                : new ArrayList<>();

                            while (currentLore.size() <= line) {
                                currentLore.add("");
                            }

                            currentLore.set(line, loreText);
                            ItemAPI.lore(itemStack, currentLore);
                            sender.sendMessage(I18n.as("itemscmd.completeLore"));
                            return true;
                        } catch (NumberFormatException e) {
                            sender.sendMessage(I18n.as("itemscmd.invalidLineNumber", args[2]));
                            return false;
                        }
                    }

                    case "remove" -> {
                        if (args.length != 3) {
                            sender.sendMessage(I18n.as("itemscmd.usage", "/items lore remove <line>"));
                            return false;
                        }

                        try {
                            int line = Integer.parseInt(args[2]);
                            if (line < 0) {
                                sender.sendMessage(I18n.as("itemscmd.lineNumberMustBePositive"));
                                return false;
                            }

                            if (!itemStack.hasItemMeta() || !itemStack.getItemMeta().hasLore()) {
                                sender.sendMessage(I18n.as("itemscmd.itemHasNoLore"));
                                return false;
                            }

                            List<String> currentLore = new ArrayList<>(itemStack.getItemMeta().getLore());
                            if (line >= currentLore.size()) {
                                sender.sendMessage(I18n.as("itemscmd.lineNumberOutOfRange"));
                                return false;
                            }

                            currentLore.remove(line);
                            ItemAPI.lore(itemStack, currentLore);
                            sender.sendMessage(I18n.as("itemscmd.completeLore"));
                            return true;
                        } catch (NumberFormatException e) {
                            sender.sendMessage(I18n.as("itemscmd.invalidLineNumber", args[2]));
                            return false;
                        }
                    }

                    default -> {
                        sender.sendMessage(I18n.as("itemscmd.usage", "/items lore <add|set|remove> [line] [text...]"));
                        return false;
                    }
                }
            }
            case "modeldata" -> {
                if (itemStack.getType().isAir()) {
                    player.sendMessage(I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }
                if (args.length != 2 || !args[1].matches("[0-9]+")) {
                    sender.sendMessage(I18n.as("itemscmd.usage", "/items modeldata <number>"));
                    return false;
                }
                ItemAPI.customModelData(itemStack, Integer.parseInt(args[1]));
                sender.sendMessage(I18n.as("itemscmd.modelDataSetSuccess", args[1]));
                return true;
            }
            case "get" -> {
                if (args.length != 2) {
                    sender.sendMessage(I18n.as("itemscmd.usage", "/items get <name>"));
                    return false;
                }
                ItemStack itemToGive = ItemsConfig.INSTANCE.get(args[1]);
                if (itemToGive.getType().isAir()) {
                    sender.sendMessage(I18n.as("itemscmd.itemNotFound"));
                    return false;
                }
                if (player.getInventory().firstEmpty() == -1) {
                    sender.sendMessage(I18n.as("itemscmd.inventoryFull"));
                    return false;
                }
                player.getInventory().addItem(itemToGive);
                return true;
            }
            case "give" -> {
                if (args.length < 3 || args.length > 4) {
                    sender.sendMessage(I18n.as("itemscmd.usage", "/items give <player> <name> [amount]"));
                    return false;
                }

                Player targetPlayer = org.bukkit.Bukkit.getPlayerExact(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage(I18n.as("silkardcmd.heal.playernotfound", args[1]));
                    return false;
                }

                ItemStack itemToGive = ItemsConfig.INSTANCE.get(args[2]);
                if (itemToGive.getType().isAir()) {
                    sender.sendMessage(I18n.as("itemscmd.itemNotFound"));
                    return false;
                }

                int amount = 1;
                if (args.length == 4) {
                    try {
                        amount = Integer.parseInt(args[3]);
                        if (amount <= 0) {
                            sender.sendMessage(I18n.as("itemscmd.amountMustBePositive"));
                            return false;
                        }
                        if (amount > itemToGive.getMaxStackSize()) {
                            sender.sendMessage(I18n.as("itemscmd.amountTooLarge", itemToGive.getMaxStackSize()));
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(I18n.as("itemscmd.invalidAmount"));
                        return false;
                    }
                }

                ItemStack itemWithAmount = itemToGive.clone();
                itemWithAmount.setAmount(amount);

                if (targetPlayer.getInventory().firstEmpty() == -1) {
                    targetPlayer.sendMessage(I18n.as("itemscmd.inventoryFull"));
                    return false;
                }
                targetPlayer.getInventory().addItem(itemWithAmount);
                sender.sendMessage(I18n.as("itemscmd.giveSuccess", amount, args[2], args[1]));
                return true;
            }
            case "remove" -> {
                if (args.length != 2) {
                    sender.sendMessage(I18n.as("itemscmd.usage", "/items remove <name>"));
                    return false;
                }
                ItemsConfig.INSTANCE.remove(args[1]);
                sender.sendMessage(I18n.as("itemscmd.removedItemp", args[1]));
                return true;
            }
            case "list" -> {
                DemoGUI wh = new DemoGUI(I18n.as("itemscmd.customizeItemsGUI"));
                for (ItemStack s : ItemsConfig.INSTANCE.getItems()) {
                    wh.addItem(new GUIItem(s) {
                        @Override
                        public void ClickAction(ClickType type, Player u, ItemStack itemStack1) {
                            if (player.getInventory().firstEmpty() != -1) {
                                player.getInventory().addItem(itemStack1);
                            }
                        }
                    });
                }
                wh.openGUI(player);
                return true;
            }
            case "attribute" -> {
                if (itemStack.getType().isAir()) {
                    player.sendMessage(I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }

                if (args.length < 3 || args.length > 4) {
                    sender.sendMessage(I18n.as("itemscmd.usage", "/items attribute <attribute> <value> [slot]"));
                    return false;
                }

                String attributeName = args[1].toUpperCase();
                org.bukkit.attribute.Attribute attribute = getAttributeFromString(attributeName);
                if (attribute == null) {
                    sender.sendMessage(I18n.as("itemscmd.invalidAttribute"));
                    return false;
                }

                double value;
                try {
                    value = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(I18n.as("itemscmd.invalidValue"));
                    return false;
                }

                org.bukkit.inventory.EquipmentSlot slot = org.bukkit.inventory.EquipmentSlot.HAND;
                String slotName = "HAND";
                if (args.length == 4) {
                    String slotInput = args[3].toUpperCase();
                    if (slotInput.equals("ANY") || slotInput.equals("ALL")) {
                        slot = null;
                        slotName = "ANY";
                    } else {
                        try {
                            slot = org.bukkit.inventory.EquipmentSlot.valueOf(slotInput);
                            slotName = slot.name();
                        } catch (IllegalArgumentException e2) {
                            sender.sendMessage(I18n.as("itemscmd.invalidSlot"));
                            return false;
                        }
                    }
                }

                ItemAPI.attribute(itemStack, attribute, value, slot);
                sender.sendMessage(I18n.as("itemscmd.attributeSuccess", attribute.name(), String.valueOf(value), slotName));
                return true;
            }
            case "unattribute" -> {
                if (itemStack.getType().isAir()) {
                    player.sendMessage(I18n.as("itemscmd.mainhandEmpty"));
                    return false;
                }

                if (args.length != 2) {
                    sender.sendMessage(I18n.as("itemscmd.usage", "/items unattribute <attribute>"));
                    return false;
                }

                String attributeName = args[1].toUpperCase();
                org.bukkit.attribute.Attribute attribute = getAttributeFromString(attributeName);
                if (attribute == null) {
                    sender.sendMessage(I18n.as("itemscmd.invalidAttribute"));
                    return false;
                }

                boolean removed = ItemAPI.removeAttribute(itemStack, attribute);
                if (removed) {
                    sender.sendMessage(I18n.as("itemscmd.attributeRemoveSuccess", attribute.name()));
                } else {
                    sender.sendMessage(I18n.as("itemscmd.attributeNotFound"));
                }
                return true;
            }
            default -> {
                sender.sendMessage(I18n.as("itemscmd.usage", usageMessage));
                return false;
            }
        }
    }

    private org.bukkit.attribute.Attribute getAttributeFromString(String input) {
        try {
            return org.bukkit.attribute.Attribute.valueOf(input);
        } catch (IllegalArgumentException ex) {
            if (!input.startsWith("GENERIC_")) {
                try {
                    return org.bukkit.attribute.Attribute.valueOf("GENERIC_" + input);
                } catch (IllegalArgumentException ignored) {
                }
            }
            String normalized = input.replace("GENERIC_", "").replace(" ", "_").toUpperCase();
            for (org.bukkit.attribute.Attribute attr : org.bukkit.attribute.Attribute.values()) {
                String attrName = attr.name().replace("GENERIC_", "");
                if (attrName.equals(normalized)) {
                    return attr;
                }
            }
            return null;
        }
    }
}
