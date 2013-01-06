package com.xoppa.android.world;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.model.Model;
import com.xoppa.android.world.attributes.BulletBody;
import com.xoppa.android.world.attributes.Light;
import com.xoppa.android.world.attributes.LocalTransform;
import com.xoppa.android.world.attributes.Renderable;
import com.xoppa.android.world.attributes.WorldTransform;
import com.xoppa.android.world.attributes.Renderable.RenderObject;
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
import com.badlogic.gdx.physics.bullet.btIndexedMesh;
import com.badlogic.gdx.physics.bullet.btSphereShape;
import com.badlogic.gdx.physics.bullet.btCylinderShape;
import com.badlogic.gdx.physics.bullet.btCylinderShapeX;
import com.badlogic.gdx.physics.bullet.btCylinderShapeZ;
import com.badlogic.gdx.physics.bullet.btStridingMeshInterface;
import com.badlogic.gdx.physics.bullet.btTransform;
import com.badlogic.gdx.physics.bullet.btTriangleIndexVertexArray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entries;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;


public class Loader {
	public interface IEntityLoader {
		void load(XmlReader.Element element, World world);
	}
	
	public interface IAttributeConstructInfoLoader {
		Entity.IAttribute.IConstructInfo load(XmlReader.Element element, World world);
	}
	
	public static abstract class IModifierLoader {
		public abstract void modify(Entity entity, XmlReader.Element element);
		public void modify(Iterable<Entity> entities, XmlReader.Element element) {
			for (Entity e : entities)
				modify(e, element);
		}
	}
	
	public static ObjectMap<String, IEntityLoader> entityLoaders = new ObjectMap<String, IEntityLoader>();
	public static ObjectMap<String, IAttributeConstructInfoLoader> attributeConstructInfoLoaders = new ObjectMap<String, IAttributeConstructInfoLoader>();
	public static ObjectMap<String, IModifierLoader> modifierLoaders = new ObjectMap<String, IModifierLoader>();
	public static ObjectMap<String, XmlReader.Element> templates = new ObjectMap<String, XmlReader.Element>(); 
	
