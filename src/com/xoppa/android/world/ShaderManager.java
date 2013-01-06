package com.xoppa.android.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.materials.MaterialAttribute;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class ShaderManager implements Disposable {
	public String vertexExtension = ".vertex.glsl";
	public String fragmentExtension = ".fragment.glsl";
	public FileHandle shaderBase = Gdx.files.internal("data/shaders");
	public String uberShader = "uber";
	protected ObjectMap<String, ShaderProgram> shaders = new ObjectMap<String, ShaderProgram>();
	
	public String readVertexShader(final String name) {
		return shaderBase.child(name+vertexExtension).readString();
	}
	
	public String readFragmentShader(final String name) {
		return shaderBase.child(name+fragmentExtension).readString();
	}
	
	public ShaderProgram get(final String name) {
		return get(name, (Array<String>)null);
	}
	
	public ShaderProgram get(final Material material, boolean lighting) {
		return get(uberShader, material, lighting);
	}
	
	protected final static Array<String> defs = new Array<String>(); 
	public ShaderProgram get(final String name, final Material material, boolean lighting) {
		if (material == null)
			return get(name);
		defs.clear();
		for (MaterialAttribute a : material)
			defs.add(a.getShaderFlag());
		if (lighting)
			defs.add("lightingFlag");
		return get(name, defs);
	}
	
	public ShaderProgram get(String name, final Array<String> definitions) {
		final String  filename = name;
		String prefix = "";
		if ((definitions != null) && (definitions.size > 0)) {
			definitions.sort();
			name += "<";
			for (int i = 0; i <definitions.size; i++) {
				if (i > 0) name += ";";
				name += definitions.get(i);
				prefix += "#define "+definitions.get(i)+"\n";
			}
			name += ">";
		}	
		return shaders.containsKey(name) ? shaders.get(name) :
					get(name, prefix + readVertexShader(filename), prefix + readFragmentShader(filename));
	}
	
	public ShaderProgram get(final String name, final String vertexShader, final String fragmentShader) {
		ShaderProgram result = shaders.get(name);
		if (result == null) {
			ShaderProgram.pedantic = false;
			if (vertexShader == null || fragmentShader == null) {
				System.out.println("One or both shaders is null");
				Gdx.app.exit();
			}
			result = new ShaderProgram("#define LIGHTS_NUM 0\n"+vertexShader, "#define LIGHTS_NUM 0\n"+fragmentShader);
			if (!result.isCompiled()) {
				System.out.println("error" + result.getLog());
				Gdx.app.exit();
			}
			shaders.put(name, result);
		}
		return result;
	}

	@Override
	public void dispose() {
		for (ShaderProgram shader : shaders.values())
			shader.dispose();
		shaders.clear();
	}
}
