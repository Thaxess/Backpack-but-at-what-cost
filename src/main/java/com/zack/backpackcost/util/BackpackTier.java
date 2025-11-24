package com.zack.backpackcost.util;

public enum BackpackTier {
    LEATHER("Leather Backpack", 1, 5, 1.20, 4.0, "Portable storage"),
    IRON("Iron Backpack", 1, 9, 1.00, 5.0, "Sturdier storage"),
    GOLD("Golden Backpack", 2, 18, 0.80, 6.0, "Gilded space"),
    DIAMOND("Diamond Backpack", 3, 27, 0.60, 7.0, "Shiny"),
    NETHERITE("Netherite Backpack", 6, 54, 0.40, 8.0, "Unyielding");

    private final String displayName;
    private final int rows;
    private final int usableSlots;
    private final double weightMultiplier;
    private final double itemWeight;
    private final String defaultLore;

    BackpackTier(String displayName, int rows, int usableSlots, double weightMultiplier, double itemWeight, String defaultLore) {
        this.displayName = displayName;
        this.rows = rows;
        this.usableSlots = usableSlots;
        this.weightMultiplier = weightMultiplier;
        this.itemWeight = itemWeight;
        this.defaultLore = defaultLore;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRows() {
        return rows;
    }

    public int getUsableSlots() {
        return usableSlots;
    }

    public double getWeightMultiplier() {
        return weightMultiplier;
    }

    public double getItemWeight() {
        return itemWeight;
    }

    public String getDefaultLore() {
        return defaultLore;
    }

    public static BackpackTier fromString(String raw) {
        for (BackpackTier tier : values()) {
            if (tier.name().equalsIgnoreCase(raw)) {
                return tier;
            }
        }
        return null;
    }
}
