package com.ordwen.odailyquests.quests;

import com.ordwen.odailyquests.ODailyQuests;
import com.ordwen.odailyquests.enums.QuestType;
import com.ordwen.odailyquests.quests.getters.QuestItemGetter;
import com.ordwen.odailyquests.quests.types.*;
import com.ordwen.odailyquests.rewards.Reward;
import com.ordwen.odailyquests.rewards.RewardType;
import com.ordwen.odailyquests.tools.ColorConvert;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.ordwen.odailyquests.tools.PluginLogger;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

public class QuestsLoader extends QuestItemGetter {

    /**
     * Load the reward of a quest.
     *
     * @param questSection the current quest section.
     * @param fileName     the file name where the quest is.
     * @param questIndex   the quest index in the file.
     * @return the reward of the quest.
     */
    private Reward createReward(ConfigurationSection questSection, String fileName, int questIndex) {
        if (!questSection.isConfigurationSection(".reward")) return new Reward(RewardType.NONE, 0);
        final ConfigurationSection rewardSection = questSection.getConfigurationSection(".reward");

        RewardType rewardType;
        try {
            rewardType = RewardType.valueOf(rewardSection.getString(".reward_type"));
        } catch (Exception e) {
            configurationError(fileName, questIndex, "reward_type", rewardSection.getString(".reward_type") + " is not a valid reward type.");
            rewardType = RewardType.NONE;
        }

        return switch (rewardType) {
            case NONE -> new Reward(RewardType.NONE, 0);
            case COMMAND -> new Reward(RewardType.COMMAND, rewardSection.getStringList(".commands"));

            case COINS_ENGINE -> {
                final String currencyLabel = rewardSection.getString(".currency_label");
                final String currencyDisplayName = rewardSection.getString(".currency_display_name");

                if (currencyLabel == null || currencyDisplayName == null) {
                    configurationError(fileName, questIndex, "currency_label", "currency_label or currency_display_name is null.");
                    yield new Reward(RewardType.NONE, 0);
                }

                yield new Reward(RewardType.COINS_ENGINE, currencyLabel, currencyDisplayName, rewardSection.getInt(".amount"));
            }

            default -> new Reward(rewardType, rewardSection.getInt(".amount"));
        };
    }

    /**
     * Create a quest with all basic information.
     *
     * @param questSection the current quest section.
     * @param fileName     the file name where the quest is.
     * @param questIndex   the quest index in the file.
     * @return the global quest.
     */
    private GlobalQuest createBasicQuest(ConfigurationSection questSection, String fileName, int questIndex) {

        /* quest name */
        String questName = ColorConvert.convertColorCode(questSection.getString(".name"));

        /* quest description */
        List<String> questDesc = questSection.getStringList(".description");
        for (String string : questDesc) questDesc.set(questDesc.indexOf(string), ColorConvert.convertColorCode(string));

        /* check if quest uses placeholders */
        boolean usePlaceholders = questSection.getBoolean(".use_placeholders");

        /* quest type */
        QuestType questType;
        final String supposedType = questSection.getString(".quest_type");
        try {
            questType = QuestType.valueOf(supposedType);
        } catch (Exception e) {
            configurationError(fileName, questIndex, "quest_type", supposedType + " is not a valid quest type.");
            return null;
        }

        /* required amount */
        int requiredAmount = questType == QuestType.LOCATION || questType == QuestType.PLACEHOLDER ? 1 : questSection.getInt(".required_amount");
        if (requiredAmount == -1) return null;

        /* required worlds */
        final List<String> requiredWorlds = questSection.getStringList(".required_worlds");

        String presumedItem = questSection.getString(".menu_item");
        if (presumedItem == null) {
            configurationError(fileName, questIndex, "menu_item", "The menu item is not defined.");
            return null;
        }

        final ItemStack menuItem = getItemStackFromMaterial(presumedItem, fileName, questIndex, "menu_item");
        if (menuItem == null) return null;

        ItemStack achievedItem;
        if (questSection.isString("achieved_menu_item")) {
            final String presumedAchievedItem = questSection.getString("achieved_menu_item");
            achievedItem = getItemStackFromMaterial(presumedAchievedItem, fileName, questIndex, "achieved_menu_item");
            if (achievedItem == null) return null;
        }
        else {
            achievedItem = menuItem;
        }

        /* reward */
        Reward reward = createReward(questSection, fileName, questIndex);

        return new GlobalQuest(questIndex, questName, fileName, questDesc, questType, menuItem, achievedItem, requiredAmount, reward, requiredWorlds, usePlaceholders);
    }

