package com.twins.core.global.model.backpack;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class Backpack {

    private final UUID uuid;
    private final AtomicReference<ItemStack[]> contents = new AtomicReference<>(new ItemStack[45]);
    private boolean isOpen;

    public Backpack(UUID uuid, ItemStack[] contents) {
        this.uuid = uuid;
        this.contents.set(contents);
    }

    public UUID getUuid() {
        return uuid;
    }

    public ItemStack[] getContents() {
        return contents.get();
    }

    public void clearContents(int newSize) {
        contents.set(new ItemStack[newSize]);
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Backpack backpack)) return false;
        return Objects.equals(uuid, backpack.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }

}
