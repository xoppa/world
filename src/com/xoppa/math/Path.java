package com.xoppa.math;

import com.badlogic.gdx.math.Vector3;

public interface Path {
	Vector3 get(final Vector3 out, final float t);
}
