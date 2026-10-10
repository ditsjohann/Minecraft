package com.armageddon.destruction;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;

/** Named boxes in a world that WMDs will never modify. Saved with the world. */
public class ProtectedRegions extends PersistentState {
	public static final String ID = "armageddon_protected_regions";

	public record Region(String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		public boolean contains(int x, int y, int z) {
			return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
		}

		public boolean intersects(int x0, int z0, int x1, int z1) {
			return x1 >= minX && x0 <= maxX && z1 >= minZ && z0 <= maxZ;
		}
	}

	private final List<Region> regions = new ArrayList<>();

	public static ProtectedRegions get(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(
				new PersistentState.Type<>(ProtectedRegions::new, ProtectedRegions::read, null), ID);
	}

	public List<Region> regions() {
		return regions;
	}

	public boolean isEmpty() {
		return regions.isEmpty();
	}

	public boolean isProtected(int x, int y, int z) {
		for (Region r : regions) {
			if (r.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	public boolean isProtected(BlockPos pos) {
		return !regions.isEmpty() && isProtected(pos.getX(), pos.getY(), pos.getZ());
	}

	public void add(String name, BlockPos a, BlockPos b) {
		remove(name);
		regions.add(new Region(name, Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
				Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
		markDirty();
	}

	public boolean remove(String name) {
		boolean removed = regions.removeIf(r -> r.name.equals(name));
		if (removed) {
			markDirty();
		}
		return removed;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		NbtList list = new NbtList();
		for (Region r : regions) {
			NbtCompound c = new NbtCompound();
			c.putString("Name", r.name);
			c.putIntArray("Box", new int[]{r.minX, r.minY, r.minZ, r.maxX, r.maxY, r.maxZ});
			list.add(c);
		}
		nbt.put("Regions", list);
		return nbt;
	}

	private static ProtectedRegions read(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		ProtectedRegions state = new ProtectedRegions();
		NbtList list = nbt.getList("Regions", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			NbtCompound c = list.getCompound(i);
			int[] b = c.getIntArray("Box");
			if (b.length == 6) {
				state.regions.add(new Region(c.getString("Name"), b[0], b[1], b[2], b[3], b[4], b[5]));
			}
		}
		return state;
	}
}
