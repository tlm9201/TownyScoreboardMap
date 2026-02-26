package timo.townyscoreboardmap;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MapHUDManager {

    private static final long UPDATE_PERIOD_TICKS = 20L;

    private final ScoreboardLibrary scoreboardLibrary;
    private final PluginConfig config;
    private final Map<UUID, MapHUD> huds = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

    public MapHUDManager(ScoreboardLibrary scoreboardLibrary, PluginConfig config) {
        this.scoreboardLibrary = scoreboardLibrary;
        this.config = config;
    }

    /**
     * Starts a per-player repeating task using Folia's player scheduler.
     * Safe to call multiple times — cancels any existing task first.
     */
    public void startUpdateTask(Player player, JavaPlugin plugin) {
        cancelTask(player.getUniqueId());
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            MapHUD hud = huds.get(player.getUniqueId());
            if (hud != null) {
                hud.update();
            }
        }, null, 1L, UPDATE_PERIOD_TICKS);
        if (task != null) {
            tasks.put(player.getUniqueId(), task);
        }
    }

    public void toggle(Player player, MapSize size) {
        MapHUD existing = huds.get(player.getUniqueId());
        if (existing != null) {
            if (existing.getSize() != size) {
                // Switch to the new size instead of disabling
                existing.close();
                huds.remove(player.getUniqueId());
                MapHUD hud = new MapHUD(scoreboardLibrary, player, config, size);
                huds.put(player.getUniqueId(), hud);
                hud.update();
                player.sendMessage(config.hudEnabled());
            } else {
                existing.close();
                huds.remove(player.getUniqueId());
                player.sendMessage(config.hudDisabled());
            }
        } else {
            MapHUD hud = new MapHUD(scoreboardLibrary, player, config, size);
            huds.put(player.getUniqueId(), hud);
            hud.update();
            player.sendMessage(config.hudEnabled());
        }
    }

    public void update(Player player) {
        MapHUD hud = huds.get(player.getUniqueId());
        if (hud != null) {
            hud.update();
        }
    }

    public void remove(Player player) {
        cancelTask(player.getUniqueId());
        MapHUD hud = huds.remove(player.getUniqueId());
        if (hud != null) {
            hud.close();
        }
    }

    public void closeAll() {
        tasks.values().forEach(ScheduledTask::cancel);
        tasks.clear();
        huds.values().forEach(MapHUD::close);
        huds.clear();
    }

    private void cancelTask(UUID uuid) {
        ScheduledTask existing = tasks.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }
    }
}