	static {
		entityLoaders.put("model", new IEntityLoader() {
			@Override
			public void load(Element element, World world) {
				//try {
					final ObjLoader objLoader = new ObjLoader(world);
					world.modelManager.add(
							element.getAttribute("name"), 
							objLoader.loadObj(
									Gdx.files.internal(element.getAttribute("source")), 
									element.getBoolean("flipV", false) ));
				/*} catch (Exception e) {
					Gdx.app.log("Loader", "Error on model: "+e.getMessage());
				}*/
			}		
		});
		
		entityLoaders.put("entity", new IEntityLoader() {
			@Override
			public void load(Element element, World world) {
				try {
					Entity.ConstructInfo info = new Entity.ConstructInfo(element.getBoolean("enabled", true));
					for (int i = 0; i < element.getChildCount(); i++) {
						final XmlReader.Element child = element.getChild(i);
						Entity.IAttribute.IConstructInfo attr = attributeConstructInfoLoaders.get(child.getName()).load(child, world);
						if (attr != null)
							info.attributes.add(attr);
						else
							Gdx.app.log("Loader", "attr is null "+child.getName());
					}
					world.constructors.put(element.getAttribute("name"), info);
				} catch (Exception e) {
					Gdx.app.log("Loader", "Error on entity: "+e.getMessage());
				}
			}
		});
		
		entityLoaders.put("group", new IEntityLoader() {
			public Group loadGroup(Element element, World world, Group result) {
				try {
					result.name = element.getAttribute("name");
					final String copy = element.getAttribute("copy", null);
					if (copy != null) {
						Gdx.app.log("Test", "Copy from "+ copy);
						Group grp = world.sub(copy);
						if (grp != null)
							grp.copyTo(null, result);
						else
							Gdx.app.log("Test", "Copy is null");
					}
					result.enabled = element.getBoolean("enabled", true);
					final int n = element.getChildCount();
					for (int i = 0; i < n; i++) {
						final Element el = element.getChild(i);
						final String name = el.getName();
						final IModifierLoader mod = modifierLoaders.get(name);
						if (mod != null) {
							mod.modify(result, el);
						} else if (name.compareTo("group")==0) {
							Group grp = new Group(); 
							result.add(grp);
							loadGroup(el, world, grp);
						} else {
							Entity ent = world.createEntity(el.getAttribute("name", null), name);
							if (ent != null) {
								result.add(ent);
								ent.enabled = el.getBoolean("enabled", ent.enabled);
								WorldTransform tr = ent.get(WorldTransform.class);
								if (tr != null) {
									float x = el.getFloat("x", 0);
									float y = el.getFloat("y", 0);
									float z = el.getFloat("z", 0);
									if ((x != 0)||(y != 0)||(z != 0))
										tr.current.translate(x, y, z);
								}
								final int s = el.getChildCount();
								for (int j = 0; j < s; j++) {
									final Element child = el.getChild(j);
									final IModifierLoader modifier = modifierLoaders.get(child.getName());
									if (modifier != null)
										modifier.modify(ent, el.getChild(j));
								}
							}
						}
					}
					return result;
				} catch (Exception e) {
					return null;
				}
			}
			@Override
			public void load(Element element, World world) {
				Group grp = new Group();
				world.add(grp);
				loadGroup(element, world, grp);
			}
		});
		
		modifierLoaders.put("translate", new IModifierLoader() {
			@Override
			public void modify(Entity entity, Element element) {
				WorldTransform tr = entity.get(WorldTransform.class);
				tr.current.trn(element.getFloat("x", 0), element.getFloat("y", 0), element.getFloat("z", 0));
			}
		});
		
		modifierLoaders.put("scale", new IModifierLoader() {
			@Override
			public void modify(Entity entity, Element element) {
				LocalTransform lt = entity.get(LocalTransform.class);
				if (lt != null)
					lt.current.scale(element.getFloat("x", 0), element.getFloat("y", 0), element.getFloat("z", 0));
				else {
					WorldTransform tr = entity.get(WorldTransform.class);
					if (tr != null)
						tr.current.scale(element.getFloat("x", 0), element.getFloat("y", 0), element.getFloat("z", 0));
				}
			}
		});
		
		modifierLoaders.put("rotate", new IModifierLoader() {
			@Override
			public void modify(Entity entity, Element element) {
				WorldTransform tr = entity.get(WorldTransform.class);
				if (tr != null)
					tr.current.rotate(element.getFloat("x", 0), element.getFloat("y", 0), element.getFloat("z", 0), element.getFloat("angle", 0));
			}
			@Override
			public void modify(Iterable<Entity> entities, XmlReader.Element element) {
				BoundingBox bb = new BoundingBox();
				Renderable.getBounds(entities, bb);
				Vector3 center = bb.getCenter();
				Gdx.app.log("Test", "Center = "+center);
				WorldTransform.rotateAround(entities, 
						element.getFloat("x", 0), element.getFloat("y", 0), element.getFloat("z", 0), element.getFloat("angle", 0), 
						center.x, center.y, center.z);				
			}
		});
		
		modifierLoaders.put("position", new IModifierLoader() {
			@Override
			public void modify(Entity entity, Element element) {
				WorldTransform tr = entity.get(WorldTransform.class);
				if (tr != null) {
					Vector3 center = tr.getPosition(Vector3.tmp);
					tr.current.trn(element.getFloat("x", center.x) - center.x, element.getFloat("y", center.y) - center.y, element.getFloat("z", center.z) - center.z);
				}
			}
			@Override
			public void modify(Iterable<Entity> entities, XmlReader.Element element) {
				BoundingBox bb = new BoundingBox();
				Renderable.getBounds(entities, bb);
				Vector3 center = Vector3.tmp.set(bb.getCenter());
				center.set(element.getFloat("x", center.x) - center.x, element.getFloat("y", center.y) - center.y, element.getFloat("z", center.z) - center.z);
				WorldTransform.move(entities, center.x, center.y, center.z);
			}
		});
	}
	
	public final static Matrix4 tmpM4 = new Matrix4();
//	public static short[] indices;
//	public static float[] vertices;
	
