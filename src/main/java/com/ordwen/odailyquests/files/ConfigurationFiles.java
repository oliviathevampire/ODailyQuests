package com.ordwen.odailyquests.files;

import com.ordwen.odailyquests.ODailyQuests;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class ConfigurationFiles {

    /**
     * Getting instance of main class.
     */
    private final ODailyQuests oDailyQuests;

    /**
     * Main class instance constructor.
     * @param oDailyQuests main class.
     */
    public ConfigurationFiles(ODailyQuests oDailyQuests) {
        this.oDailyQuests = oDailyQuests;
    }

    /* Logger for stacktrace */
    Logger logger = Bukkit.getLogger();

    private FileConfiguration config;
    private FileConfiguration messages;

    /**
     * Get the configuration file.
     * @return config file.
     */
    public FileConfiguration getConfigFile() {
        return this.config;
    }

    /**
     * Get the messages file.
     * @return messages file.
     */
    public FileConfiguration getMessagesFile() {
        return this.messages;
    }

    /**
     * Init configuration files.
     */
    public void loadConfigurationFiles() {

        File configFile = new File(oDailyQuests.getDataFolder(), "config.yml");
        File messagesFile = new File(oDailyQuests.getDataFolder(), "messages.yml");

        if (!configFile.exists()) {
            if (configFile.getParentFile().mkdirs()) {
                oDailyQuests.saveResource("config.yml", false);
                logger.info(ChatColor.GREEN + "Config file created.");
            } else {
                logger.info(ChatColor.RED + "An error occured on the creation of the configuration file.");
                logger.info(ChatColor.RED + "Please inform the developper.");
            }
        }

        if (!messagesFile.exists()) {
            if (messagesFile.getParentFile().mkdirs()) {
                oDailyQuests.saveResource("messages.yml", false);
                logger.info(ChatColor.GREEN + "Messages file created.");
            } else {
                logger.info(ChatColor.RED + "An error occured on the creation of the configuration file.");
                logger.info(ChatColor.RED + "Please inform the developper.");
            }
        }

        config = new YamlConfiguration();
        messages = new YamlConfiguration();

        try {
            config.load(configFile);

        } catch (InvalidConfigurationException | IOException e) {
            logger.info(ChatColor.RED + "An error occured on the load of the configuration file.");
            logger.info(ChatColor.RED + "Please inform the developper.");
            e.printStackTrace();
        }

        logger.info(ChatColor.GREEN + "Configuration file successfully loaded.");

        try {
            messages.load(messagesFile);
        } catch (InvalidConfigurationException | IOException e) {
            logger.info(ChatColor.RED + "An error occured on the load of the messages file.");
            logger.info(ChatColor.RED + "Please inform the developper.");
            e.printStackTrace();
        }

        logger.info(ChatColor.GREEN + "Messages file successfully loaded.");
    }

    /*
    static File easyQuestsFile;
    static File mediumQuestsFile;
    static File hardQuestsFile;

    static YamlYamlConfiguration easyQuests;
    static YamlYamlConfiguration mediumQuests;
    static YamlYamlConfiguration hardQuests;
     */

}
