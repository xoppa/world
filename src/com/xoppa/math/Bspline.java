package com.xoppa.math;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class Bspline implements Path {
	private final static float d6 = 1f / 6f;
	private final static Vector3 tmpV = new Vector3();
	public static Vector3 bSpline3(final Vector3 out, final int i, final float t, final Vector3[] points, final boolean continuous) {
		final int n = points.length;
		final float dt = 1f - t;
		final float t2 = t * t;
		final float t3 = t2 * t;
		out.set(points[i]).mul((3f * t3 - 6f * t2 + 4f) * d6);
		if (continuous || i > 0) out.add(tmpV.set(points[(n+i-1)%n]).mul(dt * dt * dt * d6));
		if (continuous || i < (n - 1)) out.add(tmpV.set(points[(i + 1)%n]).mul((-3f * t3 + 3f * t2 + 3f * t + 1f) * d6));
		if (continuous || i < (n - 2)) out.add(tmpV.set(points[(i + 2)%n]).mul(t3 * d6));
		return out;
	}
	
	public static Vector3 bSplineN(final Vector3 out, final int i, final float t, final Vector3[] points, final int degree, final boolean continuous) {
		switch(degree) {
		case 3: return bSpline3(out, i, t, points, continuous);
		}
		return out;
	}
	
	public final Vector3[] points;
	public final Vector3[] knots;
	public final int degree;
	public final boolean continuous;
	public final int spanCount;

	/**
	 * Construct an open cubic bspline path.
	 * @param controlPoints The control points of the bspline.
	 */
	public Bspline(final Vector3[] controlPoints) {
		this(controlPoints, 3);
	}
	
	/**
	 * Construct an open bspline path.
	 * @param controlPoints The control points of the bspline.
	 * @param degree The degree of the bspline (3 for cubic).
	 */
	public Bspline(final Vector3[] controlPoints, final int degree) {
		this(controlPoints, degree, false);
	}
	
	/**
	 * Construct a bspline path.
	 * @param controlPoints The control points of the bspline.
	 * @param degree The degree of the bspline (3 for cubic).
	 * @param continuous Whether the curve is continuous or not.
	 */
	public Bspline(final Vector3[] controlPoints, final int degree, final boolean continuous) {
		this.continuous = continuous;
		this.degree = degree;
		this.points = controlPoints;
		this.spanCount = continuous ? points.length : points.length - degree;
		knots = new Vector3[spanCount];
		for (int i = 0; i < spanCount; i++)
			knots[i] = bSplineN(new Vector3(), continuous ? i : (int)(i + 0.5f * degree), 0f, points, degree, continuous);
	}
	
	/*protected void calcKnots() {
		final int n = closed ? points.length : (points.length - degree);
		if (knots == null || knots.length != n)
			knots = new Vector3[n];
		for (int i = 0; i < n; i++) {
			if (knots[i] == null)
				knots[i] = new Vector3();
			bSpline3(knots[i], closed ? i : (i + (int)(degree/2f)), 0f, points, closed);
		}
	}*/
	
	/**
	 * Get the value of the path, where 0 <= t <= 1
	 */
	@Override
	public Vector3 get(final Vector3 out, final float t) {
		final int n = spanCount;
		float u = t * n;
		int i = (t >= 1f) ? (n - 1) : (int)u;
		u -= (float)i;
		return get(out, i, u);
		//return bSpline3(out, closed ? i : (i+(int)(degree/2f)), u, points, closed); // TODO Use get(out, span, t)
	}
	
	public Vector3 get(final Vector3 out, final int span, final float t) {
		return bSpline3(out, continuous ? span : (span + (int)(degree*0.5f)), t, points, continuous);
	}
	
	public int nearest(final Vector3 in) {
		return nearest(in, 0, knots.length);
	}
	
	public int nearest(final Vector3 in, int start, final int count) {
		while (start < 0) start += knots.length;
		int result = start % knots.length;
		float dst = in.dst2(knots[result]);
		for (int i = 1; i < count; i++) {
			final int idx = (start + i) % knots.length;
			final float d = in.dst2(knots[idx]);
			if (d < dst) {
				dst = d;
				result = idx;
			}
		}
		return result;
	}

	public float approximate(final Vector3 in) {
		return approximate(in, nearest(in));
	}
	
	public float approximate(final Vector3 in, int start, final int count) {
		return approximate(in, nearest(in, start, count));
	}
	
	public float approximate(final Vector3 in, final int near) {
		int n = near; //nearest(in, start, count);
		final Vector3 nearest = knots[n];
		final Vector3 previous = knots[n>0?n-1:knots.length-1];
		final Vector3 next = knots[(n+1)%knots.length];
		final float dstPrev2 = in.dst2(previous);
		final float dstNext2 = in.dst2(next);
		Vector3 P1, P2, P3;
		if (dstNext2 < dstPrev2) {
			P1 = nearest;
			P2 = next;
			P3 = in;
		} else {
			P1 = previous;
			P2 = nearest;
			P3 = in;
			n = n>0?n-1:knots.length-1;
		}
		float L1 = P1.dst(P2);
		float L2 = P3.dst(P2);
		float L3 = P3.dst(P1);
		float s = (L2*L2 + L1*L1 - L3*L3) / (2*L1);
		float u = MathUtils.clamp((L1-s)/L1, 0f, 1f);
		return ((float)n + u) / knots.length;
	}
}
