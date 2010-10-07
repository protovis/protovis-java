package pv.util;

public class Geometry {
	
    public interface VertexCallback {
    	public void vertex(double x, double y);
    }
    
    // -- cubic bezier spline -------------------------------------------
    
    /**
	 * Draws a cubic Bezier curve.
	 * @param g the graphics context to draw with
	 * @param ax x-coordinate of the starting point
	 * @param ay y-coordinate of the starting point
	 * @param bx x-coordinate of the first control point
	 * @param by y-coordinate of the first control point
	 * @param cx x-coordinate of the second control point
	 * @param cy y-coordinate of the second control point
	 * @param dx x-coordinate of the ending point
	 * @param dy y-coordinate of the ending point
	 * @param includeFirst if true the first point will be included
	 * @param vc callback to collect vertex values
	 */
	public static int cubic(double ax, double ay,
		double bx, double by, double cx, double cy, double dx, double dy,
		boolean includeFirst, VertexCallback vc)
	{
		int subdiv;
		double u, xx, yy;			
		
		// determine number of line segments
//		subdiv = (int)((
//			Math.sqrt((xx=(bx-ax))*xx + (yy=(by-ay))*yy) +
//			Math.sqrt((xx=(cx-bx))*xx + (yy=(cy-by))*yy) +
//			Math.sqrt((xx=(dx-cx))*xx + (yy=(dy-cy))*yy)) / 4);
//		if (subdiv < 1) subdiv = 1;
		subdiv = 30;

		// compute Bezier co-efficients
		double c3x = 3 * (bx - ax);
        double c2x = 3 * (cx - bx) - c3x;
        double c1x = dx - ax - c3x - c2x;
        double c3y = 3 * (by - ay);
        double c2y = 3 * (cy - by) - c3y;
        double c1y = dy - ay - c3y - c2y;
		
		if (includeFirst) vc.vertex(ax, ay);
		for (int i=0; i<=subdiv; ++i) {
			u = i/subdiv;
			xx = u*(c3x + u*(c2x + u*c1x)) + ax;
			yy = u*(c3y + u*(c2y + u*c1y)) + ay;
			vc.vertex(xx, yy);
		}
		return subdiv + (includeFirst ? 2 : 1);
	}
    
    /**
	 * Draws a cardinal spline composed of piecewise connected cubic
	 * Bezier curves. Curve control points are inferred so as to ensure
	 * C1 continuity (continuous derivative).
	 * @param p double array defining a polygon or polyline to render with a
	 *  cardinal spline,
	 * @param s a tension parameter determining the spline's "tightness"
	 * @param closed indicates if the cardinal spline should be a closed
	 *  shape. False by default.
	 * @param vc callback to collect vertex values
	 */
	public static int cardinal(double[] p, int npts, double s,
		boolean closed, VertexCallback vc)
	{
		// compute the size of the path
        int len = (npts < 0 ? p.length : 2*npts), k=1;
        if (len < 6) {
        	throw new IllegalArgumentException(
        		"Cardinal splines require at least 3 points");
        }
        
        double dx1=0, dy1=0, dx2, dy2;
        vc.vertex(p[0], p[1]);
        
        // compute first control points
        if (closed) {
            dx2 = p[2] - p[len-2];
            dy2 = p[3] - p[len-1];
        } else {
            dx2 = p[4] - p[0];
            dy2 = p[5] - p[1];
        }

        // iterate through control points
        int i = 0;
        for (i=2; i<len-2; i+=2) {
            dx1 = dx2; dy1 = dy2;
            dx2 = p[i+2] - p[i-2];
            dy2 = p[i+3] - p[i-1];
            k += cubic(p[i-2], p[i-1], p[i-2]+s*dx1, p[i-1]+s*dy1,
            	  p[i]-s*dx2, p[i+1]-s*dy2, p[i], p[i+1], false, vc);
        }
        
        // finish spline
        if (closed) {
            dx1 = dx2; dy1 = dy2;
            dx2 = p[0] - p[i-2];
            dy2 = p[1] - p[i-1];
            k += cubic(p[i-2], p[i-1], p[i-2]+s*dx1, p[i-1]+s*dy1,
            	  p[i]-s*dx2, p[i+1]-s*dy2, p[i], p[i+1], false, vc);
            
            dx1 = dx2; dy1 = dy2;
            dx2 = p[2] - p[len-2];
            dy2 = p[3] - p[len-1];
            k += cubic(p[len-2], p[len-1], p[len-2]+s*dx1, p[len-1]+s*dy1,
            	  p[0]-s*dx2, p[1]-s*dy2, p[0], p[1], false, vc);
        } else {
        	k += cubic(p[i-2], p[i-1], p[i-2]+s*dx1, p[i-1]+s*dy1,
        		  p[i]-s*dx2, p[i+1]-s*dy2, p[i], p[i+1], false, vc);
        }
        return k;
	}
    
