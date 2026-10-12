package com.spacerulerwill.adaptiverails;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rail;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class AdaptiveRails extends JavaPlugin implements Listener {
    private final Logger logger = this.getSLF4JLogger();
    private static final double DEFAULT_SPEED = 0.4;
    private final Map<@NotNull Material, @NotNull Double> speedMultipliers = new HashMap<>();

    private int loadTags() {
        int failedToLoad = 0;
        FileConfiguration config = this.getConfig();
        ConfigurationSection tagsSection = config.getConfigurationSection("tags");
        if (tagsSection != null) {
            Set<String> keys = tagsSection.getKeys(false);
            for (String key : keys) {
                // try and parse the key as a namespaced key
                NamespacedKey namespacedKey = NamespacedKey.fromString(key);
                if (namespacedKey == null) {
                    this.logger.error("Invalid namespaced key: {}", key);
                    failedToLoad++;
                    continue;
                }
                // get a tag from the namespaced key
                Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, namespacedKey, Material.class);
                if (tag == null) {
                    this.logger.error("Tag does not exist: {}", key);
                    failedToLoad++;
                    continue;
                }
                // get its value
                double value = tagsSection.getDouble(key);
                // add to speed multipliers
                for (Material material : tag.getValues()) {
                    this.speedMultipliers.put(material, value);
                }
            }
        }
        return failedToLoad;
    }

    private int loadBlocks() {
        int failedToLoad = 0;
        FileConfiguration config = this.getConfig();
        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection != null) {
            Set<String> keys = blocksSection.getKeys(false);
            for (String key : keys) {
                // try and parse the key as a namespaced key
                NamespacedKey namespacedKey = NamespacedKey.fromString(key);
                if (namespacedKey == null) {
                    this.logger.error("Invalid namespaced key: {}.", key);
                    failedToLoad++;
                    continue;
                }
                // get our block and get its material
                Material material = Registry.MATERIAL.get(namespacedKey);
                if (material == null) {
                    this.logger.error("Block does not exist: {}.", key);
                    failedToLoad++;
                    continue;
                }
                // get its value
                double value = blocksSection.getDouble(key);
                // add to speed multipliers
                this.speedMultipliers.put(material, value);
            }
        }
        return failedToLoad;
    }

    private int reloadPluginConfig() {
        // empty our existing config
        speedMultipliers.clear();
        // load new config
        int failedTags = this.loadTags();
        int failedBlocks = this.loadBlocks();
        // if it's still empty - show a warning
        if (speedMultipliers.isEmpty()) {
            this.logger.warn("Configuration is empty and no speeds are loaded - plugin will have no effect.");
        } else {
            this.logger.info("Loaded custom speeds for {} blocks.", speedMultipliers.size());
        }
        // hopefully zero
        return failedTags + failedBlocks;
    }

    private int ArctlReload(CommandContext<CommandSourceStack> context) {
        this.logger.info("Reloading config...");
        this.reloadConfig();
        int failed = this.reloadPluginConfig();
        CommandSender sender = context.getSource().getSender();
        if (failed == 0) {
            sender.sendMessage("Config reloaded successfully.");
        } else {
            sender.sendMessage("Failed to load " + failed + " entries... check server logs!");
        }
        return Command.SINGLE_SUCCESS;
    }

    private void setupCommands() {
        LiteralCommandNode<CommandSourceStack> arctl = Commands.literal("arctl")
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().isOp())
                        .executes(this::ArctlReload))
                .build();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(arctl);
        });
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.saveDefaultConfig();
        this.logger.info("Loading config...");
        this.reloadPluginConfig();
        this.setupCommands();
        this.logger.info("Adaptive Rails is enabled.");
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Minecart minecart)) {
            return;
        }

        Location location = minecart.getLocation();
        Block block = location.getBlock();

        if (!(block.getBlockData() instanceof Rail)) {
            return;
        }

        minecart.setSlowWhenEmpty(false);
        Block belowBlock = block.getRelative(0, -1, 0);
        Double speedMultiplier = this.speedMultipliers.get(belowBlock.getType());
        if (speedMultiplier == null) {
            minecart.setMaxSpeed(DEFAULT_SPEED);
        } else {
            minecart.setMaxSpeed(speedMultiplier * DEFAULT_SPEED);
        }
    }
}