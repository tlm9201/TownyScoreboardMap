package timo.townyscoreboardmap;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;


public class PluginConfig {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final FileConfiguration cfg;

    public PluginConfig(FileConfiguration cfg) {
        this.cfg = cfg;
    }

    public Component hudEnabled() {
        return parse("messages.hud.enabled");
    }

    public Component hudDisabled() {
        return parse("messages.hud.disabled");
    }

    public Component commandPlayerOnly() {
        return parse("messages.command.player-only");
    }

    public Component commandInvalidSize() {
        return parse("messages.command.invalid-size");
    }

    public Component mapTitle() {
        return parse("map.title");
    }

    public Component mapSubtitle(MapSize size) {
        return switch (size) {
            case SMALL -> parse("map.small.subtitle");
            case BIG   -> parse("map.big.subtitle");
        };
    }

    public String colorPlayer()     { return cfg.getString("map.color.player",      "<gold>"); }
    public String colorPlayerOwned(){ return cfg.getString("map.color.player-owned","<yellow>"); }
    public String colorOwnTown()    { return cfg.getString("map.color.own-town",     "<green>"); }
    public String colorAlly()       { return cfg.getString("map.color.ally",         "<green>"); }
    public String colorEnemy()      { return cfg.getString("map.color.enemy",        "<red>"); }
    public String colorNeutral()    { return cfg.getString("map.color.neutral",      "<white>"); }
    public String colorWilderness() { return cfg.getString("map.color.wilderness",   "<gray>"); }

    public String symbolClaim()     { return cfg.getString("map.symbol.claim",     "+"); }
    public String symbolPlayer()    { return cfg.getString("map.symbol.player",    "*"); }
    public String symbolHomeBlock() { return cfg.getString("map.symbol.home-block","H"); }
    public String symbolOutpost()   { return cfg.getString("map.symbol.outpost",   "O"); }
    public String symbolForSale()   { return cfg.getString("map.symbol.for-sale",  "$"); }
    public String symbolWilderness(){ return cfg.getString("map.symbol.wilderness","-"); }

    private Component parse(String key, TagResolver... resolvers) {
        String raw = cfg.getString(key, "");
        return MM.deserialize(raw, resolvers);
    }
}

