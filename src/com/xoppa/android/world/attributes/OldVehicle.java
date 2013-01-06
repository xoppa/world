package com.xoppa.android.world.attributes;

import com.badlogic.gdx.physics.bullet.btRaycastVehicle;
import com.badlogic.gdx.physics.bullet.btVehicleTuning;
import com.xoppa.android.world.Entity;

public class OldVehicle implements Entity.IAttribute {
	public final static int TYPE = 6;
	
	public btRaycastVehicle vehicle = null;
	public btVehicleTuning tuning = null;
	public Entity[] wheels = null;
	
	/**
	 * Creates a non-clonable vehicle
	 * @param vehicle
	 * @param wheels
	 */
	public OldVehicle(final btRaycastVehicle vehicle, final Entity[] wheels) {
		this.vehicle = vehicle;
		this.wheels = wheels;
	}
	
	public OldVehicle() {
		this((OldVehicle)null);
	}
	
	public OldVehicle(final OldVehicle copyFrom) {
		//if (copyFrom != null)
			//throw new UnsupportedOperationException(
					//"Vehicle cannot be cloned because it references other entities for the wheels");
	}
	
	public OldVehicle(btVehicleTuning tuning) {
		this.tuning = tuning;
	}
	
	@Override
	public void dispose() {
		if (vehicle != null)
			vehicle.delete();
		vehicle = null;
		// Don't remove tuning as it might be referenced by clones, let the GC take care of it
		tuning = null;
		wheels = null;
	}

	@Override
	public void reset() {
		// nothing to reset, the body does it for us
	}

	@Override
	public void init(Entity e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean is(int type) {
		return type == TYPE;
	}

	@Override
	public int type() {
		return TYPE;
	}

	@Override
	public OldVehicle clone() {
		return null;
	}
}
