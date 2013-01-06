package com.xoppa.android.world;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.xoppa.math.Bspline;

public class PathFollower {
	public Matrix4 transform;
	public Bspline positionPath;
	public Bspline targetPath;
	public Vector3 forward;
	public final Vector3 position = new Vector3();
	public final Vector3 target = new Vector3();
	public float targetOffset;
	
	public float value;

	public PathFollower(final Matrix4 transform, final Bspline path, final Vector3 forward) {
		this(transform, path, path, 0.01f, forward);
	}
	
	public PathFollower(final Matrix4 transform, final Bspline path, final float targetOffset, final Vector3 forward) {
		this(transform, path, path, targetOffset, forward);
	}
	
	public PathFollower(final Matrix4 transform, final Bspline positionPath, final Bspline targetPath, final float targetOffset, final Vector3 forward) {
		this.transform = transform;
		this.positionPath = positionPath;
		this.targetPath = targetPath;
		this.targetOffset = targetOffset;
		this.forward = forward;
	}
	
	public void update(final float value) {
		this.value = value;
		positionPath.get(position, value%1f);
		targetPath.get(target, (value + targetOffset)%1f);
		// targetPath.get(target, (positionPath == targetPath ? value : targetPath.approximate(position)) + targetOffset);
		transform.setToRotation(forward, Vector3.tmp3.set(target).sub(position));
		transform.trn(position);
	}
}
