package com.xoppa.android.world.systems;

import java.util.Comparator;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.xoppa.android.world.attributes.Light;

public class LightBatch {
	public static class LightInstance {
		public Light light;
		public Matrix4 worldTransform;
		public int distance;
	}
	
	public static Comparator<LightInstance> instanceComparator = new Comparator<LightInstance>() {
		@Override
		public int compare(LightInstance arg0, LightInstance arg1) {
			return arg1.distance - arg0.distance;
		}
	};
	
	public final Pool<LightInstance> instancePool = new Pool<LightInstance>() {
		@Override
		protected LightInstance newObject() {
			return new LightInstance();
		}
	};
	
	public final Array<LightInstance> instances = new Array<LightInstance>();
	
	public void add(final Light light, final Matrix4 worldTransform, final int distance) {
		final LightInstance instance = instancePool.obtain();
		instance.light = light;
		instance.worldTransform = worldTransform;
		instance.distance = distance;
		instances.add(instance);
	}
	
	public void clear() {
		for (int i = 0; i < instances.size; i++)
			instancePool.free(instances.get(i));
		instances.clear();
	}
	
	public void sort() {
		instances.sort(instanceComparator);
	}
}
