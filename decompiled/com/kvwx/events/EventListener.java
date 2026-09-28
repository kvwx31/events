/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Interaction
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.entity.ProjectileLaunchEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerItemHeldEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerToggleFlightEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package com.kvwx.events;

import com.kvwx.events.EventManager;
import com.kvwx.events.EventsPlugin;
import java.util.List;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EventListener
implements Listener {
    private final EventsPlugin plugin;

    public EventListener(EventsPlugin plugin) {
        this.plugin = plugin;
    }

    private EventManager em() {
        return this.plugin.getEventManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (this.em().getState() != EventManager.EventState.VOTING) {
            return;
        }
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Interaction)) {
            return;
        }
        UUID playerId = event.getPlayer().getUniqueId();
        EventManager.BiomeOption biome = this.em().getBiomeForPlayerInteraction(playerId, clicked.getUniqueId());
        if (biome != null) {
            event.setCancelled(true);
            this.em().castVote(event.getPlayer(), biome);
        }
    }

    @EventHandler
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Interaction) {
            UUID playerId = ((Player)event.getDamager()).getUniqueId();
            EventManager.BiomeOption biome = this.em().getBiomeForPlayerInteraction(playerId, event.getEntity().getUniqueId());
            if (biome != null && this.em().getState() == EventManager.EventState.VOTING) {
                event.setCancelled(true);
                this.em().castVote((Player)event.getDamager(), biome);
                return;
            }
        }
        if (this.em().getState() == EventManager.EventState.VOTING && this.em().isParticipant((Player)event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player p = (Player)event.getEntity();
        if (!this.em().isParticipant(p)) {
            return;
        }
        EventManager.EventState state = this.em().getState();
        if (state == EventManager.EventState.VOTING || state == EventManager.EventState.COUNTDOWN) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (this.em().getState() != EventManager.EventState.VOTING) {
            return;
        }
        Player p = event.getPlayer();
        if (!this.em().isParticipant(p)) {
            return;
        }
        Location center = this.em().getVotingCenter(p);
        if (center == null) {
            return;
        }
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (to.getX() != center.getX() || to.getY() != center.getY() || to.getZ() != center.getZ()) {
            Location fixed = new Location(center.getWorld(), center.getX(), center.getY(), center.getZ(), to.getYaw(), to.getPitch());
            event.setTo(fixed);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (!this.em().isParticipant(p)) {
            return;
        }
        EventManager.EventState state = this.em().getState();
        if (state != EventManager.EventState.ACTIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (!this.em().isParticipant(p)) {
            return;
        }
        EventManager.EventState state = this.em().getState();
        if (state != EventManager.EventState.ACTIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        if (!this.em().isParticipant(p)) {
            return;
        }
        String[] parts = event.getMessage().substring(1).split(" ");
        String cmd = parts[0].toLowerCase();
        if (cmd.equals("event") || cmd.equals("hub") || cmd.equals("stopevent") || cmd.equals("forcestart") || cmd.equals("shrink") || cmd.equals("drop") || cmd.equals("addtime") || cmd.equals("removetime") || cmd.equals("createevents")) {
            return;
        }
        EventManager.EventState state = this.em().getState();
        List allowed = state == EventManager.EventState.ACTIVE ? this.plugin.getConfig().getStringList("allowed-commands-during") : this.plugin.getConfig().getStringList("allowed-commands-before");
        if (!allowed.contains(cmd)) {
            event.setCancelled(true);
            p.sendMessage("\u00a7c[Event] You can't use that command during the event!");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if (!this.em().isParticipant(p)) {
            return;
        }
        event.getDrops().clear();
        p.setGameMode(GameMode.SPECTATOR);
        this.em().getSpectators().add(p.getUniqueId());
        p.sendMessage("\u00a77[Event] You died! You are now spectating. Use /hub to leave.");
        this.em().checkSurvivors();
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) {
            return;
        }
        Player p = (Player)event.getEntity().getShooter();
        if (!this.em().isParticipant(p)) {
            return;
        }
        if (this.em().getState() != EventManager.EventState.ACTIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        ItemMeta meta;
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (item != null && this.em().isParticipant(event.getPlayer()) && (meta = item.getItemMeta()) != null) {
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player p = event.getPlayer();
        if (this.em().getState() == EventManager.EventState.VOTING && this.em().isParticipant(p) && !event.isFlying()) {
            event.setCancelled(true);
            p.setFlying(true);
        }
    }
}

