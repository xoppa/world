package com.xoppa.android.world;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class GroupContainer implements Disposable {
	public Array<Group> groups = null;
	
	@Override
	public void dispose() {
		clear();
	}
	
	public void clear() {
		if (groups != null) {
			for (int i = 0; i < groups.size; i++)
				groups.get(i).dispose();
			groups.clear();
		}
	}
	
	public void add(final Group group) {
		if (groups == null)
			groups = new Array<Group>(false, 16);
		groups.add(group);
	}
	
	public Entity get(final int id) {
		if (groups != null) {
			for (int i = 0; i < groups.size; i++) {
				final Entity r = groups.get(i).get(id);
				if (r != null)
					return r;
			}
		}
		return null;
	}
	
	public Entity get(final String name) {
		if (groups != null) {
			final String[] parts = name.split("\\.", 2);
			if (parts.length > 1) {
				for (int i = 0; i < groups.size; i++)
					if (groups.get(i).name.compareTo(parts[0])==0)
						return groups.get(i).get(parts[1]);
			}
		}
		return null;
	}
	
	
	public Group sub(final String name) {
		if (groups != null) {
			final String[] parts = name.split("\\.", 2);
			for (int i = 0; i < groups.size; i++)
				if (groups.get(i).name.compareTo(parts[0])==0)
					return parts.length > 1 ? groups.get(i).sub(parts[1]) : groups.get(i);
		}
		return null;
	}
	
	public void reset() {
		if (groups != null) {
			for (int i = 0; i < groups.size; i++)
				groups.get(i).reset();
		}
	}
	
	public void init() {
		if (groups != null) {
			for (int i = 0; i < groups.size; i++)
				groups.get(i).init();
		}		
	}
}
