package com.xoppa.android.world.attributes;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.Entity.IAttribute;

public class LocalTransform  implements IAttribute {
	public final static int TYPE = 3;
	
	public final Matrix4 current = new Matrix4();
	public Matrix4 initial = null;
	
	public LocalTransform() {
		this((LocalTransform)null);
	}
	
	public LocalTransform(final Matrix4 transform) {
		this((LocalTransform)null);
		if (transform != null)
			current.set(transform);
	}
	
	public LocalTransform(final LocalTransform copyFrom) {
		if (copyFrom != null) {
			current.set(copyFrom.current);
			if (copyFrom.initial != null)
				initial = new Matrix4(copyFrom.initial);
		} else {
			current.idt();
		}
	}
	
	@Override
	public void dispose() {
	}

	
	@Override
	public void reset() {
		if (initial != null)
			current.set(initial);
	}
	
	public Vector3 getPosition(final Vector3 out) {
		return out;
	}
	
	@Override
	public LocalTransform clone() {
		return new LocalTransform(this);
	}
	
	@Override
	public void init(Entity e) {
		if (initial == null)
			initial = new Matrix4(current);
		else
			initial.set(current);
	}
	
	@Override
	public boolean is(final int type) {
		return type == TYPE;
	}
	
	@Override
	public int type() {
		return TYPE;
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
		
		@Override
		public void dispose() {
		}
		
		@Override
		public IAttribute construct() {
			return new LocalTransform(transform);
		}
	}
}