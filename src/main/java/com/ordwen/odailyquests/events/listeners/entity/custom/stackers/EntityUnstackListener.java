package com.ordwen.odailyquests.events.listeners.entity.custom.stackers;

import com.bgsoftware.wildstacker.api.events.EntityUnstackEvent;
import com.ordwen.odailyquests.configuration.integrations.WildStackerEnabled;
import com.ordwen.odailyquests.enums.QuestType;
import com.ordwen.odailyquests.events.antiglitch.EntitySource;
import com.ordwen.odailyquests.quests.player.progression.checkers.AbstractEntityChecker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class EntityUnstackListener extends AbstractEntityChecker implements Listener {

    @EventHandler
    public void onWildStackerEntityUnstackEvent(EntityUnstackEvent event) {
        if (event.isCancelled()) return;

        final Entity entity = event.getEntity().getLivingEntity();
        final boolean isEntityFromSpawner = EntitySource.isEntityFromSpawner(entity);
        EntitySource.removeEntityFromSpawner(entity);

        if (!WildStackerEnabled.isEnabled()) return;
        if (event.getUnstackSource() == null) return;
        if (isEntityFromSpawner) return;

        if (event.getUnstackSource() instanceof Player player) {
            setPlayerQuestProgression(player, event.getEntity().getType(), null, event.getAmount(), QuestType.KILL, null);
        }
    }
}
