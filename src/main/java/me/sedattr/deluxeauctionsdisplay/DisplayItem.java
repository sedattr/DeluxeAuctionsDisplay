package me.sedattr.deluxeauctionsdisplay;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
public class DisplayItem {
    private final String name;
    private final ItemStack item;
    private final Integer position;

    public DisplayItem(ItemStack item, String name, Integer position) {
        this.item = item;
        this.name = name;
        this.position = position;

        DisplayPlugin.getInstance().placeItems.add(this);
    }
}