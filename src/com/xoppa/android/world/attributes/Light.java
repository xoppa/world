package com.xoppa.android.world.attributes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.Entity.IAttribute;

public class Light implements IAttribute {
	public final static int TYPE = 2;
	
	public Color color;
	public Vector3 direction = null;
	public float intensity = 0;
	public float angle = 0;
	
	public Light() {
		this((Light)null);
	}
	
	public Light(final Color color) {
		this((Light)null);
		this.color.set(color);
	}
	
	public Light(final Color color, final Vector3 direction, final float intensity, final float angle) {
		this((Light)null);
		this.color.set(color);
		if (direction != null) this.direction = new Vector3(direction);
		this.intensity = intensity;
		this.angle = angle;
	}
	
	public Light(final float red, final float green, final float blue) {
		this(red, green, blue, 1);
	}
	
	public Light(final float red, final float green, final float blue, final float alpha) {
		this((Light)null);
		color.set(red, green, blue, alpha);
	}
	
	public Light(final Light copyFrom) {
		color = (copyFrom == null) ? new Color() : new Color(copyFrom.color);
		direction = (copyFrom == null) ? new Vector3() : new Vector3(copyFrom.direction);
		intensity = (copyFrom == null) ? 0 : copyFrom.intensity;
		angle = (copyFrom == null) ? 0 : copyFrom.angle;
	}

	public boolean isPointLight() {
		return (direction == null) && (intensity != 0) && (angle == 0);
	}
	
	public boolean isSpotLight() {
		return (direction != null) && (intensity != 0) && (angle != 0);
	}
	
	public boolean isDirectional() {
		return (direction != null) && (intensity == 0) && (angle == 0);
	}
	
	public boolean isAmbient() {
		return (direction == null) && (intensity == 0) && (angle == 0);
	}
	
	@Override
	public void dispose() {
	}

	@Override
	public void reset() {
	}

	@Override
	public void init(Entity e) {
	}
	
	public Light clone() {
		return new Light(this);
	}
	
	@Override
	public int type() {
		return TYPE;
	}

	@Override
	public boolean is(final int type) {
		return type == TYPE;
	}
	
	public static class ConstructInfo implements Entity.IAttribute.IConstructInfo {
		public Color color = new Color();
		public Vector3 direction = null;
		public float intensity = 0;
		public float angle = 0;
		
		public ConstructInfo() {
		}
		
		@Override
		public void dispose() {
		}
		
		@Override
		public IAttribute construct() {
			return new Light(color, direction, intensity, angle);
		}
	}
}
