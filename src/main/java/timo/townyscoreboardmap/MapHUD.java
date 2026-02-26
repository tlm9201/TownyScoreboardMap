package timo.townyscoreboardmap;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.asciimap.WildernessMapEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.map.TownyMapData;
import com.palmergames.bukkit.util.BukkitTools;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.objective.ScoreFormat;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;

public class MapHUD {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final int mapWidth;
    private final int mapHeight;
    private final int halfWidth;
    private final int halfHeight;

    private final Sidebar sidebar;
    private final Player player;
    private final PluginConfig config;
    private final MapSize size;

    public MapHUD(ScoreboardLibrary scoreboardLibrary, Player player, PluginConfig config, MapSize size) {
        this.player = player;
        this.config = config;
        this.size = size;
        this.mapWidth  = size.width;
        this.mapHeight = size.height;
        this.halfWidth  = mapWidth  / 2;
        this.halfHeight = mapHeight / 2;
        this.sidebar = scoreboardLibrary.createSidebar(mapHeight + 1);
        sidebar.addPlayer(player);
    }

    public void update() {
        update(WorldCoord.parseWorldCoord(player));
    }

    public void update(WorldCoord wc) {
        if (wc.getTownyWorld() == null || !wc.getTownyWorld().isUsingTowny()) {
            return;
        }

        int wcX = wc.getX();
        int wcZ = wc.getZ();

        sidebar.title(config.mapTitle());

        sidebar.line(0, config.mapSubtitle(size), ScoreFormat.blank());

        // Build map array [y][x] where y iterates mapWidth times, x iterates mapHeight times
        // Each cell stores a MiniMessage color tag (e.g. "<gold>") + the symbol character(s)
        String[][] map = new String[mapWidth][mapHeight];
        fillMapArray(wcX, wcZ, TownyAPI.getInstance().getResident(player.getName()), player.getWorld(), map);

        // Write map lines (shifted down by 1 to make room for ruler)
        for (int row = 0; row < mapHeight; row++) {
            StringBuilder sb = new StringBuilder();
            for (int col = mapWidth - 1; col >= 0; col--) {
                String cell = map[col][row];
                sb.append(cell);
            }
            sidebar.line(row + 1, MM.deserialize(sb.toString()), ScoreFormat.blank());
        }
    }

    public void close() {
        if (!sidebar.closed()) {
            sidebar.close();
        }
    }

    private void fillMapArray(int wcX, int wcZ, Resident resident, World world, String[][] map) {
        int x, y = 0;
        for (int tby = wcX + (mapWidth - halfWidth - 1); tby >= wcX - halfWidth; tby--) {
            x = 0;
            for (int tbx = wcZ - halfHeight; tbx <= wcZ + (mapHeight - halfHeight - 1); tbx++) {
                WorldCoord worldCoord = new WorldCoord(world, tby, tbx);
                if (worldCoord.hasTownBlock())
                    mapTownBlock(resident, map, x, y, worldCoord.getTownBlockOrNull());
                else
                    mapWilderness(map, x, y, worldCoord);
                x++;
            }
            y++;
        }
    }

    private void mapTownBlock(Resident resident, String[][] map, int x, int y, TownBlock townBlock) {
        String colorTag = getTownBlockColorTag(resident, x, y, townBlock);
        String symbol;
        if (playerLocatedAtThisCoord(x, y))
            symbol = config.symbolPlayer();
        else if (isForSale(townBlock))
            symbol = config.symbolForSale();
        else if (townBlock.isHomeBlock())
            symbol = config.symbolHomeBlock();
        else if (townBlock.isOutpost())
            symbol = config.symbolOutpost();
        else
            symbol = config.symbolClaim();

        map[y][x] = colorTag + escapeSymbol(symbol);
    }

    private String getTownBlockColorTag(Resident resident, int x, int y, TownBlock townBlock) {
        if (playerLocatedAtThisCoord(x, y))
            return config.colorPlayer();
        else if (townBlock.hasResident(resident))
            return config.colorPlayerOwned();
        else if (townBlock.getData().hasColour())
            // Towny provides a named text color — convert to MiniMessage tag
            return "<" + townBlock.getData().getColour().toString().toLowerCase().replace('_', '_') + ">";
        else if (resident.hasTown())
            return getTownBlockColorTagForResident(resident, townBlock.getTownOrNull());
        else
            return config.colorNeutral();
    }

    private String getTownBlockColorTagForResident(Resident resident, Town townAtTownBlock) {
        if (townAtTownBlock.hasResident(resident))
            return config.colorOwnTown();

        if (!resident.hasNation())
            return config.colorNeutral();

        Nation resNation = resident.getNationOrNull();
        if (resNation.hasTown(townAtTownBlock))
            return config.colorOwnTown();

        if (!townAtTownBlock.hasNation())
            return config.colorNeutral();

        Nation townBlockNation = townAtTownBlock.getNationOrNull();
        if (resNation.hasAlly(townBlockNation))
            return config.colorAlly();
        else if (resNation.hasEnemy(townBlockNation))
            return config.colorEnemy();
        else
            return config.colorNeutral();
    }

    private boolean playerLocatedAtThisCoord(int x, int y) {
        return x == halfHeight && y == halfWidth;
    }

    private boolean isForSale(TownBlock townBlock) {
        return townBlock.getPlotPrice() != -1
                || (townBlock.hasPlotObjectGroup() && townBlock.getPlotObjectGroup().getPrice() != -1);
    }

    private void mapWilderness(String[][] map, int x, int y, WorldCoord worldCoord) {
        String colorTag = playerLocatedAtThisCoord(x, y) ? config.colorPlayer() : config.colorWilderness();

        final TownyMapData data = getWildernessMapDataMap().get(worldCoord);
        if (data == null || data.isOld()) {
            WildernessMapEvent wildMapEvent = new WildernessMapEvent(worldCoord);
            BukkitTools.fireEvent(wildMapEvent);
            getWildernessMapDataMap().put(worldCoord,
                    new TownyMapData(worldCoord, wildMapEvent.getMapSymbol(), wildMapEvent.getHoverText(), wildMapEvent.getClickCommand()));

            Towny.getPlugin().getScheduler().runAsyncLater(() ->
                    getWildernessMapDataMap().computeIfPresent(worldCoord,
                            (key, cachedData) -> cachedData.isOld() ? null : cachedData),
                    20 * 35);
        }

        map[y][x] = colorTag + escapeSymbol(playerLocatedAtThisCoord(x, y) ? config.symbolPlayer() : config.symbolWilderness());
    }

    public MapSize getSize() {
        return size;
    }

    private static Map<WorldCoord, TownyMapData> getWildernessMapDataMap() {
        return TownyUniverse.getInstance().getWildernessMapDataMap();
    }

    private static String escapeSymbol(String symbol) {
        return symbol.replace("<", "\\<");
    }
}
