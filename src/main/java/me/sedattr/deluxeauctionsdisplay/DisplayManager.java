package me.sedattr.deluxeauctionsdisplay;

import lombok.Getter;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctionsdisplay.others.TaskUtils;
import me.sedattr.deluxeauctionsdisplay.others.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

@Getter
public class DisplayManager {
    private final String name;
    private final int position;
    private ArmorStand headStand;
    private ArmorStand titleStand;
    private Location location;

    private Auction auction = null;
    private Item item;
    private boolean spawnItem = true;
    private Sign sign;

    public DisplayManager(Location location, Integer position, String name) {
        this.name = name;
        this.position = position;

        if (location.getWorld() == null) {
            DeluxeAuctionsDisplay.getInstance().database.delete(name);
            return;
        }

        this.location = location.clone();
        respawnEntities();
    }

    private ItemStack getItemStack() {
        ItemStack itemStack = DeluxeAuctionsDisplay.getInstance().items.get(this.name);
        if (itemStack == null) {
            itemStack = DeluxeAuctionsDisplay.getInstance().items.get("default");
            if (itemStack == null)
                return null;

            this.spawnItem = DeluxeAuctionsDisplay.getInstance().config.getBoolean("default_item.spawn_item", true);
        } else
            this.spawnItem = DeluxeAuctionsDisplay.getInstance().config.getBoolean("display_items." + this.name + ".spawn_item", true);

        return itemStack;
    }

    public void delete() {
        Utils.loadChunk(this.location);
        this.auction = null;

        Utils.clearSign(this.sign);
        if (this.item != null)
            this.item.remove();
        if (this.headStand != null)
            this.headStand.remove();
        if (this.titleStand != null)
            this.titleStand.remove();
    }

    private void updateSign() {
        if (this.sign == null)
            return;

        if (this.auction == null) {
            Utils.clearSign(this.sign);
            return;
        }

        ConfigurationSection section = DeluxeAuctionsDisplay.getInstance().config.getConfigurationSection("sign." + this.auction.getAuctionType().name().toLowerCase());
        if (section == null) {
            Utils.clearSign(this.sign);
            return;
        }

        if (!section.getBoolean("enabled")) {
            Utils.clearSign(this.sign);
            return;
        }

        double price = this.auction.getAuctionPrice();
        if (this.auction.getAuctionBids().getHighestBid() != null)
            price = this.auction.getAuctionBids().getHighestBid().getBidPrice();

        OfflinePlayer player = Bukkit.getOfflinePlayer(this.auction.getAuctionOwner());
        String name = player.getName();

        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%display_position%", String.valueOf(this.position))
                .addPlaceholder("%auction_price%", DeluxeAuctions.getInstance().numberFormat.format(price))
                .addPlaceholder("%seller_name%", name)
                .addPlaceholder("%auction_name%", me.sedattr.deluxeauctions.others.Utils.getDisplayName(this.auction.getAuctionItem()))
                .addPlaceholder("%seller_displayname%", this.auction.getAuctionOwnerDisplayName())
                .addPlaceholder("%bid_amount%", String.valueOf(this.auction.getAuctionBids().getPlayerBids().size()));

        List<String> lines = section.getStringList("lines");
        for (int i = 0; i <= 3; i++) {
            String text = me.sedattr.deluxeauctions.others.Utils.colorize(me.sedattr.deluxeauctions.others.Utils.replacePlaceholders(lines.get(i), placeholderUtil));

            if (this.sign.getType().name().endsWith("HANGING_SIGN")) {
                if (text.length() > 12)
                    this.sign.setLine(i, text.substring(0, 12) + "...");
                else
                    this.sign.setLine(i, text);
            }
            else
                this.sign.setLine(i, text);
        }

        this.sign.update();
    }

    public void changeAuction(Auction auction) {
        TaskUtils.run(() -> {
            DisplayManager.this.auction = auction;
            Utils.loadChunk(DisplayManager.this.location);

            if (auction == null || DisplayManager.this.location.getWorld() == null) {
                if (DisplayManager.this.item != null)
                    DisplayManager.this.item.remove();

                DisplayManager.this.auction = null;
                DisplayManager.this.item = null;

                updateTitles();
                updateSign();
                return;
            }

            updateTitles();
            updateSign();

            spawnItem();
        });
    }

