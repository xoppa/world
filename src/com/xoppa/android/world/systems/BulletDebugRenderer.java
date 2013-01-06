package com.xoppa.android.world.systems;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.World;
import com.xoppa.android.world.attributes.BulletBody;

public class BulletDebugRenderer implements World.ISystem {
	public boolean enabled = true;
	public ImmediateModeRenderer renderer;
	
	@Override
	public void dispose() {
	}

	@Override
	public boolean process(float delta) {
		return true;
	}

	@Override
	public void process(float delta, Entity e) {

	}

	@Override
	public void process() {
	}

	@Override
	public boolean init() {
		return false;
	}

	@Override
	public void init(Entity e) {
	}

	@Override
	public boolean reset() {
		return false;
	}

	@Override
	public void reset(Entity e) { }
}
