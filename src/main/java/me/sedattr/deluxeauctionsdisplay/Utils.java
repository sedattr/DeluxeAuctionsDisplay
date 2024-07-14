package me.sedattr.deluxeauctionsdisplay;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class Utils {
    public static void sendMessage(CommandSender player, String text) {
        if (player == null)
            return;

        List<String> messageList = DisplayPlugin.getInstance().config.getStringList(text);
        if (messageList.isEmpty()) {
            String message = DisplayPlugin.getInstance().config.getString(text);

            if (message == null)
                return;
            if (message.isEmpty())
                return;

            player.sendMessage(me.sedattr.deluxeauctions.others.Utils.colorize(message));
        } else
            for (String message : messageList)
                player.sendMessage(me.sedattr.deluxeauctions.others.Utils.colorize(message));
    }

    public static String locationToBase64(Location loc) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(loc);

            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save location.", e);
        }
    }

    public static Location locationFromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            Location loc = (Location) dataInput.readObject();

            dataInput.close();
            return loc;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }
}