package me.sedattr.deluxeauctionsdisplay;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
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

    public static void loadChunk(Location location) {
        if (location == null || location.getWorld() == null)
            return;

        Chunk chunk = location.getChunk();
        World world = location.getWorld();
        if (!world.isChunkLoaded(chunk))
            world.loadChunk(chunk);

        if (!chunk.isLoaded())
            chunk.load(true);
    }

    public static void clearSign(Sign sign) {
        if (sign == null)
            return;

        for (int i = 0; i <= 3; i++) {
            sign.setLine(i, " ");
        }

        sign.update();
    }

    public static Sign findSign(Location location) {
        location = location.clone().add(0, 1, 0);

        Block block = location.getBlock();
        List<BlockFace> faces = Arrays.asList(BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH);
        for (BlockFace face : faces) {
            Block relative = block.getRelative(face);
            if (relative.getType().name().endsWith("SIGN") && relative.getState() instanceof Sign)
                return (Sign) relative.getState();
        }

        return null;
    }

    public static void removeOldEntities(Location location) {
        if (location == null || location.getWorld() == null)
            return;
        loadChunk(location);

        for (Entity entity : location.getWorld().getNearbyEntities(location, 2, 2, 2)) {
            if (!entity.hasMetadata("deluxeauctions_display"))
                return;

            entity.remove();
        }
    }

    public static void updateArmorStand(ArmorStand armorStand) {
        loadChunk(armorStand.getLocation());

        armorStand.setVisible(false);
        armorStand.setRemoveWhenFarAway(false);
        armorStand.setGravity(false);
        armorStand.setBasePlate(false);
        armorStand.setCustomNameVisible(false);
        armorStand.setCanPickupItems(false);
    }

    public static float getYaw(Player player) {
        float yaw = player.getLocation().getYaw();

        if (yaw >= 135 || yaw <= -135)
            return -180;
        if (yaw < 45 && yaw >= -45)
            return 0;
        if (yaw >= 45)
            return 90;

        return -90;
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