package com.xoppa.android.world;

import java.util.Iterator;
import java.util.Stack;

import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pool.Poolable;

public class EntityIterator implements Iterator<Entity> {
	protected static class EntityIteratorCrumble implements Poolable {
		public Group group = null;
		public int entityIndex = -1;
		public int groupIndex = -1;
		@Override
		public void reset() {
			group = null;
			entityIndex = groupIndex = -1;
		}
	}
	protected final static Pool<EntityIteratorCrumble> crumblePool = new Pool<EntityIteratorCrumble>() {
		@Override
		protected EntityIteratorCrumble newObject() {
			return new EntityIteratorCrumble();
		}
	};
	protected final Stack<EntityIteratorCrumble> crumbleStack = new Stack<EntityIteratorCrumble>();
	protected EntityIteratorCrumble current = crumblePool.obtain();
	protected Entity next = null;
	protected boolean end = false;
	
	public EntityIterator(final Group group) {
		current.group = group;
		current.entityIndex = current.groupIndex = -1;
	}

	@Override
	public boolean hasNext() {
		if (end) return false;
		if (next != null) return true;
		if (current.groupIndex < 0 && current.group.entities != null && ++current.entityIndex < current.group.entities.size) {
			next = current.group.entities.get(current.entityIndex);
			return true;
		}
		if (current.group.groups != null && ++current.groupIndex < current.group.groups.size) {
			final EntityIteratorCrumble crumble = crumblePool.obtain();
			crumble.group = current.group.groups.get(current.groupIndex);
			crumbleStack.push(current);
			current = crumble;
			return hasNext();
		}
		if (!crumbleStack.isEmpty()) {
			crumblePool.free(current);
			current = crumbleStack.pop();
			return hasNext();
		}
		end = true;
		return false;
	}

	@Override
	public Entity next() {
		if (next == null)
			hasNext();
		final Entity result = next;
		next = null;
		return result;
	}

	@Override
	public void remove() {
	}

}
