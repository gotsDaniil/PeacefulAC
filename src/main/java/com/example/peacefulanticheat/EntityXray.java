package com.example.peacefulanticheat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.Material;

public class EntityXray implements Listener {

    // Проверка выключена, незнаю нормальной логики проверки EntityXray + кривой код
    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            final Player attacker = (Player) event.getDamager();
            final Player defender = (Player) event.getEntity();

            if (defender.hasPotionEffect(PotionEffectType.INVISIBILITY) && defender.isSneaking() && defender.getInventory().getItemInMainHand().getType() == Material.AIR
                    && !defender.hasPotionEffect(PotionEffectType.GLOWING)) {

                for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
                    if (nearbyPlayer.getLocation().distance(defender.getLocation()) <= 3) {
                        spawnInvisibleBot(nearbyPlayer.getLocation(), 255, 20);
                        break;
                    }
                }
            }
        }
    }

    public void spawnInvisibleBot(Location location, int level, int health) {
        ArmorStand bot = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        bot.setVisible(false);
        bot.setCustomName("YaToper_A_TI_NET");
        bot.setCustomNameVisible(false);
        bot.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, level, false, false));

        bot.setHealth(health);

        bot.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        bot.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 1, false, false));
    }
}