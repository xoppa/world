package com.xoppa.android.world.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g3d.model.Model;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.PerformanceCounter;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.World;
import com.xoppa.android.world.attributes.Light;
import com.xoppa.android.world.attributes.LocalTransform;
import com.xoppa.android.world.attributes.Renderable;
import com.xoppa.android.world.attributes.WorldTransform;

public class Renderer implements World.ISystem {
	public interface RenderListener {
		void beforeRender();
	}
	public interface BatchRenderer {
		void render(final Array<RenderBatch.RenderInstance> instances, final Array<LightBatch.LightInstance> lights, final Camera camera);
	}
	
	public final static Vector3 vec = new Vector3();
	
	public int version = 10;
	public float[] tmp = new float[16];
	public Camera camera;
	public boolean enabled = true;
	public final RenderBatch renderBatch = new RenderBatch();
	public final LightBatch lightBatch = new LightBatch();
	public BatchRenderer renderer = null;
	public RenderListener listener = null;
	private PerformanceCounter processCounter = null;
	private PerformanceCounter renderCounter = null;
	//mean.addValue((System.nanoTime() - start) / 1000000000.0f);
	/**
	 * When a renderable defines a distance, it's only shown when the distance between the object and the camera is equal or below
	 * that value. If it has a negative distance it's always shown (of course respecting the camera's frustum). However, when it
	 * doesn't define a distance (distance == 0), this (FAR) value is used.
	 */
	public final static float FAR = 300.0f;
	
	public Renderer(final RenderListener listener, PerformanceCounter processCounter, PerformanceCounter renderCounter) {
		this.listener = listener;
		this.processCounter = processCounter;
		this.renderCounter = renderCounter;
	}
	
	public Renderer(PerformanceCounter processCounter, PerformanceCounter renderCounter) {
		this(null, processCounter, renderCounter);
	}
	
	@Override
	public void dispose() {
	}
		
	@Override
	public boolean process(float delta) {
		if (!enabled || renderer == null) return false;
		renderBatch.clear();
		lightBatch.clear();
		camera.update();
		if (listener != null)
			listener.beforeRender();
		return true;
	}
	
	@Override
	public void process(final float delta, final Entity e) {
		if (processCounter != null)
			processCounter.start();
		Matrix4 worldTransform = null;
		Matrix4 localTransform = null;
		Light light = null;
		Renderable renderable = null;
		float distance = 0;
		float absDist = 0;
		for (int i = 0; i < e.attributes.size; i++) {
			final Entity.IAttribute attribute = e.attributes.get(i);
			final int type = attribute.type();
			switch (type) {
			case WorldTransform.TYPE:
				worldTransform = ((WorldTransform)attribute).current;
				worldTransform.getTranslation(vec);
				absDist = Vector3.tmp.set(vec.x - camera.position.x, vec.y - camera.position.y, vec.z - camera.position.z).len();
				if (Vector3.tmp.div(distance).dot(camera.direction) < 0) 
					distance = -absDist;
				else
					distance = absDist;
				break;
			case Renderable.TYPE:
				renderable = (Renderable)attribute;
				break;
			case LocalTransform.TYPE:
				localTransform = ((LocalTransform)attribute).current;
				break;
			case Light.TYPE:
				light = (Light)attribute;
				break;
			}
		}
		
		if (light != null) {
			lightBatch.add(light, worldTransform == null ? null : worldTransform, (int)distance);
		}
		if ((worldTransform != null) && (renderable != null) && (renderable.visible) && (renderable.models != null)) {
			final int maxdist = renderable.models[renderable.models.length-1].maxDistance;
			if ((renderable.radius <= 0) || camera.frustum.sphereInFrustum(vec, renderable.radius)) {
				if (maxdist < 0 || absDist <= (maxdist == 0 ? FAR : maxdist))
					renderBatch.add(renderable, worldTransform, localTransform == null ? null : localTransform, (int)distance);
			}
		}
		if (processCounter != null)
			processCounter.stop();
	}
	
	@Override
	public void process() {
		if (renderCounter != null)
			renderCounter.start();
		renderBatch.sort();
		renderer.render(renderBatch.instances, lightBatch.instances, camera);
		if (renderCounter != null)
			renderCounter.stop();	
	}

	@Override
	public boolean init() {
		return false;
	}

	@Override
	public void init(final Entity e) {	}

	@Override
	public boolean reset() {
		return false;
	}

	@Override
	public void reset(final Entity e) {	}
}
