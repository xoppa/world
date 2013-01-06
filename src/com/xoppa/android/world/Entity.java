package com.xoppa.android.world;

import java.io.Serializable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.ObjectMap.Values;

public class Entity implements Disposable {
	public interface IAttribute extends Disposable {
		void reset();
		void init(Entity e);
		IAttribute clone();
		boolean is(int type);
		int type();
		
		public interface IConstructInfo extends Disposable {
			IAttribute construct();
		}
	}
	
	public static class ConstructInfo implements Disposable {
		public boolean enabled = true;
		public Array<IAttribute.IConstructInfo> attributes;
		
		public ConstructInfo(final boolean enabled, final IAttribute.IConstructInfo... attributes) {
			if (attributes != null)
				this.attributes = new Array<IAttribute.IConstructInfo>(attributes);
			else
				this.attributes = new Array<IAttribute.IConstructInfo>(4);
		}
		
		public ConstructInfo(final IAttribute.IConstructInfo... attributes) {
			this(true, attributes);
		}
		
		public ConstructInfo(final boolean enabled) {
			this(enabled, (IAttribute.IConstructInfo[])null);
		}
		
		public ConstructInfo() {
			this(true);
		}
		
		public <T extends IAttribute.IConstructInfo> T get(final Class<T> attr) {
			final int n = attributes.size;
			for (int i = 0; i < n; i++)
				if (attributes.get(i).getClass() == attr)
					return (T)attributes.get(i);
			return null;
		}

		@Override
		public void dispose() {
			for (int i = 0; i < attributes.size; i++)
				attributes.get(i).dispose();
			attributes.clear();
		}
	}
	
	public int id;
	public String name;
	public boolean enabled = true;
	public boolean initEnabled = true;
	
	//public ObjectMap<Class<?>, IAttribute> attributes;
	public Array<IAttribute> attributes;
	
	protected Entity(int id, String name, int size, final boolean enabled) {
		if (id == World.INVALID_GUID) id = World.newGUID();
		if (name == null) name = "#"+id;
		if (size < 1) size = 3;
		this.id = id;
		this.name = name;
		//this.attributes = new ObjectMap<Class<?>, IAttribute>(3);
		this.attributes = new Array<IAttribute>(3);
		this.enabled = enabled;
	}
	
	public Entity() { this(World.INVALID_GUID, null, 0, true); }
	public Entity(final String name) { this(World.INVALID_GUID, name, 0, true); }
	public Entity(final int id) { this(id, null, 0, true); }
	public Entity(final int id, final String name) { this(id, name, 0, true); }
	public Entity(final boolean enabled) { this(World.INVALID_GUID, null, 0, enabled); }
	public Entity(final String name, final boolean enabled) { this(World.INVALID_GUID, name, 0, enabled); }
	public Entity(final int id, final boolean enabled) { this(id, null, 0, enabled); }
	public Entity(final int id, final String name, final boolean enabled) { this(id, name, 0, enabled); }
	
	
	public Entity(final int id, final String name, final Entity copyFrom, final boolean enabled) {
		this(id, (name != null ? name : (copyFrom != null ? copyFrom.name : null)), 
				(copyFrom != null ? copyFrom.attributes.size : 0), enabled);
		if (copyFrom != null)
			for (int i = 0; i < copyFrom.attributes.size; i++)
				set(copyFrom.attributes.get(i).clone());
			//for (ObjectMap.Entry<Class<?>, IAttribute> e : copyFrom.attributes.entries())
				//set(e.value.clone());
	}
	
