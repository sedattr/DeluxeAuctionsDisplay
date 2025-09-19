package me.sedattr.deluxeauctionsdisplay;

import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.managers.AuctionType;
import me.sedattr.deluxeauctions.menus.BinViewMenu;
import me.sedattr.deluxeauctions.menus.NormalViewMenu;
import me.sedattr.deluxeauctionsdisplay.others.TaskUtils;
import me.sedattr.deluxeauctionsdisplay.others.Utils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class DisplayListeners implements Listener {
    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        for (DisplayManager displayManager : DeluxeAuctionsDisplay.getInstance().displays.values()) {
            Sign sign = displayManager.getSign();
            if (sign == null)
                continue;
            if (!sign.getBlock().equals(event.getClickedBlock()))
                continue;

            event.setCancelled(true);
            boolean result = handleAuction(displayManager, player);
            if (!result)
                Utils.clearSign(sign);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand armorStand))
            return;

        Player player = event.getPlayer();
        for (DisplayManager displayManager : DeluxeAuctionsDisplay.getInstance().displays.values()) {
            if (!displayManager.getHeadStand().equals(armorStand) && !displayManager.getTitleStand().equals(armorStand))
                continue;

            event.setCancelled(true);
            if (player.isSneaking())
                if (player.hasPermission(DeluxeAuctionsDisplay.getInstance().config.getString("permission", "auctiondisplay.command")) || player.isOp()) {
                    displayManager.delete();
                    DeluxeAuctionsDisplay.getInstance().displays.remove(displayManager.getName());
                    DeluxeAuctionsDisplay.getInstance().database.delete(displayManager.getName());

                    Utils.sendMessage(player, "deleted");
                    return;
                }

            handleAuction(displayManager, player);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(DeluxeAuctionsDisplay.getInstance().config.getString("permission", "auctiondisplay.command")) && !player.isOp())
            return;

        ItemStack item = player.getItemInHand();

        for (DisplayItem displayItem : DeluxeAuctionsDisplay.getInstance().placeItems) {
            if (displayItem.getItem().equals(item)) {
                event.setCancelled(true);

                player.getInventory().removeItem(item);

                if (DeluxeAuctionsDisplay.getInstance().displays.containsKey(displayItem.getName())) {
                    Utils.sendMessage(player, "already_created");
                    return;
                }

                Utils.sendMessage(player, "placed");

                Location blockLocation = event.getBlock().getLocation();
                Location location = new Location(player.getWorld(), blockLocation.getBlockX() + 0.5, blockLocation.getBlockY() - 1.375, blockLocation.getBlockZ() + 0.5, Utils.getYaw(player), 0);

                DisplayManager displayManager = new DisplayManager(location, displayItem.getPosition(), displayItem.getName());

                DeluxeAuctionsDisplay.getInstance().database.save(displayManager);
                DeluxeAuctionsDisplay.getInstance().displays.put(displayItem.getName(), displayManager);

                DeluxeAuctionsDisplay.getInstance().placeItems.remove(displayItem);
                return;
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            boolean isDisplay = Utils.isDisplay(entity);
            if (!isDisplay)
                continue;

            entity.remove();
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        for (DisplayManager displayManager : DeluxeAuctionsDisplay.getInstance().displays.values()) {
            if (displayManager == null)
                continue;

            Location loc = displayManager.getLocation();
            if (loc == null)
                continue;
            if (loc.getWorld() == null)
                continue;
            if (loc.getWorld() != chunk.getWorld())
                continue;
            if (loc.getBlockX() >> 4 != chunk.getX() || loc.getBlockZ() >> 4 != chunk.getZ())
                continue;

            if (displayManager.getHeadStand() == null || !displayManager.getHeadStand().isValid() ||
                    displayManager.getTitleStand() == null || !displayManager.getTitleStand().isValid()) {
                TaskUtils.run(displayManager::respawnEntities);
            }
        }
    }

    private boolean handleAuction(DisplayManager displayManager, Player player) {
        Auction auc = displayManager.getAuction();
        if (auc == null) {
            Utils.sendMessage(player, "empty_display");
            return false;
        }

        if (auc.isEnded()) {
            Utils.sendMessage(player, "ended_auction");
            return false;
        }

        if (auc.getAuctionType() == AuctionType.BIN)
            new BinViewMenu(player, auc).open("command");
        else
            new NormalViewMenu(player, auc).open("command");

        return true;
    }
}