	// -- b-spline ----------------------------------------------------

	/**
	 * Draws a cubic open uniform B-spline. The spline passes through the
	 * first and last control points, but not necessarily any others.
	 * @param p a double array of points defining the spline control points
	 * @param slack a slack parameter determining the "tightness" of the
	 *  spline. At value 1 (the default) a normal b-spline will be drawn,
	 *  at value 0 a straight line between the first and last points will
	 *  be drawn. Intermediate values interpolate smoothly between these
	 *  two extremes.
	 * @param includeFirst if true the first point will be included
	 * @param vc callback to collect vertex values
	 */
	public static int bspline(double[] p, int npts,
		boolean includeFirst, VertexCallback vc)
	{		
		int N = (npts < 0 ? p.length/2 : npts);
		int k = N<4 ? 3 : 4, nplusk = N+k;
		int i, j, s, subdiv = 40;
		double x, y, step, u;
		
		// if only two points, draw a line between them
		if (N==2) {
			if (includeFirst) vc.vertex(p[0], p[1]);
			vc.vertex(p[2], p[3]);
			return (includeFirst ? 2 : 1);
		}
		
		double[] _knot = new double[nplusk];
		double[] _basis = new double[nplusk];
		
		// initialize knot vector
		for (i=1, _knot[0]=0; i<nplusk; ++i) {
			_knot[i] = _knot[i-1] + (i>=k && i<=N ? 1 : 0);
		}
		
		// calculate the points on the bspline curve
		step = _knot[nplusk-1] / subdiv;
		for (s=0; s <= subdiv; ++s) {
			u = step * s;
			
			// calculate basis function -----
			for (i=0; i < nplusk-1; ++i) { // first-order
				_basis[i] = (u >= _knot[i] && u < _knot[i+1] ? 1 : 0);
			}
			for (j=2; j <= k; ++j) { // higher-order
				for (i=0; i < nplusk-j; ++i) {
					x = (_basis[i  ]==0 ? 0 : (u-(_knot[i])*_basis[i]) / (_knot[i+j-1]-_knot[i]));
					y = (_basis[i+1]==0 ? 0 : ((_knot[i+j]-u)*_basis[i+1]) / (_knot[i+j]-_knot[i+1]));
					_basis[i] = x + y;
				}
			}
			if (u == _knot[nplusk-1]) _basis[N-1] = 1; // last point
			
			// interpolate b-spline point -----
			for (i=0, j=0, x=0, y=0; i<N; ++i, j+=2) {
				x += _basis[i] * p[j];
				y += _basis[i] * p[j+1];
			}
			if (s==0) {
				if (includeFirst) vc.vertex(x, y);
			} else {
				vc.vertex(x, y);
			}
		}
		return subdiv + (includeFirst ? 1 : 0);
	}
	
    // -- convex hull ----------------------------------------------------
    
