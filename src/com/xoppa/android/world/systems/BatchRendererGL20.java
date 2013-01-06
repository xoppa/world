package com.xoppa.android.world.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g3d.lights.PointLight;
import com.badlogic.gdx.graphics.g3d.materials.GpuSkinningAttribute;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.materials.MaterialAttribute;
import com.badlogic.gdx.graphics.g3d.materials.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.SubMesh;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.xoppa.android.world.ChaseCamera;
import com.xoppa.android.world.ShaderManager;
import com.xoppa.android.world.systems.RenderBatch.RenderInstance;
import com.xoppa.android.world.systems.Renderer.BatchRenderer;

public class BatchRendererGL20 implements BatchRenderer {
	public final ShaderManager shaderManager = new ShaderManager();
	public final static Matrix4 modelMatrix = new Matrix4();
	public final static Matrix3 normalMatrix = new Matrix3();
	
	public final static int U_NORMALMATRIX = ShaderProgram.getGlobalUniformID("u_normalMatrix");
	public final static int U_MODELMATRIX = ShaderProgram.getGlobalUniformID("u_modelMatrix");
	public final static int U_PROJTRANS = ShaderProgram.getGlobalUniformID("u_projTrans");
	
	public final static float lightMapRadius = 40;
	public FrameBuffer shadowMap;
	private Camera lightCam;
	private Vector3 lightPos;
	private Vector3 lightDir;
	private ShaderProgram shadowGenShader;
	private ShaderProgram currentShader;
	public boolean renderShadowMap = false;
	public int texUnitOffset = 0;
	public int currentTexUnitOffset = 0;
	private final TextureAttribute lastTexture[] = new TextureAttribute[TextureAttribute.MAX_TEXTURE_UNITS];
	
	public BatchRendererGL20() {
		final int width = 512; //Gdx.graphics.getWidth();
		final int height = 512; //Gdx.graphics.getHeight();
		shadowMap = new FrameBuffer(Format.RGBA8888, width, height, true);
		boolean ortho = true;
		if (ortho) {
			if (width > height)
				lightCam = new OrthographicCamera(lightMapRadius*(float)width/(float)height, lightMapRadius);
			else
				lightCam = new OrthographicCamera(lightMapRadius, lightMapRadius*(float)height/(float)width);
		} else {
			if (width > height)
				lightCam = new PerspectiveCamera(67f, lightMapRadius*(float)width/(float)height, lightMapRadius);
			else
				lightCam = new PerspectiveCamera(67f, lightMapRadius, lightMapRadius*(float)height/(float)width);
		}
		lightCam.position.set(lightPos = new Vector3(20, 40, 20));
		lightCam.near = 1f;
		lightCam.far = 300f;
		lightDir = new Vector3(-0.5f, -1f, -0.5f);
		lightDir.nor();
		lightCam.direction.set(lightDir);
		
		shadowGenShader = new ShaderProgram(Gdx.files.internal("data/shaders/shadowgen.vertex.glsl").readString(), Gdx.files
				.internal("data/shaders/shadowgen.fragment.glsl").readString());
		if (!shadowGenShader.isCompiled())
			throw new GdxRuntimeException("Couldn't compile shadow gen shader: " + shadowGenShader.getLog());
		
		for (int i = 0; i < 3; i++) {
			positions[3 * i + 0] = -50 + i * 50;
			positions[3 * i + 1] = 20;
			positions[3 * i + 2] = 0;

			colors[3 * i + 0] = i == 0 ? 1 : 0;
			colors[3 * i + 1] = i == 1 ? 1 : 0;
			colors[3 * i + 2] = i == 2 ? 1 : 0;

			intensities[i] = 0f; // 0 = disabled
		}
	}
	
