package com.kvwx.events;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

public class WorldRegenManager {
    private final EventsPlugin plugin;
    private final File backupRoot;

    public WorldRegenManager(EventsPlugin plugin) {
        this.plugin = plugin;
        this.backupRoot = new File(this.plugin.getDataFolder(), this.plugin.getConfig().getString("regen.backup-folder", "regen"));
        if (!this.backupRoot.exists()) {
            this.backupRoot.mkdirs();
        }
    }

    public boolean saveSnapshot(String worldName) {
        if (worldName == null || worldName.trim().isEmpty()) {
            return false;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return false;
        }
        File sourceWorld = world.getWorldFolder();
        File snapshot = new File(this.backupRoot, worldName);
        try {
            deleteDirectory(snapshot);
            copyDirectory(sourceWorld.toPath(), snapshot.toPath());
            return true;
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Could not save regen snapshot for " + worldName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean restoreWorld(String worldName) {
        if (worldName == null || worldName.trim().isEmpty()) {
            return false;
        }
        File snapshot = new File(this.backupRoot, worldName);
        if (!snapshot.exists()) {
            return false;
        }
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        World current = Bukkit.getWorld(worldName);
        if (current != null) {
            Bukkit.unloadWorld(worldName, false);
        }
        try {
            deleteDirectory(worldFolder);
            copyDirectory(snapshot.toPath(), worldFolder.toPath());
            World restored = Bukkit.createWorld(new WorldCreator(worldName));
            return restored != null;
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Could not restore regen snapshot for " + worldName + ": " + e.getMessage());
            return false;
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(path -> {
                try {
                    Path targetPath = target.resolve(source.relativize(path));
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(path, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
        catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException) {
                throw (IOException)ex.getCause();
            }
            throw ex;
        }
    }

    private void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        try (Stream<Path> stream = Files.walk(directory.toPath())) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException) {
                throw (IOException)ex.getCause();
            }
            throw ex;
        }
    }
}
