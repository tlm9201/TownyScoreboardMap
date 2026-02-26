package timo.townyscoreboardmap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles "/t minimap [small|big]" as a Towny subcommand.
 * When registered via TownyCommandAddonAPI, args[0] is the first argument
 * after "minimap" (Towny strips the subcommand name automatically).
 */
public class MinimapCommand implements CommandExecutor, TabCompleter {

    private final MapHUDManager hudManager;
    private final PluginConfig config;

    public MinimapCommand(MapHUDManager hudManager, PluginConfig config) {
        this.hudManager = hudManager;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.commandPlayerOnly());
            return true;
        }

        MapSize size = MapSize.getDefault();
        if (args.length >= 1) {
            MapSize parsed = MapSize.parse(args[0]);
            if (parsed == null) {
                player.sendMessage(config.commandInvalidSize());
                return true;
            }
            size = parsed;
        }

        hudManager.toggle(player, size);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("small", "big").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}

