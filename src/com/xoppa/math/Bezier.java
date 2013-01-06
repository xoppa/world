package com.xoppa.math;

import com.badlogic.gdx.math.Vector3;

public class Bezier {
	private final static Vector3 tmpV = new Vector3();
	
	/** Simple linear interpolation */
	public static Vector3 bezier1(final Vector3 out, final float t, final Vector3 p0, final Vector3 p1) {
		return out.set(p0).mul(1f - t).add(tmpV.set(p1).mul(t));
	}
	
	/** Quadratic Bézier curve */
	public static Vector3 bezier2(final Vector3 out, final float t, final Vector3 p0, final Vector3 p1, final Vector3 p2) {
		final float dt = 1f - t;
		return out.set(p0).mul(dt*dt).add(tmpV.set(p1).mul(2*dt*t)).add(tmpV.set(p2).mul(t*t));
	}
	
	/** Cubic Bézier curve [same a bezier1(t, bezier2(t, p0, p1, p2), bezier2(t, p1, p2, p3))] 
	 * R(t) = ((1 - t)^3 * P0) + (3 * t * (1 - t)^2 * P1) + (3 * t^2 * (1 - t) * p2) + (t^3 * P3) 
	 * or in the form of: f(t) = at^3 + bt^2 + ct + d =>
	 * R(t) = (-P0 + 3P1 - 3P2 + P3)*t^3 + (3P0 - 6P1 + 3P2)*t^2 + (3P1 + 3P0)*t + P0
	 */
	public static Vector3 bezier3(final Vector3 out, final float t, final Vector3 p0, final Vector3 p1, final Vector3 p2, final Vector3 p3) {
		final float dt = 1f - t;
		final float dt2 = dt * dt;
		final float t2 = t * t;
		return out.set(p0).mul(dt2*dt).add(tmpV.set(p1).mul(3*dt2*t)).add(tmpV.set(p2).mul(3*dt*t2)).add(tmpV.set(p3).mul(t2*t));
	}
	
	public static Vector3 bezierN(final Vector3 out, final float t, final Vector3... p) {
		return bezierN(out, t, p, 0, p.length);
	}
	
	public static Vector3 bezierN(final Vector3 out, final float t, final Vector3[] p, final int offset, final int count) {
		int degree = (p.length - offset) - 1;
		if (count <= degree) degree = count - 1;
		switch(degree) {
		case 3: return bezier3(out, t, p[offset], p[offset+1], p[offset+2], p[offset+3]);
		case 2: return bezier2(out, t, p[offset], p[offset+1], p[offset+2]);
		case 1: return bezier1(out, t, p[offset], p[offset+1]);
		case 0: return out.set(p[offset]);
		// TODO: Add interpolation for higher degrees
		default: return out;
		}
	}

	public static class Curve implements Path {
		public Vector3[] cp;
		public int offset;
		public int size;
		
		public Curve(final Vector3[] cp, final int offset, final int size) {
			this.cp = cp;
			this.offset = offset;
			this.size = size;
		}
		
		@Override
		public Vector3 get(final Vector3 out, final float t) {
			return Bezier.bezierN(out, t, cp, offset, size);
		}
	}
	
	public static class Curves implements Path {
		public Curve curve;
		public int offset;
		public int size;
		
		public Curves(final Vector3[] cp, final int offset, final int size) {
			curve = new Curve(cp, this.offset = offset, this.size = size);
		}
		
		@Override
		public Vector3 get(final Vector3 out, final float t) {
			int c = (curve.cp.length - 1) / (size - 1);
			float x = t * (float)c;
			c = (int)x;
			curve.offset = c * (size - 1);
			return curve.get(out, x - (float)c);
		}
	}
}
