package me.sedattr.deluxeauctionsdisplay;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class DeluxeAuctionsDisplay extends JavaPlugin {
    @Getter private static DeluxeAuctionsDisplay instance;

    public DisplayDatabase database;
    public FileConfiguration config;

    public HashMap<String, ItemStack> items = new HashMap<>();
    public HashMap<String, DisplayManager> displays = new HashMap<>();
    public Set<DisplayItem> placeItems = new HashSet<>();

    @Override
    public void onEnable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("DeluxeAuctions")) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&bDeluxeAuctions Display&8] &fDeluxeAuctions &cis not found, plugin is disabling..."));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        instance = this;

        saveDefaultConfig();
        this.config = getConfig();
        this.database = new DisplayDatabase();

        Bukkit.getPluginManager().registerEvents(new DisplayListeners(), DeluxeAuctionsDisplay.getInstance());

        PluginCommand command = getCommand("auctiondisplay");
        if (command != null)
            command.setExecutor(new DisplayCommand());

        this.database.loadItems();
        this.database.load();
        this.database.updater();

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&bDeluxeAuctions Display&8] &aPlugin is enabled! Plugin Version: &fv" + getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
        if (instance != null) {
            for (DisplayManager manager : this.displays.values())
                manager.delete();

            HandlerList.unregisterAll(this);
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&bDeluxeAuctions Display&8] &ePlugin is disabled! &8(&7sedattr was here...&8)"));
        }
    }
}