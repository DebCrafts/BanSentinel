package com.deb.simpleban;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIHandler implements Listener {
    private final Map<UUID, String> activeSessions = new HashMap<>();

    // Added this constructor back to fix the "Undefined" error
    public GUIHandler(SimpleBan plugin) {
        // We don't need to store 'plugin' if we don't use it, 
        // but the constructor must exist for SimpleBan.java to work.
    }

    public void openPunishMenu(Player admin, String target) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Punish: " + target));
        activeSessions.put(admin.getUniqueId(), target);

        gui.setItem(11, createItem(Material.IRON_AXE, "§e7 Day Ban", "§7Reason: Griefing"));
        gui.setItem(13, createItem(Material.GOLDEN_AXE, "§630 Day Ban", "§7Reason: Cheating"));
        gui.setItem(15, createItem(Material.NETHERITE_AXE, "§cPermanent Ban", "§7Reason: Severe Violation"));

        admin.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.startsWith("Punish: ")) return;
        
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player admin)) return;
        String target = activeSessions.get(admin.getUniqueId());
        if (target == null) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        String action = "";
        if (item.getType() == Material.IRON_AXE) action = "ban " + target + " 7d Griefing";
        else if (item.getType() == Material.GOLDEN_AXE) action = "ban " + target + " 30d Cheating";
        else if (item.getType() == Material.NETHERITE_AXE) action = "ban " + target + " Permanent";

        if (!action.isEmpty()) {
            admin.performCommand(action);
            admin.closeInventory();
            activeSessions.remove(admin.getUniqueId());
        }
    }

    private ItemStack createItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.lore(java.util.List.of(Component.text(lore)));
            item.setItemMeta(meta);
        }
        return item;
    }
}