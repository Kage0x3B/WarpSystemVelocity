package de.codingair.warpsystem.spigot.api.blocks.utils;

import de.codingair.codingapi.API;
import de.codingair.codingapi.utils.Removable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class Block implements Removable {
    private final UUID uniqueId = UUID.randomUUID();
    private Location location;

    public Block(Location location) {
        this.location = location;
        API.addRemovable(this);
    }

    public abstract void create();

    @Override
    public void destroy() {
        API.removeRemovable(this);
        location.getBlock().setType(Material.AIR);
    }

    @Override
    public Player getPlayer() {
        return null;
    }

    @Override
    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public Location getLocation() {
        return location;
    }
}
