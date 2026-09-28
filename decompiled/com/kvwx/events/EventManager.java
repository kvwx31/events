/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.WorldBorder
 *  org.bukkit.block.Block
 *  org.bukkit.entity.BlockDisplay
 *  org.bukkit.entity.Display$Billboard
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Interaction
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.TextDisplay
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.Transformation
 *  org.bukkit.util.Vector
 *  org.joml.AxisAngle4f
 *  org.joml.Vector3f
 */
package com.kvwx.events;

import com.kvwx.events.EventsPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class EventManager {
    private final EventsPlugin plugin;
    private EventState state = EventState.IDLE;
    private final Set<UUID> participants = new HashSet<UUID>();
    private final Set<UUID> spectators = new HashSet<UUID>();
    private final Map<UUID, String> votes = new HashMap<UUID, String>();
    private int countdownSeconds = 300;
    private int borderSize = 100;
    private String selectedWorld = "none";
    private int votingDuration = 60;
    private BukkitTask countdownTask;
    private BukkitTask votingTask;
    private BukkitTask landingTask;
    private final Map<UUID, Location> playerVotingCenters = new HashMap<UUID, Location>();
    private final Map<UUID, List<Entity>> playerVotingEntities = new HashMap<UUID, List<Entity>>();
    private final Map<UUID, Map<UUID, BiomeOption>> playerInteractionToBiome = new HashMap<UUID, Map<UUID, BiomeOption>>();
    private final Map<UUID, Map<String, TextDisplay>> playerBiomePercentages = new HashMap<UUID, Map<String, TextDisplay>>();
    private final Map<UUID, Map<String, BlockDisplay>> playerBiomeDisplays = new HashMap<UUID, Map<String, BlockDisplay>>();
    private Map<UUID, Location> originalPositions = new HashMap<UUID, Location>();
    public final List<BiomeOption> BIOME_OPTIONS = Arrays.asList(new BiomeOption("plains", "Plains", Material.GRASS_BLOCK, "world_minecraft_plains"), new BiomeOption("desert", "Desert", Material.SAND, "world_minecraft_desert"), new BiomeOption("badlands", "Badlands", Material.TERRACOTTA, "world_minecraft_badlands"), new BiomeOption("snow", "Snow", Material.SNOW_BLOCK, "world_minecraft_snowy_fields"), new BiomeOption("mushroom", "Mushroom", Material.RED_MUSHROOM_BLOCK, "world_minecraft_mushroom"), new BiomeOption("cherry", "Cherry", Material.CHERRY_LOG, "world_minecraft_cherry"));

    public Location getVotingCenter(Player player) {
        return this.playerVotingCenters.get(player.getUniqueId());
    }

    public boolean hasVotingCenter(Player player) {
        return this.playerVotingCenters.containsKey(player.getUniqueId());
    }

    public EventManager(EventsPlugin plugin) {
        this.plugin = plugin;
        this.votingDuration = plugin.getConfig().getInt("voting-duration", 60);
    }

    public EventState getState() {
        return this.state;
    }

    public Set<UUID> getParticipants() {
        return this.participants;
    }

    public Set<UUID> getSpectators() {
        return this.spectators;
    }

    public int getCountdownSeconds() {
        return this.countdownSeconds;
    }

    public void setCountdownSeconds(int seconds) {
        this.countdownSeconds = seconds;
    }

    public int getBorderSize() {
        return this.borderSize;
    }

    public void setBorderSize(int size) {
        this.borderSize = size;
    }

    public String getSelectedWorld() {
        return this.selectedWorld;
    }

    public void setSelectedWorld(String world) {
        this.selectedWorld = world;
    }

    public static String formatDuration(int totalSeconds) {
        if (totalSeconds >= 60) {
            return totalSeconds / 60 + "m";
        }
        return Math.max(totalSeconds, 0) + "s";
    }

    private void sendActionBar(Player p, String message) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText((String)message));
    }

    public void addParticipant(Player player) {
        this.participants.add(player.getUniqueId());
        player.sendMessage("\u00a7a[Event] You joined the event!");
        if (this.state == EventState.COUNTDOWN) {
            this.sendActionBar(player, "\u00a7bEvent starting in " + EventManager.formatDuration(this.countdownSeconds));
        } else if (this.state == EventState.VOTING) {
            this.sendActionBar(player, "\u00a7bBiome vote in progress");
        }
    }

    public void removeParticipant(Player player) {
        this.participants.remove(player.getUniqueId());
        this.spectators.remove(player.getUniqueId());
        this.votes.remove(player.getUniqueId());
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    public boolean isParticipant(Player player) {
        return this.participants.contains(player.getUniqueId());
    }

    public boolean isSpectator(Player player) {
        return this.spectators.contains(player.getUniqueId());
    }

    public void startEventCountdown() {
        if (this.state != EventState.IDLE) {
            return;
        }
        this.state = EventState.COUNTDOWN;
        this.broadcast("\u00a7b[Event] Event starting in " + EventManager.formatDuration(this.countdownSeconds) + "!");
        this.countdownTask = new BukkitRunnable(){

            public void run() {
                if (EventManager.this.countdownSeconds <= 0) {
                    this.cancel();
                    EventManager.this.beginMatch();
                    return;
                }
                if ("none".equalsIgnoreCase(EventManager.this.selectedWorld) && EventManager.this.countdownSeconds == 10) {
                    this.cancel();
                    EventManager.this.startBiomeVoting(EventManager.this.votingDuration);
                    return;
                }
                String bar = "\u00a7bEvent starting in " + EventManager.formatDuration(EventManager.this.countdownSeconds);
                for (UUID uid : EventManager.this.participants) {
                    Player p = Bukkit.getPlayer((UUID)uid);
                    if (p == null) continue;
                    EventManager.this.sendActionBar(p, bar);
                }
                if (EventManager.this.countdownSeconds == 300 || EventManager.this.countdownSeconds == 180 || EventManager.this.countdownSeconds == 60 || EventManager.this.countdownSeconds == 30 || EventManager.this.countdownSeconds <= 10) {
                    EventManager.this.broadcast("\u00a7b[Event] Starting in " + EventManager.formatDuration(EventManager.this.countdownSeconds) + "!");
                }
                --EventManager.this.countdownSeconds;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 20L);
    }

    public void forceStart() {
        if (this.countdownTask != null) {
            this.countdownTask.cancel();
        }
        if ("none".equalsIgnoreCase(this.selectedWorld) && this.state != EventState.VOTING) {
            this.startBiomeVoting(this.votingDuration);
        } else {
            this.beginMatch();
        }
    }

    public void startBiomeVoting(int duration) {
        this.state = EventState.VOTING;
        this.votes.clear();
        this.cleanupVotingEntities();
        this.originalPositions.clear();
        this.broadcast("\u00a7b[Event] Biome voting started! Right-click a floating block to vote! (" + duration + "s)");
        for (UUID uid : this.participants) {
            Player p = Bukkit.getPlayer((UUID)uid);
            if (p == null || !p.isOnline()) continue;
            this.originalPositions.put(uid, p.getLocation().clone());
        }
        this.floatPlayersUp(this.originalPositions, () -> {
            for (UUID uid : this.participants) {
                Player p = Bukkit.getPlayer((UUID)uid);
                if (p == null || !p.isOnline()) continue;
                World world = p.getWorld();
                Location playerCenter = p.getLocation().clone();
                playerCenter.setPitch(0.0f);
                this.playerVotingCenters.put(uid, playerCenter);
                p.setAllowFlight(true);
                p.setFlying(true);
                p.sendTitle("\u00a7bBiome Voting!", "\u00a77Look at and right-click a block to vote", 10, 40, 10);
                ArrayList<Object> entities = new ArrayList<Object>();
                HashMap<UUID, BiomeOption> interactionMap = new HashMap<UUID, BiomeOption>();
                HashMap<String, TextDisplay> percentageMap = new HashMap<String, TextDisplay>();
                HashMap<String, BlockDisplay> displayMap = new HashMap<String, BlockDisplay>();
                double radius = 2.0;
                double startAngle = -60.0;
                double endAngle = 60.0;
                double step = (endAngle - startAngle) / (double)(this.BIOME_OPTIONS.size() - 1);
                double yawRad = Math.toRadians(playerCenter.getYaw());
                double forwardX = -Math.sin(yawRad);
                double forwardZ = Math.cos(yawRad);
                double rightX = -Math.cos(yawRad);
                double rightZ = -Math.sin(yawRad);
                for (int i = 0; i < this.BIOME_OPTIONS.size(); ++i) {
                    BiomeOption opt = this.BIOME_OPTIONS.get(i);
                    double angleDeg = startAngle + (double)i * step;
                    double rad = Math.toRadians(angleDeg);
                    double localRight = radius * Math.sin(rad);
                    double localForward = radius * Math.cos(rad);
                    double x = rightX * localRight + forwardX * localForward;
                    double z = rightZ * localRight + forwardZ * localForward;
                    Location blockLoc = playerCenter.clone().add(x, 0.5, z);
                    double angleToCenter = Math.atan2(-x, -z);
                    float baseRotation = (float)Math.toDegrees(angleToCenter);
                    float[] offsets = new float[]{45.0f, -45.0f, 30.0f, -30.0f, 60.0f, -60.0f};
                    float yawRotation = baseRotation + offsets[i % offsets.length];
                    BlockDisplay blockDisplay = (BlockDisplay)world.spawnEntity(blockLoc, EntityType.BLOCK_DISPLAY);
                    blockDisplay.setBlock(opt.material.createBlockData());
                    blockDisplay.setGlowing(false);
                    blockDisplay.setGlowColorOverride(Color.fromRGB((int)0, (int)168, (int)255));
                    float scale = 0.7f;
                    Transformation transform = new Transformation(new Vector3f(-scale / 2.0f, -scale / 2.0f, -scale / 2.0f), new AxisAngle4f(0.0f, 1.0f, 0.0f, (float)Math.toRadians(yawRotation)), new Vector3f(scale, scale, scale), new AxisAngle4f(0.0f, 0.0f, 1.0f, 0.0f));
                    blockDisplay.setTransformation(transform);
                    entities.add(blockDisplay);
                    displayMap.put(opt.id, blockDisplay);
                    Location textLoc = blockLoc.clone().add(0.0, 0.8, 0.0);
                    TextDisplay textDisplay = (TextDisplay)world.spawnEntity(textLoc, EntityType.TEXT_DISPLAY);
                    textDisplay.setText("\u00a7c0%");
                    textDisplay.setBillboard(Display.Billboard.CENTER);
                    textDisplay.setDefaultBackground(false);
                    textDisplay.setBackgroundColor(Color.fromARGB((int)0, (int)0, (int)0, (int)0));
                    entities.add(textDisplay);
                    percentageMap.put(opt.id, textDisplay);
                    Location interactLoc = blockLoc.clone().add(0.0, -0.5, 0.0);
                    Interaction interaction = (Interaction)world.spawnEntity(interactLoc, EntityType.INTERACTION);
                    interaction.setInteractionWidth(0.9f);
                    interaction.setInteractionHeight(0.9f);
                    interaction.setResponsive(true);
                    entities.add(interaction);
                    interactionMap.put(interaction.getUniqueId(), opt);
                }
                this.playerVotingEntities.put(uid, entities);
                this.playerInteractionToBiome.put(uid, interactionMap);
                this.playerBiomePercentages.put(uid, percentageMap);
                this.playerBiomeDisplays.put(uid, displayMap);
            }
            final int[] timeLeft = new int[]{duration};
            this.votingTask = new BukkitRunnable(){

                public void run() {
                    if (timeLeft[0] <= 0) {
                        this.cancel();
                        EventManager.this.finishBiomeVoting(EventManager.this.originalPositions);
                        return;
                    }
                    String bar = "\u00a7bBiome vote " + EventManager.formatDuration(timeLeft[0]);
                    for (UUID uid : EventManager.this.participants) {
                        Player p = Bukkit.getPlayer((UUID)uid);
                        if (p == null) continue;
                        EventManager.this.sendActionBar(p, bar);
                    }
                    timeLeft[0] = timeLeft[0] - 1;
                }
            }.runTaskTimer((Plugin)this.plugin, 0L, 20L);
        });
    }

    private void floatPlayersUp(final Map<UUID, Location> originalPositions, final Runnable onComplete) {
        for (Map.Entry<UUID, Location> entry : originalPositions.entrySet()) {
            Player p = Bukkit.getPlayer((UUID)entry.getKey());
            if (p == null || !p.isOnline()) continue;
            Location loc = entry.getValue();
            p.getWorld().spawnParticle(Particle.CLOUD, loc, 25, 0.4, 0.1, 0.4, 0.1);
            p.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.2f);
            p.setVelocity(new Vector(0.0, 1.75, 0.0));
        }
        int durationTicks = 24;
        new BukkitRunnable(){
            int ticksElapsed = 0;

            public void run() {
                ++this.ticksElapsed;
                if (this.ticksElapsed >= 24) {
                    for (Map.Entry entry : originalPositions.entrySet()) {
                        Player p = Bukkit.getPlayer((UUID)((UUID)entry.getKey()));
                        if (p == null || !p.isOnline()) continue;
                        p.setVelocity(new Vector(0, 0, 0));
                        p.setAllowFlight(true);
                        p.setFlying(true);
                        Location hoverLoc = p.getLocation().clone();
                        EventManager.this.playerVotingCenters.put((UUID)entry.getKey(), hoverLoc);
                    }
                    this.cancel();
                    onComplete.run();
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    public void createVotingBlocksForPlayer(Player p) {
        if (this.state != EventState.VOTING) {
            return;
        }
        if (this.playerVotingEntities.containsKey(p.getUniqueId())) {
            return;
        }
        Location originalPos = p.getLocation().clone();
        this.originalPositions.put(p.getUniqueId(), originalPos);
        this.floatSinglePlayerUp(p, originalPos, () -> {
            World world = p.getWorld();
            Location playerCenter = p.getLocation().clone();
            playerCenter.setPitch(0.0f);
            this.playerVotingCenters.put(p.getUniqueId(), playerCenter);
            p.setAllowFlight(true);
            p.setFlying(true);
            p.sendTitle("\u00a7bBiome Voting!", "\u00a77Look at and right-click a block to vote", 10, 40, 10);
            ArrayList<Object> entities = new ArrayList<Object>();
            HashMap<UUID, BiomeOption> interactionMap = new HashMap<UUID, BiomeOption>();
            HashMap<String, TextDisplay> percentageMap = new HashMap<String, TextDisplay>();
            HashMap<String, BlockDisplay> displayMap = new HashMap<String, BlockDisplay>();
            double radius = 2.0;
            double startAngle = -60.0;
            double endAngle = 60.0;
            double step = (endAngle - startAngle) / (double)(this.BIOME_OPTIONS.size() - 1);
            double yawRad = Math.toRadians(playerCenter.getYaw());
            double forwardX = -Math.sin(yawRad);
            double forwardZ = Math.cos(yawRad);
            double rightX = -Math.cos(yawRad);
            double rightZ = -Math.sin(yawRad);
            for (int i = 0; i < this.BIOME_OPTIONS.size(); ++i) {
                BiomeOption opt = this.BIOME_OPTIONS.get(i);
                double angleDeg = startAngle + (double)i * step;
                double rad = Math.toRadians(angleDeg);
                double localRight = radius * Math.sin(rad);
                double localForward = radius * Math.cos(rad);
                double x = rightX * localRight + forwardX * localForward;
                double z = rightZ * localRight + forwardZ * localForward;
                Location blockLoc = playerCenter.clone().add(x, 0.5, z);
                double angleToCenter = Math.atan2(-x, -z);
                float baseRotation = (float)Math.toDegrees(angleToCenter);
                float[] offsets = new float[]{45.0f, -45.0f, 30.0f, -30.0f, 60.0f, -60.0f};
                float yawRotation = baseRotation + offsets[i % offsets.length];
                BlockDisplay blockDisplay = (BlockDisplay)world.spawnEntity(blockLoc, EntityType.BLOCK_DISPLAY);
                blockDisplay.setBlock(opt.material.createBlockData());
                blockDisplay.setGlowing(false);
                blockDisplay.setGlowColorOverride(Color.fromRGB((int)0, (int)168, (int)255));
                float scale = 0.7f;
                Transformation transform = new Transformation(new Vector3f(-scale / 2.0f, -scale / 2.0f, -scale / 2.0f), new AxisAngle4f(0.0f, 1.0f, 0.0f, (float)Math.toRadians(yawRotation)), new Vector3f(scale, scale, scale), new AxisAngle4f(0.0f, 0.0f, 1.0f, 0.0f));
                blockDisplay.setTransformation(transform);
                entities.add(blockDisplay);
                displayMap.put(opt.id, blockDisplay);
                Location textLoc = blockLoc.clone().add(0.0, 0.8, 0.0);
                TextDisplay textDisplay = (TextDisplay)world.spawnEntity(textLoc, EntityType.TEXT_DISPLAY);
                textDisplay.setText("\u00a7c0%");
                textDisplay.setBillboard(Display.Billboard.CENTER);
                textDisplay.setDefaultBackground(false);
                textDisplay.setBackgroundColor(Color.fromARGB((int)0, (int)0, (int)0, (int)0));
                entities.add(textDisplay);
                percentageMap.put(opt.id, textDisplay);
                Location interactLoc = blockLoc.clone().add(0.0, -0.5, 0.0);
                Interaction interaction = (Interaction)world.spawnEntity(interactLoc, EntityType.INTERACTION);
                interaction.setInteractionWidth(0.9f);
                interaction.setInteractionHeight(0.9f);
                interaction.setResponsive(true);
                entities.add(interaction);
                interactionMap.put(interaction.getUniqueId(), opt);
            }
            this.playerVotingEntities.put(p.getUniqueId(), entities);
            this.playerInteractionToBiome.put(p.getUniqueId(), interactionMap);
            this.playerBiomePercentages.put(p.getUniqueId(), percentageMap);
            this.playerBiomeDisplays.put(p.getUniqueId(), displayMap);
            this.updateBiomePercentages();
        });
    }

    private void floatSinglePlayerUp(final Player p, Location originalPos, final Runnable onComplete) {
        if (p == null || !p.isOnline()) {
            onComplete.run();
            return;
        }
        final double originalX = originalPos.getX();
        final double originalZ = originalPos.getZ();
        final double startY = originalPos.getY();
        final double targetY = startY + 20.0;
        p.setAllowFlight(true);
        p.setFlying(true);
        int totalTicks = 100;
        new BukkitRunnable(){
            int tick = 0;

            public void run() {
                ++this.tick;
                if (p == null || !p.isOnline()) {
                    this.cancel();
                    onComplete.run();
                    return;
                }
                double progress = Math.min(1.0, (double)this.tick / 100.0);
                double eased = (1.0 - Math.cos(progress * Math.PI)) / 2.0;
                double y = startY + (targetY - startY) * eased;
                Location loc = new Location(p.getWorld(), originalX, y, originalZ, p.getLocation().getYaw(), p.getLocation().getPitch());
                p.teleport(loc);
                Location current = p.getLocation();
                current.setX(originalX);
                current.setZ(originalZ);
                p.teleport(current);
                if (this.tick >= 100) {
                    Location center = p.getLocation().clone();
                    EventManager.this.playerVotingCenters.put(p.getUniqueId(), center);
                    this.cancel();
                    onComplete.run();
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    public void castVote(Player player, BiomeOption biome) {
        if (this.state != EventState.VOTING) {
            return;
        }
        this.votes.put(player.getUniqueId(), biome.id);
        player.sendTitle("\u00a7bBiome Selected", "\u00a7f" + biome.displayName, 5, 25, 5);
        player.sendMessage("\u00a7a[Event] You voted for \u00a7f" + biome.displayName + "\u00a7a!");
        this.updateBiomePercentages();
        if (this.votes.size() >= this.participants.size() && !this.participants.isEmpty()) {
            this.broadcast("\u00a7a[Event] All participants voted! Finishing voting early...");
            if (this.votingTask != null) {
                this.votingTask.cancel();
            }
            new BukkitRunnable(){

                public void run() {
                    EventManager.this.finishBiomeVoting(EventManager.this.originalPositions);
                }
            }.runTaskLater((Plugin)this.plugin, 20L);
        }
    }

    private void updateBiomePercentages() {
        int totalVotes = this.votes.size();
        HashMap<String, Integer> counts = new HashMap<String, Integer>();
        for (BiomeOption bo : this.BIOME_OPTIONS) {
            counts.put(bo.id, 0);
        }
        for (String id : this.votes.values()) {
            counts.put(id, counts.getOrDefault(id, 0) + 1);
        }
        for (BiomeOption bo : this.BIOME_OPTIONS) {
            int c = counts.getOrDefault(bo.id, 0);
            for (Map<String, TextDisplay> map : this.playerBiomePercentages.values()) {
                TextDisplay td = map.get(bo.id);
                if (td == null) continue;
                int pct = totalVotes > 0 ? (int)Math.round((double)c * 100.0 / (double)totalVotes) : 0;
                String color = pct > 0 ? "\u00a7a" : "\u00a7c";
                td.setText("\u00a7e" + bo.displayName + "\n" + color + pct + "%");
            }
            for (Map<String, TextDisplay> map : this.playerBiomeDisplays.values()) {
                BlockDisplay bd = (BlockDisplay)map.get(bo.id);
                if (bd == null) continue;
                bd.setGlowing(c > 0);
            }
        }
    }

    public void finishBiomeVoting(Map<UUID, Location> originalPositions) {
        Location currentLoc;
        Player p;
        if (this.state != EventState.VOTING) {
            return;
        }
        if (this.votingTask != null) {
            this.votingTask.cancel();
        }
        HashMap<String, Integer> counts = new HashMap<String, Integer>();
        for (String bId : this.votes.values()) {
            counts.put(bId, counts.getOrDefault(bId, 0) + 1);
        }
        BiomeOption winner = this.BIOME_OPTIONS.get(0);
        int maxVotes = -1;
        for (BiomeOption bo : this.BIOME_OPTIONS) {
            int c = counts.getOrDefault(bo.id, 0);
            if (c <= maxVotes) continue;
            maxVotes = c;
            winner = bo;
        }
        this.broadcast("\u00a76[Event] Biome \u00a7e" + winner.displayName + " \u00a76won with " + (maxVotes == -1 ? 0 : maxVotes) + " votes!");
        this.cleanupVotingEntities();
        World targetWorld = Bukkit.getWorld((String)winner.worldName);
        if (targetWorld == null) {
            targetWorld = (World)Bukkit.getWorlds().get(0);
        }
        HashMap<UUID, Double> playerGroundY = new HashMap<UUID, Double>();
        HashMap<UUID, Float> playerYaws = new HashMap<UUID, Float>();
        for (UUID uid : this.participants) {
            p = Bukkit.getPlayer((UUID)uid);
            if (p == null) continue;
            currentLoc = p.getLocation();
            int groundY = targetWorld.getHighestBlockYAt(currentLoc.getBlockX(), currentLoc.getBlockZ());
            playerGroundY.put(uid, (double)groundY + 1.0);
            playerYaws.put(uid, Float.valueOf(currentLoc.getYaw()));
        }
        for (UUID uid : this.participants) {
            p = Bukkit.getPlayer((UUID)uid);
            if (p == null) continue;
            currentLoc = p.getLocation();
            double groundY = playerGroundY.getOrDefault(uid, 100.0);
            double floatHeight = 20.0;
            float yaw = playerYaws.getOrDefault(uid, Float.valueOf(0.0f)).floatValue();
            Location startLoc = new Location(targetWorld, currentLoc.getX(), groundY + floatHeight, currentLoc.getZ(), yaw, 0.0f);
            p.setAllowFlight(true);
            p.setFlying(true);
            p.teleport(startLoc);
        }
        this.countdownSeconds = 5;
        this.state = EventState.COUNTDOWN;
        this.countdownTask = new BukkitRunnable(){

            public void run() {
                if (EventManager.this.countdownSeconds <= 0) {
                    this.cancel();
                    EventManager.this.beginMatch();
                    return;
                }
                String bar = "\u00a7bMatch starting in " + EventManager.formatDuration(EventManager.this.countdownSeconds);
                for (UUID uid : EventManager.this.participants) {
                    Player p = Bukkit.getPlayer((UUID)uid);
                    if (p == null) continue;
                    EventManager.this.sendActionBar(p, bar);
                }
                EventManager.this.broadcast("\u00a7b[Event] Match starting in " + EventManager.formatDuration(EventManager.this.countdownSeconds) + "!");
                --EventManager.this.countdownSeconds;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 20L);
    }

    public void beginMatch() {
        this.state = EventState.ACTIVE;
        this.broadcast("\u00a7a\u00a7l[Event] The event has officially begun! Fight to be the last one standing!");
        World matchWorld = null;
        for (UUID uid : this.participants) {
            Player p = Bukkit.getPlayer((UUID)uid);
            if (p == null) continue;
            matchWorld = p.getWorld();
            break;
        }
        if (matchWorld != null) {
            WorldBorder border = matchWorld.getWorldBorder();
            border.setCenter(matchWorld.getSpawnLocation());
            border.setSize((double)this.borderSize);
        }
    }

    public void checkSurvivors() {
        if (this.state != EventState.ACTIVE) {
            return;
        }
        ArrayList<Player> alive = new ArrayList<Player>();
        for (UUID uid : this.participants) {
            Player p;
            if (this.spectators.contains(uid) || (p = Bukkit.getPlayer((UUID)uid)) == null || !p.isOnline()) continue;
            alive.add(p);
        }
        if (alive.size() == 1) {
            Player winner = (Player)alive.get(0);
            this.declareWinner(winner);
        } else if (alive.isEmpty()) {
            this.stopEvent("No survivors remaining!");
        }
    }

    public void declareWinner(Player winner) {
        this.state = EventState.ENDED;
        String winTitle = "\u00a7e" + winner.getName();
        String winSubtitle = "\u00a76\u00a7lWINNER";
        for (UUID uid : this.participants) {
            Player p = Bukkit.getPlayer((UUID)uid);
            if (p == null) continue;
            p.sendTitle(winTitle, winSubtitle, 10, 100, 20);
            p.sendMessage("\u00a76\u00a7l" + "=".repeat(35));
            p.sendMessage("\u00a7eWinner: \u00a7f" + winner.getName());
            p.sendMessage("\u00a76\u00a7l" + "=".repeat(35));
        }
        new BukkitRunnable(){

            public void run() {
                EventManager.this.stopEvent("Event ended! Returning to hub...");
            }
        }.runTaskLater((Plugin)this.plugin, 140L);
    }

    public void stopEvent(String reason) {
        this.state = EventState.ENDED;
        if (this.countdownTask != null) {
            this.countdownTask.cancel();
        }
        if (this.votingTask != null) {
            this.votingTask.cancel();
        }
        if (this.landingTask != null) {
            this.landingTask.cancel();
        }
        this.cleanupVotingEntities();
        this.broadcast("\u00a7c[Event] " + reason);
        String hubServer = this.plugin.getConfig().getString("hub-server", "hub");
        boolean diffServer = this.plugin.getConfig().getBoolean("different-server.enabled", false);
        for (UUID uid : new HashSet<UUID>(this.participants)) {
            Player p = Bukkit.getPlayer((UUID)uid);
            if (p == null) continue;
            p.getInventory().clear();
            p.setFlying(false);
            p.setAllowFlight(false);
            p.setGameMode(GameMode.SURVIVAL);
            if (diffServer) {
                this.plugin.sendToServer(p, hubServer);
                continue;
            }
            World w = (World)Bukkit.getWorlds().get(0);
            p.teleport(w.getSpawnLocation());
        }
        this.participants.clear();
        this.spectators.clear();
        this.votes.clear();
        this.state = EventState.IDLE;
    }

    public void cleanupVotingEntities() {
        for (List<Entity> entities : this.playerVotingEntities.values()) {
            for (Entity e : entities) {
                if (e == null || !e.isValid()) continue;
                e.remove();
            }
        }
        this.playerVotingEntities.clear();
        this.playerVotingCenters.clear();
        this.playerInteractionToBiome.clear();
        this.playerBiomePercentages.clear();
        this.playerBiomeDisplays.clear();
    }

    public void shrinkBorder(int blocks) {
        World matchWorld = null;
        for (UUID uid : this.participants) {
            Player p = Bukkit.getPlayer((UUID)uid);
            if (p == null) continue;
            matchWorld = p.getWorld();
            break;
        }
        if (matchWorld != null) {
            WorldBorder border = matchWorld.getWorldBorder();
            double newSize = Math.max(10.0, border.getSize() - (double)blocks);
            border.setSize(newSize, 5L);
            this.broadcast("\u00a7e[Event] Border shrunk by " + blocks + " blocks! New size: " + (int)newSize);
        }
    }

    public void dropBlocks(String level) {
        World world = null;
        for (UUID uid : this.participants) {
            Player p = Bukkit.getPlayer((UUID)uid);
            if (p == null) continue;
            world = p.getWorld();
            break;
        }
        if (world == null) {
            return;
        }
        final World matchWorld = world;
        WorldBorder border = matchWorld.getWorldBorder();
        final Location center = border.getCenter();
        final int radius = (int)(border.getSize() / 2.0);
        final int targetY = "bedrock".equalsIgnoreCase(level) ? -60 : -50;
        this.broadcast("\u00a7c[Event] Dropping all players to " + level + "!");
        new BukkitRunnable(this){

            public void run() {
                for (int x = center.getBlockX() - radius; x <= center.getBlockX() + radius; ++x) {
                    for (int z = center.getBlockZ() - radius; z <= center.getBlockZ() + radius; ++z) {
                        for (int y = targetY + 1; y <= 320; ++y) {
                            Block b = matchWorld.getBlockAt(x, y, z);
                            if (b.getType() == Material.AIR || b.getType() == Material.BEDROCK) continue;
                            b.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }.runTask((Plugin)this.plugin);
    }

    public void addTime(int seconds) {
        this.countdownSeconds += seconds;
        this.broadcast("\u00a7a[Event] Added " + seconds + "s to the countdown! (" + this.countdownSeconds + "s remaining)");
    }

    public void removeTime(int seconds) {
        this.countdownSeconds = Math.max(1, this.countdownSeconds - seconds);
        this.broadcast("\u00a7c[Event] Removed " + seconds + "s from the countdown! (" + this.countdownSeconds + "s remaining)");
    }

    public BiomeOption getBiomeForInteraction(UUID interactionId) {
        for (Map<UUID, BiomeOption> map : this.playerInteractionToBiome.values()) {
            BiomeOption biome = map.get(interactionId);
            if (biome == null) continue;
            return biome;
        }
        return null;
    }

    public BiomeOption getBiomeForPlayerInteraction(UUID playerId, UUID interactionId) {
        Map<UUID, BiomeOption> map = this.playerInteractionToBiome.get(playerId);
        if (map != null) {
            return map.get(interactionId);
        }
        return null;
    }

    public void broadcast(String message) {
        for (UUID uid : this.participants) {
            Player p = Bukkit.getPlayer((UUID)uid);
            if (p == null) continue;
            p.sendMessage(message);
        }
    }

    public static enum EventState {
        IDLE,
        COUNTDOWN,
        VOTING,
        ACTIVE,
        ENDED;

    }

    public static class BiomeOption {
        public final String id;
        public final String displayName;
        public final Material material;
        public final String worldName;

        public BiomeOption(String id, String displayName, Material material, String worldName) {
            this.id = id;
            this.displayName = displayName;
            this.material = material;
            this.worldName = worldName;
        }
    }
}