    /**
     * Load a quest that require entities.
     *
     * @param base         the base quest.
     * @param questSection the current quest section.
     * @param fileName     the file name where the quest is.
     * @param questIndex   the quest index in the file.
     * @return an entity quest.
     */
    private AbstractQuest loadEntityQuest(GlobalQuest base, ConfigurationSection questSection, String fileName, int questIndex) {
        if (!questSection.contains(".required_entity")) return base;

        /* all required entities */
        final List<EntityType> entityTypes = new ArrayList<>();

        /* color of sheep for quests that require a ship (obviously) */
        String presumedDyeColor = questSection.getString(".sheep_color");
        DyeColor dyeColor = getDyeColor(presumedDyeColor, fileName, questIndex);

        if (questSection.isString(".required_entity")) {
            EntityType entityType = getEntityType(fileName, questIndex, questSection.getString(".required_entity"));
            entityTypes.add(entityType);
        } else {
            for (String presumedEntity : questSection.getStringList(".required_entity")) {
                EntityType entityType = getEntityType(fileName, questIndex, presumedEntity);
                entityTypes.add(entityType);
            }
        }

        return new EntityQuest(base, entityTypes, dyeColor);
    }

    /**
     * Load a quest that require a custom mob.
     * @param base the base quest.
     * @param questSection the current quest section.
     * @param fileName the file name where the quest is.
     * @param questIndex the quest index in the file.
     * @return an entity quest.
     */
    private AbstractQuest loadCustomMobQuest(GlobalQuest base, ConfigurationSection questSection, String fileName, int questIndex) {

        final List<String> entityNames = new ArrayList<>();
        if (questSection.isString(".entity_name")) {
            entityNames.add(questSection.getString(".entity_name"));
        } else {
            entityNames.addAll(questSection.getStringList(".entity_name"));
        }

        for (String entityName : entityNames) {
            entityNames.set(entityNames.indexOf(entityName), ColorConvert.convertColorCode(entityName));
        }

        if (entityNames.isEmpty()) {
            configurationError(fileName, questIndex, null, "There is no entity name defined for quest type CUSTOM_MOBS.");
            return null;
        }

        return new EntityQuest(base, entityNames);
    }

