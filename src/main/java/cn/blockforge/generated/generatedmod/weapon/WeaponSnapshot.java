package cn.blockforge.generated.generatedmod.weapon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/** Version-neutral serialized ItemStack; NBT is intentionally retained for guns and attachments. */
public record WeaponSnapshot(String itemId, String tagBase64, int count) {
    public static WeaponSnapshot empty() {
        return new WeaponSnapshot("", "", 0);
    }

    public static WeaponSnapshot from(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return empty();
        try {
            CompoundTag tag = stack.save(new CompoundTag());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, bytes);
            return new WeaponSnapshot(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString(),
                    Base64.getEncoder().encodeToString(bytes.toByteArray()), Math.max(1, stack.getCount()));
        } catch (Exception ignored) {
            return empty();
        }
    }

    public ItemStack stack() {
        if (itemId.isBlank() || tagBase64.isBlank() || count <= 0) return ItemStack.EMPTY;
        try {
            byte[] decoded = Base64.getDecoder().decode(tagBase64);
            CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(decoded));
            ItemStack stack = ItemStack.of(tag);
            stack.setCount(Math.min(count, Math.max(1, stack.getMaxStackSize())));
            return stack;
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    public String displayName() {
        ItemStack stack = stack();
        return stack.isEmpty() ? itemId : stack.getHoverName().getString();
    }

    public boolean valid() {
        return !stack().isEmpty();
    }
}
