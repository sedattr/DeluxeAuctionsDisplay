package me.sedattr.deluxeauctionsdisplay;

import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static me.sedattr.deluxeauctionsdisplay.others.Utils.getYaw;

public class DisplayCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!commandSender.hasPermission(DeluxeAuctionsDisplay.getInstance().config.getString("permission", "auctiondisplay.command")) && !commandSender.isOp()) {
            me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(commandSender, "no_permission");
            return false;
        }

        if (!(commandSender instanceof Player player)) {
            me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(commandSender, "not_player");
            return false;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("give")) {
                if (args.length < 2) {
                    me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "give_usage");
                    return false;
                }

                int position;
                try {
                    position = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "give_usage");
                    return false;
                }

                if (position <= 0) {
                    me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "give_usage");
                    return false;
                }

                String name = "";
                if (args.length > 2) {
                    name = args[2];

                    if (DeluxeAuctionsDisplay.getInstance().displays.containsKey(name)) {
                        me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "already_created");
                        return false;
                    }
                } else
                    for (String item : DeluxeAuctionsDisplay.getInstance().items.keySet()) {
                        if (item.contains(args[1]) && !DeluxeAuctionsDisplay.getInstance().displays.containsKey(item)) {
                            name = item;
                            break;
                        }
                    }

                if (name.isEmpty())
                    name = String.valueOf(UUID.randomUUID()).replace("-", "").substring(0, 10);

                ConfigurationSection section = DeluxeAuctionsDisplay.getInstance().config.getConfigurationSection("place_item");
                if (section == null)
                    return false;

                ItemStack itemStack = Utils.createItemFromSection(section, null);
                if (itemStack == null)
                    return false;

                PlaceholderUtil placeholderUtil = new PlaceholderUtil()
                        .addPlaceholder("%display_name%", name)
                        .addPlaceholder("%display_position%", String.valueOf(position));

                Utils.changeLore(itemStack, section.getStringList("lore"), placeholderUtil);
                Utils.changeName(itemStack, section.getString("name"), placeholderUtil);

                player.getInventory().addItem(itemStack);
                player.updateInventory();

                new DisplayItem(itemStack, name, position);

                me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "given");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                for (DisplayManager manager : DeluxeAuctionsDisplay.getInstance().displays.values())
                    manager.delete();

                DeluxeAuctionsDisplay.getInstance().config = DeluxeAuctionsDisplay.getInstance().getConfig();

                DeluxeAuctionsDisplay.getInstance().database.loadItems();
                DeluxeAuctionsDisplay.getInstance().database.load();
                DeluxeAuctionsDisplay.getInstance().database.updater();

                me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "reloaded");
                return true;
            }

            if (args[0].equalsIgnoreCase("teleport")) {
                if (args.length < 2) {
                    me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "teleport_usage");
                    return false;
                }

                String name = args[1];
                DisplayManager displayManager = DeluxeAuctionsDisplay.getInstance().displays.get(name);

                if (displayManager == null) {
                    me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "wrong_name");
                    return false;
                }

                player.teleport(displayManager.getTitleStand().getLocation().clone().add(0, 2, 0));
                me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "teleported");
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                if (args.length < 3) {
                    me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "create_usage");
                    return false;
                }

                String name = args[1];
                if (DeluxeAuctionsDisplay.getInstance().displays.containsKey(name)) {
                    me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "already_created");
                    return false;
                }

                int rank = Integer.parseInt(args[2]);
                if (rank <= 0) {
                    me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "create_usage");
                    return false;
                }

                Location location = new Location(player.getWorld(), player.getLocation().getBlockX() + 0.5, player.getLocation().getBlockY() - 1.375, player.getLocation().getBlockZ() + 0.5, getYaw(player), 0);
                DisplayManager displayManager = new DisplayManager(location, rank, name);

                DeluxeAuctionsDisplay.getInstance().database.save(displayManager);
                DeluxeAuctionsDisplay.getInstance().displays.put(name, displayManager);
                me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "created");
                return true;
            }

            if (args[0].equalsIgnoreCase("delete")) {
                if (args.length < 2) {
                    me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "delete_usage");
                    return false;
                }
                String name = args[1];

                DisplayManager displayManager = DeluxeAuctionsDisplay.getInstance().displays.get(name);
                if (displayManager == null) {
                    me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "wrong_name");
                    return false;
                }

                displayManager.delete();
                DeluxeAuctionsDisplay.getInstance().displays.remove(name);
                DeluxeAuctionsDisplay.getInstance().database.delete(name);

                me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "deleted");
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                String list = DeluxeAuctionsDisplay.getInstance().config.getString("display_list", "&8- &aName: &f%display_name% &8| &ePosition: &f%display_position%");

                for (DisplayManager displayManager : DeluxeAuctionsDisplay.getInstance().displays.values()) {
                    player.sendMessage(Utils.colorize(list
                            .replace("%display_position%", String.valueOf(displayManager.getPosition()))
                            .replace("%display_name%", displayManager.getName())));
                }

                String total = DeluxeAuctionsDisplay.getInstance().config.getString("total_display");
                if (total == null || total.isEmpty())
                    return false;

                player.sendMessage(Utils.colorize(total
                        .replace("%display_amount%", String.valueOf(DeluxeAuctionsDisplay.getInstance().displays.size()))));
                return true;
            }
        }

        me.sedattr.deluxeauctionsdisplay.others.Utils.sendMessage(player, "command_usage");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        if (!commandSender.hasPermission(DeluxeAuctionsDisplay.getInstance().config.getString("permission", "auctiondisplay.command")) && !commandSender.isOp())
            return null;

        if (args.length > 0)
            if (args[0].equalsIgnoreCase("teleport") || args[0].equalsIgnoreCase("delete"))
                return DeluxeAuctionsDisplay.getInstance().displays.keySet().stream().toList();

        ArrayList<String> complete = new ArrayList<>(Arrays.asList("create", "give", "teleport", "list", "delete"));

        if (args.length == 1)
            return complete;
        return null;
    }
}