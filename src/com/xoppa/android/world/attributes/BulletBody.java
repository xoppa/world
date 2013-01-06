package com.xoppa.android.world.attributes;

import java.nio.ByteBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.model.Model;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.PHY_ScalarType;
import com.badlogic.gdx.physics.bullet.btBoxShape;
import com.badlogic.gdx.physics.bullet.btBvhTriangleMeshShape;
import com.badlogic.gdx.physics.bullet.btCollisionShape;
import com.badlogic.gdx.physics.bullet.btConvexShape;
import com.badlogic.gdx.physics.bullet.btCylinderShape;
import com.badlogic.gdx.physics.bullet.btDefaultMotionState;
import com.badlogic.gdx.physics.bullet.btIndexedMesh;
import com.badlogic.gdx.physics.bullet.btMotionState;
import com.badlogic.gdx.physics.bullet.btRigidBody;
import com.badlogic.gdx.physics.bullet.btRigidBodyConstructionInfo;
import com.badlogic.gdx.physics.bullet.btSphereShape;
import com.badlogic.gdx.physics.bullet.btStridingMeshInterface;
import com.badlogic.gdx.physics.bullet.btTransform;
import com.badlogic.gdx.physics.bullet.btTriangleIndexVertexArray;
import com.badlogic.gdx.physics.bullet.gdxBulletJNI;
import com.badlogic.gdx.utils.Array;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.Entity.IAttribute;

public class BulletBody implements IAttribute {
	public interface BulletBodyListener {
		void setWorldTransform();
	}
	
	public static class MotionState extends btMotionState {
		public WorldTransform worldTransform = null;

		public void attachTo(final WorldTransform worldTransform) {
			this.worldTransform = worldTransform;
		}
		
		public boolean isAttached() {
			return worldTransform != null;
		}
		
		@Override
		public void getWorldTransform (final Matrix4 worldTrans) {
			if (worldTransform != null)
				worldTrans.set(worldTransform.current);
		}
		
		@Override
		public void setWorldTransform (final Matrix4 worldTrans) {
			worldTransform.current.set(worldTrans);
		}
		
		public boolean getInitialTransform(final Matrix4 out) {
			if (worldTransform != null) {
				if (worldTransform.initial != null)
					out.set(worldTransform.initial);
				else
					out.set(worldTransform.current);
				return true;
			}
			return false;
		}
		
		@Override
		public MotionState clone() {
			return new MotionState();
		}
	}
	
	public static class ExtendedMotionState extends MotionState {
		public BulletBody.BulletBodyListener listener;
		@Override
		public void setWorldTransform(final Matrix4 worldTrans) {
			super.setWorldTransform(worldTrans);
			if (listener != null)
				listener.setWorldTransform();
		}
	}
	
	public final static int TYPE = 1;
	
	public btRigidBody body;
	public MotionState motionState;
	public btRigidBodyConstructionInfo constructInfo;
	public btCollisionShape shape;
	
	public BulletBody() {
	}

	public BulletBody(final btRigidBody body, final MotionState motionState) {
		this.body = body;
		this.motionState = motionState;
	}
	
	public BulletBody(final btRigidBodyConstructionInfo bodyInfo, final MotionState motionState) {
		this(bodyInfo == null ? null : new btRigidBody(bodyInfo), motionState);
		constructInfo = bodyInfo;
	}
	
	public BulletBody(final BulletBody copyFrom) {
		this(copyFrom == null ? null : copyFrom.constructInfo, copyFrom == null ? null : copyFrom.motionState.clone());
	}
	
	@Override
	public void dispose() {
		if (body != null)
			body.delete();
		body = null;
		if (motionState != null)
			motionState.delete();
		motionState = null;
	}
	
	private static final Matrix4 resetTransform = new Matrix4();
	@Override
	public void reset() {
		if ((body != null)&&(motionState.getInitialTransform(resetTransform))) {
			body.setWorldTransform(resetTransform);
			body.setInterpolationWorldTransform(resetTransform);
			if (!body.isStaticObject()) {
				body.setLinearVelocity(Vector3.Zero);
				body.setAngularVelocity(Vector3.Zero);
				body.activate();
			}
		}
	}

