package com.minkang.tutorial.listeners;

import com.minkang.tutorial.TutorialWarpPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;

/**
 * /튜토리얼 시작 GUI 클릭 처리.
 */
public class TutorialGuiListener implements Listener {

    private final TutorialWarpPlugin plugin;

    public TutorialGuiListener(TutorialWarpPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryView view = e.getView();
        if (view == null) return;
        String title = ChatColor.stripColor(view.getTitle());
        String expected = ChatColor.stripColor(plugin.getModeGuiTitle());
        if (!expected.equalsIgnoreCase(title)) return;

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());

        String mode;
        if (name.contains("풀")) {
            mode = "full";
        } else if (name.contains("요약")) {
            mode = "quick";
        } else if (name.contains("시스템")) {
            mode = "system";
        } else {
            return;
        }

        plugin.setMode(p, mode);
        p.closeInventory();
    }
}
