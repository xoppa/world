package com.xoppa.android.world.attributes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.model.Model;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.xoppa.android.world.Entity;
import com.xoppa.android.world.Entity.IAttribute;

public class Renderable implements Entity.IAttribute {
	public static class RenderObject {
		public RenderObject(final Model model) { this.model = model; }
		public Model model = null;
		public int maxDistance = 0;
		public boolean castShadows = true;
		public boolean receiveShadows = true;
		public boolean enableLighting = true;
		public boolean enableBlending = false;
		public Vector3 centerOffset = null;
	}
	
	public final static int TYPE = 4;
	
	public boolean visible = true;
	public RenderObject[] models;
	// public Model[] models = null;
	// public boolean lighting = true;
	public boolean depthtest = true;
	/** The radius of the sphere (center at (0,0,0) in local space) that includes the complete model */
	public float radius;
	public BoundingBox bounds = new BoundingBox();
	//public int distance = 0;
	public int priority = 0;
	
	public Renderable() {
		this((Renderable)null);
	}
	
	/*public Renderable(final Model model) {
		this(model, true);
	}
	
	public Renderable(final Model model, final boolean visible) {
		this(model, visible, true);
	}
	
	public Renderable(final Model model, final boolean visible, final boolean lighting) {
		this(new Model[] {model}, null, lighting, visible);
	}
	
	public Renderable(final Model[] models, final int[] distances, final boolean lighting, final boolean visible) {
		this();
		this.visible = visible;
		//this.lighting = lighting;
		setModels(models, distances);
	}*/
	
	public Renderable(final RenderObject[] models) {
		this();
		setModels(models);
	}
	
	public Renderable(final Renderable copyFrom) {
		if (copyFrom != null) {
			visible = copyFrom.visible;
			models = copyFrom.models;
			radius = copyFrom.radius;
			//lighting = copyFrom.lighting;
			depthtest = copyFrom.depthtest;
			//distances = copyFrom.distances;
			priority = copyFrom.priority;
		}
	}
	
	@Override
	public void dispose() {
	}
	
	public void setModels(final RenderObject[] models) {
		this.models = models;
		if (models != null && models.length > 0) {
			models[0].model.getBoundingBox(bounds);
			radius = bounds.getCenter().len() + bounds.getDimensions().len() * 0.5f;
		} else {
			bounds.inf();
			radius = 0;
		}
	}
	
	/*public void setModels(final Model[] models, final int[] distances) {
		this.models = models;
		this.distances = null;
		radius = 0;
		if (models != null && models.length > 0) {
			if (models.length > 1) {
				if (distances == null || distances.length != models.length)
					throw new IllegalArgumentException("When setting multiple models, as much distances must be provided");
				this.distances = distances;
				distance = distances[distances.length-1];				
			} else if (distances != null && distances.length > 0)
				distance = distances[0];
			
			models[0].getBoundingBox(bounds);
			radius = bounds.getCenter().len() + bounds.getDimensions().len() * 0.5f;
		}
	}*/
	
	public Entity.IAttribute clone() {
		return new Renderable(this);
	}
	
	@Override
	public void reset() {
	}
	
	@Override
	public void init(Entity e) {
	}
	
	@Override
	public boolean is(final int type) {
		return type == TYPE;
	}
	
	@Override
	public int type() {
		return TYPE;
	}
	
	/*public BoundingBox getBounds(final BoundingBox out) {
		if (models != null && models.length > 0)
			models[0].getBoundingBox(out);
		return out;
	}*/
	
	public static BoundingBox getBounds(final Renderable renderable, final BoundingBox out) {
		if (renderable != null)
			out.set(renderable.bounds);
			//renderable.getBounds(out);
		return out;
	}
	public static BoundingBox getBounds(final Entity entity, final BoundingBox out) {
		if (entity != null) {
			getBounds(entity.get(Renderable.class), out);
			WorldTransform transform = entity.get(WorldTransform.class);
			if (transform != null) {
				Vector3.tmp.set(out.min).mul(transform.current);
				Vector3.tmp2.set(out.max).mul(transform.current);
				out.inf();
				out.set(Vector3.tmp, Vector3.tmp2);
			}
		}
		return out;
	}
	public final static BoundingBox tmpbounds = new BoundingBox();
	public static BoundingBox getBounds(final Iterable<Entity> entities, final BoundingBox out) {
		tmpbounds.inf();
		for (final Entity e : entities) {
			if (getBounds(e, tmpbounds).isValid())
				out.ext(tmpbounds);
			else
				Gdx.app.log("Test", "Bounds are invalid!!");
		}
		return out;
	}
	
	public static class ConstructInfo implements Entity.IAttribute.IConstructInfo {
		public boolean visible = true;
		//public boolean lighting = true;
		public boolean depthtest = true;
		//public int[] distances = null;
		//public int distance = 0;
		public int priority;
		public float radius = Float.NaN;
		public RenderObject[] models = null;
		
		/*public ConstructInfo(final boolean visible, final Model model) {
			this(visible, model, true);
		}*/
		
		public ConstructInfo(final RenderObject[] models) {
			this.models = models;
		}
		
		/*public ConstructInfo(final Model model) {
			this(true, model);
		}*/
		
		@Override
		public void dispose() {
		}
		
		@Override
		public IAttribute construct() {
			Renderable result = new Renderable(models);
			//if (distance != 0)
				//result.distance = distance;
			result.priority = priority;
			result.depthtest = depthtest;
			if (radius >= 0)
				result.radius = radius;
			return result;
		}
	}
}
