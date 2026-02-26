package timo.townyscoreboardmap;

public enum MapSize {
    SMALL(11, 11),
    BIG(13, 13);

    public final int width;
    public final int height;

    MapSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static MapSize getDefault() {
        return SMALL;
    }

    public static MapSize parse(String input) {
        if (input == null) return null;
        return switch (input.toLowerCase()) {
            case "small" -> SMALL;
            case "big"   -> BIG;
            default      -> null;
        };
    }
}

