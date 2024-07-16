package me.sedattr.deluxeauctionsdisplay;

import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.sedattr.deluxeauctionsdisplay.Utils.getYaw;

public class DisplayCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!commandSender.hasPermission(DisplayPlugin.getInstance().config.getString("permission", "auctiondisplay.command")) && !commandSender.isOp()) {
            me.sedattr.deluxeauctionsdisplay.Utils.sendMessage(commandSender, "no_permission");
            return false;
        }

        if (!(commandSender instanceof Player player)) {
            me.sedattr.deluxeauctionsdisplay.Utils.sendMessage(commandSender, "not_player");
            return false;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("teleport")) {
                if (args.length < 2) {
                    me.sedattr.deluxeauctionsdisplay.Utils.sendMessage(player, "teleport_usage");
                    return false;
                }

                String name = args[1];
                DisplayManager displayManager = DisplayPlugin.getInstance().displays.get(name);

                if (displayManager == null) {
                    me.sedattr.deluxeauctionsdisplay.Utils.sendMessage(player, "wrong_name");
                    return false;
                }

                player.teleport(displayManager.getTitleStand().getLocation().clone().add(0, 2, 0));
                me.sedattr.deluxeauctionsdisplay.Utils.sendMessage(player, "teleported");
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                if (args.length < 3) {
                    me.sedattr.deluxeauctionsdisplay.Utils.sendMessage(player, "create_usage");
                    return false;
                }

                String name = args[1];
                if (DisplayPlugin.getInstance().displays.containsKey(name)) {
                    me.sedattr.deluxeauctionsdisplay.Utils.sendMessage(player, "already_created");
                    return false;
                }

                int rank = Integer.parseInt(args[2]);
                if (rank <= 0) {
                    me.sedattr.deluxeauctionsdisplay.Utils.sendMessage(player, "create_usage");
                    return false;
                }

                Location location = new Location(player.getWorld(), player.getLocation().getBlockX() + 0.5, player.getLocation().getBlockY() - 1.375, player.getLocation().getBlockZ() + 0.5, getYaw(player), 0);
                DisplayManager displayManager = new DisplayManager(location, rank, name);

                DisplayPlugin.getInstance().database.save(displayManager);
                DisplayPlugin.getInstance().displays.put(name, displayManager);
                me.sedattr.deluxeauctionsdisplay.Utils.sendMessage(player, "created");
                return true;
            }

            if (args[0].equalsIgnoreCase("delete")) {
                if (args.length < 2) {
                    me.sedattr.deluxeauctionsdisplay.Utils.sendMessage(player, "delete_usage");
                    return false;
                }
                String name = args[1];

                DisplayManager displayManager = DisplayPlugin.getInstance().displays.get(name);
                if (displayManager == null) {
                    me.sedattr.deluxeauctionsdisplay.Utils.sendMessage(player, "wrong_name");
                    return false;
                }

                displayManager.delete();
                DisplayPlugin.getInstance().displays.remove(name);

                DisplayPlugin.getInstance().database.delete(name);

                me.sedattr.deluxeauctionsdisplay.Utils.sendMessage(player, "deleted");
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                String list = DisplayPlugin.getInstance().config.getString("display_list", "&8- &aName: &f%display_name% &8| &ePosition: &f%display_position%");

                for (DisplayManager displayManager : DisplayPlugin.getInstance().displays.values()) {
                    player.sendMessage(Utils.colorize(list
                            .replace("%display_position%", String.valueOf(displayManager.getPosition()))
                            .replace("%display_name%", displayManager.getName())));
                }

                String total = DisplayPlugin.getInstance().config.getString("total_display");
                if (total == null || total.isEmpty())
                    return false;

                player.sendMessage(Utils.colorize(total
                        .replace("%display_amount%", String.valueOf(DisplayPlugin.getInstance().displays.size()))));
                return true;
            }
        }

        me.sedattr.deluxeauctionsdisplay.Utils.sendMessage(player, "command_usage");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        if (!commandSender.hasPermission(DisplayPlugin.getInstance().config.getString("permission", "auctiondisplay.command")) && !commandSender.isOp())
            return null;

        ArrayList<String> complete = new ArrayList<>(Arrays.asList("create", "list", "delete"));

        if (args.length == 1)
            return complete;
        return null;
    }
}