	/**
     * Computes the 2D convex hull of a set of points using Graham's
     * scanning algorithm. The algorithm has been implemented as described
     * in Cormen, Leiserson, and Rivest's Introduction to Algorithms.
     * 
     * The running time of this algorithm is O(n log n), where n is
     * the number of input points.
     * 
     * @param pts the input points in [x0,y0,x1,y1,...] order
     * @return the convex hull of the input points
     */
	public static double[] convexHull(double[] pts) {
		return convexHull(pts, pts.length);
	}
	
    /**
     * Computes the 2D convex hull of a set of points using Graham's
     * scanning algorithm. The algorithm has been implemented as described
     * in Cormen, Leiserson, and Rivest's Introduction to Algorithms.
     * 
     * The running time of this algorithm is O(n log n), where n is
     * the number of input points.
     * 
     * @param pts the input points in [x0,y0,x1,y1,...] order
     * @param len the length of the pts array to consider (2 * #points)
     * @return the convex hull of the input points
     */
    public static double[] convexHull(double[] pts, int len) {
        if (len < 6) {
            throw new IllegalArgumentException(
                    "Input must have at least 3 points");
        }
        int plen = len/2-1;
        float[] angles = new float[plen];
        int[] idx    = new int[plen];
        int[] stack  = new int[len/2];
        return convexHull(pts, len, angles, idx, stack);
    }
    
    /**
     * Computes the 2D convex hull of a set of points using Graham's
     * scanning algorithm. The algorithm has been implemented as described
     * in Cormen, Leiserson, and Rivest's Introduction to Algorithms.
     * 
     * The running time of this algorithm is O(n log n), where n is
     * the number of input points.
     * 
     * @param pts
     * @return the convex hull of the input points
     */
    public static double[] convexHull(double[] pts, int len, 
            float[] angles, int[] idx, int[] stack)
    {
        // check arguments
        int plen = len/2 - 1;
        if (len < 6) {
            throw new IllegalArgumentException(
                    "Input must have at least 3 points");
        }
        if (angles.length < plen || idx.length < plen || stack.length < len/2) {
            throw new IllegalArgumentException(
                    "Pre-allocated data structure too small");
        }
        
        int i0 = 0;
        // find the starting ref point: leftmost point with the minimum y coord
        for (int i=2; i<len; i += 2) {
            if (pts[i+1] < pts[i0+1]) {
                i0 = i;
            } else if (pts[i+1] == pts[i0+1]) {
                i0 = (pts[i] < pts[i0] ? i : i0);
            }
        }
        
        // calculate polar angles from ref point and sort
        for (int i=0, j=0; i<len; i+=2) {
            if (i == i0) continue;
            angles[j] = (float)Math.atan2(pts[i+1]-pts[i0+1], pts[i]-pts[i0]);
            idx[j++]  = i;
        }
        sort(angles,idx,plen);
        
        // toss out duplicated angles
        float angle = angles[0];
        int ti = 0, tj = idx[0];
        for (int i=1; i<plen; i++) {
            int j = idx[i];
            if (angle == angles[i]) {
                // keep whichever angle corresponds to the most distant
                // point from the reference point
                double x1 = pts[tj]   - pts[i0];
                double y1 = pts[tj+1] - pts[i0+1];
                double x2 = pts[j]    - pts[i0];
                double y2 = pts[j+1]  - pts[i0+1];
                double d1 = x1*x1 + y1*y1;
                double d2 = x2*x2 + y2*y2;
                if ( d1 >= d2 ) {
                    idx[i] = -1;
                } else {
                    idx[ti] = -1;
                    angle = angles[i];
                    ti = i;
                    tj = j;
                }
            } else {
                angle = angles[i];
                ti = i;
                tj = j;
            }
        }
        
        // initialize our stack
        int sp = 0;
        stack[sp++] = i0;
        int j = 0;
        for (int k=0; k<2; j++) {
            if (idx[j] != -1) {
                stack[sp++] = idx[j];
                k++;
            }
        }
        
        // do graham's scan
        for (; j < plen; j++) {
            if (idx[j] == -1) continue; // skip tossed out points
            while (isNonLeft(i0, stack[sp-2], stack[sp-1], idx[j], pts)) {
                sp--;
            }
            stack[sp++] = idx[j];
        }

        // construct the hull
        double[] hull = new double[2*sp];
        for ( int i=0; i<sp; i++ ) {
            hull[2*i]   = pts[stack[i]];
            hull[2*i+1] = pts[stack[i]+1];
        }
        
        return hull;
    }
    
