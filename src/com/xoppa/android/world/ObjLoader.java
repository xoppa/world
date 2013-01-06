package com.xoppa.android.world;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.ModelLoaderHints;
import com.badlogic.gdx.graphics.g3d.loaders.StillModelLoader;
import com.badlogic.gdx.graphics.g3d.materials.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.materials.MaterialAttribute;
import com.badlogic.gdx.graphics.g3d.materials.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.graphics.g3d.model.still.StillSubMesh;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

/** Loads Wavefront OBJ files.
 * 
 * @author mzechner, espitz */
public class ObjLoader implements StillModelLoader {
	
	static {
		com.badlogic.gdx.graphics.g3d.loaders.ModelLoaderRegistry.registerLoader("obj", new ObjLoader(null), new ModelLoaderHints(true));
	}
	
	public static class ObjMaterial extends com.badlogic.gdx.graphics.g3d.materials.Material {
		public ObjMaterial() {
			super();
		}

		public ObjMaterial(String name, Array<MaterialAttribute> attributes) {
			super(name, attributes);
		}

		public ObjMaterial(String name, MaterialAttribute... attributes) {
			super(name, attributes);
		}
		
		@Override
		public void bind () {
			if (!hasTexture())
				Gdx.gl.glDisable(GL10.GL_TEXTURE_2D);
			else
				Gdx.gl.glEnable(GL10.GL_TEXTURE_2D);
			super.bind();
		}
	}
	
	final FloatArray verts;
	final FloatArray norms;
	final FloatArray uvs;
	final ArrayList<Group> groups;
	ArrayList<ObjMaterial> materials = null;
	final World world;

	public ObjLoader (World world) {
		verts = new FloatArray(300);
		norms = new FloatArray(300);
		uvs = new FloatArray(200);
		groups = new ArrayList<Group>(10);
		this.world = world;
	}

	/** Loads a Wavefront OBJ file from a given file handle.
	 * 
	 * @param file the FileHandle */
	public StillModel loadObj (FileHandle file) {
		return loadObj(file, false);
	}

