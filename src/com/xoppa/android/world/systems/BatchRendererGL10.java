package com.xoppa.android.world.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.utils.Array;
import com.xoppa.android.world.attributes.LocalTransform;
import com.xoppa.android.world.systems.RenderBatch.RenderInstance;

public class BatchRendererGL10 implements Renderer.BatchRenderer {
	private float[] lightDiffuseColor = {0.6f, 0.6f, 0.6f, 0};
	private float[] lightAmbientColor = {0.6f, 0.6f, 0.6f, 0};
	private float[] lightPosition = {0, 30, 0, 20};
	
	@Override
	public void render(final Array<RenderInstance> instances, final Array<LightBatch.LightInstance> lights, final Camera camera) {
		final GL10 gl = Gdx.gl10;
		gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glClearColor(0.7f, 0.7f, 0.7f, 1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDisable(GL10.GL_LIGHTING);
		gl.glEnable(GL10.GL_COLOR_MATERIAL);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glEnable(GL10.GL_LIGHT0);
		gl.glEnable(GL10.GL_BLEND);
		//gl.glEnable(GL10.GL_CULL_FACE);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, lightDiffuseColor, 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, lightAmbientColor, 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPosition, 0);
		camera.update();
		camera.apply(gl);
		
		for (int i = 0; i < instances.size; i++) {
			final RenderInstance instance = instances.get(i);
			if (instance.renderable.models[0].enableLighting)
				gl.glEnable(GL10.GL_LIGHTING);
			else
				gl.glDisable(GL10.GL_LIGHTING);
			gl.glPushMatrix();
			if (instance.worldTransform != null)
				gl.glMultMatrixf(instance.worldTransform.val, 0);
			if (instance.localTransform != null)
				gl.glMultMatrixf(instance.localTransform.val, 0);
			((StillModel)instance.renderable.models[0].model).render();
			gl.glPopMatrix();
		}
	}

}
