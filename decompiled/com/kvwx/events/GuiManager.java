/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package com.kvwx.events;

import com.kvwx.events.EventManager;
import com.kvwx.events.EventsPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class GuiManager
implements Listener {
    private final EventsPlugin plugin;
    private final Map<UUID, SignInputType> pendingSignInputs = new HashMap<UUID, SignInputType>();
    private final Map<UUID, Integer> selectedKit = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> editingKit = new HashMap<UUID, Integer>();
    private final Set<UUID> openingNewGui = ConcurrentHashMap.newKeySet();

    public GuiManager(EventsPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)plugin);
    }

    public void openMainGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, (int)27, (String)"\u00a78Create Event");
        EventManager em = this.plugin.getEventManager();
        inv.setItem(10, this.createItem(Material.CLOCK, "\u00a7eStart Timer", "\u00a77Current: \u00a7f" + this.formatTime(em.getCountdownSeconds()), "\u00a77Click to set timer"));
        inv.setItem(12, this.createItem(Material.BARRIER, "\u00a7cBorder Size", "\u00a77Current: \u00a7f" + em.getBorderSize() + "x" + em.getBorderSize(), "\u00a77Click to set border"));
        inv.setItem(14, this.createItem(Material.GRASS_BLOCK, "\u00a7aWorld / Biome", "\u00a77Current: \u00a7f" + em.getSelectedWorld(), "\u00a77Click to toggle"));
        inv.setItem(16, this.createItem(Material.CHEST, "\u00a76Kits", "\u00a77Manage event kits", "\u00a77Click to open"));
        inv.setItem(22, this.createItem(Material.EMERALD_BLOCK, "\u00a7a\u00a7lCreate Event", "\u00a77Click to start countdown"));
        player.openInventory(inv);
    }

    public void openKitsGui(Player player) {
        int kitNum;
        int i;
        Inventory inv = Bukkit.createInventory(null, (int)36, (String)"\u00a78Kits");
        int selKit = this.selectedKit.getOrDefault(player.getUniqueId(), -1);
        for (i = 0; i < 9; ++i) {
            kitNum = i + 1;
            boolean isSelected = kitNum == selKit;
            ItemStack item = this.createItem(Material.LIGHT_BLUE_SHULKER_BOX, isSelected ? "\u00a7aKit #" + kitNum : "\u00a7eKit #" + kitNum, isSelected ? "\u00a7aSELECTED" : "\u00a77Click to select");
            inv.setItem(9 + i, item);
        }
        for (i = 0; i < 9; ++i) {
            kitNum = i + 1;
            inv.setItem(18 + i, this.createItem(Material.CHEST, "\u00a7aKit #" + kitNum, "\u00a77Click to open editor"));
        }
        inv.setItem(35, this.createItem(Material.ARROW, "\u00a7cBack", "\u00a77Return to main menu"));
        player.openInventory(inv);
    }

    public void openKitEditor(Player player, int kitNumber) {
        this.editingKit.put(player.getUniqueId(), kitNumber);
        Inventory inv = Bukkit.createInventory(null, (int)54, (String)("\u00a78Kit Editor \u00a77- \u00a7eKit #" + kitNumber));
        ItemStack glass = this.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", new String[0]);
        for (int i = 36; i < 45; ++i) {
            inv.setItem(i, glass);
        }
        inv.setItem(45, this.createItem(Material.AIR, "\u00a7eHelmet", "\u00a77Place helmet here"));
        inv.setItem(46, this.createItem(Material.AIR, "\u00a7eChestplate", "\u00a77Place chestplate here"));
        inv.setItem(47, this.createItem(Material.AIR, "\u00a7eLeggings", "\u00a77Place leggings here"));
        inv.setItem(48, this.createItem(Material.AIR, "\u00a7eBoots", "\u00a77Place boots here"));
        inv.setItem(49, this.createItem(Material.AIR, "\u00a7eOffhand", "\u00a77Place offhand here"));
        inv.setItem(50, glass);
        inv.setItem(51, this.createItem(Material.CHEST, "\u00a7aImport Inventory", "\u00a77Click to import your current inventory"));
        inv.setItem(52, this.createItem(Material.RED_CANDLE, "\u00a7cBack", "\u00a77Return to kits menu"));
        inv.setItem(53, this.createItem(Material.GREEN_CANDLE, "\u00a7aSave Kit #" + kitNumber, "\u00a77Save this kit"));
        this.loadKitIntoEditor(inv, kitNumber);
        player.openInventory(inv);
    }

    private void loadKitIntoEditor(Inventory inv, int kitNumber) {
        String path = "kits.kit" + kitNumber + ".items";
        List list = this.plugin.getKitsConfig().getList(path);
        if (list != null) {
            int slot = 0;
            for (Object obj : list) {
                if (!(obj instanceof ItemStack) || slot >= 36) continue;
                ItemStack is = ((ItemStack)obj).clone();
                this.makeUnbreakable(is);
                inv.setItem(slot++, is);
            }
        }
        String[] armorPaths = new String[]{"armor.helmet", "armor.chestplate", "armor.leggings", "armor.boots", "offhand"};
        for (int i = 0; i < 5; ++i) {
            ItemStack armor = this.plugin.getKitsConfig().getItemStack("kits.kit" + kitNumber + "." + armorPaths[i]);
            if (armor == null) continue;
            armor = armor.clone();
            this.makeUnbreakable(armor);
            inv.setItem(45 + i, armor);
        }
    }

    private void makeUnbreakable(ItemStack is) {
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            is.setItemMeta(meta);
        }
    }

    private ItemStack createItem(Material mat, String name, String ... lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            is.setItemMeta(meta);
        }
        return is;
    }

    private String formatTime(int secs) {
        if (secs >= 60 && secs % 60 == 0) {
            return secs / 60 + "m";
        }
        return secs + "s";
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        if (title.equals("\u00a78Create Event")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            EventManager em = this.plugin.getEventManager();
            if (slot == 10) {
                player.closeInventory();
                this.openSignEditor(player, SignInputType.TIMER);
            } else if (slot == 12) {
                player.closeInventory();
                this.openSignEditor(player, SignInputType.BORDER);
            } else if (slot == 14) {
                List<String> opts = List.of("none", "world_minecraft_plains", "world_minecraft_desert", "world_minecraft_snowy_fields", "world_minecraft_cherry", "world_minecraft_badlands", "world_minecraft_mushroom");
                int idx = opts.indexOf(em.getSelectedWorld());
                em.setSelectedWorld(opts.get((idx + 1) % opts.size()));
                this.openMainGui(player);
            } else if (slot == 16) {
                this.openKitsGui(player);
            } else if (slot == 22) {
                player.closeInventory();
                em.startEventCountdown();
            }
        } else if (title.equals("\u00a78Kits")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 9 && slot <= 17) {
                int kitNum = slot - 9 + 1;
                if (this.selectedKit.getOrDefault(player.getUniqueId(), -1) == kitNum) {
                    this.selectedKit.remove(player.getUniqueId());
                } else {
                    this.selectedKit.put(player.getUniqueId(), kitNum);
                }
                this.openKitsGui(player);
                return;
            }
            if (slot >= 18 && slot <= 26) {
                int kitNum = slot - 18 + 1;
                this.openingNewGui.add(player.getUniqueId());
                this.openKitEditor(player, kitNum);
                return;
            }
            if (slot == 35) {
                this.openMainGui(player);
                return;
            }
        } else if (title.startsWith("\u00a78Kit Editor")) {
            int slot = event.getRawSlot();
            int kitNum = this.editingKit.getOrDefault(player.getUniqueId(), 1);
            if (slot >= 36 && slot <= 44 || slot >= 51 && slot <= 53) {
                event.setCancelled(true);
            }
            if (slot == 51) {
                event.setCancelled(true);
                this.importPlayerInventory(player, kitNum);
            } else if (slot == 52) {
                event.setCancelled(true);
                this.openingNewGui.add(player.getUniqueId());
                this.openKitsGui(player);
            } else if (slot == 53) {
                event.setCancelled(true);
                this.saveKit(event.getInventory(), kitNum);
                player.sendMessage("\u00a7a[Kit] Kit #" + kitNum + " saved!");
                this.openKitEditor(player, kitNum);
            }
            if (slot >= 0 && slot <= 35 || slot >= 45 && slot <= 49) {
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                    ItemStack o;
                    ItemStack c = event.getCursor();
                    if (c != null && c.getType() != Material.AIR) {
                        this.makeUnbreakable(c);
                    }
                    if ((o = player.getItemOnCursor()) != null && o.getType() != Material.AIR) {
                        this.makeUnbreakable(o);
                    }
                });
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (this.openingNewGui.remove(uuid)) {
            return;
        }
        String title = event.getView().getTitle();
        if (title.equals("\u00a78Kits")) {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openMainGui(player));
        } else if (title.startsWith("\u00a78Kit Editor")) {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openKitsGui(player));
        }
    }

    private void importPlayerInventory(Player player, int kitNumber) {
        ItemStack off;
        Inventory inv = player.getOpenInventory().getTopInventory();
        for (int i = 0; i < 36; ++i) {
            ItemStack is = player.getInventory().getItem(i);
            if (is == null || is.getType() == Material.AIR) continue;
            inv.setItem(i, this.makeUnbreakableClone(is));
        }
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor[3] != null) {
            inv.setItem(45, this.makeUnbreakableClone(armor[3]));
        }
        if (armor[2] != null) {
            inv.setItem(46, this.makeUnbreakableClone(armor[2]));
        }
        if (armor[1] != null) {
            inv.setItem(47, this.makeUnbreakableClone(armor[1]));
        }
        if (armor[0] != null) {
            inv.setItem(48, this.makeUnbreakableClone(armor[0]));
        }
        if ((off = player.getInventory().getItemInOffHand()) != null && off.getType() != Material.AIR) {
            inv.setItem(49, this.makeUnbreakableClone(off));
        }
        player.sendMessage("\u00a7a[Kit] Imported inventory!");
    }

    private ItemStack makeUnbreakableClone(ItemStack is) {
        ItemStack c = is.clone();
        this.makeUnbreakable(c);
        return c;
    }

    private void saveKit(Inventory inv, int kitNumber) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        for (int i = 0; i < 36; ++i) {
            ItemStack is = inv.getItem(i);
            if (is == null || is.getType() == Material.AIR || is.getType() == Material.GRAY_STAINED_GLASS_PANE) continue;
            items.add(is.clone());
        }
        this.plugin.getKitsConfig().set("kits.kit" + kitNumber + ".items", items);
        String[] armorPaths = new String[]{"armor.helmet", "armor.chestplate", "armor.leggings", "armor.boots", "offhand"};
        for (int i = 0; i < 5; ++i) {
            ItemStack is = inv.getItem(45 + i);
            if (is != null && is.getType() != Material.AIR && is.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                this.plugin.getKitsConfig().set("kits.kit" + kitNumber + "." + armorPaths[i], (Object)is.clone());
                continue;
            }
            this.plugin.getKitsConfig().set("kits.kit" + kitNumber + "." + armorPaths[i], null);
        }
        this.plugin.saveKitsConfig();
    }

    public void openSignEditor(Player player, SignInputType type) {
        this.pendingSignInputs.put(player.getUniqueId(), type);
        player.sendMessage("\u00a7e[Event] Type value in chat (e.g. \u00a7f5m\u00a7e, \u00a7f30s\u00a7e, \u00a7f100\u00a7e):");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (this.pendingSignInputs.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            SignInputType type = this.pendingSignInputs.remove(player.getUniqueId());
            String msg = event.getMessage().trim().toLowerCase();
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                EventManager em = this.plugin.getEventManager();
                try {
                    if (type == SignInputType.TIMER) {
                        int secs = 300;
                        secs = msg.endsWith("m") ? Integer.parseInt(msg.substring(0, msg.length() - 1)) * 60 : (msg.endsWith("s") ? Integer.parseInt(msg.substring(0, msg.length() - 1)) : Integer.parseInt(msg));
                        em.setCountdownSeconds(secs);
                        player.sendMessage("\u00a7a[Event] Timer set to " + msg + "!");
                    } else if (type == SignInputType.BORDER) {
                        int b = Integer.parseInt(msg);
                        em.setBorderSize(b);
                        player.sendMessage("\u00a7a[Event] Border set to " + b + "x" + b + "!");
                    }
                }
                catch (Exception ex) {
                    player.sendMessage("\u00a7c[Event] Invalid input.");
                }
                this.openMainGui(player);
            });
        }
    }

    public static enum SignInputType {
        TIMER,
        BORDER;

    }
}

