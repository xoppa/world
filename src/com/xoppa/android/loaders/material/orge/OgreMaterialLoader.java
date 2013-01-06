package com.xoppa.android.loaders.material.orge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g3d.materials.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.materials.MaterialAttribute;
import com.badlogic.gdx.graphics.g3d.materials.MaterialCollection;
import com.badlogic.gdx.graphics.g3d.materials.TextureAttribute;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class OgreMaterialLoader extends SynchronousAssetLoader<MaterialCollection, AssetLoaderParameters<MaterialCollection>> {
	MaterialCollection result = null;
	Array<AssetDescriptor> dependencies = new Array<AssetDescriptor>();
	ObjectMap<TextureAttribute, String> textures = new ObjectMap<TextureAttribute, String>(); 

	public OgreMaterialLoader(FileHandleResolver resolver) {
		super(resolver);
	}
	
	@Override
	public MaterialCollection load(AssetManager assetManager, String fileName, AssetLoaderParameters<MaterialCollection> params) {
		for (ObjectMap.Entry<TextureAttribute, String> entry : textures.entries()) {
			entry.key.texture = assetManager.get(entry.value, Texture.class);
			Gdx.app.log("OgreMaterialLoader", "Set texture of material to: "+entry.key.texture);
		}
		MaterialCollection result = this.result;
		this.result = null;
		return result;
	}
	
	@Override
	public Array<AssetDescriptor> getDependencies(String fileName, AssetLoaderParameters<MaterialCollection> params) {
		this.result = new MaterialCollection();
		this.textures.clear();
		this.dependencies.clear();
		FileHandle file = resolve(fileName);
		BufferedReader reader = null;
		try {
			load(reader = new BufferedReader(new InputStreamReader(file.read()), 4096), file.parent());
			Gdx.app.log("OgeMaterialLoader", "Loaded material: "+file.path());
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
		return dependencies;
	}
	
	protected void load(final BufferedReader reader, final FileHandle base) throws IOException {
		String line;
		Material current = null;
		while((line = reader.readLine()) != null) {
			final String[] tokens = line.trim().split("\\s+");
			if (tokens[0].compareTo("material")==0) {
				if (current != null)
					result.add(current);
				current = new Material(tokens[1], new Array<MaterialAttribute>(1));
			}
			else if (current != null) {
				if (tokens[0].compareTo("diffuse") == 0)
					current.addAttribute(new ColorAttribute(parseColor(tokens), ColorAttribute.diffuse));
				else if (tokens[0].compareTo("specular") == 0)
					current.addAttribute(new ColorAttribute(parseColor(tokens), ColorAttribute.specular));
				else if (tokens[0].compareTo("texture") == 0)
					current.addAttribute(parseTextureAttribute(tokens, base));
			}
		}
		if (current != null)
			result.add(current);
	}
	
	protected Color parseColor(final String[] tokens) {
		final Color result = new Color(0f, 0f, 0f, 1f);
		if (tokens.length > 1) result.r = Float.parseFloat(tokens[1]);
		if (tokens.length > 2) result.g = Float.parseFloat(tokens[2]);
		if (tokens.length > 3) result.b = Float.parseFloat(tokens[3]);
		if (tokens.length > 4) result.a = Float.parseFloat(tokens[4]);
		return result;
	}
	
	protected TextureAttribute parseTextureAttribute(final String[] tokens, final FileHandle base) {
		final TextureAttribute result = new TextureAttribute(null, 0, "s_tex", TextureFilter.Linear, TextureFilter.Linear, TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
		final String filename = base.child(tokens[1]).path();
		boolean found = false;
		for (int i = 0; i < dependencies.size; i++) {
			if (dependencies.get(i).fileName.compareTo(filename)==0) {
				found = true;
				break;
			}
		}
		if (!found)
			dependencies.add(new AssetDescriptor(filename, Texture.class));
		textures.put(result, filename);
		return result;
	}
}