    /**
     * Load a quest that require items.
     *
     * @param base         the base quest.
     * @param questSection the current quest section.
     * @param fileName     the file name where the quest is.
     * @param questIndex   the quest index in the file.
     * @return an item quest.
     */
    private AbstractQuest loadItemQuest(GlobalQuest base, ConfigurationSection questSection, String fileName, int questIndex) {
        if (!questSection.contains(".required_item")) return base;

        /* menu item */
        ItemStack menuItem = base.getMenuItem();

        /* all required items */
        final List<ItemStack> requiredItems = new ArrayList<>();

        /* variables for VILLAGER_TRADE quest */
        Villager.Profession profession = null;
        int villagerLevel = 0;

        final List<String> requiredItemStrings = new ArrayList<>();
        if (questSection.isList(".required_item")) {
            requiredItemStrings.addAll(questSection.getStringList(".required_item"));
        }
        else {
            requiredItemStrings.add(questSection.getString(".required_item"));
        }

        for (String itemType : requiredItemStrings) {

            /* check if the required item is a custom item */
            if (itemType.equals("CUSTOM_ITEM")) {
                final ConfigurationSection section = questSection.getConfigurationSection(".custom_item");
                if (section == null) {
                    configurationError(fileName, questIndex, null, "The custom item is not defined.");
                    return null;
                }
                final ItemStack item = loadCustomItem(section, fileName, questIndex);
                if (item == null) return null;

                requiredItems.add(item);
            }

            else {
                final ItemStack requiredItem = getItemStackFromMaterial(itemType, fileName, questIndex, "required_item");
                if (requiredItem == null) return null;

                if (itemType.equals("POTION") || itemType.equals("SPLASH_POTION") || itemType.equals("LINGERING_POTION")) {
                    final PotionMeta potionMeta = loadPotionItem(questSection, fileName, questIndex, requiredItem);

                    if (potionMeta != null) {
                        requiredItem.setItemMeta(potionMeta);
                        if (menuItem.getType() == Material.POTION
                                || menuItem.getType() == Material.SPLASH_POTION
                                || menuItem.getType() == Material.LINGERING_POTION) {
                            menuItem.setItemMeta(potionMeta);
                        }
                    }
                }

                requiredItems.add(requiredItem);
            }
        }

        /* check if the item have to be obtained by a villager */
        if (questSection.contains(".villager_profession")) {
            profession = Villager.Profession.valueOf(questSection.getString(".villager_profession"));
        }
        if (questSection.contains(".villager_level")) {
            villagerLevel = questSection.getInt(".villager_level");
        }

        /* apply Persistent Data Container to the menu item to differentiate GET quests */
        if (base.getQuestType() == QuestType.GET) {
            ItemMeta meta = menuItem.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();

            container.set(new NamespacedKey(ODailyQuests.INSTANCE, "quest_type"), PersistentDataType.STRING, "get");
            container.set(new NamespacedKey(ODailyQuests.INSTANCE, "quest_index"), PersistentDataType.INTEGER, questIndex);
            container.set(new NamespacedKey(ODailyQuests.INSTANCE, "file_name"), PersistentDataType.STRING, fileName);

            menuItem.setItemMeta(meta);
        }

        if (base.getQuestType() == QuestType.VILLAGER_TRADE) return new VillagerQuest(base, requiredItems, profession, villagerLevel);
        else return new ItemQuest(base, requiredItems);
    }

    /**
     * Load the potion attributes.
     *
     * @param section quest section
     * @param fileName file name where the quest is
     * @param questIndex quest index in the file
     * @param requiredItem current required item
     * @return potion meta
     */
    private PotionMeta loadPotionItem(ConfigurationSection section, String fileName, int questIndex, ItemStack requiredItem) {
        PotionMeta potionMeta = null;

        PotionType potionType;
        boolean upgraded = false;
        boolean extended = false;

        final ConfigurationSection potionSection = section.getConfigurationSection(".potion");
        if (potionSection == null) return null;

        if (potionSection.contains("type")) {
            try {
                potionType = PotionType.valueOf(potionSection.getString("type"));
            }
            catch (IllegalArgumentException e) {
                configurationError(fileName, questIndex, "type", "Invalid potion type.");
                return null;
            }
        } else {
            configurationError(fileName, questIndex, "type", "Potion type is not defined.");
            return null;
        }

        if (potionSection.contains("upgraded")) upgraded = potionSection.getBoolean("upgraded");
        if (potionSection.contains("extended")) extended = potionSection.getBoolean("extended");

        if (upgraded && extended) {
            configurationError(fileName, questIndex, null, "Potion cannot be both upgraded and extended.");
            return null;
        }

        if (requiredItem.getType() == Material.POTION
                || requiredItem.getType() == Material.SPLASH_POTION
                || requiredItem.getType() == Material.LINGERING_POTION) {

            potionMeta = (PotionMeta) requiredItem.getItemMeta();
            potionMeta.setBasePotionData(new PotionData(potionType, extended, upgraded));
        }

        return potionMeta;
    }