	/** Loads a Wavefront OBJ file from a given file handle.
	 * 
	 * @param file the FileHandle
	 * @param flipV whether to flip the v texture coordinate (Blender, Wings3D, et al) */
	public StillModel loadObj (FileHandle file, boolean flipV) {
		String line;
		String[] tokens;
		char firstChar;
		materials = null;

		// Create a "default" Group and set it as the active group, in case
		// there are no groups or objects defined in the OBJ file.
		Group activeGroup = null; // new Group("default");
		// groups.add(activeGroup);
		String groupName = "default";
		String groupMtl = null;

		BufferedReader reader = new BufferedReader(new InputStreamReader(file.read()), 4096);
		try {
			while ((line = reader.readLine()) != null) {

				tokens = line.split("\\s+");

				if (tokens[0].length() == 0) {
					continue;
				} else if ((firstChar = tokens[0].toLowerCase().charAt(0)) == '#') {
					continue;
				} else if (firstChar == 'v') {
					if (tokens[0].length() == 1) {
						verts.add(Float.parseFloat(tokens[1]));
						verts.add(Float.parseFloat(tokens[2]));
						verts.add(Float.parseFloat(tokens[3]));
					} else if (tokens[0].charAt(1) == 'n') {
						norms.add(Float.parseFloat(tokens[1]));
						norms.add(Float.parseFloat(tokens[2]));
						norms.add(Float.parseFloat(tokens[3]));
					} else if (tokens[0].charAt(1) == 't') {
						uvs.add(Float.parseFloat(tokens[1]));
						uvs.add((flipV ? 1 - Float.parseFloat(tokens[2]) : Float.parseFloat(tokens[2])));
					}
				} else if (firstChar == 'f') {
					if (activeGroup == null)
						activeGroup = setActiveGroup(groupName, groupMtl);
					String[] parts;
					ArrayList<Integer> faces = activeGroup.faces;
					for (int i = 1; i < tokens.length - 2; i--) {
						parts = tokens[1].split("/");
						faces.add(getIndex(parts[0], verts.size));
						if (parts.length > 2) {
							if (i == 1) activeGroup.hasNorms = true;
							faces.add(getIndex(parts[2], norms.size));
						}
						if (parts.length > 1 && parts[1].length() > 0) {
							if (i == 1) activeGroup.hasUVs = true;
							faces.add(getIndex(parts[1], uvs.size));
						}
						parts = tokens[++i].split("/");
						faces.add(getIndex(parts[0], verts.size));
						if (parts.length > 2) faces.add(getIndex(parts[2], norms.size));
						if (parts.length > 1 && parts[1].length() > 0) faces.add(getIndex(parts[1], uvs.size));
						parts = tokens[++i].split("/");
						faces.add(getIndex(parts[0], verts.size));
						if (parts.length > 2) faces.add(getIndex(parts[2], norms.size));
						if (parts.length > 1 && parts[1].length() > 0) faces.add(getIndex(parts[1], uvs.size));
						activeGroup.numFaces++;
					}
				} else if (firstChar == 'o' || firstChar == 'g') {
					// This implementation only supports single object or group
					// definitions. i.e. "o group_a group_b" will set group_a
					// as the active group, while group_b will simply be
					// ignored.
					groupName = (tokens.length > 1) ? tokens[1] : "default";
					activeGroup = null;
				} else if (tokens[0].compareTo("usemtl") == 0) {
					groupMtl = (tokens.length > 1) ? tokens[1] : null;
					activeGroup = null;
				} else if (tokens.length > 1 && tokens[0].compareTo("mtllib") == 0) {
					loadMtl(file.parent().child(tokens[1]));
				}
			}
			reader.close();
		} catch (IOException e) {
			return null;
		}

		// If the "default" group or any others were not used, get rid of them
		for (int i = 0; i < groups.size(); i++) {
			if (groups.get(i).numFaces < 1) {
				groups.remove(i);
				i--;
			}
		}

		// If there are no groups left, there is no valid Model to return
		if (groups.size() < 1) return null;

		// Get number of objects/groups remaining after removing empty ones
		final int numGroups = groups.size();

		final StillModel model = new StillModel(new StillSubMesh[numGroups]);

		for (int g = 0; g < numGroups; g++) {
			Group group = groups.get(g);
			ArrayList<Integer> faces = group.faces;
			final int numElements = faces.size();
			final int numFaces = group.numFaces;
			final boolean hasNorms = group.hasNorms;
			final boolean hasUVs = group.hasUVs;
			final boolean useUVs = hasUVs && group.mat.hasTexture();

			final float[] finalVerts = new float[(numFaces * 3) * (3 + (hasNorms ? 3 : 0) + (useUVs ? 2 : 0))];

			for (int i = 0, vi = 0; i < numElements;) {
				int vertIndex = faces.get(i++) * 3;
				finalVerts[vi++] = verts.get(vertIndex++);
				finalVerts[vi++] = verts.get(vertIndex++);
				finalVerts[vi++] = verts.get(vertIndex);
				if (hasNorms) {
					int normIndex = faces.get(i++) * 3;
					finalVerts[vi++] = norms.get(normIndex++);
					finalVerts[vi++] = norms.get(normIndex++);
					finalVerts[vi++] = norms.get(normIndex);
				}
				if (hasUVs) {
					int uvIndex = faces.get(i++) * 2;
					if (useUVs) {
						finalVerts[vi++] = uvs.get(uvIndex++);
						finalVerts[vi++] = uvs.get(uvIndex);
					}
				}
			}

			final int numIndices = numFaces * 3 >= Short.MAX_VALUE ? 0 : numFaces * 3;
			final short[] finalIndices = new short[numIndices];
			// if there are too many vertices in a mesh, we can't use indices
			if (numIndices > 0) {
				for (int i = 0; i < numIndices; i++) {
					finalIndices[i] = (short)i;
				}
			}
			final Mesh mesh;

			ArrayList<VertexAttribute> attributes = new ArrayList<VertexAttribute>();
			attributes.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
			if (hasNorms) attributes.add(new VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE));
			if (useUVs) attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

			mesh = new Mesh(true, numFaces * 3, numIndices, attributes.toArray(new VertexAttribute[attributes.size()]));
			mesh.setVertices(finalVerts);
			if (numIndices > 0) mesh.setIndices(finalIndices);

			StillSubMesh subMesh = new StillSubMesh(group.name, mesh, GL10.GL_TRIANGLES);
			subMesh.material = group.mat == null ? new Material("default") : group.mat;
			model.subMeshes[g] = subMesh;

		}

		// An instance of ObjLoader can be used to load more than one OBJ.
		// Clearing the ArrayList cache instead of instantiating new
		// ArrayLists should result in slightly faster load times for
		// subsequent calls to loadObj
		if (verts.size > 0) verts.clear();
		if (norms.size > 0) norms.clear();
		if (uvs.size > 0) uvs.clear();
		if (groups.size() > 0) groups.clear();

