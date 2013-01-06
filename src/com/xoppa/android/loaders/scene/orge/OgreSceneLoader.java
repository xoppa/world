package com.xoppa.android.loaders.scene.orge;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.xoppa.android.world.Scene;

public class OgreSceneLoader<T> extends SynchronousAssetLoader<Scene, OgreSceneLoader.Parameters> {
	public static class Parameters extends AssetLoaderParameters<Scene> {
		public final Class<?> dependencyType;
		public final String dependencySuffix;
		public Parameters() {
			this(null, null);
		}
		public Parameters(final Class<?> dependType, final String dependSuffix) {
			this.dependencyType = dependType;
			this.dependencySuffix = dependSuffix;
		}
	}
	
	public OgreSceneLoader(FileHandleResolver resolver) {
		super(resolver);
	}

	public Scene load(final FileHandle file) {
		final Scene result = new Scene(file.parent());
		try {
			XmlReader reader = new XmlReader();
			XmlReader.Element root = reader.parse(file);
			final Array<XmlReader.Element> nodes = root.getChildByName("nodes").getChildrenByName("node");// getChildrenByNameRecursively("node");
			if (nodes != null)
				for (XmlReader.Element node : nodes)
					result.nodes.add(parseNode(node));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	private final static Quaternion tmpQuat = new Quaternion();
	private Scene.Node<T> parseNode(final XmlReader.Element element) {
		final Scene.Node<T> result = new Scene.Node<T>();
		result.name = element.getAttribute("name", null);
		final int childCount = element.getChildCount();
		for (int i = 0; i < childCount; i++) {
			final XmlReader.Element child = element.getChild(i);
			if (child.getName().compareTo("position") == 0)
				result.transform.translate(child.getFloat("x", 0), child.getFloat("y", 0), child.getFloat("z", 0));
			else if (child.getName().compareTo("scale") == 0)
				result.transform.scale(child.getFloat("x", 0), child.getFloat("y", 0), child.getFloat("z", 0));
			else if (child.getName().compareTo("rotation") == 0)
				result.transform.rotate(tmpQuat.set(child.getFloat("qx", 0), child.getFloat("qy", 0), child.getFloat("qz", 0), child.getFloat("qw", 0)));
			else if (child.getName().compareTo("entity") == 0) {
				result.castShadows = child.getBoolean("castShadows", true);
				result.receiveShadows = child.getBoolean("receiveShadows", true);
				result.type = child.getAttribute("meshFile", "");
				if (result.type.endsWith(".mesh"))
					result.type = result.type.replace(".mesh", "");
			}
			else if (child.getName().compareTo("node") == 0) {
				if (result.children == null)
					result.children = new Array<Scene.Node<T>>();
				result.children.add(parseNode(child));
			}
		}
		return result;
	}
	
	Scene result = null;
	
	private void load(Scene.Node<?> node, AssetManager assetManager, Parameters parameters) {
		node.entity = assetManager.get(this.result.root+"/"+node.type+parameters.dependencySuffix);
		if (node.children != null)
			for (int i = 0; i < node.children.size; i++)
				load(node.children.get(i), assetManager, parameters);
	}
	
	@Override
	public Scene load(AssetManager assetManager, String fileName, Parameters parameters) {
		if (result.nodes != null)
			for (int i = 0; i < result.nodes.size; i++)
				load(result.nodes.get(i), assetManager, parameters);
		Scene result = this.result;
		this.result = null;
		return result;
	}
	
	private void addDependency(Array<AssetDescriptor> result, Scene.Node<?> node, Parameters parameters) {
		final String fn = this.result.root+"/"+node.type+parameters.dependencySuffix;
		boolean found = false;
		for (int j = 0; j < result.size; j++) {
			if (result.get(j).fileName.compareTo(fn)==0) {
				found = true;
				break;
			}
		}
		if (!found)
			result.add(new AssetDescriptor(fn, parameters.dependencyType));
		if (node.children != null)
			for (int i = 0; i < node.children.size; i++)
				addDependency(result, node.children.get(i), parameters);
	}

	@Override
	public Array<AssetDescriptor> getDependencies(String fileName, Parameters parameters) {
		this.result = load(resolve(fileName));
		if (result.nodes == null || parameters == null || parameters.dependencyType == null)
			return null;

		Array<AssetDescriptor> result = new Array<AssetDescriptor>();
		for (int i = 0; i < this.result.nodes.size; i++)
			addDependency(result, this.result.nodes.get(i), parameters);
		return result;
	}
}