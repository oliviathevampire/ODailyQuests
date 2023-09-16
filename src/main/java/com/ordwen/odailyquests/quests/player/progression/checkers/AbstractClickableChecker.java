package com.ordwen.odailyquests.quests.player.progression.checkers;

import com.ordwen.odailyquests.api.events.QuestCompletedEvent;
import com.ordwen.odailyquests.api.progression.ValidateItemQuest;
import com.ordwen.odailyquests.configuration.essentials.Synchronization;
import com.ordwen.odailyquests.configuration.functionalities.DisabledWorlds;
import com.ordwen.odailyquests.enums.QuestsMessages;
import com.ordwen.odailyquests.events.antiglitch.OpenedRecipes;
import com.ordwen.odailyquests.externs.hooks.placeholders.PAPIHook;
import com.ordwen.odailyquests.quests.ConditionType;
import com.ordwen.odailyquests.enums.QuestType;
import com.ordwen.odailyquests.quests.player.QuestsManager;
import com.ordwen.odailyquests.quests.player.progression.AbstractProgressionIncreaser;
import com.ordwen.odailyquests.quests.player.progression.Progression;
import com.ordwen.odailyquests.quests.types.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;

public abstract class AbstractClickableChecker extends AbstractProgressionIncreaser {

    /**
     * Increase player quest progression.
     *
     * @param player the player to increase progression for.
     */
    public void setPlayerQuestProgression(Player player, ItemStack clickedItem) {
        if (DisabledWorlds.isWorldDisabled(player.getWorld().getName())) {
            final String msg = QuestsMessages.WORLD_DISABLED.getMessage(player);
            if (msg != null) player.sendMessage(msg);

            return;
        }

        if (QuestsManager.getActiveQuests().containsKey(player.getName())) {

            final HashMap<AbstractQuest, Progression> playerQuests = QuestsManager.getActiveQuests().get(player.getName()).getPlayerQuests();
            for (AbstractQuest abstractQuest : playerQuests.keySet()) {

                if (abstractQuest instanceof ItemQuest quest) {
                    if (isAppropriateQuestMenuItem(clickedItem, quest.getMenuItem()) && quest.getQuestType() == QuestType.GET) {
                        final Progression progression = playerQuests.get(abstractQuest);
                        if (!progression.isAchieved()) {
                            ValidateItemQuest.makeQuestProgress(player, progression, quest);
                        }
                        break;
                    }
                }

                else if (abstractQuest instanceof LocationQuest quest) {
                    if (isAppropriateQuestMenuItem(clickedItem, quest.getMenuItem()) && quest.getQuestType() == QuestType.LOCATION) {

                        final Progression progression = playerQuests.get(quest);
                        if (!progression.isAchieved()) {
                            validateLocationQuestType(player, progression, quest);
                        }
                    }
                }

                else if (abstractQuest instanceof PlaceholderQuest quest) {
                    if (isAppropriateQuestMenuItem(clickedItem, quest.getMenuItem()) && quest.getQuestType() == QuestType.PLACEHOLDER) {

                        final Progression progression = playerQuests.get(quest);
                        if (!progression.isAchieved()) {
                            validatePlaceholderQuestType(player, progression, quest);
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if the clicked item is corresponding to a quest menu item, by checking the persistent data container.
     * @param clickedItem clicked item to check.
     * @param menuItem quest menu item to compare.
     * @return true if the clicked item is a GET quest menu item.
     */
    private boolean isAppropriateQuestMenuItem(ItemStack clickedItem, ItemStack menuItem) {
        ItemMeta clickedItemMeta = clickedItem.getItemMeta();
        ItemMeta menuItemMeta = menuItem.getItemMeta();

        if (clickedItem.getType() != menuItem.getType()) return false;
        if (clickedItemMeta == null || menuItemMeta == null) return false;

        return clickedItemMeta.getPersistentDataContainer().equals(menuItemMeta.getPersistentDataContainer());
    }

    /**
     * Validate VILLAGER_TRADE quest type.
     *
     * @param player         player who is trading.
     * @param villager       villager which one the player is trading.
     * @param selectedRecipe item trade.
     * @param quantity       quantity trade.
     */
    public void validateTradeQuestType(Player player, Villager villager, MerchantRecipe selectedRecipe, int quantity) {
        if (QuestsManager.getActiveQuests().containsKey(player.getName())) {

            HashMap<AbstractQuest, Progression> playerQuests = QuestsManager.getActiveQuests().get(player.getName()).getPlayerQuests();

            for (AbstractQuest abstractQuest : playerQuests.keySet()) {
                if (abstractQuest instanceof VillagerQuest quest) {
                    boolean valid = false;
                    Progression questProgression = playerQuests.get(quest);
                    if (!questProgression.isAchieved() && quest.getQuestType() == QuestType.VILLAGER_TRADE) {

                        if (quest.getRequiredItems() == null) valid = true;
                        else {
                            for (ItemStack item : quest.getRequiredItems()) {
                                if (item.getType() == selectedRecipe.getResult().getType()) valid = true;
                            }
                        }

                        if (selectedRecipe.getUses() <= OpenedRecipes.get(selectedRecipe)) {
                            valid = false;
                        }

                        if (valid) {
                            if (villager != null) {
                                if (quest.getVillagerProfession() != null) {
                                    if (!quest.getVillagerProfession().equals(villager.getProfession())) {
                                        valid = false;
                                    }
                                }
                                if (quest.getVillagerLevel() != 0) {
                                    if (!(quest.getVillagerLevel() == villager.getVillagerLevel())) {
                                        valid = false;
                                    }
                                }
                            }
                        }

                        if (valid) {
                            increaseProgression(player, questProgression, quest, quantity);
                            if (!Synchronization.isSynchronised()) break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Validate LOCATION quest type.
     *
     * @param player      player who is on the location.
     * @param progression progression of the quest.
     * @param quest       quest to validate.
     */
    private void validateLocationQuestType(Player player, Progression progression, LocationQuest quest) {
        final Location requiredLocation = quest.getRequiredLocation();

        if (!requiredLocation.getWorld().equals(player.getLocation().getWorld())) {
            final String msg = QuestsMessages.BAD_WORLD_LOCATION.getMessage(player);
            if (msg != null) player.sendMessage(msg);
            return;
        }

        double distance = player.getLocation().distance(requiredLocation);
        if (distance <= quest.getRadius()) {
            final QuestCompletedEvent event = new QuestCompletedEvent(player, progression, quest);
            Bukkit.getPluginManager().callEvent(event);

            player.closeInventory();
        } else {
            final String msg = QuestsMessages.TOO_FAR_FROM_LOCATION.getMessage(player);
            if (msg != null) player.sendMessage(msg);
        }
    }

    /**
     * Validate PLACEHOLDER quest type.
     *
     * @param player player who is checking the placeholder.
     * @param progression progression of the quest.
     * @param quest quest to validate.
     */
    private void validatePlaceholderQuestType(Player player, Progression progression, PlaceholderQuest quest) {
        final String placeholder = quest.getPlaceholder();
        final String value = quest.getExpectedValue();
        final ConditionType conditionType = quest.getConditionType();

        if (!PAPIHook.isPlaceholderAPIHooked()) {
            final String msg = QuestsMessages.PLACEHOLDER_API_NOT_ENABLED.toString();
            if (msg != null) player.sendMessage(msg);
            return;
        }

        final String placeholderValue = PAPIHook.getPlaceholders(player, placeholder);

        boolean valid = false;
        switch (conditionType) {
            case EQUALS -> valid = placeholderValue.equals(value);
            case NOT_EQUALS -> valid = !placeholderValue.equals(value);

            case GREATER_THAN, GREATER_THAN_OR_EQUALS, LESS_THAN, LESS_THAN_OR_EQUALS -> {
                float currentNumberValue = 0;
                float expectedNumberValue = 0;

                try {
                    currentNumberValue = Float.parseFloat(placeholderValue);
                    expectedNumberValue = Float.parseFloat(value);
                } catch (NumberFormatException exception) {
                    String msg = QuestsMessages.PLACEHOLDER_NOT_NUMBER.toString();
                    if (msg != null) {
                        msg = msg.replace("%placeholder%", placeholder);
                        player.sendMessage(msg);
                    }
                }

                switch (conditionType) {
                    case GREATER_THAN -> valid = currentNumberValue > expectedNumberValue;
                    case GREATER_THAN_OR_EQUALS -> valid = currentNumberValue >= expectedNumberValue;
                    case LESS_THAN -> valid = currentNumberValue < expectedNumberValue;
                    case LESS_THAN_OR_EQUALS -> valid = currentNumberValue <= expectedNumberValue;
                }
            }
        }

        if (valid) {
            final QuestCompletedEvent event = new QuestCompletedEvent(player, progression, quest);
            Bukkit.getPluginManager().callEvent(event);

            player.closeInventory();
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', quest.getErrorMessage()));
        }
    }
}
