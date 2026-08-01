package de.andrechan2008.damagelogSystemGM.listener;

import de.andrechan2008.damagelogSystemGM.builder.DamagelogBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/// Erfasst Schlaege zwischen Spielern und -Kills fuer den [DamagelogBuilder].
public class PlayerHitListener implements Listener {

    private static final Set<EntityDamageEvent.DamageCause> DIRECT_HIT_CAUSES = EnumSet.of(EntityDamageEvent.DamageCause.ENTITY_ATTACK, EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK, EntityDamageEvent.DamageCause.PROJECTILE );

    /**
     * Erfasst Schlaege zwischen Spielern
     *
     * @param event ausgeloestes Schadens-Event
     */
    @EventHandler
    public void onEntityHit(EntityDamageEvent event) {
        if (!DIRECT_HIT_CAUSES.contains(event.getCause())) {
            return;
        }

        Entity attackerEntity = event.getDamageSource().getCausingEntity();

        if (attackerEntity instanceof Player attacker && event.getEntity() instanceof Player victim && attacker != victim) {
            Component weapon = getWeaponDisplayName(attacker);
            DamagelogBuilder.addHit(attacker.getName(), victim.getName(), LocalTime.now(), false, weapon);
        }
    }

    /**
     * Erfasst einen Kill durch einen Spieler.
     *
     * @param event ausgeloestes Todes-Event
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        EntityDamageEvent lastDamageCause = event.getEntity().getLastDamageCause();

        if (null == lastDamageCause || !DIRECT_HIT_CAUSES.contains(lastDamageCause.getCause())) {
            return;
        }

        Entity attackerEntity = event.getDamageSource().getCausingEntity();

        if (attackerEntity instanceof Player attacker && event.getEntity() instanceof Player victim && attacker != victim) {
            Component weapon = getWeaponDisplayName(attacker);
            DamagelogBuilder.addHit(attacker.getName(), victim.getName(), LocalTime.now(), true, weapon);
        }
    }

    /**
     * Gibt den Anzeigenamen des items in der Hand von 'attacker'. Ist die Hand
     * leer, wird stattdessen "Hand" zurueckgegeben.
     *
     * @param attacker Spieler, dessen item in der Hand ausgelesen wird
     * @return Anzeigename des items in der Hand des attacker
     */
    private Component getWeaponDisplayName(Player attacker) {
        Component weapon = attacker.getInventory().getItemInMainHand().displayName();

        if (attacker.getInventory().getItemInMainHand().getType().isAir()) {
            return Component.text("Hand");
        }

        return weapon;
    }
}