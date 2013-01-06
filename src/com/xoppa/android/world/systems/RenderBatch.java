package com.xoppa.android.world.systems;

import java.util.Comparator;
import java.util.Iterator;

import com.badlogic.gdx.graphics.g3d.model.Model;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.attributes.Light;
import com.xoppa.android.world.attributes.LocalTransform;
import com.xoppa.android.world.attributes.Renderable;
import com.xoppa.android.world.attributes.WorldTransform;

public class RenderBatch {
	public static class RenderInstance {
		public Renderable renderable;
		public Matrix4 worldTransform;
		public Matrix4 localTransform;
		public int distance;
	}
	
	public static Comparator<RenderInstance> instanceComparator = new Comparator<RenderInstance>() {
		@Override
		public int compare(RenderInstance arg0, RenderInstance arg1) {
			if (arg0.renderable.priority != arg1.renderable.priority)
				return arg1.renderable.priority - arg0.renderable.priority;
			return arg0.distance - arg1.distance;
		}
	};
	
	public final Pool<RenderInstance> instancePool = new Pool<RenderInstance>() {
		@Override
		protected RenderInstance newObject() {
			return new RenderInstance();
		}
	};
	
	public final Array<RenderInstance> instances = new Array<RenderInstance>();
	
	public RenderBatch() {}
	public RenderBatch(final Iterable<Entity> it) {
		add(it);
	}
	public RenderBatch(final Iterator<Entity> it) {
		add(it);
	}
	
	public void add(final Entity e) {
		add(e, 0);
	}
	
	public void add(final Entity e, final int distance) {
		Matrix4 worldTransform = null;
		Matrix4 localTransform = null;
		Renderable renderable = null;
		for (int i = 0; i < e.attributes.size; i++) {
			final Entity.IAttribute attribute = e.attributes.get(i);
			final int type = attribute.type();
			switch (type) {
			case WorldTransform.TYPE:
				worldTransform = ((WorldTransform)attribute).current;
				break;
			case Renderable.TYPE:
				renderable = (Renderable)attribute;
				break;
			case LocalTransform.TYPE:
				localTransform = ((LocalTransform)attribute).current;
				break;
			}
		}
		
		if ((worldTransform != null) && (renderable != null) && (renderable.visible) && (renderable.models != null)) {	
				add(renderable, worldTransform, localTransform, distance);
		}
	}
	
	public void add(final Iterable<Entity> it) {
		add(it.iterator());
	}
	
	public void add(final Iterator<Entity> it) {
		while(it.hasNext())
			add(it.next());
	}
	
	public void add(final Renderable renderable, final Matrix4 worldTransform, final Matrix4 localTransform, final int distance) {
		final RenderInstance instance = instancePool.obtain();
		instance.renderable = renderable;
		instance.worldTransform = worldTransform;
		instance.localTransform = localTransform;
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
