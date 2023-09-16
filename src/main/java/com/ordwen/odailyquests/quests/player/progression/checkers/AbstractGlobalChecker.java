package com.ordwen.odailyquests.quests.player.progression.checkers;

import com.ordwen.odailyquests.configuration.essentials.Synchronization;
import com.ordwen.odailyquests.configuration.functionalities.DisabledWorlds;
import com.ordwen.odailyquests.enums.QuestType;
import com.ordwen.odailyquests.quests.player.QuestsManager;
import com.ordwen.odailyquests.quests.player.progression.AbstractProgressionIncreaser;
import com.ordwen.odailyquests.quests.player.progression.Progression;
import com.ordwen.odailyquests.quests.types.AbstractQuest;
import com.ordwen.odailyquests.tools.PluginLogger;
import org.bukkit.entity.Player;

import java.util.HashMap;

public abstract class AbstractGlobalChecker extends AbstractProgressionIncreaser {

    /**
     * Increase player quest progression.
     *
     * @param player    the player to increase progression for.
     * @param amount    the amount to increase progression by.
     * @param questType the quest type to increase progression for.
     */
    public void setPlayerQuestProgression(Player player, int amount, QuestType questType) {
        if (!QuestsManager.getActiveQuests().containsKey(player.getName())) {
            PluginLogger.warn(player.getName() + " is not in the active quests list.");
            return;
        }

        if (DisabledWorlds.isWorldDisabled(player.getWorld().getName())) {
            return;
        }

        final HashMap<AbstractQuest, Progression> playerQuests = QuestsManager.getActiveQuests().get(player.getName()).getPlayerQuests();

        for (AbstractQuest abstractQuest : playerQuests.keySet()) {

            final Progression progression = playerQuests.get(abstractQuest);
            if (!progression.isAchieved() && abstractQuest.getQuestType() == questType) {
                increaseProgression(player, progression, abstractQuest, amount);
                if (!Synchronization.isSynchronised()) {
                    break;
                }
            }
        }
    }
}
