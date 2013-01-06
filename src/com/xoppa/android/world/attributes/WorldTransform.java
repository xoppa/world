package com.xoppa.android.world.attributes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.Entity.IAttribute;

public class WorldTransform implements IAttribute {
	public final static int TYPE = 5;
	
	public boolean changed = false;
	public boolean dirty = true;
	public final Matrix4 current = new Matrix4();
	public Matrix4 initial = null;
	
	public WorldTransform() {
		this((WorldTransform)null);
	}
	
	public WorldTransform(final Matrix4 transform) {
		this((WorldTransform)null);
		if (transform != null)
			current.set(transform);
	}
	
	public WorldTransform(final float x, final float y, final float z) {
		this((WorldTransform)null);
		current.translate(x, y, z);
		//current.origin.set(x, y, z);
	}
	
	public WorldTransform(final WorldTransform copyFrom) {
		if (copyFrom != null) {
			changed = copyFrom.changed;
			current.set(copyFrom.current);
			if (copyFrom.initial != null)
				initial = new Matrix4(copyFrom.initial); //new Transform(copyFrom.initial);
		} else {
			current.idt(); //setIdentity();
		}
	}
	
	@Override
	public void dispose() {
	}

	
	@Override
	public void reset() {
		if (initial != null) {
			current.set(initial);
			dirty = true;
			changed = false;
		}		
	}
	
	public Vector3 getPosition(final Vector3 out) {
		current.getTranslation(out);
		return out;
	}
	
	@Override
	public WorldTransform clone() {
		return new WorldTransform(this);
	}
	
	@Override
	public boolean is(final int type) {
		return type == TYPE;
	}
	
	@Override
	public int type() {
		return TYPE;
	}

	public Matrix4 getInitial(final Matrix4 out) {
		if (initial == null)
			out.idt();
		else
			out.set(initial);
		return out;
	}
	
	public void setInitial(final Matrix4 value) {
		if (initial == null)
			initial = new Matrix4(value);
		else
			initial.set(value);
		if (!changed) {
			current.set(initial);
			dirty = true;
		}
	}
	
	@Override
	public void init(Entity e) {
		if (initial == null)
			initial = new Matrix4(current);
		else
			initial.set(current);
		changed = false;
	}

	public void transform(final Matrix4 rhs) {
		current.mul(rhs);
	}
	public static void transform(final WorldTransform transform, final Matrix4 rhs) {
		if (transform != null)
			transform.transform(rhs);
	}
	public static void transform(final Entity entity, final Matrix4 rhs) {
		if (entity != null)
			transform(entity.get(WorldTransform.class), rhs);
	}
	public static void transform(final Iterable<Entity> entities, final Matrix4 rhs) {
		for (Entity e : entities)
			transform(e, rhs);
	}
	
	public void move(final float x, final float y, final float z) {
		current.trn(x, y, z);
	}
	public static void move(final WorldTransform transform, final float x, final float y, final float z) {
		if (transform != null)
			transform.move(x, y, z);
	}
	public static void move(final Entity entity, final float x, final float y, final float z) {
		if (entity != null)
			move(entity.get(WorldTransform.class), x, y, z);
	}
	public static void move(final Iterable<Entity> entities, final float x, final float y, final float z) {
		for (Entity e : entities)
			move(e, x, y, z);
	}

	public void rotate(final float x, final float y, final float z, final float angle) {
		current.rotate(x, y, z, angle);
	}
	public void rotate(final float x, final float y, final float z, final float angle, final float offsetX, final float offsetY, final float offsetZ) {
		Gdx.app.log("WorldTransform", "Rotate offset: "+offsetX+", "+offsetY+", "+offsetZ);
		Matrix4 tmp = new Matrix4();
		//tmp.translate(offsetX, offsetY, offsetZ);
		tmp.rotate(x, y, z, angle);
		//tmp.translate(-offsetX, -offsetY, -offsetZ);
		tmp.mul(current);
		current.set(tmp);
	}
	public void rotateAround(final float x, final float y, final float z, final float angle, final float pointX, final float pointY, final float pointZ) {
		current.getTranslation(Vector3.tmp);
		rotate(x, y, z, angle, pointX - Vector3.tmp.x, pointY - Vector3.tmp.y, pointZ - Vector3.tmp.z);
	}
	public static void rotate(final WorldTransform transform, final float x, final float y, final float z, final float angle) {
		if (transform != null)
			transform.rotate(x, y, z, angle);
	}
	public static void rotate(final WorldTransform transform, final float x, final float y, final float z, final float angle, final float offsetX, final float offsetY, final float offsetZ) {
		if (transform != null)
			transform.rotate(x, y, z, angle, offsetX, offsetY, offsetZ);
	}
	public static void rotateAround(final WorldTransform transform, final float x, final float y, final float z, final float angle, final float pointX, final float pointY, final float pointZ) {
		if (transform != null)
			transform.rotateAround(x, y, z, angle, pointX, pointY, pointZ);
	}
	public static void rotate(final Entity entity, final float x, final float y, final float z, final float angle) {
		if (entity != null)
			rotate(entity.get(WorldTransform.class), x, y, z, angle);
	}	
	public static void rotate(final Entity entity, final float x, final float y, final float z, final float angle, final float offsetX, final float offsetY, final float offsetZ) {
		if (entity != null)
			rotate(entity.get(WorldTransform.class), x, y, z, angle, offsetX, offsetY, offsetZ);
	}
	public static void rotateAround(final Entity entity, final float x, final float y, final float z, final float angle, final float pointX, final float pointY, final float pointZ) {
		if (entity != null)
			rotateAround(entity.get(WorldTransform.class), x, y, z, angle, pointX, pointY, pointZ);
	}
	public static void rotate(final Iterable<Entity> entities, final float x, final float y, final float z, final float angle) {
		for (Entity e: entities)
			rotate(e, x, y, z, angle);
	}
	public static void rotate(final Iterable<Entity> entities, final float x, final float y, final float z, final float angle, final float offsetX, final float offsetY, final float offsetZ) {
		for (Entity e: entities)
			rotate(e, x, y, z, angle, offsetX, offsetY, offsetZ);
	}
	public static void rotateAround(final Iterable<Entity> entities, final float x, final float y, final float z, final float angle, final float pointX, final float pointY, final float pointZ) {
		for (Entity e: entities)
			rotateAround(e, x, y, z, angle, pointX, pointY, pointZ);
	}

	public static class ConstructInfo implements Entity.IAttribute.IConstructInfo {
		public Matrix4 transform;
		
		public ConstructInfo() {
			this(null);
		}
		
		public ConstructInfo(final Matrix4 transform) {
			if (transform != null)
				this.transform = new Matrix4(transform);
		}
		
		public ConstructInfo(final float x, final float y, final float z) {
			transform = new Matrix4();
			transform.idt();
			transform.translate(x, y, z); //origin.set(x, y, z);
		}
		
		@Override
		public void dispose() {
		}
		
		@Override
		public IAttribute construct() {
			return new WorldTransform(transform);
		}
	}
}
