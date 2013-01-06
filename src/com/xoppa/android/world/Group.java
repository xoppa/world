package com.xoppa.android.world;

import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.xoppa.android.world.PredicateIterator.Predicate;
import com.xoppa.android.world.PredicateIterator.PredicateIterable;
import com.xoppa.android.world.Scene.Node;
import com.xoppa.android.world.attributes.WorldTransform;

public class Group extends GroupContainer implements Iterable<Entity> {
	public int id;
	public String name;
	public boolean enabled = true;
	public boolean initEnabled = true;
	
	public Array<Entity> entities = null;
	
	public Group(final int id, final String name, final boolean enabled) {
		this.id = (id == World.INVALID_GUID ? World.newGUID() : id);
		this.name = (name == null) ? "#"+this.id : name;
	}
	
	public Group() { this(World.INVALID_GUID, null, true); }
	public Group(final int id) { this(id, null, true); }
	public Group(final String name) { this(World.INVALID_GUID, name, true); }
	public Group(final int id, final String name) { this(id, name, true); }
	
	public Entity add(final Entity entity) {
		if (entity == null) 
			return null;
		if (entities == null)
			entities = new Array<Entity>(false, 16);
		entities.add(entity);
		return entity;
	}
	
	public Entity get(final int id) {
		if (entities != null) {
			for (int i = 0; i < entities.size; i++)
				if (entities.get(i).id == id)
					return entities.get(i);
		}
		return super.get(id);
	}
	
	public Entity get(final String name) {
		Entity r = super.get(name);
		if (r != null)
			return r;
		if (entities != null) {
			for (int i = 0; i < entities.size; i++)
				if ((r = entities.get(i)).name.compareTo(name) == 0)
					return r;
		}
		return null;
	}
	
	@Override
	public void clear() {
		super.clear();
		if (entities != null) {
			for (int i = 0; i < entities.size; i++)
				entities.get(i).dispose();
			entities.clear();
		}
	}
	
	@Override
	public void reset() {
		super.reset();
		enabled = initEnabled;
		if (entities != null)
			for (int i = 0; i < entities.size; i++)
				entities.get(i).reset();
	}
	
	@Override
	public void init() {
		super.init();
		initEnabled = enabled;
		if (entities != null)
			for (int i = 0; i < entities.size; i++)
				entities.get(i).init();
	}
	
	public void load(final Scene scene) {
		load(scene.nodes);
	}

	public void load(final Array<Scene.Node<?>> nodes) {
		final int count = nodes.size;
		for (int i = 0; i < count; i++)
			add((Scene.Node<Entity.ConstructInfo>)nodes.get(i));
	}
	
	public void add(final Scene.Node<Entity.ConstructInfo> node) {
		final Entity entity = new Entity(node.name, node.entity);
		if (node.children != null && node.children.size > 0) {
			final Group grp = new Group(node.name);
			grp.add(entity);
			final int count = node.children.size;
			for (int i = 0; i < count; i++)
				grp.add((Scene.Node<Entity.ConstructInfo>)(node.children.get(i)));
			node.transform.getTranslation(Vector3.tmp);
			WorldTransform.move(grp, Vector3.tmp.x, Vector3.tmp.y, Vector3.tmp.z);
			//WorldTransform.transform(grp, node.transform);
			add(grp);
		} else {
			final WorldTransform transform = entity.get(WorldTransform.class);
			if (transform != null)
				transform.current.set(node.transform);
			add(entity);
		}
	}
	
	public Group clone() {
		return copyTo(null, new Group(name));
	}
	
	public Group copyTo(final String prefix, final Group target) {
		copySubgroupsTo(prefix, target);
		copyEntitiesTo(prefix, target);
		return target;
	}
	
	public Group copyTo(final Group target) {
		return copyTo(null, target);
	}
	
	public void copySubgroupsTo(final String prefix, final Group target) {
		if (groups != null) {
			for (int i = 0; i < groups.size; i++) {
				final Group grp = groups.get(i).clone();
				if (prefix != null)
					grp.name = prefix + grp.name;
				target.add(grp);
			}
		}
	}
	
	public void copyEntitiesTo(final String prefix, final Group target) {
		if (entities != null) {
			for (int i = 0; i < entities.size; i++) {
				final Entity ent = entities.get(i).clone();
				if (prefix != null)
					ent.name = prefix + ent.name;
				target.add(ent);
			}
		}
	}
	
	public void moveSubgroupsTo(final String prefix, final Group target) {
		if (groups != null) {
			for (int i = 0; i < groups.size; i++) {
				final Group grp = groups.get(i);
				if (prefix != null)
					grp.name = prefix + grp.name;
				target.add(grp);
			}
			groups.clear();
		}
	}
	
	public void moveEntitiesTo(final String prefix, final Group target) {
		if (entities != null) {
			for (int i = 0; i < entities.size; i++) {
				final Entity ent = entities.get(i);
				if (prefix != null)
					ent.name = prefix + ent.name;
				target.add(ent);
			}
			entities.clear();
		}
	}
	
	protected void flattenTo(int levels, boolean selfPrefix, String prefix, final Group target) {
		if (levels > 0) levels--;
		if (selfPrefix)
			prefix = (prefix == null) ? name + "_" : prefix + name + "_";
		moveEntitiesTo(prefix, target);
		if (groups != null) {
			if (levels != 0) {
				for (int i = 0; i < groups.size; i++)
					groups.get(i).flattenTo(levels, selfPrefix, prefix, target);
				groups.clear();
			}
			else
				moveSubgroupsTo(prefix, target);
		}
	}
	
	public void flatten(int levels, boolean prefix) {
		final String pr = prefix ? name + "_" : null;
		if (levels > 0) levels--;
		if (groups != null) {
			Array<Group> grps = groups;
			groups = null;
			for (int i = 0; i < grps.size; i++)
				grps.get(i).flattenTo(levels, prefix, pr, this);
			grps.clear();
		}
	}
	
	public void flatten() {
		flatten(-1, false);
	}
	
	public PredicateIterable<Entity> select(Predicate<Entity> predicate) {
		return new PredicateIterable<Entity>(this, predicate);
	}
	
	public <T extends Entity.IAttribute> PredicateIterable<Entity> select(final Class<T> attr) {
		return select(new Predicate<Entity>(){
			@Override
			public boolean evaluate(Entity arg0) {
				return arg0.has(attr);
			}
		});
	}
	
	public <T extends Entity.IAttribute> PredicateIterable<Entity> select(final Class<T>... attr) {
		final int n = attr.length;
		return select(new Predicate<Entity>(){
			@Override
			public boolean evaluate(Entity arg0) {
				for (int i = 0; i < n; i++)
					if (arg0.has(attr[i]))
						return true;
				return false;
			}
		});
	}

	@Override
	public Iterator<Entity> iterator() {
		return new EntityIterator(this);
	}
}
