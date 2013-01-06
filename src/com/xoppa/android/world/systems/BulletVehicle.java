package com.xoppa.android.world.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.btDefaultVehicleRaycaster;
import com.badlogic.gdx.physics.bullet.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.btRaycastVehicle;
import com.badlogic.gdx.physics.bullet.btVector3;
import com.badlogic.gdx.physics.bullet.btVehicleRaycaster;
import com.badlogic.gdx.physics.bullet.btVehicleTuning;
import com.badlogic.gdx.physics.bullet.btWheelInfo;
import com.badlogic.gdx.utils.Array;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.Group;
import com.xoppa.android.world.Scene;
import com.xoppa.android.world.World;
import com.xoppa.android.world.attributes.BulletBody;
import com.xoppa.android.world.attributes.BulletBody.BulletBodyListener;
import com.xoppa.android.world.attributes.Renderable;
import com.xoppa.android.world.attributes.WorldTransform;

public class BulletVehicle extends Group implements BulletBodyListener {
	public BulletVehicle() { super(); }
	public BulletVehicle(int id, String name, boolean enabled) { super(id, name, enabled); }
	public BulletVehicle(int id, String name) { super(id, name); }
	public BulletVehicle(int id) { super(id); }
	public BulletVehicle(String name) { super(name); }

	public BulletVehicle.ConstructInfo constructInfo;
	public btRaycastVehicle vehicle;
	public btVehicleTuning tuning;
	public Entity chassis;
	public Entity[] wheels;
	public float maxBrake = 1000f;
	public float maxAcceleration = 2000f;
	public float minAcceleration = -1000f;
	public float maxSteering = MathUtils.PI * 0.25f;
	public float minSteering = MathUtils.PI * -0.25f;
	
	public float accelerateForce = 0f;
	public float acceleratePercent = 0f;
	public float brakeForce = 0f;
	public float brakePercent = 0f;
	public float steerAngle = 0f;
	public float steerPercent = 0f;
	
	private final static BoundingBox bb = new BoundingBox();
	public BulletVehicle(final String name, final BulletVehicle.ConstructInfo constructInfo) {
		load(constructInfo.scene);
		chassis = get(constructInfo.chassis);
		this.constructInfo = constructInfo;
	}
	
	public void init(final btVehicleRaycaster rayCaster, btDiscreteDynamicsWorld world) {
		tuning = constructInfo.tuning;
		vehicle = new btRaycastVehicle(tuning, chassis.get(BulletBody.class).body, rayCaster);
		world.addVehicle(vehicle);
		chassis.get(BulletBody.class).body.setActivationState(4);
		vehicle.setCoordinateSystem(constructInfo.coordinateSystem[0], constructInfo.coordinateSystem[1], constructInfo.coordinateSystem[2]);
		initWheels();
	}
	
	private btVehicleTuning wheelTuning;
	private final static Matrix4 matrix = new Matrix4();
	public void initWheels() {
		// TODO change wheels according to current transformation of chassis
		//final Matrix4 curpos = chassis.get(WorldTransform.class).current.cpy().inv();
		final Vector3 curpos = new Vector3();
		chassis.get(WorldTransform.class).current.getTranslation(curpos);
		final Vector3 point = new Vector3();
		final Vector3 direction = new Vector3(0, -1, 0);
		final Vector3 axle = new Vector3(-1, 0, 0);
		float susRestLength = 0.1f;
		float susStiffness = 20f;
		float dampRelax = 2.3f;
		float dampCompress = 4.4f;
		float frictionSlip = 1000f;
		float rollInfluence = 0.1f;
		float radius;
		wheelTuning = tuning;
		wheels = new Entity[constructInfo.wheels.size];
		for (int i = 0; i < constructInfo.wheels.size; i++) {
			final ConstructInfo.Wheel wheel = constructInfo.wheels.get(i); 
			wheels[i] = get(wheel.node);
			if (wheel.tuning != null) wheelTuning = wheel.tuning;
			if (wheel.direction != null) direction.set(wheel.direction);
			if (wheel.axle != null) axle.set(wheel.axle);
			wheels[i].get(WorldTransform.class).current.getTranslation(point);
			point.sub(curpos);
			if (wheel.suspensionRestLength >= 0f) susRestLength = wheel.suspensionRestLength;
			radius = wheel.radius;
			if (radius <= 0) {
				Vector3 dim = wheels[i].get(Renderable.class).bounds.getDimensions();
				radius = 0.5f * ((dim.x > dim.y) ? (dim.x > dim.z ? dim.x : dim.z) : (dim.y > dim.z ? dim.y : dim.z));
			}
			if (wheel.wheelsDampingRelaxation >= 0f) dampRelax = wheel.wheelsDampingRelaxation;
			if (wheel.wheelsDampingCompression >= 0f) dampCompress = wheel.wheelsDampingCompression;
			if (wheel.suspensionStiffness > 0f) susStiffness = wheel.suspensionStiffness;
			if (wheel.frictionSlip >= 0f) frictionSlip = wheel.frictionSlip;
			if (wheel.rollInfluence >= 0f) rollInfluence = wheel.rollInfluence;
			
			vehicle.addWheel(point, direction, axle, susRestLength, radius, wheelTuning, wheel.isFrontWheel);
			final btWheelInfo info = vehicle.getWheelInfo(i);
			info.setM_wheelsDampingRelaxation(dampRelax);
			info.setM_wheelsDampingCompression(dampCompress);
			info.setM_suspensionStiffness(susStiffness);
			info.setM_frictionSlip(frictionSlip);
			info.setM_rollInfluence(rollInfluence);
		}
		if (chassis.get(BulletBody.class).motionState instanceof BulletBody.ExtendedMotionState)
			((BulletBody.ExtendedMotionState)chassis.get(BulletBody.class).motionState).listener = this;
	}
	
	
	@Override
	public void setWorldTransform() {
		for (int i = 0; i < wheels.length; i++) {
			vehicle.updateWheelTransform(i, true);
			vehicle.getWheelInfo(i).getM_worldTransform().getOpenGLMatrix(wheels[i].get(WorldTransform.class).current.val);
		}
	}
	
