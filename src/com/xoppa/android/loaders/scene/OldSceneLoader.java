package com.xoppa.android.loaders.scene;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.loaders.ogre.OgreXmlLoader;
import com.badlogic.gdx.graphics.g3d.model.Model;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.PHY_ScalarType;
import com.badlogic.gdx.physics.bullet.btBoxShape;
import com.badlogic.gdx.physics.bullet.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.btCollisionShape;
import com.badlogic.gdx.physics.bullet.btCompoundShape;
import com.badlogic.gdx.physics.bullet.btConeShape;
import com.badlogic.gdx.physics.bullet.btConeShapeX;
import com.badlogic.gdx.physics.bullet.btConeShapeZ;
import com.badlogic.gdx.physics.bullet.btCylinderShape;
import com.badlogic.gdx.physics.bullet.btCylinderShapeX;
import com.badlogic.gdx.physics.bullet.btCylinderShapeZ;
import com.badlogic.gdx.physics.bullet.btIndexedMesh;
import com.badlogic.gdx.physics.bullet.btSphereShape;
import com.badlogic.gdx.physics.bullet.btStridingMeshInterface;
import com.badlogic.gdx.physics.bullet.btTransform;
import com.badlogic.gdx.physics.bullet.btTriangleIndexVertexArray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;
import com.xoppa.android.loaders.model.g3d.G3dxLoader;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.Group;
import com.xoppa.android.world.MaterialManager;
import com.xoppa.android.world.World;
import com.xoppa.android.world.Entity.ConstructInfo;
import com.xoppa.android.world.Loader.IAttributeConstructInfoLoader;
import com.xoppa.android.world.attributes.BulletBody;
import com.xoppa.android.world.attributes.Renderable;
import com.xoppa.android.world.attributes.WorldTransform;

public class OldSceneLoader {
	public final MaterialManager materials;
	//public final OgreXmlLoader ogreLoader;
	
	public World world;
	public FileHandle base;
	
	public OldSceneLoader() {
		materials = new MaterialManager();
		//ogreLoader = new OgreXmlLoader();
		//ogreLoader.materials = materials;
	}
	
