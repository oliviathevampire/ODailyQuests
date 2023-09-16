package com.ordwen.odailyquests.events.listeners.entity;

import com.ordwen.odailyquests.enums.QuestType;
import com.ordwen.odailyquests.quests.player.progression.checkers.AbstractEntityChecker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

public class ProjectileHitListener extends AbstractEntityChecker implements Listener {

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.isCancelled()) return;

        final Entity entity = event.getHitEntity();
        if (entity == null) return;

        final ProjectileSource shooter = event.getEntity().getShooter();
        if (!(shooter instanceof Player player)) return;

        // check if player is reflecting fireball
        if (event.getEntityType() == EntityType.FIREBALL) {
            if (entity instanceof Ghast) {
                setPlayerQuestProgression(player, EntityType.GHAST, null, 1, QuestType.KILL, null);
            }
        }
    }
}
