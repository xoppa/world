package com.xoppa.android.loaders.scene;

import java.io.IOException;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.model.Model;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.math.Matrix4;
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
import com.badlogic.gdx.physics.bullet.btConvexHullShape;
import com.badlogic.gdx.physics.bullet.btCylinderShape;
import com.badlogic.gdx.physics.bullet.btCylinderShapeX;
import com.badlogic.gdx.physics.bullet.btCylinderShapeZ;
import com.badlogic.gdx.physics.bullet.btIndexedMesh;
import com.badlogic.gdx.physics.bullet.btShapeHull;
import com.badlogic.gdx.physics.bullet.btSphereShape;
import com.badlogic.gdx.physics.bullet.btStridingMeshInterface;
import com.badlogic.gdx.physics.bullet.btTransform;
import com.badlogic.gdx.physics.bullet.btTriangleIndexVertexArray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.XmlReader;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.Entity.ConstructInfo;
import com.xoppa.android.world.attributes.BulletBody;
import com.xoppa.android.world.attributes.Renderable;
import com.xoppa.android.world.attributes.WorldTransform;

public class EntityLoader extends SynchronousAssetLoader<Entity.ConstructInfo, AssetLoaderParameters<Entity.ConstructInfo>> {
	private final static ObjectMap<String, Class<?>> dependencies = new ObjectMap<String, Class<?>>(3);
	
	public EntityLoader(FileHandleResolver resolver) {
		super(resolver);
	}

	@Override
	public ConstructInfo load(AssetManager assetManager, String fileName, AssetLoaderParameters<ConstructInfo> parameter) {
		return load(resolve(fileName), assetManager);
	}

	@Override
	public Array<AssetDescriptor> getDependencies(String fileName, AssetLoaderParameters<ConstructInfo> parameter) {
		dependencies.clear();
		load(resolve(fileName), dependencies);
		if (dependencies.size == 0) return null;
		final Array<AssetDescriptor> r = new Array<AssetDescriptor> ();
		for (ObjectMap.Entry<String, Class<?>> entry : dependencies.entries())
			r.add(new AssetDescriptor(entry.key, entry.value));
		return r;
	}
	
	public static Entity.ConstructInfo load(FileHandle file, ObjectMap<String, Class<?>> dependencies) {
		return load(null, file, dependencies);
	}
	
	public static Entity.ConstructInfo load(FileHandle file, AssetManager assets) {
		return load(assets, file, null);
	}
	
