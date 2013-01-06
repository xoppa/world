package com.xoppa.android.world.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.BroadphaseNativeTypes;
import com.badlogic.gdx.physics.bullet.btBoxShape;
import com.badlogic.gdx.physics.bullet.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.btCollisionObject;
import com.badlogic.gdx.physics.bullet.btCollisionObjectArray;
import com.badlogic.gdx.physics.bullet.btCollisionShape;
import com.badlogic.gdx.physics.bullet.btCompoundShape;
import com.badlogic.gdx.physics.bullet.btCylinderShape;
import com.badlogic.gdx.physics.bullet.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.btDefaultMotionState;
import com.badlogic.gdx.physics.bullet.btDefaultVehicleRaycaster;
import com.badlogic.gdx.physics.bullet.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.btRigidBody;
import com.badlogic.gdx.physics.bullet.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.btStaticPlaneShape;
import com.badlogic.gdx.physics.bullet.btTransform;
import com.badlogic.gdx.physics.bullet.btVehicleRaycaster;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.PerformanceCounter;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.World;
import com.xoppa.android.world.attributes.BulletBody;
import com.xoppa.android.world.attributes.OldVehicle;
import com.xoppa.android.world.attributes.WorldTransform;

public class BulletPhysics implements World.ISystem {
	private btDefaultCollisionConfiguration collisionConfiguration;
	private btCollisionDispatcher dispatcher;
	private btDbvtBroadphase broadphase;
	private btSequentialImpulseConstraintSolver solver;
	private btVehicleRaycaster rayCaster = null;
	public btDiscreteDynamicsWorld dynamicsWorld;
	public final Vector3 gravity = new Vector3(0, -10, 0);
	public Array<BulletVehicle> vehicles = null;
	private PerformanceCounter performanceCounter;
	
	public BulletPhysics(PerformanceCounter perfCounter) {
		performanceCounter = perfCounter;
	}
	
	@Override
	public void dispose() {
		dynamicsWorld.delete();
		solver.delete();
		broadphase.delete();
		dispatcher.delete();
		collisionConfiguration.delete();
	}
	
	public void addVehicle(final BulletVehicle vehicle) {
		if (vehicles == null)
			vehicles = new Array<BulletVehicle>();
		vehicles.add(vehicle);
		if (dynamicsWorld != null)
			initVehicle(vehicle);
	}
	
	private void initVehicle(final BulletVehicle vehicle) {
		if (rayCaster == null)
			rayCaster = new btDefaultVehicleRaycaster(dynamicsWorld);
		vehicle.init(rayCaster, dynamicsWorld);
	}
	
	public int getVehicleCount() {
		return vehicles == null ? 0 : vehicles.size;
	}
	
	public BulletVehicle getVehicle(final int index) {
		return vehicles.get(index);
	}
	
	@Override
	public boolean process(final float delta) {
		if (performanceCounter != null)
			performanceCounter.start();
		dynamicsWorld.stepSimulation(delta, 5);
		if (performanceCounter != null)
			performanceCounter.stop();
		return true;
	}
	
	@Override
	public void process(final float delta, final Entity e) {
		OldVehicle vehicle = e.get(OldVehicle.class);
		if (vehicle != null) {
			final Entity[] wheels = vehicle.wheels;
			for (int i = 0; i < wheels.length && i < vehicle.vehicle.getNumWheels(); i++) {
				vehicle.vehicle.updateWheelTransform(i, true);
				final btTransform tr = vehicle.vehicle.getWheelInfo(i).getM_worldTransform();
				tr.getOpenGLMatrix(wheels[i].get(WorldTransform.class).current.val);
			}
		}
	}

	@Override
	public boolean init() {
		collisionConfiguration = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(collisionConfiguration);
		broadphase = new btDbvtBroadphase();
		solver = new btSequentialImpulseConstraintSolver();
		dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
		dynamicsWorld.setGravity(gravity);
		return true;
	}

	@Override
	public void init(final Entity e) {
		BulletBody b = e.get(BulletBody.class);
		if (b != null && b.body != null)
			dynamicsWorld.addRigidBody(b.body);
	}
	
	@Override public void process() {}
	@Override public boolean reset() { return false; }
	@Override public void reset(final Entity e) {}

	public void debugDraw(final ImmediateModeRenderer renderer, final Matrix4 projection) {
		if (dynamicsWorld == null) return;
		btCollisionObjectArray objects = dynamicsWorld.getCollisionObjectArray();
		final int n = objects.size();
		final Matrix4 m = new Matrix4();
		for (int i = 0; i < n; i++) {
			final btCollisionObject object = objects.at(i);
			object.getWorldTransform(m);
			debugDraw(object.getCollisionShape(), renderer, projection, m);
		}
	}
	
	//public final static btTransform tmpTransform1 = new btTransform();
	//public final static btTransform tmpTransform2 = new btTransform();
	public void debugDraw(final btCollisionShape shape, final ImmediateModeRenderer renderer, final Matrix4 projection, final Matrix4 transform) {
		if (shape.isCompound()) {
			btCompoundShape cmp = (btCompoundShape)shape;
			final int n = cmp.getNumChildShapes();
			final Matrix4 m = new Matrix4();
			for (int i = 0; i < n; i++) {
				m.set(cmp.getChildTransform(i));
				debugDraw(cmp.getChildShape(i), renderer, projection, m.mul(transform));
			}
		} else if (shape.getShapeType() == BroadphaseNativeTypes.BOX_SHAPE_PROXYTYPE) {
			btBoxShape box = (btBoxShape)shape;

			debugDrawBox(renderer, projection, box.getHalfExtentsWithMargin(), transform, 1f, 1f, 1f, 1f);
		} else if (shape.getShapeType() == BroadphaseNativeTypes.CYLINDER_SHAPE_PROXYTYPE) {
			btCylinderShape cylinder = (btCylinderShape)shape;
			debugDrawBox(renderer, projection, cylinder.getHalfExtentsWithMargin(), transform, 1f, 1f, 1f, 1f);
		}
		//}
	}
	
	public void debugDrawBox(final ImmediateModeRenderer renderer, final Matrix4 projection, final Vector3 dim, final Matrix4 transform, final float red, final float green, final float blue, final float alpha) {
		Gdx.gl11.glPushMatrix();
		renderer.begin(projection, GL10.GL_LINE_STRIP);
		Gdx.gl11.glMultMatrixf(transform.val, 0);
		renderer.color(red, green, blue, alpha);
		renderer.vertex(dim.x, dim.y, dim.z);
		renderer.color(red, green, blue, alpha);
		renderer.vertex(-dim.x, dim.y, dim.z);
		renderer.color(red, green, blue, alpha);
		renderer.vertex(-dim.x, -dim.y, dim.z);
		renderer.color(red, green, blue, alpha);
		renderer.vertex(dim.x, -dim.y, dim.z);
		renderer.color(red, green, blue, alpha);
		renderer.vertex(dim.x, dim.y, dim.z);
		renderer.color(red, green, blue, alpha);
		renderer.vertex(dim.x, dim.y, -dim.z);
		renderer.color(red, green, blue, alpha);
		renderer.vertex(-dim.x, dim.y, -dim.z);
		renderer.color(red, green, blue, alpha);
		renderer.vertex(-dim.x, -dim.y, -dim.z);
		renderer.color(red, green, blue, alpha);
		renderer.vertex(dim.x, -dim.y, -dim.z);
		renderer.color(red, green, blue, alpha);
		renderer.vertex(dim.x, dim.y, -dim.z);
		renderer.end();
		Gdx.gl11.glPopMatrix();
	}
}
