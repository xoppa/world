package com.xoppa.android.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.model.Model;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

// TODO: For now this is just a placeholder, however it should be a more better/intelligent implementation.
public class ModelManager implements Disposable {
	public ObjectMap<String, Model> models = new ObjectMap<String, Model>();
	public Array<Material> materials = new Array<Material>();
	
	public void add(final String name, final Model model) {
		models.put(name, model);
	}
	
	public Model get(final String name) {
		return models.get(name);
	}
	
	public Material validate(final Material material) {
		for (int i = 0; i < materials.size; i++) {
			final Material mat = materials.get(i);
			if (mat.equals(material)) {
				Gdx.app.log("Materials", "Reusing material: "+mat.getName());
				return mat;
			}
		}
		materials.add(material);
		return material;
	}

	@Override
	public void dispose() {
		for (Model m : models.values())
			m.dispose();
		models.clear();
	}
}