    private void spawnItem() {
        if (!DisplayManager.this.spawnItem)
            return;

        if (DisplayManager.this.item != null) {
            if (DisplayManager.this.item.isValid()) {
                Utils.loadChunk(DisplayManager.this.item.getLocation());
                DisplayManager.this.item.remove();
            }
            DisplayManager.this.item = null;
        }

        if (this.auction == null)
            return;
        if (DisplayManager.this.location == null)
            return;
        if (DisplayManager.this.location.getWorld() == null)
            return;

        ItemStack itemStack = this.auction.getAuctionItem().clone();
        DisplayManager.this.item = DisplayManager.this.location.getWorld().dropItem(DisplayManager.this.location.clone().add(0, 1.5, 0), itemStack);
        DisplayManager.this.item.setVelocity(new Vector(0, 0, 0));
        DisplayManager.this.item.setPickupDelay(Integer.MAX_VALUE);
        DisplayManager.this.item.setCustomNameVisible(false);
        Utils.setDisplayTag(DisplayManager.this.item);
    }

    private void updateTitles() {
        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%display_position%", String.valueOf(this.position));

        if (this.auction == null) {
            updateTitle("without_auction", placeholderUtil);
            return;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(this.auction.getAuctionOwner());
        String name = player.getName();

        double price = this.auction.getAuctionPrice();
        if (this.auction.getAuctionBids().getHighestBid() != null)
            price = this.auction.getAuctionBids().getHighestBid().getBidPrice();

        placeholderUtil
                .addPlaceholder("%seller_displayname%", this.auction.getAuctionOwnerDisplayName())
                .addPlaceholder("%auction_price%", DeluxeAuctions.getInstance().numberFormat.format(price))
                .addPlaceholder("%seller_name%", name)
                .addPlaceholder("%auction_name%", me.sedattr.deluxeauctions.others.Utils.getDisplayName(this.auction.getAuctionItem()));

        updateTitle("with_auction", placeholderUtil);
    }

    private void updateTitle(String type, PlaceholderUtil placeholderUtil) {
        ConfigurationSection section = DeluxeAuctionsDisplay.getInstance().config.getConfigurationSection("titles." + type);
        if (section == null || !section.getBoolean("enabled")) {
            this.headStand.setCustomNameVisible(false);
            this.titleStand.setCustomNameVisible(false);
            return;
        }

        String line1 = section.getString("line_1");
        if (line1 != null && !line1.isEmpty()) {
            this.titleStand.setCustomName(me.sedattr.deluxeauctions.others.Utils.colorize(me.sedattr.deluxeauctions.others.Utils.replacePlaceholders(line1, placeholderUtil)));
            this.titleStand.setCustomNameVisible(true);
        } else
            this.titleStand.setCustomNameVisible(false);

        String line2 = section.getString("line_2");
        if (line2 != null && !line2.isEmpty()) {
            this.headStand.setCustomName(me.sedattr.deluxeauctions.others.Utils.colorize(me.sedattr.deluxeauctions.others.Utils.replacePlaceholders(line2, placeholderUtil)));
            this.headStand.setCustomNameVisible(true);
        } else
            this.headStand.setCustomNameVisible(false);
    }

    public void respawnEntities() {
        Utils.loadChunk(this.location);
        Utils.removeOldEntities(this.location);

        this.headStand = this.location.getWorld().spawn(this.location, ArmorStand.class);
        Utils.setDisplayTag(this.headStand);
        Utils.updateArmorStand(this.headStand);

        this.titleStand = this.location.getWorld().spawn(this.location.clone().add(0, 0.25, 0), ArmorStand.class);
        Utils.setDisplayTag(this.titleStand);
        Utils.updateArmorStand(this.titleStand);

        ItemStack itemStack = getItemStack();
        if (itemStack != null)
            this.headStand.setHelmet(itemStack);

        this.sign = Utils.findSign(this.location);

        updateTitles();
        updateSign();

        spawnItem();
    }
}