package me.sedattr.deluxeauctionsdisplay;

import lombok.Getter;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.UUID;

import static me.sedattr.deluxeauctionsdisplay.Utils.*;

@Getter
public class DisplayManager {
    private final String name;
    private final int position;
    private ArmorStand headStand;
    private ArmorStand titleStand;
    private Location location;

    private UUID auction;
    private Item item;
    private boolean spawnItem = true;

    public DisplayManager(Location location, Integer position, String name) {
        this.name = name;
        this.position = position;

        if (location.getWorld() == null) {
            DisplayPlugin.getInstance().database.delete(name);
            return;
        }

        this.location = location.clone();
        Utils.removeOldEntities(this.location);

        this.headStand = location.getWorld().spawn(location, ArmorStand.class);
        this.titleStand = location.getWorld().spawn(location.clone().add(0, 0.25, 0), ArmorStand.class);

        Utils.updateArmorStand(this.headStand);
        Utils.updateArmorStand(this.titleStand);

        ItemStack itemStack = getItemStack();
        if (itemStack != null)
            this.headStand.setHelmet(itemStack);

        updateTitles(null);
    }

    private ItemStack getItemStack() {
        ItemStack itemStack = DisplayPlugin.getInstance().items.get(this.name);
        if (itemStack == null) {
            itemStack = DisplayPlugin.getInstance().items.get("default");
            if (itemStack == null)
                return null;

            this.spawnItem = DisplayPlugin.getInstance().config.getBoolean("default_item.spawn_item", true);
        } else
            this.spawnItem = DisplayPlugin.getInstance().config.getBoolean("display_items." + this.name + ".spawn_item", true);

        return itemStack;
    }

    public void delete() {
        Utils.loadChunk(this.location);
        this.auction = null;

        if (this.item != null)
            this.item.remove();
        if (this.headStand != null)
            this.headStand.remove();
        if (this.titleStand != null)
            this.titleStand.remove();
    }

    public void changeAuction(Auction auction) {
        Utils.loadChunk(this.location);
        updateTitles(auction);

        if (auction == null || this.location.getWorld() == null) {
            if (this.item != null)
                this.item.remove();

            this.auction = null;
            this.item = null;

            return;
        }

        if (this.auction != null && this.auction.equals(auction.getAuctionUUID()))
            return;

        if (this.spawnItem) {
            if (this.item != null)
                this.item.remove();

            ItemStack itemStack = auction.getAuctionItem().clone();
            this.item = this.location.getWorld().dropItem(this.location.clone().add(0, 1.5, 0), itemStack);

            this.item.setVelocity(new Vector(0, 0, 0));
            this.item.setPickupDelay(Integer.MAX_VALUE);

            this.item.setCustomNameVisible(false);
            this.item.setCustomName("DeluxeAuctions");
        }

        this.auction = auction.getAuctionUUID();
    }

    private void updateTitles(Auction auc) {
        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%display_position%", String.valueOf(this.position));

        if (auc == null) {
            updateTitle("without_auction", placeholderUtil);
            return;
        }

        Player player = Bukkit.getPlayer(auc.getAuctionOwner());
        String name = "";
        if (player != null)
            name = player.getName();

        double price = auc.getAuctionPrice();
        if (auc.getAuctionBids().getHighestBid() != null)
            price = auc.getAuctionBids().getHighestBid().getBidPrice();

        placeholderUtil
                .addPlaceholder("%seller_displayname%", auc.getAuctionOwnerDisplayName())
                .addPlaceholder("%auction_price%", DeluxeAuctions.getInstance().numberFormat.format(price))
                .addPlaceholder("%seller_name%", name)
                .addPlaceholder("%auction_name%", me.sedattr.deluxeauctions.others.Utils.getDisplayName(auc.getAuctionItem()));

        updateTitle("with_auction", placeholderUtil);
    }

    private void updateTitle(String type, PlaceholderUtil placeholderUtil) {
        loadChunk(this.location);

        ConfigurationSection section = DisplayPlugin.getInstance().config.getConfigurationSection("titles." + type);
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
}