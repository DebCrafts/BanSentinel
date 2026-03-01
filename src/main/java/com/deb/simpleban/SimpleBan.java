package com.deb.simpleban;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;

public class SimpleBan extends JavaPlugin {
    private GUIHandler guiHandler;

    @Override
    public void onEnable() {
        printBranding();

        String[] dirs = {"active_bans", "history", "backups"};
        for (String dir : dirs) {
            File f = new File(getDataFolder(), dir);
            if (!f.exists()) f.mkdirs();
        }

        saveDefaultConfig();
        this.guiHandler = new GUIHandler(this);

        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("ipban").setExecutor(new BanCommand(this));
        getCommand("unban").setExecutor(new BanCommand(this));
        getCommand("check").setExecutor(new CheckCommand(this));
        
        getCommand("banreload").setExecutor((sender, cmd, label, args) -> {
            if (!sender.hasPermission("simpleban.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            reloadConfig();
            sender.sendMessage("§6§l[BanSentinel] §fConfig reloaded.");
            return true;
        });

        getCommand("punish").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player p && args.length > 0) {
                guiHandler.openPunishMenu(p, args[0]);
            }
            return true;
        });

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(guiHandler, this);
        
        startBackupTask();
    }

    private void printBranding() {
        getLogger().info(" ");
        getLogger().info(" §6 ____                 §e ____             _   _             _ ");
        getLogger().info(" §6| __ )  __ _ _ __    §e/ ___|  ___ _ __ | |_(_)_ __   ___| |");
        getLogger().info(" §6|  _ \\ / _` | '_ \\   §e\\___ \\ / _ \\ '_ \\| __| | '_ \\ / _ \\ |");
        getLogger().info(" §6| |_) | (_| | | | |   §e___) |  __/ | | | |_| | | | |  __/ |");
        getLogger().info(" §6|____/ \\__,_|_| |_|  §e|____/ \\___|_| |_|\\__|_|_| |_|\\___|_|");
        getLogger().info("                         §7v1.0.0 by deb");
        getLogger().info(" ");
    }

    private void startBackupTask() {
        long ticks = getConfig().getLong("settings.backup-frequency-hours", 6) * 72000L; 

        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            File active = new File(getDataFolder(), "active_bans");
            File backupRoot = new File(getDataFolder(), "backups");
            File target = new File(backupRoot, "backup-" + System.currentTimeMillis());

            if (active.exists() && active.listFiles() != null) {
                try {
                    target.mkdirs();
                    for (File f : active.listFiles()) {
                        Files.copy(f.toPath(), new File(target, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    cleanupOldBackups(backupRoot);
                } catch (IOException ignored) {}
            }
        }, 1200L, ticks);
    }

    private void cleanupOldBackups(File root) {
        File[] backups = root.listFiles(File::isDirectory);
        int max = getConfig().getInt("settings.max-backups-to-keep", 5);
        if (backups != null && backups.length > max) {
            Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < backups.length - max; i++) {
                deleteDir(backups[i]);
            }
        }
    }

    private void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) f.delete();
        dir.delete();
    }
}