	boolean test = false;
	@Override
	public void render(Array<RenderInstance> instances, final Array<LightBatch.LightInstance> lights, Camera camera) {
		currentTexUnitOffset = texUnitOffset = 0;
		Gdx.gl.glEnable(GL10.GL_DEPTH_TEST);
		Gdx.gl.glDepthFunc(GL10.GL_LEQUAL);
		//Gdx.gl.glDisable(GL10.GL_DITHER);
		//Gdx.gl.glEnable(GL10.GL_CULL_FACE);
		
		// render shadowmap:
		lightCam.position.set(camera.direction).mul(lightMapRadius*0.5f).add(camera.position).add(lightCam.direction.mul(-40));
		lightPos.set(lightCam.position);
		lightCam.direction.mul(-1).nor();
		lightCam.update();
			
		if (!renderShadowMap) shadowMap.begin();
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glDisable(GL20.GL_CULL_FACE);

		Gdx.gl.glDisable(GL10.GL_BLEND);
		//Gdx.gl.glEnable(GL20.GL_CULL_FACE);
		//Gdx.gl.glCullFace(GL20.GL_FRONT);
		shadowGenShader.begin();
		shadowGenShader.setUniformMatrixByGUID(U_PROJTRANS, lightCam.combined);
		shadowGenShader.setUniformfByGUID(DIRLIGHTPOS, lightPos);
		//shadowGenShader.setUniformf("u_lightPos", lightCam.position);

		//shadowGenShader.setUniformf("u_lightPos", lightPos);
		//shadowGenShader.setUniformf("u_invFar", 1/lightCam.far);
		for (int i = 0; i < instances.size; i++) {
			RenderInstance instance = instances.get(i);
			instance.worldTransform.getTranslation(Vector3.tmp);
			if ((instance.renderable.radius > 0.0f) && (!lightCam.frustum.sphereInFrustum(Vector3.tmp, instance.renderable.radius)))
				continue;
			if (!instance.renderable.models[0].enableLighting)
				continue;
			modelMatrix.set(instance.worldTransform);
			if (instance.localTransform != null)
				modelMatrix.mul(instance.localTransform);
			shadowGenShader.setUniformMatrixByGUID(U_MODELMATRIX, modelMatrix, false);
			final SubMesh subMeshes[] = instance.renderable.models[0].model.getSubMeshes();
			for (int j = 0; j < subMeshes.length; j++) {
				final SubMesh subMesh = subMeshes[j];
				subMesh.mesh.render(shadowGenShader, subMesh.primitiveType);
			}
		}
		shadowGenShader.end();
		if (!renderShadowMap) shadowMap.end();

		//Gdx.gl.glEnable(GL10.GL_ALPHA_TEST);
		
		//final float camFar = camera.far; 
		if (!renderShadowMap) 
			render2(instances, camera, true, false);
	}
	
	public void render2(Array<RenderInstance> instances, Camera camera, boolean lighting, boolean debug) {
		Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		
		boolean depthtest = true;
		Gdx.gl.glEnable(GL10.GL_DEPTH_TEST);
		Gdx.gl.glDepthFunc(GL10.GL_LEQUAL);
		Gdx.gl.glEnable(GL10.GL_BLEND);
		Gdx.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		
		boolean camChanged = true;
		for (int i = 0; i < instances.size; i++) {
			RenderInstance instance = instances.get(i);
			if (debug)
				Gdx.app.log("Bla", "Render "+i);

			modelMatrix.set(instance.worldTransform);
			if (instance.localTransform != null)
				modelMatrix.mul(instance.localTransform);
			normalMatrix.set(modelMatrix);
			
			if (instance.renderable.depthtest != depthtest) {
				depthtest = instance.renderable.depthtest;
				if (depthtest) {
					Gdx.gl.glEnable(GL10.GL_DEPTH_TEST);
					Gdx.gl.glDepthFunc(GL10.GL_LEQUAL);
				} else
					Gdx.gl.glDisable(GL10.GL_DEPTH_TEST);
			}

			final SubMesh subMeshes[] = instance.renderable.models[0].model.getSubMeshes();

			boolean matrixChanged = true;
			
			for (int j = 0; j < subMeshes.length; j++) {

				final SubMesh subMesh = subMeshes[j];
				final Material material = subMesh.material;

				// bind new shader if material can't use old one
				final boolean shaderChanged = bindShader(material, lighting ? instance.renderable.models[0].enableLighting : false);

				if (shaderChanged || matrixChanged) {
					currentShader.setUniformMatrixByGUID(U_NORMALMATRIX, normalMatrix, false);
					currentShader.setUniformMatrixByGUID(U_MODELMATRIX, modelMatrix, false);
					matrixChanged = false;
				}
				if (shaderChanged || camChanged) {
					currentShader.setUniformMatrixByGUID(U_PROJECTIONVIEWMATRIX, camera.combined);
					currentShader.setUniformfByGUID(CAMPOS, camera.position.x, camera.position.y, camera.position.z, 1.2f / camera.far);
					currentShader.setUniformfByGUID(CAMDIR, camera.direction.x, camera.direction.y, camera.direction.z);
					camChanged = false;
				}
				
				int texunit = currentTexUnitOffset;

				for (MaterialAttribute atrib : material) {

					// special case for textures. really important to batch these
					if (atrib instanceof TextureAttribute) {
						final TextureAttribute texAtrib = (TextureAttribute)atrib;
						texAtrib.unit = ++texunit;
						if (!texAtrib.texturePortionEquals(lastTexture[texAtrib.unit])) {
							lastTexture[texAtrib.unit] = texAtrib;
							texAtrib.bind(currentShader);
						} else {
							// need to be done, shader textureAtribute name could be changed.
							currentShader.setUniformiByGUID(texAtrib.guid, texAtrib.unit);
						}
					} else if (atrib instanceof GpuSkinningAttribute) {
						GpuSkinningAttribute gpuAttrib = (GpuSkinningAttribute) atrib;
						gpuAttrib.setModelMatrix(modelMatrix);
						gpuAttrib.bind(currentShader);
					} else {
						atrib.bind(currentShader);
					}
				}

				// finally render current submesh
				subMesh.getMesh().render(currentShader, subMesh.primitiveType);
			}
		}
		if (currentShader != null) {
			currentShader.end();
			currentShader = null;
		}
		//if (camera.far != camFar)
			//camera.far = camFar;
	}
	public static int passes = 0;
	public final int maxLightsPerModel = 3;
	final private float[] positions = new float[3 * maxLightsPerModel];
	final private float[] colors = new float[3 * maxLightsPerModel];
	final private float[] intensities = new float[maxLightsPerModel];

