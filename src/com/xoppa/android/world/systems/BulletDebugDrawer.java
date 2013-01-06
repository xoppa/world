package com.xoppa.android.world.systems;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.physics.bullet.btCollisionWorld;
import com.badlogic.gdx.physics.bullet.btIDebugDraw;
import com.badlogic.gdx.physics.bullet.btVector3;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.World;

public class BulletDebugDrawer extends btIDebugDraw implements World.ISystem {
	public int debugMode = 0;
	public ShapeRenderer lineRenderer = new ShapeRenderer();
	public final Renderer renderer;
	public final BulletPhysics physics;
	private btCollisionWorld world = null;
	
	public BulletDebugDrawer(final BulletPhysics physics, final Renderer renderer, final int mode) {
		this.physics = physics;
		this.renderer = renderer;
		this.debugMode = mode;
	}
	
	@Override
	public void dispose() {
		world = null;
		lineRenderer.dispose();
	}

	@Override
	public boolean process(float delta) {
		if (debugMode > 0 && renderer != null && renderer.camera != null && world != null) {
			lineRenderer.setProjectionMatrix(renderer.camera.combined);
			begin();
			physics.dynamicsWorld.debugDrawWorld();
			end();
		}
		return false;
	}

	@Override
	public void process(float delta, Entity e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void process() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean init() {
		if (physics != null)
			world = physics.dynamicsWorld;
		if (world != null)
			world.setDebugDrawer(this);
		return false;
	}

	@Override
	public void init(Entity e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean reset() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void reset(Entity e) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void drawLine (btVector3 from, btVector3 to, btVector3 color) {
		lineRenderer.setColor(color.getX(), color.getY(), color.getZ(), 1f);
		lineRenderer.line(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());
	}

	@Override
	public void drawContactPoint (btVector3 PointOnB, btVector3 normalOnB, float distance, int lifeTime, btVector3 color) {
	}

	@Override
	public void reportErrorWarning (String warningString) {
	}
	
	@Override
	public void draw3dText (btVector3 location, String textString) {
	}
	
	@Override
	public void setDebugMode (int debugMode) {
		this.debugMode = debugMode;
	}

	@Override
	public int getDebugMode () {
		return debugMode;
	}
	
	public void begin() {
		lineRenderer.begin(ShapeType.Line);
	}
	
	public void end() {
		lineRenderer.end();
	}
}