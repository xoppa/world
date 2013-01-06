package com.xoppa.android.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.xoppa.android.world.attributes.WorldTransform;

public class Scene {
	public static class Node<T> {
		public String name;
		public String type;
		public T entity;
		public final Matrix4 transform = new Matrix4();
		public boolean castShadows;
		public boolean receiveShadows;
		public Array<Node<T>> children = null;
	}
	
	/**
	 * The path of the root folder of the scene. This can be used to load dependencies.
	 */
	public final String root;
	public final Array<Node<?>> nodes = new Array<Node<?>>();
	
	public Scene(final String root) {
		this.root = root;
	}
	
	public Scene(final FileHandle root) {
		this(root.path());
	}
}