	public void load(final FileHandle file, final World world, final Group group) {
		this.world = world;
		this.base = file.parent();
		try {
			XmlReader reader = new XmlReader();
			XmlReader.Element scene = reader.parse(file);
			final Array<XmlReader.Element> nodes = scene.getChildrenByNameRecursively("node");
			if (nodes != null)
				for (XmlReader.Element node : nodes)
					group.add(getEntity(node));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Quaternion quat = new Quaternion();
	protected Entity getEntity(XmlReader.Element node) {
		final String name = node.getAttribute("name", null);
		XmlReader.Element el = node.getChildByName("entity");
		if (el == null) return null;
		final String m = el.getAttribute("meshFile", null);
		if (m == null) return null;
		final String type = getType(m);
		if (type == null) return null;
		final Entity result = world.createEntity(name, type);
		if ((el = node.getChildByName("position")) != null)
			result.get(WorldTransform.class).move(el.getFloat("x", 0f), el.getFloat("y", 0f), el.getFloat("z", 0f));
		if ((el = node.getChildByName("rotation")) != null)
			result.get(WorldTransform.class).current.rotate(quat.set(el.getFloat("qx", 0f), el.getFloat("qy", 0f), el.getFloat("qz", 0f), el.getFloat("qw", 0f)));
		return result;
	}
	
	protected String getType(final String name) {
		final String result = name.split("\\.")[0];
		if (world.constructors.containsKey(result))
			return result;
		Gdx.app.log("SceneLoader", "Creating constructinfo: "+result+" ("+name+")");
		final StillModel model = G3dxLoader.loadStillModel(base.child(name.replace(".mesh", ".g3d"))); //ogreLoader.load(base.child(name+".xml"));
		world.modelManager.add(name, model);
		Entity.ConstructInfo ec = new Entity.ConstructInfo(new WorldTransform.ConstructInfo(), new Renderable.ConstructInfo(new Renderable.RenderObject[] {new Renderable.RenderObject(model)}));
		final BulletBody.ConstructInfo body = getBody(result, model);
		if (body != null)
			ec.attributes.add(body);
		world.constructors.put(result, ec);
		return result;
	}
	
	protected BulletBody.ConstructInfo getBody(final String name, final Model model) {
		final FileHandle file = base.child(name+".body");
		if (!file.exists())
			return new BulletBody.ConstructInfo(0, model, BulletBody.ConstructInfo.BODY_SHAPE_MESH);
		try {
			XmlReader reader = new XmlReader();
			XmlReader.Element scene = reader.parse(file);
			XmlReader.Element node = (scene.getName().compareTo("body")==0) ? scene : scene.getChildByNameRecursive("body");
			
			final float mass = node.getFloat("mass", 0);
			final btCollisionShape shape = loadShape(node, model); 

			BulletBody.ConstructInfo result = new BulletBody.ConstructInfo(mass, shape);
			try { result.body.setM_friction(node.getFloat("friction")); } catch (Exception e) {}
			try { result.body.setM_restitution(node.getFloat("restitution")); } catch (Exception e) {}
			try { result.body.setM_angularDamping(node.getFloat("angularDamping")); } catch (Exception e) {}
			try { result.body.setM_linearDamping(node.getFloat("linearDamping")); } catch (Exception e) {}
			return result;		
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected btCollisionShape loadShape(XmlReader.Element element, final Model model) {
		final String shape = element.getAttribute("shape", element.getName());
		if (shape.compareTo("compound")==0) return loadCompoundShape(element, model);
		if (shape.compareTo("box")==0) return loadBoxShape(element, model);
		if (shape.compareTo("cylinder")==0) return loadCylinderShape(element, model);
		if (shape.compareTo("cone")==0) return loadConeShape(element, model);
		if (shape.compareTo("sphere")==0) return loadSphereShape(element, model);
		if (shape.compareTo("mesh")==0) return loadMeshShape(element, model);
		// capsule
		// multisphere
		if (shape.compareTo("mesh")==0) return loadMeshShape(element, model);
		return null;
	}
		
	public final static Matrix4 tmpM4 = new Matrix4();
	protected btCollisionShape loadCompoundShape(XmlReader.Element element, final Model model) {
		btCompoundShape result = new btCompoundShape(false);
		final int n = element.getChildCount();
		for (int i = 0; i < n; i++) {
			final XmlReader.Element child = element.getChild(i);
			btCollisionShape add = loadShape(child, model);
			if (add != null) {
				tmpM4.setToTranslation(child.getFloat("x", 0), child.getFloat("y", 0), child.getFloat("z", 0));
				result.addChildShape(tmpM4, add);
			}
		}
		result.createAabbTreeFromChildren();
		world.shapes.add(result);
		return result;
	}
		
	public final BoundingBox bb = new BoundingBox();
	public btCollisionShape loadBoxShape(XmlReader.Element element, final Model model) {
		float w = 0, h = 0, d = 0;
		final String modelName = element.getAttribute("model", null);
		if (modelName != null) {
			//final Model model = world.modelManager.get(modelName);
			if (model != null) {
				model.getBoundingBox(bb);
				Vector3 dim = bb.getDimensions();
				w = dim.x;
				h = dim.y;
				d = dim.z;
			}
		}
		btCollisionShape result = new btBoxShape(Vector3.tmp.set(element.getFloat("width", w) * 0.5f, element.getFloat("height", h) * 0.5f, element.getFloat("depth", d) * 0.5f));
		world.shapes.add(result);
		return result;
	}
	
	public btCollisionShape loadCylinderShape(XmlReader.Element element, final Model model) {
		float w = 0, h = 0, d = 0;
		final String orientation = element.getAttribute("orientation", "y");
		final String modelName = element.getAttribute("model", null);
		if (modelName != null) {
			//final Model model = world.modelManager.get(modelName);
			if (model != null) {
				model.getBoundingBox(bb);
				Vector3 dim = bb.getDimensions();
				w = dim.x;
				h = dim.y;
				d = dim.z;
			}
		}
		Vector3 tmp = Vector3.tmp.set(element.getFloat("width", w) * 0.5f, element.getFloat("height", h) * 0.5f, element.getFloat("depth", d) * 0.5f);
		btCollisionShape result;
		if (orientation.compareTo("x")==0)
			result = new btCylinderShapeX(tmp);
		else if (orientation.compareTo("z")==0)
			result = new btCylinderShapeZ(tmp);
		else result = new btCylinderShape(tmp);
		world.shapes.add(result);
		return result;
	}
	
	public btCollisionShape loadConeShape(XmlReader.Element element, final Model model) {
		float w = 0, h = 0, d = 0;
		final String orientation = element.getAttribute("orientation", "y");
		final String modelName = element.getAttribute("model", null);
		if (modelName != null) {
			//final Model model = world.modelManager.get(modelName);
			if (model != null) {
				model.getBoundingBox(bb);
				Vector3 dim = bb.getDimensions();
				w = dim.x;
				h = dim.y;
				d = dim.z;
			}
		}
		btCollisionShape result;
		if (orientation.compareTo("x")==0)
			result = new btConeShapeX((h>d?h:d)*0.5f,w);
		else if (orientation.compareTo("z")==0)
			result = new btConeShapeZ((w>h?w:h)*0.5f,d);
		else result = new btConeShape((w>d?w:d)*0.5f,h);
		world.shapes.add(result);
		return result;
	}
	
	public btCollisionShape loadSphereShape(XmlReader.Element element, final Model model) {
		float r = 0;
		final String modelName = element.getAttribute("model", null);
		if (modelName != null) {
			//final Model model = world.modelManager.get(modelName);
			if (model != null) {
				model.getBoundingBox(bb);
				Vector3 dim = bb.getDimensions();
				r = dim.x;
				if (dim.y > r) r = dim.y;
				if (dim.z > r) r = dim.z;
				r *= 0.5f;
			}
		}
		btCollisionShape result = new btSphereShape(element.getFloat("radius", r));
		world.shapes.add(result);
		return result;
	}
	
	// Need to keep reference to the meshes to avoid memory being freed to early.
	class btMyTriangleIndexVertexArray extends btTriangleIndexVertexArray {
		Array<btIndexedMesh> meshes = new Array<btIndexedMesh>();
		
		@Override
		public void addIndexedMesh(btIndexedMesh mesh, int indexType) {
			super.addIndexedMesh(mesh, indexType);
			meshes.add(mesh);
		}
		
		@Override
		public synchronized void delete() {
			super.delete();
			meshes.clear();
		}
	}
	
	// Need to keep reference to meshInterface to avoid memory being freed to early.
	class btMyBvhTriangleMeshShape extends btBvhTriangleMeshShape {
		btStridingMeshInterface meshInterface;
		public btMyBvhTriangleMeshShape(btStridingMeshInterface meshInterface, boolean useQuantizedAabbCompression) {
			super(meshInterface, useQuantizedAabbCompression);
			this.meshInterface = meshInterface;
		}
		
		@Override
		public synchronized void delete() {
			super.delete();
			meshInterface = null;
		}
	}
	
	public btCollisionShape loadMeshShape(XmlReader.Element element, final Model model) {
		final String modelName = element.getAttribute("model", null);
		if (modelName != null) {
			//final Model model = world.modelManager.get(modelName);
			if (model != null) {
				Mesh mesh = model.getSubMeshes()[0].getMesh();
				btIndexedMesh indexedMesh = new btIndexedMesh();
				indexedMesh.setM_indexType(PHY_ScalarType.PHY_SHORT);
				indexedMesh.setM_numTriangles(mesh.getNumIndices()/3);
				indexedMesh.setM_numVertices(mesh.getNumVertices());
				indexedMesh.setM_triangleIndexStride(6);
				indexedMesh.setM_vertexStride(mesh.getVertexSize());
				indexedMesh.setM_vertexType(PHY_ScalarType.PHY_FLOAT);
				indexedMesh.setTriangleIndexBase(mesh.getIndicesBuffer());
				indexedMesh.setVertexBase(mesh.getVerticesBuffer());
				btTriangleIndexVertexArray meshInterface = new btMyTriangleIndexVertexArray();
				meshInterface.addIndexedMesh(indexedMesh, PHY_ScalarType.PHY_SHORT);
				btBvhTriangleMeshShape result = new btMyBvhTriangleMeshShape(meshInterface,true);
				world.shapes.add(result);
				return result;
			}
		}
		return null;
	}
}