	public void accelerate(float force, final boolean percentage) {
		if (force != 0)
			brake(0, false);
		if (vehicle != null) {
			if (percentage)
				force = (force > 0f ? force * maxAcceleration : -force * minAcceleration);
			force = MathUtils.clamp(force, minAcceleration, maxAcceleration);
			vehicle.applyEngineForce(-force, 2);
			vehicle.applyEngineForce(-force, 3);
			accelerateForce = force;
			acceleratePercent = force < 0f ? -force / minAcceleration : force / maxAcceleration;
		}
	}
	
	public void brake(float force, final boolean percentage) {
		if (vehicle != null) {
			if (percentage)
				force = force * maxBrake;
			force = MathUtils.clamp(force, 0, maxBrake);
			vehicle.setBrake(force, 2);
			vehicle.setBrake(force, 3);
			accelerateForce = 0f;
			acceleratePercent = 0f;
			brakeForce = force;
			brakePercent = force / maxBrake;
		}
	}
	
	public void steer(float angle, final boolean percentage) {
		if (vehicle != null) {
			if (percentage)
				angle = (angle > 0f ? angle * maxSteering : -angle * minSteering);
			angle = MathUtils.clamp(angle, minSteering, maxSteering);
			vehicle.setSteeringValue(-angle, 2);
			vehicle.setSteeringValue(-angle, 3);
			steerAngle = angle;
			steerPercent = angle < 0f ? -angle / minSteering : angle / maxSteering; 
		}
	}
	
	public static class ConstructInfo {
		public static class Wheel {
			/** Tuning for this wheel, or null to use the vehicles tuning */
			public btVehicleTuning tuning = null;
			/** The name of node/entity within the scene */
			public String node;
			public Vector3 direction = null;
			public Vector3 axle = null;
			public float suspensionRestLength = -1f;
			public float suspensionStiffness = -1f;
			public float wheelsDampingRelaxation = -1f;
			public float wheelsDampingCompression = -1f;
			public float frictionSlip = -1f;
			public float rollInfluence = -1f;
			/** The radius of the wheel, or <= 0 to automatically calculate */
			public float radius = 0;
			public boolean isFrontWheel = false;
			
			public Wheel() { this((Wheel)null); }
			public Wheel(final String node) { this((Wheel)null, node); }
			public Wheel(final Wheel copyFrom) {this(copyFrom, (String)null); }
			public Wheel(final Wheel copyFrom, final String node) { this(copyFrom, node, copyFrom==null?false:copyFrom.isFrontWheel); }
			public Wheel(final Wheel copyFrom, final String node, final boolean isFront) {
				if (copyFrom != null) {
					tuning = copyFrom.tuning;
					this.node = copyFrom.node;
					direction = copyFrom.direction;
					axle = copyFrom.axle;
					suspensionRestLength = copyFrom.suspensionRestLength;
					suspensionStiffness = copyFrom.suspensionStiffness;
					wheelsDampingRelaxation = copyFrom.wheelsDampingRelaxation;
					wheelsDampingCompression = copyFrom.wheelsDampingCompression;
					frictionSlip = copyFrom.frictionSlip;
					rollInfluence = copyFrom.rollInfluence;
					radius = copyFrom.radius;
				}
				if (node != null)
					this.node = node;
				isFrontWheel = isFront;
			}
		}
		
		public btVehicleTuning tuning;
		public final int[] coordinateSystem = {0, 1, 2};
		public Scene scene;
		public String chassis;
		public final Array<Wheel> wheels = new Array<Wheel>(4);
	}
}
