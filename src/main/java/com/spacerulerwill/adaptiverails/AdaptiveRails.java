package com.spacerulerwill.adaptiverails;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;


public final class AdaptiveRails extends JavaPlugin implements Listener {
    private final Logger logger = this.getSLF4JLogger();
    private static final double DEFAULT_SPEED = 0.4;
    private static final Map<Material, Double> DEFAULT_SPEED_MULTIPLIERS = new HashMap<>();

    @SafeVarargs
    static void addTagsToDefaultSpeedMultipliers(double value, Tag<Material>... tags) {
        for (Tag<Material> tag : tags) {
            tag.getValues().forEach(material -> {
                DEFAULT_SPEED_MULTIPLIERS.put(material, value);
            });
        }
    }

    static void addMaterialsToDefaultSpeedMultipliers(double value, Material... materials) {
        for (Material material : materials) {
            DEFAULT_SPEED_MULTIPLIERS.put(material, value);
        }
    }

    static {
        // Soul sand and soil and the slowest etc
        addTagsToDefaultSpeedMultipliers(0.5, Tag.SOUL_SPEED_BLOCKS);
        // Grass and dirt are slightly slower than usual
        addTagsToDefaultSpeedMultipliers(0.85, Tag.GRASS_BLOCKS, Tag.DIRT);
        // Sandy type blocks are slower still
        addTagsToDefaultSpeedMultipliers(0.65, Tag.SAND, Tag.CONCRETE_POWDERS);
        addMaterialsToDefaultSpeedMultipliers(0.65, Material.GRAVEL, Material.SUSPICIOUS_GRAVEL);
        // Cobblestone is almost full speed - cobbles slow you down
        addMaterialsToDefaultSpeedMultipliers(0.95, Material.COBBLESTONE, Material.MOSSY_COBBLESTONE);
        // Wood logs are a slight speed boost
        addTagsToDefaultSpeedMultipliers(1.05, Tag.LOGS);
        // Copper is slightly faster initially, but if it oxidizes it gets slower
        addMaterialsToDefaultSpeedMultipliers(1.15, Material.COPPER_BLOCK, Material.WAXED_COPPER_BLOCK);
        addMaterialsToDefaultSpeedMultipliers(1.0, Material.EXPOSED_COPPER, Material.WAXED_EXPOSED_COPPER);
        addMaterialsToDefaultSpeedMultipliers(0.9, Material.WEATHERED_COPPER, Material.WAXED_WEATHERED_COPPER);
        addMaterialsToDefaultSpeedMultipliers(0.75, Material.OXIDIZED_COPPER, Material.WAXED_OXIDIZED_COPPER);
        // Iron
        addMaterialsToDefaultSpeedMultipliers(1.25, Material.IRON_BLOCK);
        // Gold
        addMaterialsToDefaultSpeedMultipliers(1.35, Material.GOLD_BLOCK);
        // Blue ice is fastest
        addMaterialsToDefaultSpeedMultipliers(1.5, Material.BLUE_ICE);
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
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
        Double speedMultiplier = DEFAULT_SPEED_MULTIPLIERS.get(belowBlock.getType());
        if (speedMultiplier == null) {
            minecart.setMaxSpeed(DEFAULT_SPEED);
        } else {
            minecart.setMaxSpeed(speedMultiplier * DEFAULT_SPEED);
        }
    }
}