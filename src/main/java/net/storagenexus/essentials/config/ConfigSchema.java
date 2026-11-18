package net.storagenexus.essentials.config;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Every setting in {@link NexusConfig}: its group on the settings screen, whether it is a switch or a number (with its
 * limits), and whether only the player's own game uses it. {@code NexusConfig.clamp} and the settings screen both read
 * this, so a limit is written once.
 */
public final class ConfigSchema {
    /** One setting. For a switch, min and max are unused. */
    public record Entry(String group, String field, boolean isSwitch, int min, int max, boolean clientSide) {
        private Field reflect() {
            try {
                return NexusConfig.class.getField(field);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e);
            }
        }

        public boolean getBoolean(NexusConfig config) {
            try {
                return reflect().getBoolean(config);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        public int getInt(NexusConfig config) {
            try {
                return reflect().getInt(config);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        public void setBoolean(NexusConfig config, boolean value) {
            try {
                reflect().setBoolean(config, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        public void setInt(NexusConfig config, int value) {
            try {
                reflect().setInt(config, Math.max(min, Math.min(max, value)));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        /** Puts this setting back to the default. */
        public void reset(NexusConfig config) {
            NexusConfig defaults = new NexusConfig();
            if (isSwitch) setBoolean(config, getBoolean(defaults));
            else setInt(config, getInt(defaults));
        }

        public boolean isDefault(NexusConfig config) {
            NexusConfig defaults = new NexusConfig();
            return isSwitch ? getBoolean(config) == getBoolean(defaults) : getInt(config) == getInt(defaults);
        }

        public String nameKey() {
            return "config.storagenexus." + field;
        }

        public String tipKey() {
            return "config.storagenexus." + field + ".tip";
        }
    }

    /** The groups, in the order the screen lists them. */
    public static final List<String> GROUPS = List.of("mobs", "items", "tools", "armor", "farms", "freezer");

    public static final List<Entry> ENTRIES = List.of(
        bool("mobs", "creepersBreakBlocks"),
        bool("mobs", "ghastFireballsBreakBlocks"),
        bool("mobs", "explosionsSpareDecorations"),
        bool("mobs", "endermenPickUpBlocks"),
        bool("mobs", "mobPickUp"),
        bool("mobs", "onlyOwnersPickUpPets"),
        number("mobs", "bossPickUpLevels", 0, 1000),
        number("items", "magnetMaxRange", 4, 64),
        bool("items", "magnetPullsXp"),
        bool("items", "elytraProtectionEnchantments"),
        bool("items", "netheriteArmorCalmsPiglins"),
        number("items", "bedrockBreakerSeconds", 1, 86400),
        bool("items", "ancientDebrisDropsScrap"),
        bool("items", "rightClickHarvest"),
        number("items", "harvestVeinMax", 1, 1024),
        bool("items", "anvilNoCostLimit"),
        bool("tools", "aiotVeinMining"),
        number("tools", "aiotVeinMaxBlocks", 1, 512),
        bool("tools", "aiotAreaMining"),
        number("tools", "aiotTunnelMaxLength", 1, 64),
        bool("tools", "aiotNexusLink"),
        bool("tools", "aiotMiningPreview", true),
        bool("tools", "builderPreview", true),
        bool("tools", "diagonalPreview", true),
        bool("tools", "aiotHud", true),
        number("tools", "aiotHudSeconds", 0, 600, true),
        bool("tools", "terminalSearchSync", true),
        bool("armor", "nexusRestock"),
        number("armor", "restockTopUpAt", 1, 63),
        number("armor", "restockEatWhenMissing", 1, 20),
        number("farms", "librarianVillagers", 1, 54),
        number("farms", "ironFarmGolemTicks", 1, 24000),
        number("farms", "villagerBreederTicks", 1, 1000000),
        number("farms", "piglinBarterTicks", 1, 1000000),
        number("farms", "zombifiedPiglinTicks", 1, 1000000),
        number("farms", "witherSkeletonTicks", 1, 1000000),
        number("farms", "shulkerFarmTicks", 1, 1000000),
        number("farms", "witchFarmTicks", 1, 1000000),
        number("farms", "sugarCaneFarmTicks", 1, 1000000),
        number("farms", "bambooFarmTicks", 1, 1000000),
        number("farms", "netherWartFarmTicks", 1, 1000000),
        number("farms", "monsterFarmTicks", 1, 1000000),
        number("farms", "netherMobFarmTicks", 1, 1000000),
        number("farms", "oceanFarmTicks", 1, 1000000),
        number("farms", "slimeFarmTicks", 1, 1000000),
        number("farms", "livestockFarmTicks", 1, 1000000),
        number("farms", "cropFarmTicks", 1, 1000000),
        number("farms", "gourdFarmTicks", 1, 1000000),
        number("farms", "generatorFarmTicks", 1, 1000000),
        number("farms", "beeFarmTicks", 1, 1000000),
        number("farms", "mossFarmTicks", 1, 1000000),
        number("farms", "froglightFarmTicks", 1, 1000000),
        number("farms", "treeFarmTicks", 1, 1000000),
        number("freezer", "freezerIceTicks", 1, 1000000),
        number("freezer", "freezerPackedIceTicks", 1, 1000000),
        number("freezer", "freezerBlueIceTicks", 1, 1000000)
    );

    private ConfigSchema() {}

    public static List<Entry> in(String group) {
        return ENTRIES.stream().filter(e -> e.group().equals(group)).toList();
    }

    private static Entry bool(String group, String field) {
        return new Entry(group, field, true, 0, 0, false);
    }

    private static Entry bool(String group, String field, boolean clientSide) {
        return new Entry(group, field, true, 0, 0, clientSide);
    }

    private static Entry number(String group, String field, int min, int max) {
        return new Entry(group, field, false, min, max, false);
    }

    private static Entry number(String group, String field, int min, int max, boolean clientSide) {
        return new Entry(group, field, false, min, max, clientSide);
    }
}