	protected static Entity.ConstructInfo load(AssetManager assets, FileHandle file, ObjectMap<String, Class<?>> dependencies) {
		if (assets == null && dependencies == null)
			throw new IllegalArgumentException("Need either an asset manager or dependencies");
		try {
			XmlReader reader = new XmlReader();
			XmlReader.Element root = reader.parse(file);
			final Entity.ConstructInfo result = (dependencies != null) ? null : new Entity.ConstructInfo();
			final int n = root.getChildCount();
			for (int i = 0; i < n; i++) {
				final XmlReader.Element child = root.getChild(i);
				if (child.getName().compareTo("renderable")==0) {
					Array<XmlReader.Element> models = child.getChildrenByName("model");
					models.insert(0, child);
					if (dependencies != null) {
						for (int j = 0; j < models.size; j++) {
							final String model = models.get(j).getAttribute("model", null);
							if (model != null)
								dependencies.put(file.parent().child(model).path(), StillModel.class);
						}
					} else {
						Array<Renderable.RenderObject> objs = new Array<Renderable.RenderObject>(1);
						for (int j = 0; j < models.size; j++) {
							final String model = models.get(j).getAttribute("model", null);
							if (model != null) {
								Renderable.RenderObject obj = new Renderable.RenderObject(assets.get(file.parent().child(model).path(), StillModel.class));
								obj.enableLighting = child.getBoolean("lighting", obj.enableLighting);
								obj.maxDistance = child.getInt("distance", obj.maxDistance);
								objs.add(obj);
							}
						}
						if (objs.size > 0) {
							Renderable.RenderObject[] rro = new Renderable.RenderObject[objs.size];
							for (int j = 0; j < rro.length; j++)
								rro[j] = objs.get(j);
							Renderable.ConstructInfo rc = new Renderable.ConstructInfo(rro);
							rc.priority = child.getInt("priority", rc.priority);
							result.attributes.add(rc);
						}
					}
					/* final String model = file.parent().child(child.getAttribute("model")).path();
					if (dependencies != null) dependencies.put(model, StillModel.class);
					else {
						Renderable.RenderObject obj = new Renderable.RenderObject(assets.get(model, StillModel.class));
						obj.enableLighting = child.getBoolean("lighting", obj.enableLighting);
						obj.maxDistance = child.getInt("distance", obj.maxDistance);
						Renderable.ConstructInfo rc = new Renderable.ConstructInfo(new Renderable.RenderObject[] {obj});
						rc.priority = child.getInt("priority", rc.priority);
						result.attributes.add(rc);
					} */
				}
				else if (child.getName().compareTo("worldtransform")==0) {
					if (dependencies == null) result.attributes.add(new WorldTransform.ConstructInfo());
				}
				else if (child.getName().compareTo("body")==0) {
					XmlReader.Element shapeEl = child.getChildByName("shape");
					if (shapeEl == null) shapeEl = child;
					final String model = file.parent().child(shapeEl.getAttribute("model")).path();
					if (dependencies != null) dependencies.put(model, StillModel.class);
					else {
						final Model mdl = assets.get(model, StillModel.class);
						final float mass = child.getFloat("mass", 0);
						final btCollisionShape shape = loadShape(shapeEl, mdl); 

						BulletBody.ConstructInfo bbci = new BulletBody.ConstructInfo(mass, shape);
						try { bbci.body.setM_friction(child.getFloat("friction")); } catch (Exception e) {}
						try { bbci.body.setM_restitution(child.getFloat("restitution")); } catch (Exception e) {}
						try { bbci.body.setM_angularDamping(child.getFloat("angularDamping")); } catch (Exception e) {}
						try { bbci.body.setM_linearDamping(child.getFloat("linearDamping")); } catch (Exception e) {}
						
						bbci.extended = child.getBoolean("extended", false);
							
						result.attributes.add(bbci);
					}
				}
			}
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected static btCollisionShape loadShape(XmlReader.Element element, final Model model) {
		final String shape = element.getAttribute("shape", element.getName());
		if (shape.compareTo("compound")==0) return loadCompoundShape(element, model);
		if (shape.compareTo("box")==0) return loadBoxShape(element, model);
		if (shape.compareTo("cylinder")==0) return loadCylinderShape(element, model);
		if (shape.compareTo("cone")==0) return loadConeShape(element, model);
		if (shape.compareTo("sphere")==0) return loadSphereShape(element, model);
		if (shape.compareTo("mesh")==0) return loadMeshShape(element, model);
		if (shape.compareTo("convexhull")==0) return loadConvexHullShape(element, model);
		// capsule
		// multisphere
		return null;
	}

	public final static BoundingBox bb = new BoundingBox();
	public final static Matrix4 tmpM4 = new Matrix4();
	protected static btCollisionShape loadCompoundShape(XmlReader.Element element, final Model model) {
		btMyCompoundShape result = new btMyCompoundShape(false);
		final int n = element.getChildCount();
		model.getBoundingBox(bb);
		final Vector3 center = model == null ? Vector3.Zero : bb.getCenter();
		for (int i = 0; i < n; i++) {
			final XmlReader.Element child = element.getChild(i);
			btCollisionShape add = loadShape(child, model);
			if (add != null) {
				tmpM4.setToTranslation(child.getFloat("x", center.x), child.getFloat("y", center.y), child.getFloat("z", center.z));
				result.addChildShape(tmpM4, add);
			}
		}
		result.createAabbTreeFromChildren();
		return result;
	}
		
	public static btCollisionShape loadBoxShape(XmlReader.Element element, final Model model) {
		float w = 0, h = 0, d = 0;
		if (model != null) {
			model.getBoundingBox(bb);
			Vector3 dim = bb.getDimensions();
			w = dim.x;
			h = dim.y;
			d = dim.z;
		}
		return new btBoxShape(Vector3.tmp.set(element.getFloat("width", w) * 0.5f, element.getFloat("height", h) * 0.5f, element.getFloat("depth", d) * 0.5f));
	}
	
	public static btCollisionShape loadCylinderShape(XmlReader.Element element, final Model model) {
		float w = 0, h = 0, d = 0;
		final String orientation = element.getAttribute("orientation", "y");
		if (model != null) {
			model.getBoundingBox(bb);
			Vector3 dim = bb.getDimensions();
			w = dim.x;
			h = dim.y;
			d = dim.z;
		}
		Vector3 tmp = Vector3.tmp.set(element.getFloat("width", w) * 0.5f, element.getFloat("height", h) * 0.5f, element.getFloat("depth", d) * 0.5f);
		if (orientation.compareTo("x")==0)
			return  new btCylinderShapeX(tmp);
		else if (orientation.compareTo("z")==0)
			return new btCylinderShapeZ(tmp);
		else 
			return new btCylinderShape(tmp);
	}
	
	public static btCollisionShape loadConeShape(XmlReader.Element element, final Model model) {
		float w = 0, h = 0, d = 0;
		final String orientation = element.getAttribute("orientation", "y");
		if (model != null) {
			model.getBoundingBox(bb);
			Vector3 dim = bb.getDimensions();
			w = dim.x;
			h = dim.y;
			d = dim.z;
		}
		if (orientation.compareTo("x")==0)
			return new btConeShapeX((h>d?h:d)*0.5f,w);
		else if (orientation.compareTo("z")==0)
			return new btConeShapeZ((w>h?w:h)*0.5f,d);
		else 
			return new btConeShape((w>d?w:d)*0.5f,h);
	}
	
	public static btCollisionShape loadSphereShape(XmlReader.Element element, final Model model) {
		float r = 0;
		if (model != null) {
			model.getBoundingBox(bb);
			Vector3 dim = bb.getDimensions();
			r = dim.x;
			if (dim.y > r) r = dim.y;
			if (dim.z > r) r = dim.z;
			r *= 0.5f;
		}
		return new btSphereShape(element.getFloat("radius", r));
	}
	
	public static btCollisionShape loadMeshShape(XmlReader.Element element, final Model model) {
		if (model == null) 
			return null;
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
		return new btMyBvhTriangleMeshShape(meshInterface,true);
	}
	
	public static btCollisionShape loadConvexHullShape(XmlReader.Element element, final Model model) {
		if (model == null)
			return null;
		final boolean reduce = element.getBoolean("reduce", false);
		final Mesh mesh = model.getSubMeshes()[0].getMesh();
		final btConvexHullShape shape = new btConvexHullShape(mesh.getVerticesBuffer(), mesh.getNumVertices(), mesh.getVertexSize());
		if (!reduce)
			return shape;
		final btShapeHull hull = new btShapeHull(shape);
		hull.buildHull(shape.getMargin());
		final btConvexHullShape result = new btConvexHullShape(hull);
		shape.delete();
		return result;
	}
	
	// TODO Classes below this line should be eliminated
	
	static class btMyCompoundShape extends btCompoundShape {
		public btMyCompoundShape(boolean enableDynamicAabbTree) {
			super(enableDynamicAabbTree);
		}

		public final Array<btCollisionShape> children = new Array<btCollisionShape>();
		
		@Override
		public void addChildShape(Matrix4 localTransform, btCollisionShape shape) {
			super.addChildShape(localTransform, shape);
			children.add(shape);
		}
		
		@Override
		public synchronized void delete() {
			for (int i = 0; i < children.size; i++)
				children.get(i).delete();
			children.clear();
			super.delete();
		}
	}
	
	// Need to keep reference to the meshes to avoid memory being freed to early.
	static class btMyTriangleIndexVertexArray extends btTriangleIndexVertexArray {
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
	static class btMyBvhTriangleMeshShape extends btBvhTriangleMeshShape {
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
}
