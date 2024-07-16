package me.sedattr.deluxeauctionsdisplay;

import lombok.Getter;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
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
    private Sign sign;

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

        this.sign = Utils.findSign(this.location);

        updateTitles(null);
        updateSign(null);
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

        Utils.clearSign(this.sign);
        if (this.item != null)
            this.item.remove();
        if (this.headStand != null)
            this.headStand.remove();
        if (this.titleStand != null)
            this.titleStand.remove();
    }

    private void updateSign(Auction auctionManager) {
        if (this.sign == null)
            return;

        if (auctionManager == null) {
            Utils.clearSign(this.sign);
            return;
        }

        ConfigurationSection section = DisplayPlugin.getInstance().config.getConfigurationSection("sign." + auctionManager.getAuctionType().name().toLowerCase());
        if (section == null) {
            Utils.clearSign(this.sign);
            return;
        }

        if (!section.getBoolean("enabled")) {
            Utils.clearSign(this.sign);
            return;
        }

        double price = auctionManager.getAuctionPrice();
        if (auctionManager.getAuctionBids().getHighestBid() != null)
            price = auctionManager.getAuctionBids().getHighestBid().getBidPrice();

        OfflinePlayer player = Bukkit.getOfflinePlayer(auctionManager.getAuctionOwner());
        String name = player.getName();

        PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                .addPlaceholder("%display_position%", String.valueOf(this.position))
                .addPlaceholder("%auction_price%", DeluxeAuctions.getInstance().numberFormat.format(price))
                .addPlaceholder("%seller_name%", name)
                .addPlaceholder("%auction_name%", me.sedattr.deluxeauctions.others.Utils.getDisplayName(auctionManager.getAuctionItem()))
                .addPlaceholder("%seller_displayname%", auctionManager.getAuctionOwnerDisplayName())
                .addPlaceholder("%bid_amount%", String.valueOf(auctionManager.getAuctionBids().getPlayerBids().size()));

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
        Utils.loadChunk(this.location);

        if (auction == null || this.location.getWorld() == null) {
            if (this.item != null)
                this.item.remove();

            this.auction = null;
            this.item = null;

            updateTitles(null);
            updateSign(null);
            return;
        }

        if (this.auction != null && this.auction.equals(auction.getAuctionUUID()))
            return;

        updateTitles(auction);
        updateSign(auction);

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

        OfflinePlayer player = Bukkit.getOfflinePlayer(auc.getAuctionOwner());
        String name = player.getName();

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