    /**
     * Load a required item with custom name and lore.
     *
     * @param section configuration section of the custom item
     * @param fileName file name where the quest is
     * @param questIndex quest index in the file
     * @return the custom item
     */
    private ItemStack loadCustomItem(ConfigurationSection section, String fileName, int questIndex) {
        ItemStack requiredItem = getItemStackFromMaterial(section.getString(".type"), fileName, questIndex, "type (CUSTOM_ITEM)");
        if (requiredItem == null) return null;

        ItemMeta meta = requiredItem.getItemMeta();
        meta.setDisplayName(ColorConvert.convertColorCode(section.getString(".name")));

        List<String> lore = section.getStringList(".lore");
        for (String str : lore) {
            lore.set(lore.indexOf(str), ColorConvert.convertColorCode(str));
        }
        meta.setLore(lore);
        requiredItem.setItemMeta(meta);

        return requiredItem;
    }

    /**
     * Load a location quest.
     *
     * @param base the basic quest
     * @param questSection current quest section
     * @param fileName file name where the quest is
     * @param questIndex quest index in the file
     * @param menuItem menu item of the quest
     * @return the location quest
     */
    private LocationQuest loadLocationQuest(GlobalQuest base, ConfigurationSection questSection, String fileName, int questIndex, ItemStack menuItem) {
        final ConfigurationSection section = questSection.getConfigurationSection("location");

        /* reach type */
        Location location = null;
        int radius = 1;

        if (section == null) {
            configurationError(fileName, questIndex, null, "You need to specify a location.");
            location = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
        } else {
            final String wd = section.getString(".world");
            final int x = section.getInt(".x");
            final int y = section.getInt(".y");
            final int z = section.getInt(".z");

            radius = section.getInt(".radius");
            final World world = Bukkit.getWorld(wd);
            if (world == null) {
                configurationError(fileName, questIndex, null, "The world specified in the location is not loaded.");
                location = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
            } else {
                location = new Location(world, x, y, z);
            }
        }

        /* apply Persistent Data Container to the menu item to differentiate LOCATION quests */
        ItemMeta meta = menuItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(new NamespacedKey(ODailyQuests.INSTANCE, "quest_type"), PersistentDataType.STRING, "location");
        container.set(new NamespacedKey(ODailyQuests.INSTANCE, "quest_index"), PersistentDataType.INTEGER, questIndex);
        container.set(new NamespacedKey(ODailyQuests.INSTANCE, "file_name"), PersistentDataType.STRING, fileName);

        menuItem.setItemMeta(meta);

        return new LocationQuest(base, location, radius);
    }

    /**
     * Load a placeholder quest.
     *
     * @param base the basic quest
     * @param questSection current quest section
     * @param fileName file name where the quest is
     * @param questIndex quest index in the file
     * @param menuItem menu item of the quest
     * @return the placeholder quest
     */
    private PlaceholderQuest loadPlaceholderQuest(GlobalQuest base, ConfigurationSection questSection, String fileName, int questIndex, ItemStack menuItem) {
        final ConfigurationSection section = questSection.getConfigurationSection(".placeholder");

        /* variables for PLACEHOLDER quests */
        String placeholder = null;
        String expectedValue = null;
        ConditionType conditionType = null;
        String errorMessage = null;

        if (section == null) {
            configurationError(fileName, questIndex, null, "You need to specify a placeholder.");
        } else {
            placeholder = section.getString(".value");
            conditionType = ConditionType.valueOf(section.getString(".operator"));
            expectedValue = section.getString(".expected");
            errorMessage = section.getString(".error_message");
        }

        /* apply Persistent Data Container to the menu item to differentiate PLACEHOLDER quests */
        ItemMeta meta = menuItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(new NamespacedKey(ODailyQuests.INSTANCE, "quest_type"), PersistentDataType.STRING, "placeholder");
        container.set(new NamespacedKey(ODailyQuests.INSTANCE, "quest_index"), PersistentDataType.INTEGER, questIndex);
        container.set(new NamespacedKey(ODailyQuests.INSTANCE, "file_name"), PersistentDataType.STRING, fileName);

        menuItem.setItemMeta(meta);

        return new PlaceholderQuest(base, placeholder, conditionType, expectedValue, errorMessage);
    }

