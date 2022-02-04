package com.ordwen.odailyquests;

import com.ordwen.odailyquests.files.ConfigurationFiles;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class ODailyQuests extends JavaPlugin {

    /**
     * Getting instance of configuration files class.
     */
    private ConfigurationFiles configurationFiles;

    /* Technical items */
    Logger logger = getLogger();
    String dataPath = this.getDataFolder().getPath();

    @Override
    public void onEnable() {
        logger.info(ChatColor.GOLD + "Plugin is starting...");

        this.configurationFiles = new ConfigurationFiles(this);
        configurationFiles.loadConfigurationFiles();

        logger.info(ChatColor.GREEN + "Plugin is started !");

        /*
        logger.info("Name of quest is : " + quest.getQuestName());
        logger.info("Description of quest is : " + quest.getQuestDesc());
        logger.info("Type of quest is : " + quest.getType().getTypeName());
        logger.info("Item required of quest is : " + quest.getItemRequired());
        logger.info("Amount required of quest is : " + quest.getAmountRequired());
        logger.info("Reward type of quest is : " + quest.getReward().getRewardType());
        logger.info("Reward of quest is : " + quest.getReward().getRewardCommand());
        this.getServer().dispatchCommand(Bukkit.getConsoleSender(), quest.getReward().getRewardCommand());
         */
    }

    @Override
    public void onDisable() {
        logger.info(ChatColor.RED + "Plugin is shutting down...");
    }

    public String getPluginFolder() {
        return dataPath;
    }
}

