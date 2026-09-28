/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.kvwx.events;

import com.kvwx.events.CommandManager;
import com.kvwx.events.EventListener;
import com.kvwx.events.EventManager;
import com.kvwx.events.GuiManager;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class EventsPlugin
extends JavaPlugin {
    private static EventsPlugin instance;
    private EventManager eventManager;
    private GuiManager guiManager;
    private File kitsFile;
    private FileConfiguration kitsConfig;

    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        this.loadKitsConfig();
        this.eventManager = new EventManager(this);
        this.guiManager = new GuiManager(this);
        CommandManager cmdMgr = new CommandManager(this);
        this.getCommand("event").setExecutor((CommandExecutor)cmdMgr);
        this.getCommand("createevents").setExecutor((CommandExecutor)cmdMgr);
        this.getCommand("forcestart").setExecutor((CommandExecutor)cmdMgr);
        this.getCommand("shrink").setExecutor((CommandExecutor)cmdMgr);
        this.getCommand("drop").setExecutor((CommandExecutor)cmdMgr);
        this.getCommand("hub").setExecutor((CommandExecutor)cmdMgr);
        this.getCommand("stopevent").setExecutor((CommandExecutor)cmdMgr);
        this.getCommand("addtime").setExecutor((CommandExecutor)cmdMgr);
        this.getCommand("removetime").setExecutor((CommandExecutor)cmdMgr);
        this.getServer().getPluginManager().registerEvents((Listener)new EventListener(this), (Plugin)this);
        this.getServer().getMessenger().registerOutgoingPluginChannel((Plugin)this, "BungeeCord");
        this.getLogger().info("Events plugin enabled successfully!");
    }

    public void onDisable() {
        if (this.eventManager != null) {
            this.eventManager.cleanupVotingEntities();
        }
    }

    public static EventsPlugin getInstance() {
        return instance;
    }

    public EventManager getEventManager() {
        return this.eventManager;
    }

    public GuiManager getGuiManager() {
        return this.guiManager;
    }

    public void sendToServer(Player player, String serverName) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage((Plugin)this, "BungeeCord", b.toByteArray());
        }
        catch (Exception e) {
            player.sendMessage("\u00a7cCould not connect to " + serverName + ": " + e.getMessage());
        }
    }

    public FileConfiguration getKitsConfig() {
        return this.kitsConfig;
    }

    public void loadKitsConfig() {
        this.kitsFile = new File(this.getDataFolder(), "kits.yml");
        if (!this.kitsFile.exists()) {
            this.saveResource("kits.yml", false);
        }
        this.kitsConfig = YamlConfiguration.loadConfiguration((File)this.kitsFile);
    }

    public void saveKitsConfig() {
        try {
            this.kitsConfig.save(this.kitsFile);
        }
        catch (IOException e) {
            this.getLogger().severe("Could not save kits.yml: " + e.getMessage());
        }
    }
}