	static {
		attributeConstructInfoLoaders.put("worldtransform", new IAttributeConstructInfoLoader() {
			@Override
			public Entity.IAttribute.IConstructInfo load(Element element, World world) {
				return new WorldTransform.ConstructInfo();
			}
		});
		
		attributeConstructInfoLoaders.put("localtransform", new IAttributeConstructInfoLoader() {
			@Override
			public Entity.IAttribute.IConstructInfo load(Element element, World world) {
				return new LocalTransform.ConstructInfo();
			}
		});
		
		attributeConstructInfoLoaders.put("renderable", new IAttributeConstructInfoLoader() {
			@Override
			public Entity.IAttribute.IConstructInfo load(Element element, World world) {
				RenderObject obj = new RenderObject(world.modelManager.get(element.getAttribute("model")));
				obj.enableLighting = element.getBoolean("lighting", true);
				obj.maxDistance = element.getInt("distance", 0);
				Renderable.ConstructInfo result = new Renderable.ConstructInfo(new RenderObject[] {obj});
				/*Renderable.ConstructInfo result = new Renderable.ConstructInfo(element.getBoolean("visible", true), 
						,
						);
				result.distance = element.getInt("distance", 0);*/
				result.priority = element.getInt("priority", 0);
				result.radius = element.getFloat("radius", -1);
				return result;
			}
		});
		
		attributeConstructInfoLoaders.put("light", new IAttributeConstructInfoLoader() {
			@Override
			public Entity.IAttribute.IConstructInfo load(Element element, World world) {
				Light.ConstructInfo ci = new Light.ConstructInfo();
				ci.color.r = element.getFloat("red", 0);
				ci.color.g = element.getFloat("green", 0);
				ci.color.b = element.getFloat("blue", 0);
				ci.color.a = element.getFloat("alpha", 1);
				final float x = element.getFloat("x", 0);
				final float y = element.getFloat("y", 0);
				final float z = element.getFloat("z", 0);
				if ((x != 0)||(y != 0)||(z != 0))
					ci.direction = new Vector3(x, y, z);
				ci.intensity = element.getFloat("intensity", 0);
				ci.angle = element.getFloat("angle", 0);
				return ci;
			}
		});
		
		attributeConstructInfoLoaders.put("bulletbody", new IAttributeConstructInfoLoader() {
			public btCollisionShape loadShape(Element element, World world) {
				final String shape = element.getAttribute("shape", element.getName());
				if (shape.compareTo("compound")==0) return loadCompoundShape(element, world);
				if (shape.compareTo("box")==0) return loadBoxShape(element, world);
				if (shape.compareTo("cylinder")==0) return loadCylinderShape(element, world);
				if (shape.compareTo("cone")==0) return loadConeShape(element, world);
				if (shape.compareTo("sphere")==0) return loadSphereShape(element, world);
				if (shape.compareTo("mesh")==0) return loadMeshShape(element, world);
				// capsule
				// multisphere
				if (shape.compareTo("mesh")==0) return loadMeshShape(element, world);
				return null;
			}
			
			public btCollisionShape loadCompoundShape(Element element, World world) {
				btCompoundShape result = new btCompoundShape(false);
				final int n = element.getChildCount();
				for (int i = 0; i < n; i++) {
					final Element child = element.getChild(i);
					btCollisionShape add = loadShape(child, world);
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
			public btCollisionShape loadBoxShape(Element element, World world) {
				float w = 0, h = 0, d = 0;
				final String modelName = element.getAttribute("model", null);
				if (modelName != null) {
					final Model model = world.modelManager.get(modelName);
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
			
			public btCollisionShape loadCylinderShape(Element element, World world) {
				float w = 0, h = 0, d = 0;
				final String orientation = element.getAttribute("orientation", "y");
				final String modelName = element.getAttribute("model", null);
				if (modelName != null) {
					final Model model = world.modelManager.get(modelName);
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
			
			public btCollisionShape loadConeShape(Element element, World world) {
				float w = 0, h = 0, d = 0;
				final String orientation = element.getAttribute("orientation", "y");
				final String modelName = element.getAttribute("model", null);
				if (modelName != null) {
					final Model model = world.modelManager.get(modelName);
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
			
			public btCollisionShape loadSphereShape(Element element, World world) {
				float r = 0;
				final String modelName = element.getAttribute("model", null);
				if (modelName != null) {
					final Model model = world.modelManager.get(modelName);
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
			
			public btCollisionShape loadMeshShape(Element element, World world) {
				final String modelName = element.getAttribute("model", null);
				if (modelName != null) {
					final Model model = world.modelManager.get(modelName);
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
			
			@Override
			public Entity.IAttribute.IConstructInfo load(Element element, World world) {
				final float mass = element.getFloat("mass", 0);
				final btCollisionShape shape = loadShape(element, world); 
				/*final String modelName = element.getAttribute("model", null);
				final String shapeType = element.getAttribute("shape", null).toLowerCase();
				if ((modelName == null) || (shapeType == null)) {
					Gdx.app.log("Loader", "model or shape is null");
					return null;
				}
				final Model model = world.modelManager.get(modelName);
				if (model == null) {
					Gdx.app.log("Loader", "model is null");
					return null;
				}
				int shape;
				if (shapeType.compareTo("box")==0) shape = BulletBody.ConstructInfo.BODY_SHAPE_BOX;
				else if (shapeType.compareTo("cylinder")==0) shape = BulletBody.ConstructInfo.BODY_SHAPE_CYLINDER;
				else if (shapeType.compareTo("sphere")==0) shape = BulletBody.ConstructInfo.BODY_SHAPE_SPHERE;
				else if (shapeType.compareTo("mesh")==0) shape = BulletBody.ConstructInfo.BODY_SHAPE_MESH;
				else {
					Gdx.app.log("Loader", "shape is null "+shapeType);
					return null;
				}*/
				BulletBody.ConstructInfo result = new BulletBody.ConstructInfo(mass, shape);
				try { result.body.setM_friction(element.getFloat("friction")); } catch (Exception e) {}
				try { result.body.setM_restitution(element.getFloat("restitution")); } catch (Exception e) {}
				try { result.body.setM_angularDamping(element.getFloat("angularDamping")); } catch (Exception e) {}
				try { result.body.setM_linearDamping(element.getFloat("linearDamping")); } catch (Exception e) {}
				return result;
			}
		});
	}
	
	public World world;
	
	public Loader(World world) {
		this.world = world;
	}
	
	public void load(FileHandle file) {
		XmlReader r = new XmlReader();
		try {
			load(r.parse(file));
		} catch (IOException e) {
			e.printStackTrace();
			Gdx.app.exit();
		}
	}
	
	public void load(XmlReader.Element element) {
		preload1(element);
		preload2(element);
		doload(element);
	}
	
	protected void doload(XmlReader.Element element) {
		IEntityLoader loader = entityLoaders.get(element.getName());
		if (loader != null)
			loader.load(element, world);
		else {
			Gdx.app.log("Loader", "Unknown element: "+element.getName());
			ObjectMap<String, String> attrs = element.getAttributes();
			if (attrs != null) {
				Entries<String, String> attr = element.getAttributes().entries();
				for (Entry<String, String> a : attr){
					Gdx.app.log("Loader", "Unknown attr: "+a.key+" = "+a.value);
				}
			}
			for (int i = 0; i < element.getChildCount(); i++)
				doload(element.getChild(i));
		}
	}
	
	protected void preload1(XmlReader.Element element) {
		if (element.getName().compareTo("template")==0) {
			final String name = element.getAttribute("name", null);
			Gdx.app.log("Loader", "Added template: "+name);
			if (name != null)
				templates.put(name, element);
			element.remove();
		} else {
			final int n = element.getChildCount();
			for (int i = n-1; i >= 0; i--)
				preload1(element.getChild(i));
		}
	}
	
	protected void preload2(XmlReader.Element element) {
		XmlReader.Element el = templates.get(element.getName());
		if (el != null) {
			ObjectMap<String, String> attrs = el.getAttributes();
			if (attrs != null)
				for (Entry<String, String> attr : attrs.entries())
					if (element.getAttributes() == null || !element.getAttributes().containsKey(attr.key))
						element.setAttribute(attr.key, attr.value);
			Array<Element> tmp = new Array<Element>();
			while(element.getChildCount() > 0) {
				tmp.add(element.getChild(0));
				element.removeChild(0);
			}
			for (int i = 0; i < el.getChildCount(); i++)
				element.addChild(el.getChild(i));
			for (int i = 0; i < tmp.size; i++)
				element.addChild(tmp.get(i));
			//element.setName(el.getAttribute("element", "unknown"));
			preload2(element); // do again, because the templated element might itself be also a template
			return;
		}
		final int n = element.getChildCount();
		for (int i = 0; i < n; i++)
			preload2(element.getChild(i));
	}
}
