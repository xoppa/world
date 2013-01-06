package com.xoppa.android.world;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g3d.loaders.ogre.OgreXmlLoader.MaterialProvider;
import com.badlogic.gdx.graphics.g3d.materials.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.materials.MaterialAttribute;
import com.badlogic.gdx.graphics.g3d.materials.TextureAttribute;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class MaterialManager implements MaterialProvider {
	private ObjectMap<String, Material> materials = new ObjectMap<String, Material>();
	public AssetManager assets;
	
	public Material get(final String name) {
		return materials.get(name);
	}
	
	@Override
	public Material get(final String name, final FileHandle base) {
		final Material result = materials.get(name);
		if (result != null || base == null) {
			if (result != null)
				Gdx.app.log("MaterialManager", "Reusing material: "+name);
			else if (base == null)
				Gdx.app.log("MaterialManager", "Cannot load material (no base): "+name);
			return result;
		}
		load(name, base); // The loader is responsible for adding the material, as it might contain multiple materials
		return get(name);
	}
	
	protected void load(final String name, final FileHandle base) {
		FileHandle file;
		if ((file = base.child(name+".material")).exists() && !file.isDirectory())
			loadOgreMaterial(file);
		else
			Gdx.app.log("MaterialManager", "File does not exist: "+file.path());
	}
	
	protected void loadOgreMaterial(final FileHandle file) {
		BufferedReader reader = null;
		try {
			loadOgreMaterial(reader = new BufferedReader(new InputStreamReader(file.read()), 4096), file.parent());
			Gdx.app.log("MaterialManager", "Loaded material: "+file.path());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	protected void loadOgreMaterial(final BufferedReader reader, final FileHandle base) throws IOException {
		String line;
		Material current = null;
		while((line = reader.readLine()) != null) {
			final String[] tokens = line.trim().split("\\s+");
			if (tokens[0].compareTo("material")==0) {
				if (current != null)
					materials.put(current.getName(), current);
				current = new Material(tokens[1], new Array<MaterialAttribute>(1));
			}
			else if (current != null) {
				if (tokens[0].compareTo("diffuse") == 0)
					current.addAttribute(new ColorAttribute(parseColor(tokens), ColorAttribute.diffuse));
				else if (tokens[0].compareTo("specular") == 0)
					current.addAttribute(new ColorAttribute(parseColor(tokens), ColorAttribute.specular));
				else if (tokens[0].compareTo("texture") == 0)
					current.addAttribute(new TextureAttribute(parseTexture(tokens, base), 4, TextureAttribute.diffuseTexture));
			}
		}
		if (current != null)
			materials.put(current.getName(), current);
	}
	
	protected Color parseColor(final String[] tokens) {
		final Color result = new Color(0f, 0f, 0f, 1f);
		if (tokens.length > 1) result.r = Float.parseFloat(tokens[1]);
		if (tokens.length > 2) result.g = Float.parseFloat(tokens[2]);
		if (tokens.length > 3) result.b = Float.parseFloat(tokens[3]);
		if (tokens.length > 4) result.a = Float.parseFloat(tokens[4]);
		return result;
	}
	
	protected Texture parseTexture(final String[] tokens, final FileHandle base) {
		Texture result = new Texture(base.child(tokens[1]), true);
		result.setFilter(TextureFilter.MipMapNearestLinear, TextureFilter.Linear);
		Gdx.app.log("MaterialLoader", "Loaded texture: "+base.child(tokens[1]).path());
		return result;
	}
}
