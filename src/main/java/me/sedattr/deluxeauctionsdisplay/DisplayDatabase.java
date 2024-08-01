package me.sedattr.deluxeauctionsdisplay;

import me.sedattr.deluxeauctions.cache.AuctionCache;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.managers.AuctionType;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DisplayDatabase {
    private final File database = new File(DisplayPlugin.getInstance().getDataFolder(), "database.yml");
    private BukkitTask task;

    public void load() {
        if (!this.database.exists())
            return;

        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(this.database);

            Set<String> keys = configuration.getKeys(false);
            if (keys.isEmpty())
                return;

            HashMap<String, DisplayManager> displayManagers = new HashMap<>();
            for (String name : keys) {
                ConfigurationSection displaySection = configuration.getConfigurationSection(name);
                if (displaySection == null)
                    continue;

                Location location = Utils.locationFromBase64(displaySection.getString("location"));
                if (location == null)
                    continue;

                int position = displaySection.getInt("position");
                if (position <= 0)
                    continue;

                if (location.getWorld() == null) {
                    DisplayPlugin.getInstance().database.delete(name);
                    return;
                }

                DisplayManager displayManager = new DisplayManager(location, position, name);
                displayManagers.put(name, displayManager);
            }

            DisplayPlugin.getInstance().displays = displayManagers;
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(DisplayManager displayManager) {
        if (!this.database.exists())
            try {
                this.database.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(this.database);

            configuration.set(displayManager.getName() + ".position", displayManager.getPosition());
            configuration.set(displayManager.getName() + ".location", Utils.locationToBase64(displayManager.getLocation()));

            configuration.save(this.database);
            if (this.database.length() <= 0)
                this.database.delete();
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(String name) {
        if (!this.database.exists())
            return;

        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(this.database);

            configuration.set(name, null);

            configuration.save(this.database);
            if (this.database.length() <= 0)
                this.database.delete();
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public void updater() {
        if (this.task != null)
            this.task.cancel();

        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                List<Auction> auctions = AuctionCache.getFilteredAuctions(AuctionType.valueOf(DisplayPlugin.getInstance().config.getString("auction_type", "ALL")), null, null);
                auctions.sort(Comparator.comparing(Auction::getAuctionPrice));
                Collections.reverse(auctions);

                for (DisplayManager displayManager : DisplayPlugin.getInstance().displays.values()) {
                    if (auctions.size() < displayManager.getPosition()) {
                        displayManager.changeAuction(null);
                        continue;
                    }

                    displayManager.changeAuction(auctions.get(displayManager.getPosition() - 1));
                }
            }
        }.runTaskTimer(DisplayPlugin.getInstance(), 100, DisplayPlugin.getInstance().config.getInt("refresh_time", 60) * 20L);
    }

    public void loadItems() {
        ConfigurationSection section = DisplayPlugin.getInstance().config.getConfigurationSection("display_items");
        if (section == null)
            return;

        HashMap<String, ItemStack> newItems = new HashMap<>();
        for (String item : section.getKeys(false)) {
            ItemStack itemStack = me.sedattr.deluxeauctions.others.Utils.createItemFromSection(section.getConfigurationSection(item), null);
            if (itemStack == null)
                continue;

            newItems.put(item, itemStack);
        }

        ItemStack itemStack = me.sedattr.deluxeauctions.others.Utils.createItemFromSection(DisplayPlugin.getInstance().config.getConfigurationSection("default_item"), null);
        if (itemStack != null)
            newItems.put("default", itemStack);

        DisplayPlugin.getInstance().items = newItems;
    }
}