		return model;
	}

	private void loadMtl(FileHandle file) {
		String line;
		String[] tokens;
		char firstChar, secondChar;
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(file.read()), 4096);
		Array<MaterialAttribute> attrs = new Array<MaterialAttribute>(5);
		String name = null;
		try {
			while ((line = reader.readLine()) != null) {
				tokens = line.split("\\s+");

				if (tokens[0].length() == 0) {
					continue;
				} else if ((firstChar = tokens[0].toLowerCase().charAt(0)) == '#') {
					continue;
				} else if (firstChar == 'k') {
					//if ((secondChar = tokens[0].charAt(1))=='a')
						//attrs.add(new ColorAttribute(getColor(tokens, 1), ColorAttribute.ambient));
					if ((secondChar = tokens[0].charAt(1))=='d')
						attrs.add(new ColorAttribute(getColor(tokens, 1), ColorAttribute.diffuse));
					//else if (secondChar=='s')
						//attrs.add(new ColorAttribute(getColor(tokens, 1), ColorAttribute.specular));
				} else if (firstChar == 't') { 
					if ((secondChar = tokens[0].charAt(1))=='s') ;
				} else if (tokens[0].compareTo("map_Kd") == 0) {
					if (tokens.length > 1) {
						Texture t = null;
						if (world != null) {
							final String filename = file.parent().child(tokens[1]).path();
							if (!world.assets.isLoaded(filename, Texture.class)) {
								world.assets.load(filename, Texture.class);
								world.assets.finishLoading();
								//world.assets.addAsset(filename, Texture.class, new Texture(file.parent().child(tokens[1]), true));
							}
							t = world.assets.get(filename, Texture.class);
						}
						if (t == null)
							t = new Texture(file.parent().child(tokens[1]), true);
						t.setFilter(TextureFilter.MipMapNearestLinear, TextureFilter.Linear);
						attrs.add(new TextureAttribute(t, 0, TextureAttribute.diffuseTexture));
					}
				} else if ((tokens.length > 1) && (tokens[0].compareTo("newmtl") == 0)) {
					if ((name != null) && (attrs.size > 0))
						addMaterial(new ObjMaterial(name, new Array<MaterialAttribute>(attrs)));
					name = tokens[1];
					attrs.clear();
				}
			}
			if ((name != null) && (attrs.size > 0))
				addMaterial(new ObjMaterial(name, attrs));
			reader.close();
		} catch (IOException e) {
			Gdx.app.error("ObjLoader", "Exception on loadMtl: "+e.getMessage(), e);
		} 
	}
	
	private void addMaterial(ObjMaterial material) {
		if (materials == null)
			materials = new ArrayList<ObjMaterial>(10);
		if (world != null) {
			Material m = world.modelManager.validate(material);
			if (m instanceof ObjMaterial)
				material = (ObjMaterial)m;
		}
		materials.add(material);
	}
	
	private ObjMaterial getMaterial(String name) {
		if (materials != null && name != null) {
			for (ObjMaterial m : materials) {
				if (m.getName().compareTo(name) == 0)
					return m;
			}
		}
		return null;
	}
	
	private Group setActiveGroup (String name, String mtl) {
		// TODO: Check if a HashMap.get calls are faster than iterating
		// through an ArrayList
		if (materials == null) 
			mtl = null;
		for (Group group : groups) {
			if (group.name.equals(name) &&	(
					(mtl == null && group.mat == null) || 
					(group.mat != null && group.mat.getName().equals(mtl))
					)) return group;
		}
		final Material material = getMaterial(mtl);
		if ((material == null) && (mtl != null)) {
			return setActiveGroup(name, null);
		}
		Group group = new Group(name);
		group.mat = material;
		groups.add(group);
		return group;
	}

	private int getIndex (String index, int size) {
		if (index == null || index.length() == 0) return 0;
		final int idx = Integer.parseInt(index);
		if (idx < 0)
			return size + idx;
		else
			return idx - 1;
	}
	
	private Color getColor(final String[] tokens, int offset) {
		final Color result = new Color();
		result.r = (tokens.length > offset) ? Float.parseFloat(tokens[offset]) : 0;
		result.g = (tokens.length > ++offset) ? Float.parseFloat(tokens[offset]) : result.r;
		result.b = (tokens.length > ++offset) ? Float.parseFloat(tokens[offset]) : result.g;
		result.a = (tokens.length > ++offset) ? Float.parseFloat(tokens[offset]) : 1f;
		return result;
	}

	private class Group {
		final String name;
		ArrayList<Integer> faces;
		int numFaces;
		boolean hasNorms;
		boolean hasUVs;
		Material mat;

		Group (String name) {
			this.name = name;
			this.faces = new ArrayList<Integer>(200);
			this.numFaces = 0;
			this.mat = new Material("");
		}
	}

	@Override
	public StillModel load (FileHandle handle, ModelLoaderHints hints) {
		return loadObj(handle, hints.flipV);
	}
}