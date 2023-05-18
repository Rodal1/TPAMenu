package org.terminum.tpamenu;

import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class Main extends JavaPlugin {

    Logger logger = getLogger();

    @Override
    public void onEnable() {
        logger.info("Has been enabled. Made by Omega01");
        Objects.requireNonNull(getCommand("tpa")).setExecutor(new Command());
        Objects.requireNonNull(getCommand("tpaccept")).setExecutor(new Command());
        Objects.requireNonNull(getCommand("tpadeny")).setExecutor(new Command());
        PluginManager m = getServer().getPluginManager();
        m.registerEvents(new Command(), this);
        saveDefaultConfig();

    }

    @Override
    public void onDisable() {
        logger.info("Has been disabled. Made by Omega01");
    }

    public static Main getPlugin() {
        return getPlugin(Main.class);
    }

    public String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public List<String> colorizeList(List<String> list) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            result.add(ChatColor.translateAlternateColorCodes('&',s));
        }
        return result;
    }
}
