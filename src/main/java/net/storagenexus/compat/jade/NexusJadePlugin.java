package net.storagenexus.compat.jade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.storagenexus.StorageNexus;
import net.storagenexus.conduit.ConduitBundleBlockEntity;
import net.storagenexus.conduit.ConduitType;
import net.storagenexus.conduit.EnergyNetwork;
import net.storagenexus.energy.NexusEnergy;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.List;

/**
 * Shows Nexus Energy in Jade's tooltip for the farms, the Freezer, the generators, Batteries, the Redstone Reactor and
 * Energy Conduits (the conduit shows its whole network's pool). Jade loads this through the {@code jade} entrypoint, so it
 * does nothing, and nothing here is loaded, unless Jade is installed.
 */
public class NexusJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEnergyStorage(NexusEnergyProvider.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEnergyStorageClient(NexusEnergyProvider.INSTANCE);
    }

    /** Reads the energy of what the player looks at: a conduit's network, or any block that has Nexus Energy. */
    public enum NexusEnergyProvider implements IServerExtensionProvider<EnergyView.Data>, IClientExtensionProvider<EnergyView.Data, EnergyView> {
        INSTANCE;

        private static final Identifier UID = StorageNexus.id("energy");

        @Override
        public Identifier getUid() {
            return UID;
        }

        /** The stored and capacity to show for this target, or null when it has no Nexus Energy. */
        private static long @Nullable [] read(Accessor<?> accessor) {
            if (!(accessor instanceof BlockAccessor block) || block.getLevel() == null) return null;
            if (block.getBlockEntity() instanceof ConduitBundleBlockEntity conduit) {
                if (!conduit.has(ConduitType.ENERGY)) return null;
                EnergyNetwork network = EnergyNetwork.of(conduit);
                return new long[]{network.stored(), network.capacity()};
            }
            NexusEnergy energy = NexusEnergy.SIDED.find(block.getLevel(), block.getPosition(), null);
            return energy == null ? null : new long[]{energy.stored(), energy.capacity()};
        }

        @Override
        public @Nullable List<ViewGroup<EnergyView.Data>> getGroups(Accessor<?> accessor) {
            long[] values = read(accessor);
            if (values == null || values[1] <= 0) return null;
            return List.of(new ViewGroup<>(List.of(new EnergyView.Data(values[0], values[1]))));
        }

        @Override
        public boolean shouldRequestData(Accessor<?> accessor) {
            long[] values = read(accessor);
            return values != null && values[1] > 0;
        }

        @Override
        public List<ClientViewGroup<EnergyView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<EnergyView.Data>> groups) {
            return ClientViewGroup.map(groups, data -> EnergyView.read(data, "NE"), null);
        }
    }
}
