package com.armageddon.client.fx;

import com.armageddon.event.events.FossilEvents;
import net.minecraft.util.math.Vec3d;

import java.io.PrintWriter;

/** Writes the fossil bombs' planned skeletons out as voxel lists, for looking at outside the game. */
public final class BoneDump {
	public static void main(String[] args) throws Exception {
		Vec3d base = new Vec3d(0.5, 0, 0.5), h = new Vec3d(1, 0, 0);
		dump(args[0] + "/mammoth.txt", FossilEvents.MammothFossil.plan(base, h));
		dump(args[0] + "/leviathan.txt", FossilEvents.LeviathanFossil.plan(base, h));
		dump(args[0] + "/spire.txt", FossilEvents.PterosaurFossil.planSpire(base));
	}

	private static void dump(String path, FossilEvents.Bones b) throws Exception {
		try (PrintWriter w = new PrintWriter(path)) {
			for (int[] v : b.list) {
				w.println(v[0] + " " + v[1] + " " + v[2] + " " + v[3]);
			}
		}
	}
}