	@Override
	public void init(Entity e) {
		if ((body != null) && (e != null) && (motionState != null) && (!motionState.isAttached())) {
			WorldTransform worldTransform = e.get(WorldTransform.class);
			if (worldTransform != null) {
				motionState.attachTo(worldTransform);
				body.setMotionState(motionState);
			}
		}
	}

	@Override
	public BulletBody clone() {
		return new BulletBody(this);
	}
	
	@Override
	public int type() {
		return TYPE;
	}
	
	@Override
	public boolean is(final int type) {
		return type == TYPE;
	}
	
	public static class ConstructInfo implements Entity.IAttribute.IConstructInfo {
		public final static int BODY_SHAPE_NONE = 0;
		public final static int BODY_SHAPE_BOX = 1;
		public final static int BODY_SHAPE_SPHERE = 2;
		public final static int BODY_SHAPE_CYLINDER = 3;
		public final static int BODY_SHAPE_MESH = 4;
		
		public btRigidBodyConstructionInfo body;
		public btCollisionShape shape;
		public boolean extended;
		
		public ConstructInfo(final btRigidBodyConstructionInfo body) {
			this.body = body;
		}

		public ConstructInfo(final float mass, final btCollisionShape shape, final Vector3 localInertia) {
			this(new btRigidBodyConstructionInfo(mass, null, shape, localInertia));
			this.shape = shape;
		}
		
		public ConstructInfo(final float mass, final btCollisionShape shape) {
			this(new btRigidBodyConstructionInfo(mass, null, shape, mass <= 0 ? Vector3.Zero : calcLocalInertia(shape, mass)));
			this.shape = shape;
		}
			
		public ConstructInfo(final float mass, final Model model, final int shapeType, final Vector3 localInertia) {
			this(mass, createShape(model, shapeType), localInertia);
		}
		
		public ConstructInfo(final float mass, final Model model, final int shapeType) {
			this(mass, createShape(model, shapeType));
		}
		
		@Override
		public void dispose() {
			if (body != null)
				body.delete();
			body = null;
			if (shape != null)
				shape.delete();
			shape = null;
		}
		
		@Override
		public IAttribute construct() {
			BulletBody result = new BulletBody(body, extended ? new ExtendedMotionState() : new MotionState());
			result.shape = shape;
			return result;
		}
		
		public static Vector3 calcLocalInertia(final btCollisionShape shape, final float mass) {
			Vector3 result = new Vector3();
			if (mass <= 0)
				result.set(0,0,0);
			else
				shape.calculateLocalInertia(mass, result);
			return result;
		}
		
		public static btCollisionShape createShape(final Model pModel, final int pShapeType) {
			BoundingBox bb;
			Vector3 dim;
			switch (pShapeType) {
			case BODY_SHAPE_BOX:
				bb = new BoundingBox();
				pModel.getBoundingBox(bb);
				dim = bb.getDimensions();
				return new btBoxShape(new Vector3(dim.x*0.5f, dim.y*0.5f, dim.z*0.5f));
			case BODY_SHAPE_SPHERE:
				bb = new BoundingBox();
				pModel.getBoundingBox(bb);
				dim = bb.getDimensions();
				float r = dim.x;
				if (dim.y > r) r = dim.y;
				if (dim.z > r) r = dim.z;
				return new btSphereShape(r*0.5f);
			case BODY_SHAPE_CYLINDER:
				bb = new BoundingBox();
				pModel.getBoundingBox(bb);
				dim = bb.getDimensions();
				return new btCylinderShape(new Vector3(dim.x*0.5f, dim.y*0.5f, dim.z*0.5f));
			case BODY_SHAPE_MESH:
				if (pModel == null)
					return null;
				Mesh mesh = pModel.getSubMeshes()[0].getMesh();
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
				return result;
			default: return null;
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
}
