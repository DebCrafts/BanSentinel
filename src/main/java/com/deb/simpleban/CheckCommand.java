package com.deb.simpleban;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

public class CheckCommand implements CommandExecutor {
    private final SimpleBan plugin;

    public CheckCommand(SimpleBan plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("simpleban.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length < 1) return false;

        String target = args[0];
        sender.sendMessage("§6§lAudit Record: §f" + target);
        
        lookup(sender, new File(plugin.getDataFolder(), "active_bans"), "§c[ACTIVE]", target);
        lookup(sender, new File(plugin.getDataFolder(), "history"), "§7[HISTORY]", target);
        return true;
    }

    private void lookup(CommandSender sender, File dir, String tag, String target) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            String name = cfg.getString("username", "");
            
            if (name.equalsIgnoreCase(target)) {
                String ip = cfg.getString("ip", "Unknown");
                String reason = cfg.getString("reason", "No reason provided");
                sender.sendMessage(tag + " §7IP: §f" + ip + " §8| §7Reason: §f" + reason);
            }
        }
    }
}