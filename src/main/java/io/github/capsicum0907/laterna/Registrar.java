package io.github.capsicum0907.laterna;

//? if fabric {
/*import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;

public final class Registrar<T> {
    private final net.minecraft.core.Registry<T> registry;
    private final Map<String, Entry<? extends T>> entries = new LinkedHashMap<>();

    public Registrar(net.minecraft.core.Registry<T> registry) {
        this.registry = registry;
    }

    public <I extends T> Supplier<I> register(String id, Supplier<I> factory) {
        Entry<I> entry = new Entry<>(factory);
        if (entries.putIfAbsent(id, entry) != null) {
            throw new IllegalStateException("Duplicate registration of " + id);
        }
        return entry;
    }

    public void register() {
        for (Map.Entry<String, Entry<? extends T>> entry : entries.entrySet()) {
            entry.getValue().register(registry, entry.getKey());
        }
    }

    private static final class Entry<I> implements Supplier<I> {
        private final Supplier<I> factory;
        private I value;

        private Entry(Supplier<I> factory) {
            this.factory = factory;
        }

        @SuppressWarnings("unchecked")
        private <T> void register(net.minecraft.core.Registry<T> registry, String id) {
            value = factory.get();
            net.minecraft.core.Registry.register(registry, ResourceLocation.fromNamespaceAndPath(Laterna.MODID, id), (T) value);
        }

        @Override
        public I get() {
            if (value == null) {
                throw new IllegalStateException("Not registered yet");
            }
            return value;
        }
    }
}
*///?}
