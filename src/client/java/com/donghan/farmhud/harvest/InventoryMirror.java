package com.donghan.farmhud.harvest;

import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class InventoryMirror {

    public static class Entry {
        public final String  itemId;
        public final int     count;
        public final boolean plain;

        public Entry(String itemId, int count, boolean plain) {
            this.itemId = itemId;
            this.count  = count;
            this.plain  = plain;
        }
    }

    private final Map<Integer, Entry> mirror = new HashMap<>();

    public Entry  get(int slot)          { return mirror.get(slot); }
    public void   put(int slot, Entry e) { mirror.put(slot, e); }
    public void   clear()                { mirror.clear(); }
    public boolean contains(int slot)    { return mirror.containsKey(slot); }

    public static Entry fromStack(ItemStack stack, CropFilter filter) {
        String  id    = filter.itemId(stack);
        int     count = (stack == null || stack.isEmpty()) ? 0 : stack.getCount();
        boolean plain = filter.isPlain(stack);
        return new Entry(id, count, plain);
    }
}
