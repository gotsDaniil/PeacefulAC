package com.example.peacefulanticheat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

// Эта проверка оключена, из-за плохой логики
public class FastSlots implements Listener {
    private final Map<Player, Long> lastSwitchTime = new HashMap<>();
    private final long switchDelay = 200; // 0.2 секунды в миллисекундах

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Проверяем, что игрок переключает слот
        if (event.getSlotType() == InventoryType.SlotType.QUICKBAR) {
            long currentTime = System.currentTimeMillis();
            long lastTime = lastSwitchTime.getOrDefault(player, 0L);

            if (currentTime - lastTime <= switchDelay) {
                // Если переключаемся на слот с определенным предметом
                if (event.getCurrentItem() != null) {
                    player.kickPlayer("Вы были кикнуты за быстрый переключение слотов.");
                }
            }
            // Обновляем время последнего переключения
            lastSwitchTime.put(player, currentTime);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
    }
}