    /**
     * Convex hull helper method for detecting a non left turn about 3 points
     */
    private static boolean isNonLeft(int i0, int i1, int i2, int i3, double[] pts) {
        double x, y, l1, l2, l4, l5, l6, angle1, angle2;
        
        y = pts[i2+1]-pts[i1+1]; x = pts[i2]-pts[i1]; l1 = x*x + y*y;
        y = pts[i3+1]-pts[i2+1]; x = pts[i3]-pts[i2]; l2 = x*x + y*y;
        y = pts[i3+1]-pts[i0+1]; x = pts[i3]-pts[i0]; l4 = x*x + y*y;
        y = pts[i1+1]-pts[i0+1]; x = pts[i1]-pts[i0]; l5 = x*x + y*y;
        y = pts[i2+1]-pts[i0+1]; x = pts[i2]-pts[i0]; l6 = x*x + y*y;

        angle1 = Math.acos((l2+l6-l4) / (2*Math.sqrt(l2*l6)) );
        angle2 = Math.acos((l6+l1-l5) / (2*Math.sqrt(l6*l1)) );
        return ((Math.PI-angle1) - angle2) <= 0.0;
    }
    
    // -- float / int sorting -------------------------------------------

    /**
     * Arrays with lengths beneath this value will be insertion sorted.
     */
    private static final int SORT_THRESHOLD = 30;
    
    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     */
    protected static final void sort(float[] a, int[] b) {
        mergesort(a, b, 0, a.length - 1);
    }

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param length the length of the range to be sorted
     */
    protected static final void sort(float[] a, int[] b, int length) {
        mergesort(a, b, 0, length - 1);
    }

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param begin the start index of the range to sort
     * @param end the end index, exclusive, of the range to sort
     */
    protected static final void sort(float[] a, int[] b, int begin, int end) {
        mergesort(a, b, begin, end - 1);
    }

    // -- Insertion Sort --

    protected static final void insertionsort(float[] a, int[] b, int p, int r) {
        for (int j = p + 1; j <= r; ++j) {
            float key = a[j];
            int val = b[j];
            int i = j - 1;
            while (i >= p && a[i] > key) {
                a[i + 1] = a[i];
                b[i + 1] = b[i];
                --i;
            }
            a[i + 1] = key;
            b[i + 1] = val;
        }
    }

    // -- Mergesort --

    protected static final void mergesort(float[] a, int[] b, int p, int r) {
        if (p >= r) {
            return;
        }
        if (r - p + 1 < SORT_THRESHOLD) {
            insertionsort(a, b, p, r);
        } else {
            int q = (p + r) / 2;
            mergesort(a, b, p, q);
            mergesort(a, b, q + 1, r);
            merge(a, b, p, q, r);
        }
    }

    protected static final void merge(float[] a, int[] b, int p, int q, int r) {
        float[] t = new float[r - p + 1];
        int[] v = new int[r - p + 1];
        int i, p1 = p, p2 = q + 1;
        for (i = 0; p1 <= q && p2 <= r; ++i) {
            if (a[p1] < a[p2]) {
                v[i] = b[p1];
                t[i] = a[p1++];
            } else {
                v[i] = b[p2];
                t[i] = a[p2++];
            }
        }
        for (; p1 <= q; ++p1, ++i) {
            v[i] = b[p1];
            t[i] = a[p1];
        }
        for (; p2 <= r; ++p2, ++i) {
            v[i] = b[p2];
            t[i] = a[p2];
        }
        for (i = 0, p1 = p; i < t.length; i++, p1++) {
            b[p1] = v[i];
            a[p1] = t[i];
        }
    }
	
}