	public final static int AMBIENT = ShaderProgram.getGlobalUniformID("ambient");
	public final static int DIRLIGHTDIR = ShaderProgram.getGlobalUniformID("dirLightDir");
	public final static int DIRLIGHTCOL = ShaderProgram.getGlobalUniformID("dirLightCol");
	public final static int DIRLIGHTPOS = ShaderProgram.getGlobalUniformID("dirLightPos");
	public final static int U_INVFAR = ShaderProgram.getGlobalUniformID("u_invFar");
	public final static int S_SHADOWMAP = ShaderProgram.getGlobalUniformID("s_shadowMap");
	public final static int U_LIGHTPROJTRANS = ShaderProgram.getGlobalUniformID("u_lightProjTrans");
	public final static int U_LIGHTPOS = ShaderProgram.getGlobalUniformID("u_lightPos");
	public final static int U_LIGHTCOL = ShaderProgram.getGlobalUniformID("u_lightCol");
	public final static int U_LIGHTINT = ShaderProgram.getGlobalUniformID("u_lightInt");
	public final static int U_PROJECTIONVIEWMATRIX = ShaderProgram.getGlobalUniformID("u_projectionViewMatrix");
	public final static int CAMPOS = ShaderProgram.getGlobalUniformID("camPos");
	public final static int CAMDIR = ShaderProgram.getGlobalUniformID("camDir");
	/** @param material
	 * @return true if new shader was binded */
	private boolean bindShader (Material material, boolean lighting) {
		ShaderProgram shader = material.getShader();
		if (shader == null) {
			shader = shaderManager.get(material, lighting);
			material.setShader(shader);
		}
		if (shader == currentShader) return false;
		if (currentShader != null) {
			//currentShader.end();
			currentTexUnitOffset = texUnitOffset;
		}
		
		currentShader = shader;
		currentShader.begin();
		
		currentShader.setUniformfByGUID(AMBIENT, 0.4f, 0.4f, 0.4f);
		currentShader.setUniformfByGUID(DIRLIGHTDIR, lightDir);
		currentShader.setUniformfByGUID(DIRLIGHTCOL, 0.7f, 0.7f, 0.7f);
		currentShader.setUniformfByGUID(DIRLIGHTPOS, lightPos);
		//currentShader.setUniformfByGUID(U_INVFAR, 1/cam.far);
		shadowMap.getColorBufferTexture().bind(currentTexUnitOffset);
		currentShader.setUniformiByGUID(S_SHADOWMAP, currentTexUnitOffset);
		currentTexUnitOffset++;
		currentShader.setUniformMatrixByGUID(U_LIGHTPROJTRANS, lightCam.combined);
		currentShader.setUniformfByGUID(U_LIGHTPOS, lightCam.position);
		//currentShader.setUniform3fvByGUID(U_LIGHTPOS, positions, 0, maxLightsPerModel * 3);
		//currentShader.setUniform3fvByGUID(U_LIGHTCOL, colors, 0, maxLightsPerModel * 3);
		//currentShader.setUniform1fvByGUID(U_LIGHTINT, intensities, 0, maxLightsPerModel);
		//lightManager.applyGlobalLights(currentShader);
		//lightManager.applyLights(currentShader);
		return true;
	}
	
}
