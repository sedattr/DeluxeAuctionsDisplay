package me.sedattr.deluxeauctionsdisplay;

import me.sedattr.deluxeauctions.cache.AuctionCache;
import me.sedattr.deluxeauctions.managers.Auction;
import me.sedattr.deluxeauctions.managers.AuctionType;
import me.sedattr.deluxeauctions.menus.BinViewMenu;
import me.sedattr.deluxeauctions.menus.NormalViewMenu;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class DisplayListener implements Listener {
    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand armorStand))
            return;
        Player player = event.getPlayer();

        for (DisplayManager displayManager : DisplayPlugin.getInstance().displays.values()) {
            if (!displayManager.getHeadStand().equals(armorStand) && !displayManager.getTitleStand().equals(armorStand))
                continue;
            event.setCancelled(true);

            if (displayManager.getAuction() == null) {
                Utils.sendMessage(player, "empty_display");
                return;
            }

            Auction auc = AuctionCache.getAuction(displayManager.getAuction());
            if (auc == null || auc.isEnded()) {
                Utils.sendMessage(player, "ended_auction");
                return;
            }

            if (auc.getAuctionType() == AuctionType.BIN)
                new BinViewMenu(player, auc).open("command");
            else
                new NormalViewMenu(player, auc).open("command");
        }
    }
}
