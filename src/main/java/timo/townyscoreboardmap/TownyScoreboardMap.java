package timo.townyscoreboardmap;

import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.object.AddonCommand;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class TownyScoreboardMap extends JavaPlugin implements Listener {

    private static TownyScoreboardMap instance;
    private ScoreboardLibrary scoreboardLibrary;
    private MapHUDManager hudManager;
    private PluginConfig pluginConfig;
    private AddonCommand minimapAddonCommand;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        pluginConfig = new PluginConfig(getConfig());

        try {
            scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this);
        } catch (NoPacketAdapterAvailableException e) {
            scoreboardLibrary = new NoopScoreboardLibrary();
            getLogger().warning("Server version unsupported, scoreboard functionality will not be visible!");
        }

        hudManager = new MapHUDManager(scoreboardLibrary, pluginConfig);

        MinimapCommand minimapCommand = new MinimapCommand(hudManager, pluginConfig);
        minimapAddonCommand = new AddonCommand(CommandType.TOWN, "minimap", minimapCommand);
        minimapAddonCommand.setTabCompleter(minimapCommand);
        minimapAddonCommand.setTabCompletion(1, List.of("small", "big"));
        TownyCommandAddonAPI.addSubCommand(minimapAddonCommand);

        Bukkit.getPluginManager().registerEvents(this, this);

        // Schedule update tasks for any players already online (e.g. reload)
        for (Player player : Bukkit.getOnlinePlayers()) {
            hudManager.startUpdateTask(player, this);
        }
    }

    @Override
    public void onDisable() {
        if (minimapAddonCommand != null) {
            TownyCommandAddonAPI.removeSubCommand(minimapAddonCommand);
        }
        if (hudManager != null) {
            hudManager.closeAll();
        }
        if (scoreboardLibrary != null) {
            scoreboardLibrary.close();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        hudManager.startUpdateTask(event.getPlayer(), this);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        hudManager.remove(event.getPlayer());
    }



    public static TownyScoreboardMap getInstance() {
        return instance;
    }

    public MapHUDManager getHUDManager() {
        return hudManager;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }
}
