package com.xoppa.android.world;

import java.util.ArrayList;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.physics.bullet.btCollisionShape;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.xoppa.android.world.Entity.ConstructInfo;
import com.xoppa.android.world.Scene.Node;
import com.xoppa.android.world.attributes.WorldTransform;

public class World extends GroupContainer {
	public interface ISystem extends Disposable {
		/**
		 * Gets called every frame. Only if this method returns true the other two process methods are called
		 * @param delta The time (in seconds) passed since the last call to process
		 * @return True if futher processing is required, false otherwise
		 */
		boolean process(float delta);
		/**
		 * Gets called every frame, for every entity. Only gets called if {@link #process(float)} returns true
		 * @param delta The time (in seconds) passed since the last call to process. 
		 * @param e The entity currently being processed.
		 */
		void process(float delta, Entity e);
		/**
		 * Get called every frame, after all entities are processed. Only gets called if {@link #process(float)} returns true.
		 */
		void process();
		boolean init();
		void init(Entity e);
		boolean reset();
		void reset(Entity e);
	}
	
	public static class System implements ISystem {
		@Override public void dispose() {}
		@Override public boolean process(float delta) {return false;}
		@Override public void process(float delta, Entity e) {}
		@Override public void process() {}
		@Override public boolean init() { return false; }
		@Override public void init(Entity e) {}
		@Override public boolean reset() { return false; }
		@Override public void reset(Entity e) { }
	}
	
	public final static int INVALID_GUID = -1;
	private static int guidCounter = 0;
	public Array<ISystem> systems = new Array<ISystem>();
	public ObjectMap<String, Entity.ConstructInfo> constructors = new ObjectMap<String, Entity.ConstructInfo>();
	public ModelManager modelManager = new ModelManager();
	public Array<btCollisionShape> shapes = new Array<btCollisionShape>();
	public AssetManager assets;
	
	public World() {
		super();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		for (int i = 0; i < systems.size; i++)
			systems.get(i).dispose();
		systems.clear();
		for (Entity.ConstructInfo ec : constructors.values())
			ec.dispose();
	}
	
	public static int newGUID() {
		return ++guidCounter;
	}
	
	public void register(final String name, final Entity.ConstructInfo constructInfo) {
		constructors.put(name, constructInfo);
	}
	
	public Entity.ConstructInfo constructor(final String name) {
		return constructors.get(name);
	}
	
	public Group createGroup(final String name) {
		return new Group(newGUID(), name);
	}
	
	public Entity createEntity(final String name) {
		return new Entity(newGUID(), name);
	}
	
	public Entity createEntity(final String name, final Entity.IAttribute... attributes) {
		return new Entity(newGUID(), name, attributes);
	}
	
	public Entity createEntity(final String name, final Entity.ConstructInfo constructInfo) {
		return new Entity(newGUID(), name, constructInfo);
	}
	
	public Entity createEntity(final String name, final String type) {
		return new Entity(newGUID(), name, constructor(type));
	}
	
	public Group createGroup(final String name, final Scene scene) {
		final Group result = createGroup(name);
		result.load(scene);
		return result;
	}
	
	public void add(final ISystem system) {
		systems.add(system);
	}
	
	public <T extends ISystem> T system(Class<T> type) {
		ISystem result;
		for (int i = 0; i < systems.size; i++)
			if (type.isInstance(result = systems.get(i)))
				return (T)result;
		return null;
	}
	
	public void process(final float delta) {
		final int n = systems.size;
		ISystem system;
		for (int i = 0; i < n; i++) {
			system = systems.get(i);
			if (system.process(delta))
				process(delta, system);
		}
	}
	
	protected void process(final float delta, final ISystem system) {
		final int n = groups.size;
		Group group;
		for (int i = 0; i < n; i++)
			if ((group = groups.get(i)).enabled)
				process(delta, system, group);
		system.process();
	}
	
	protected void process(final float delta, final ISystem system, final Group group) {
		if (group.groups != null) {
			Group sub;
			final int n = group.groups.size;
			for (int i = 0; i < n; i++)
				if ((sub = group.groups.get(i)).enabled)
					process(delta, system, sub);
		}
		if (group.entities != null) {
			Entity ent;
			final int n = group.entities.size;
			for (int i = 0; i < n; i++)
				if ((ent = group.entities.get(i)).enabled)
					system.process(delta, ent);
		}
	}
	
	@Override
	public void init() {
		super.init();
		ISystem system;
		final int n = systems.size;
		for (int i = 0; i < n; i++)
			if ((system = systems.get(i)).init())
				init(system);
	}
	
	protected void init(final ISystem system) {
		final int n = groups.size;
		for (int i = 0; i < n; i++)
			init(system, groups.get(i));
	}
	
	protected void init(final ISystem system, final Group group) {
		if (group.groups != null) {
			final int n = group.groups.size;
			for (int i = 0; i < n; i++)
				init(system, group.groups.get(i));
		}
		if (group.entities != null) {
			final int n = group.entities.size;
			for (int i = 0; i < n; i++)
				system.init(group.entities.get(i));
		}
	}
	
	@Override
	public void reset() {
		super.reset();
		ISystem system;
		final int n = systems.size;
		for (int i = 0; i < n; i++)
			if ((system = systems.get(i)).reset())
				reset(system);
	}
	
	protected void reset(final ISystem system) {
		final int n = groups.size;
		Group group;
		for (int i = 0; i < n; i++)
			if ((group = groups.get(i)).enabled)
				reset(system, group);
	}
	
	protected void reset(final ISystem system, final Group group) {
		if (group.groups != null) {
			Group sub;
			final int n = group.groups.size;
			for (int i = 0; i < n; i++)
				if ((sub = group.groups.get(i)).enabled)
					reset(system, sub);
		}
		if (group.entities != null) {
			Entity ent;
			final int n = group.entities.size;
			for (int i = 0; i < n; i++)
				if ((ent = group.entities.get(i)).enabled)
					system.reset(ent);
		}
	}
}
