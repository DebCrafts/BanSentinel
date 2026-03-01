package com.deb.simpleban;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.BanList;
import org.bukkit.BanEntry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BanCommand implements CommandExecutor {
    private final SimpleBan plugin;
    private final Set<UUID> pendingUnbanAll = new HashSet<>();

    public BanCommand(SimpleBan plugin) { 
        this.plugin = plugin; 
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("simpleban.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length < 1) return false;

        if (label.equalsIgnoreCase("unban")) {
            if (args[0].equals("*")) return handleUnbanAll(sender);
            if (args[0].equalsIgnoreCase("confirm")) return confirmUnbanAll(sender);
            return handleSingleUnban(sender, args[0]);
        }

        String targetName = args[0];
        long duration = 0;
        StringBuilder reasonBuilder = new StringBuilder();
        boolean silent = false;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if (arg.equals("-s")) {
                silent = true;
            } else if (duration == 0 && isTime(arg)) {
                duration = parseTime(arg);
            } else {
                reasonBuilder.append(args[i]).append(" ");
            }
        }

        long expTime = (duration == 0) ? -1 : System.currentTimeMillis() + duration;
        Date expiryDate = (duration == 0) ? null : new Date(expTime);
        
        // Fix: Translate colors for the reason so it doesn't show &c in the kick screen
        String rawReason = reasonBuilder.toString().trim().isEmpty() ? "Banned by Admin" : reasonBuilder.toString().trim();
        String finalReason = ChatColor.translateAlternateColorCodes('&', rawReason);
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, finalReason, expiryDate, sender.getName());
        
        save(target.getUniqueId(), targetName, rawReason, expTime, label.equalsIgnoreCase("ipban"));

        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            String appeal = plugin.getConfig().getString("messages.appeal-link", "");
            String kickMsg = ChatColor.RED + "" + ChatColor.BOLD + "[BanSentinel]\n" + 
                             ChatColor.GRAY + "Reason: " + ChatColor.WHITE + finalReason + "\n" + 
                             ChatColor.translateAlternateColorCodes('&', appeal);
            online.kick(Component.text(kickMsg));
        }

        if (!silent) {
            String broadcastMsg = ChatColor.GOLD + "[BanSentinel] " + ChatColor.WHITE + targetName + 
                                  ChatColor.GRAY + " banned by " + ChatColor.YELLOW + sender.getName();
            Bukkit.broadcast(Component.text(broadcastMsg));
        }
        return true;
    }

    private boolean isTime(String s) {
        return s.matches("\\d+[dhms]");
    }

    private long parseTime(String s) {
        try {
            long v = Long.parseLong(s.substring(0, s.length() - 1));
            return switch (s.substring(s.length() - 1)) {
                case "d" -> v * 86400000L;
                case "h" -> v * 3600000L;
                case "m" -> v * 60000L;
                case "s" -> v * 1000L;
                default -> 0;
            };
        } catch (Exception ignored) {}
        return 0;
    }

    private boolean handleUnbanAll(CommandSender sender) {
        if (!(sender instanceof Player p)) return true;
        pendingUnbanAll.add(p.getUniqueId());
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "BanSentinel " + ChatColor.DARK_GRAY + "» " + ChatColor.YELLOW + "Unban ALL? Type " + ChatColor.WHITE + "/unban confirm");
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean confirmUnbanAll(CommandSender sender) {
        if (!(sender instanceof Player p) || !pendingUnbanAll.contains(p.getUniqueId())) return true;
        
        // Fix: Use BanList<?> wildcard to satisfy the raw type warning
        BanList<?> list = Bukkit.getBanList(BanList.Type.NAME);
        for (BanEntry<?> entry : list.getBanEntries()) {
            list.pardon(entry.getTarget().toString());
        }

        File folder = new File(plugin.getDataFolder(), "active_bans");
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) f.renameTo(new File(plugin.getDataFolder(), "history/" + f.getName()));
        }
        pendingUnbanAll.remove(p.getUniqueId());
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "BanSentinel " + ChatColor.DARK_GRAY + "» " + ChatColor.GREEN + "All cleared.");
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleSingleUnban(CommandSender sender, String target) {
        Bukkit.getBanList(BanList.Type.NAME).pardon(target);
        Bukkit.getBanList(BanList.Type.IP).pardon(target);

        File folder = new File(plugin.getDataFolder(), "active_bans");
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                FileConfiguration data = YamlConfiguration.loadConfiguration(f);
                if (data.getString("username", "").equalsIgnoreCase(target)) {
                    f.renameTo(new File(plugin.getDataFolder(), "history/" + f.getName()));
                }
            }
        }
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "BanSentinel " + ChatColor.DARK_GRAY + "» " + ChatColor.GREEN + "Pardoned " + target);
        return true;
    }

    private void save(UUID id, String name, String r, long e, boolean ip) {
        File f = new File(plugin.getDataFolder(), "active_bans/" + id + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        Player p = Bukkit.getPlayer(id);
        cfg.set("username", name);
        cfg.set("ip", p != null ? p.getAddress().getAddress().getHostAddress() : "Unknown");
        cfg.set("reason", r);
        cfg.set("expiry", e);
        cfg.set("is_ip", ip);
        try { cfg.save(f); } catch (IOException ignored) {}
    }
}