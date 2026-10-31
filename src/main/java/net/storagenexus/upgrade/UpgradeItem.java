package net.storagenexus.upgrade;

import net.minecraft.world.item.Item;


public class UpgradeItem extends Item {
    public enum Kind {
        CAPACITY, VOID, LOCK, TRANSFER_SPEED, INTAKE_RANGE, CONDUIT_SPEED;

        /** Upgrades that go in drawer / tank upgrade slots. */
        public boolean isStorageUpgrade() {
            return this == CAPACITY || this == VOID || this == LOCK;
        }
    }

    private final Kind kind;

    public UpgradeItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

}
