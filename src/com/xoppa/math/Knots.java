package com.xoppa.math;

// http://hg.postspectacular.com/toxiclibs/src/eff3353aadef/src.core/toxi/geom/nurbs/KnotVector.java?at=default
// http://hg.postspectacular.com/toxiclibs/src/eff3353aadef/src.core/toxi/geom/nurbs/BasicNurbsCurve.java?at=default
public class Knots {
	public final boolean open;
	public final float[] values;
	public final int degree;
	public final int n;
	
	public Knots(final float[] values, final int degree) {
		for (int i = 1; i < values.length; i++)
			if (values[i - 1] > values[i])
				throw new IllegalArgumentException("Invalid values");
		
		this.values = values;
		this.degree = degree;
		this.n = values.length - degree - 2;
		
		boolean open = true;
		for (int i = 0; open && i < degree; i++)
			if (values[i] != values[i+1])
				open = false;
		for (int i = values.length - (degree + 1); open && i < values.length; i++)
			if (values[i - 1] != values[i])
				open = false;
		this.open = open;
	}
	
	public float[] basis(final float u) {
		return basis(getSpan(u), u);
	}
	
	public float[] basis(final int span, final float u) {
		final int d1 = degree + 1;
		final float res[] = new float[d1];
		final float left[] = new float[d1];
		final float right[] = new float[d1];
		res[0] = 1;
		for (int i = 1; i < d1; i++) {
			left[i] = u - values[span + 1 - i];
			float s = 0;
			for (int j = 0; j < i; j++) {
				final float tmp = res[j] / (right[j+1] + left[i - j]);
				res[j] = s + right[j + 1] * tmp;
				s = left[i - j] * tmp;
			}
			res[i] = s;
		}
		return res;
	}
	
	public int getSpan(final float u) {
		if (u >= values[n+1])
			return n;
		
		int low = degree;
		int high = n + 1;
		int mid = (low + high) / 2;
		while ((u < values[mid] || u >= values[mid+1]) && low < high) {
			if (u < values[mid])
				high = mid;
			else
				low = mid;
			mid = (low + high) / 2;
		}
		return mid;
	}
	
	public int getSegmentCount() {
		int seg = 0;
		float u = values[0];
		for (int i = 1; i < values.length; i++) {
			if (u != values[i]) {
				seg++;
				u = values[i];
			}
		}
		return seg;
	}
	
	public float[][] deriv(final float u, final int grade) {
		return deriv(getSpan(u), u, grade);
	}
	
	public float[][] deriv(final int span, final float u, final int grade) {
        float[][] ders = new float[grade + 1][degree + 1];
        float[][] ndu = new float[degree + 1][degree + 1];
        ndu[0][0] = 1.0f;
        float[] left = new float[degree + 1];
        float[] right = new float[degree + 1];
        int j1, j2;
        for (int j = 1; j <= degree; j++) {
            left[j] = u - values[span + 1 - j];
            right[j] = values[span + j] - u;
            float saved = 0.0f;
            for (int r = 0; r < j; r++) {
                ndu[j][r] = right[r + 1] + left[j - r];
                float temp = ndu[r][j - 1] / ndu[j][r];
                ndu[r][j] = saved + right[r + 1] * temp;
                saved = left[j - r] * temp;
            }
            ndu[j][j] = saved;
        }
        for (int j = 0; j <= degree; j++) {
            ders[0][j] = ndu[j][degree];
        }
        for (int r = 0; r <= degree; r++) {
            int s1 = 0;
            int s2 = 1;
            float[][] a = new float[2][degree + 1];
            a[0][0] = 1.0f;
            for (int k = 1; k <= grade; k++) {
                float d = 0.0f;
                final int rk = r - k;
                final int pk = degree - k;
                final float[] as1 = a[s1];
                final float[] as2 = a[s2];
                if (r >= k) {
                    as2[0] = d = as1[0] / ndu[pk + 1][rk];
                    d *= ndu[rk][pk];
                }
                if (rk >= -1) {
                    j1 = 1;
                } else {
                    j1 = -rk;
                }
                if (r - 1 <= pk) {
                    j2 = k - 1;
                } else {
                    j2 = degree - r;
                }
                for (int j = j1; j <= j2; j++) {
                    as2[j] = (as1[j] - as1[j - 1]) / ndu[pk + 1][rk + j];
                    d += as2[j] * ndu[rk + j][pk];
                }
                if (r <= pk) {
                    as2[k] = -as1[k - 1] / ndu[pk + 1][r];
                    d += as2[k] * ndu[r][pk];
                }
                ders[k][r] = d;
                int j = s1;
                s1 = s2;
                s2 = j;
            }
        }
        int r = degree;
        for (int k = 1; k <= grade; k++) {
            for (int j = 0; j <= degree; j++) {
                ders[k][j] *= r;
            }
            r *= (degree - k);
        }
        return ders;

	}
}
