package me.sedattr.deluxeauctionsdisplay.others;

import me.sedattr.deluxeauctionsdisplay.DeluxeAuctionsDisplay;
import org.bukkit.scheduler.BukkitRunnable;

public final class TaskUtils {
    public static boolean isFolia;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (final ClassNotFoundException e) {
            isFolia = false;
        }
    }

    public static void run(Runnable runnable) {
        if (isFolia) {
            DeluxeAuctionsDisplay.getInstance().getServer().getGlobalRegionScheduler().execute(DeluxeAuctionsDisplay.getInstance(), runnable);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTask(DeluxeAuctionsDisplay.getInstance());
        }
    }

    public static void runTimerAsync(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia) {
            DeluxeAuctionsDisplay.getInstance().getServer().getGlobalRegionScheduler().runAtFixedRate(DeluxeAuctionsDisplay.getInstance(), task -> runnable.run(), delayTicks, periodTicks);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            }.runTaskTimerAsynchronously(DeluxeAuctionsDisplay.getInstance(), delayTicks, periodTicks);
        }
    }
}