package com.kvwx.events;

import com.kvwx.events.EventManager;
import com.kvwx.events.EventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandManager
implements CommandExecutor {
    private final EventsPlugin plugin;

    public CommandManager(EventsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        EventManager em = this.plugin.getEventManager();
        String commandName = cmd.getName().toLowerCase();
        
        if ("set-regen".equals(commandName)) {
            if (args.length < 1) {
                sender.sendMessage("\u00a7cUsage: /set-regen <worldName>");
                return true;
            }
            String worldName = args[0];
            boolean saved = this.plugin.getWorldRegenManager().saveSnapshot(worldName);
            if (saved) {
                sender.sendMessage("\u00a7a[Regen] Saved snapshot for " + worldName + " to disk.");
            } else {
                sender.sendMessage("\u00a7c[Regen] Could not save a snapshot for " + worldName + ". Make sure the world exists and is loaded.");
            }
            return true;
        }
        
        if ("regen".equals(commandName)) {
            if (args.length < 1) {
                sender.sendMessage("\u00a7cUsage: /regen <worldName>");
                return true;
            }
            String worldName = args[0];
            boolean restored = this.plugin.getWorldRegenManager().restoreWorld(worldName);
            if (restored) {
                sender.sendMessage("\u00a7a[Regen] Restored world " + worldName + " from backup.");
            } else {
                sender.sendMessage("\u00a7c[Regen] No saved snapshot exists for " + worldName + ". Use /set-regen first.");
            }
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player p = (Player)sender;
        switch (cmd.getName().toLowerCase()) {
            case "event": {
                if (em.getState() == EventManager.EventState.IDLE) {
                    p.sendMessage("\u00a7c[Event] No event is currently running.");
                    return true;
                }
                boolean diffServer = this.plugin.getConfig().getBoolean("different-server.enabled", false);
                if (diffServer && !em.isParticipant(p)) {
                    String serverIp = this.plugin.getConfig().getString("different-server.server-ip", "event-server");
                    this.plugin.sendToServer(p, serverIp);
                } else if (!em.isParticipant(p)) {
                    em.addParticipant(p);
                    if (em.getState() == EventManager.EventState.VOTING) {
                        em.createVotingBlocksForPlayer(p);
                    }
                } else {
                    p.sendMessage("\u00a7c[Event] You already joined the event!");
                }
                return true;
            }
            case "createevents": {
                this.plugin.getGuiManager().openMainGui(p);
                return true;
            }
            case "forcestart": {
                if (em.getState() == EventManager.EventState.IDLE) {
                    p.sendMessage("\u00a7c[Event] No event countdown running.");
                    return true;
                }
                p.sendMessage("\u00a7a[Event] Force starting...");
                em.forceStart();
                return true;
            }
            case "shrink": {
                if (args.length < 1) {
                    p.sendMessage("\u00a7cUsage: /shrink <blocks>");
                    return true;
                }
                try {
                    em.shrinkBorder(Integer.parseInt(args[0]));
                }
                catch (NumberFormatException e) {
                    p.sendMessage("\u00a7cInvalid number.");
                }
                return true;
            }
            case "drop": {
                if (args.length < 1 || !args[0].equalsIgnoreCase("deepslate") && !args[0].equalsIgnoreCase("bedrock")) {
                    p.sendMessage("\u00a7cUsage: /drop <deepslate|bedrock>");
                    return true;
                }
                em.dropBlocks(args[0].toLowerCase());
                return true;
            }
            case "hub": {
                em.removeParticipant(p);
                p.getInventory().clear();
                p.setGameMode(GameMode.SURVIVAL);
                boolean diffServer = this.plugin.getConfig().getBoolean("different-server.enabled", false);
                if (diffServer) {
                    String hub = this.plugin.getConfig().getString("hub-server", "hub");
                    this.plugin.sendToServer(p, hub);
                } else {
                    p.teleport(((World)Bukkit.getWorlds().get(0)).getSpawnLocation());
                }
                p.sendMessage("\u00a7a[Event] Sent to hub!");
                return true;
            }
            case "stopevent": {
                em.stopEvent("Event stopped. Everyone sent to hub!");
                return true;
            }
            case "addtime": {
                if (args.length < 1) {
                    p.sendMessage("\u00a7cUsage: /addtime <5m|30s>");
                    return true;
                }
                int secs = this.parseTime(args[0]);
                if (secs <= 0) {
                    p.sendMessage("\u00a7cInvalid time.");
                    return true;
                }
                em.addTime(secs);
                return true;
            }
            case "removetime": {
                if (args.length < 1) {
                    p.sendMessage("\u00a7cUsage: /removetime <2m|30s>");
                    return true;
                }
                int secs = this.parseTime(args[0]);
                if (secs <= 0) {
                    p.sendMessage("\u00a7cInvalid time.");
                    return true;
                }
                em.removeTime(secs);
                return true;
            }
        }
        return false;
    }

    private int parseTime(String input) {
        input = input.trim().toLowerCase();
        try {
            if (input.endsWith("m")) {
                return Integer.parseInt(input.substring(0, input.length() - 1)) * 60;
            }
            if (input.endsWith("s")) {
                return Integer.parseInt(input.substring(0, input.length() - 1));
            }
            return Integer.parseInt(input);
        }
        catch (Exception e) {
            return -1;
        }
    }
}
