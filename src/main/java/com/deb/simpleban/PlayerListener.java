package com.deb.simpleban;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import java.io.File;

public class PlayerListener implements Listener {
    private final SimpleBan plugin;

    public PlayerListener(SimpleBan plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        File folder = new File(plugin.getDataFolder(), "active_bans");
        File banFile = new File(folder, event.getUniqueId() + ".yml");

        if (!banFile.exists()) {
            checkIPBan(event, folder);
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(banFile);
        long expiry = cfg.getLong("expiry");

        if (expiry != -1 && System.currentTimeMillis() > expiry) {
            // Fix: Use wildcard BanList<?>
            BanList<?> list = Bukkit.getBanList(BanList.Type.NAME);
            list.pardon(event.getName());
            
            banFile.renameTo(new File(plugin.getDataFolder(), "history/" + banFile.getName()));
            return;
        }

        // Fix: Translate colors for the reason and appeal link
        String rawReason = ChatColor.translateAlternateColorCodes('&', cfg.getString("reason", "Banned"));
        String appeal = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.appeal-link", "Appeal on Discord"));
        
        String fullMessage = ChatColor.RED + "" + ChatColor.BOLD + "[BanSentinel]\n" +
                             ChatColor.GRAY + "Reason: " + ChatColor.WHITE + rawReason + "\n" +
                             appeal;

        Component kickComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(fullMessage);
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickComponent);
    }

    private void checkIPBan(AsyncPlayerPreLoginEvent event, File folder) {
        File[] files = folder.listFiles();
        if (files == null) return;

        String ip = event.getAddress().getHostAddress();
        for (File f : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            if (cfg.getBoolean("is_ip") && ip.equals(cfg.getString("ip"))) {
                String rawReason = ChatColor.translateAlternateColorCodes('&', cfg.getString("reason", "IP Blacklisted"));
                
                String fullMessage = ChatColor.RED + "" + ChatColor.BOLD + "[BanSentinel]\n" +
                                     ChatColor.GRAY + "Your IP is blacklisted.\n" +
                                     ChatColor.GRAY + "Reason: " + ChatColor.WHITE + rawReason;
                
                Component kickComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(fullMessage);
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickComponent);
                break;
            }
        }
    }
}