	public Entity(final Entity copyFrom) { this(World.INVALID_GUID, null, copyFrom, true); }
	public Entity(final int id, final Entity copyFrom) { this(id, null, copyFrom, true); }
	public Entity(final String name, final Entity copyFrom) { this(World.INVALID_GUID, name, copyFrom, true); }
	public Entity(final int id, final String name, final Entity copyFrom) { this(id, name, copyFrom, true); }
	public Entity(final Entity copyFrom, final boolean enabled) { this(World.INVALID_GUID, null, copyFrom, enabled); }
	public Entity(final int id, final Entity copyFrom, final boolean enabled) { this(id, null, copyFrom, enabled); }
	public Entity(final String name, final Entity copyFrom, final boolean enabled) { this(World.INVALID_GUID, name, copyFrom, enabled); }
	
	public Entity(final int id, final String name, final IAttribute... attributes) {
		this(id, name, attributes == null ? 0 : attributes.length, true);
		if (attributes != null)
			for (int i = 0; i < attributes.length; i++)
				set(attributes[i]);
	}
	
	public Entity(final String name, final IAttribute...attributes) { this(0, name, attributes); }
	public Entity(final int id, final IAttribute...attributes) { this(id, null, attributes); }
	public Entity(final IAttribute...attributes) { this(World.INVALID_GUID, null, attributes); }
	
	public Entity(final int id, final String name, final IAttribute.IConstructInfo... attributes) {
		this(id, name, attributes == null ? 0 : attributes.length, true);
		if (attributes != null)
			for (int i = 0; i < attributes.length; i++)
				set(attributes[i].construct());
	}
	
	public Entity(final String name, final IAttribute.IConstructInfo... attributes) { this(World.newGUID(), name, attributes); }
	public Entity(final int id, final IAttribute.IConstructInfo... attributes) { this(id, null, attributes); }
	public Entity(final IAttribute.IConstructInfo... attributes) { this(World.newGUID(), null, attributes); }
	
	public Entity(final int id, final String name, final ConstructInfo constructInfo) {
		this(id, name, (constructInfo == null || constructInfo.attributes == null) ? 0 : constructInfo.attributes.size, constructInfo != null ? constructInfo.enabled : true);
		if (constructInfo != null)
			for (int i = 0; i < constructInfo.attributes.size; i++)
				set(constructInfo.attributes.get(i).construct());
	}
	
	public Entity(final String name, final ConstructInfo constructInfo) { this(World.newGUID(), name, constructInfo); }
	public Entity(final int id, final ConstructInfo constructInfo) { this(id, null, constructInfo); }
	public Entity(final ConstructInfo constructInfo) { this(World.newGUID(), null, constructInfo); }
	
	public Entity clone() {
		return new Entity(this);
	}
	
	public <T extends IAttribute> T get(final Class<T> attr) {
		IAttribute result;
		for (int i = 0; i < attributes.size; i++)
			if (attr.isInstance(result = attributes.get(i)))
				return (T)result;
		return null;
		//return (T)attributes.get(attr);
	}
	
	public <T extends IAttribute> boolean has(final Class<T> attr) {
		for (int i = 0; i < attributes.size; i++)
			if (attr.isInstance(attributes.get(i)))
				return true;
		return false;
	}
	
	public <T extends IAttribute> void set(final T attr) {
		attributes.add(attr);
		//attributes.put(attr.getClass(), attr);
	}
	
	public void reset() {
		enabled = initEnabled;
		for (int i = 0; i < attributes.size; i++)
			attributes.get(i).reset();
		/*Values<IAttribute> values = attributes.values();
		IAttribute attr;
		values.reset();
		while((attr = values.next())!=null)
			attr.reset();*/
	}
	
	public void init() {
		initEnabled = enabled;
		for (int i = 0; i < attributes.size; i++)
			attributes.get(i).init(this);
		/*Values<IAttribute> values = attributes.values();
		IAttribute attr;
		values.reset();
		while((attr = values.next())!=null)
			attr.init(this);*/
	}

	@Override
	public void dispose() {
		for (int i = 0; i < attributes.size; i++)
			attributes.get(i).dispose();
		/*Values<IAttribute> values = attributes.values();
		IAttribute attr;
		values.reset();
		while((attr = values.next())!=null)
			attr.dispose();*/
	}
}