    /**
     * Load quests from a file.
     *
     * @param file     file to check.
     * @param quests   list for quests.
     * @param fileName file name for PluginLogger.
     */
    public void loadQuests(FileConfiguration file, ArrayList<AbstractQuest> quests, String fileName) {

        /* load quests */
        if (file.getConfigurationSection("quests") != null) {

            int questIndex = 0;
            for (String fileQuest : file.getConfigurationSection("quests").getKeys(false)) {

                final ConfigurationSection questSection = file.getConfigurationSection("quests." + fileQuest);
                if (questSection == null) continue;

                final GlobalQuest base = createBasicQuest(questSection, fileName, questIndex);
                if (base == null) continue;

                QuestType questType = base.getQuestType();
                ItemStack menuItem = base.getMenuItem();

                switch (questType) {

                    /* type that does not require a specific entity/item */
                    case MILKING, EXP_POINTS, EXP_LEVELS, CARVE, PLAYER_DEATH, FIREBALL_REFLECT -> quests.add(base);

                    /* types that requires an entity */
                    case KILL, BREED, TAME, SHEAR -> {
                        AbstractQuest entityQuest = loadEntityQuest(base, questSection, fileName, questIndex);
                        if (entityQuest != null) quests.add(entityQuest);
                    }

                    /* types that requires a custom mob */
                    case CUSTOM_MOBS -> {
                        AbstractQuest customMobsQuest = loadCustomMobQuest(base, questSection, fileName, questIndex);
                        if (customMobsQuest != null) quests.add(customMobsQuest);
                    }

                    /* types that requires an item */
                    case BREAK, PLACE, CRAFT, PICKUP, LAUNCH, CONSUME, GET, COOK, ENCHANT, VILLAGER_TRADE, FISH, FARMING -> {
                        AbstractQuest itemQuest = loadItemQuest(base, questSection, fileName, questIndex);
                        if (itemQuest != null) quests.add(itemQuest);
                    }

                    /* type that requires a location */
                    case LOCATION -> {
                        AbstractQuest locationQuest = loadLocationQuest(base, questSection, fileName, questIndex, menuItem);
                        quests.add(locationQuest);
                    }

                    /* type that requires a placeholder */
                    case PLACEHOLDER -> {
                        AbstractQuest placeholderQuest = loadPlaceholderQuest(base, questSection, fileName, questIndex, menuItem);
                        quests.add(placeholderQuest);
                    }
                }

                questIndex++;
            }

            PluginLogger.info(fileName + " array successfully loaded (" + quests.size() + ").");
        } else
            PluginLogger.error("Impossible to load " + fileName + " : there is no quests in " + fileName + " file !");
    }

    /**
     * @param material   the material to get
     * @param fileName   the file name
     * @param questIndex the quest index
     * @return the item stack
     */
    private ItemStack getItemStackFromMaterial(String material, String fileName, int questIndex, String parameter) {
        ItemStack requiredItem;

        if (material.contains(":")) {
            requiredItem = super.getItem(material, fileName, questIndex, parameter);
            if (requiredItem == null) {
                configurationError(fileName, questIndex, parameter, "Invalid material type detected.");
                return null;
            }
        } else {
            try {
                requiredItem = new ItemStack(Material.valueOf(material));
            } catch (Exception e) {
                configurationError(fileName, questIndex, parameter, "Invalid material type detected.");
                return null;
            }
        }

        return requiredItem;
    }

    /**
     * @param fileName   the file name
     * @param questIndex the quest index
     * @param value      the value
     * @return the entity type
     */
    private EntityType getEntityType(String fileName, int questIndex, String value) {
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(value);
        } catch (Exception e) {
            configurationError(fileName, questIndex, "required_entity", "Invalid entity type detected.");
            return null;
        }
        return entityType;
    }

    /**
     * @param dye        the dye to get
     * @param fileName   the file name
     * @param questIndex the quest index
     * @return the dye color
     */
    private DyeColor getDyeColor(String dye, String fileName, int questIndex) {
        if (dye == null) return null;

        DyeColor dyeColor;
        try {
            dyeColor = DyeColor.valueOf(dye.toUpperCase());
        } catch (Exception e) {
            configurationError(fileName, questIndex, "sheep_color", "Invalid dye type detected.");
            return null;
        }
        return dyeColor;
    }
}
