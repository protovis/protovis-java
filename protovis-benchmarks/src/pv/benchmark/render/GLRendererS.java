package pv.benchmark.render;


import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;

import pv.mark.constants.Interpolate;
import pv.mark.constants.MarkType;
import pv.render.awt.gl.GLRenderer;
import pv.scene.DotItem;
import pv.scene.GroupItem;
import pv.scene.Item;
import pv.scene.PanelItem;
import pv.style.Fill;
import pv.style.Stroke;
import pv.style.Fill.Solid;
import pv.util.Geometry;
import pv.util.Objects;
import pv.util.Geometry.VertexCallback;

import com.sun.opengl.util.BufferUtil;

public class GLRendererS extends GLRenderer {
	
	private static GLRendererS s_instance = null;
	public static GLRendererS instance() {
		if (s_instance == null) s_instance = new GLRendererS();
		return s_instance;
	}
	
	private final Map<String,GroupRenderer> _map
		= new HashMap<String,GroupRenderer>();
	
	// Allocate 1MB for vertices and 0.5MB for colors
	private FloatBuffer vb = BufferUtil.newFloatBuffer(1024*8*2*50);
	private IntBuffer cb = BufferUtil.newIntBuffer(1024*8*50);
	
	//private float[] _matrix = {1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
	private Fill _curFill = null;
	private double _curAlpha = Double.NaN;
	
	private GL gl;
	
	public GLRendererS() {
		_map.put(MarkType.Bar,   new BarRenderer());
		_map.put(MarkType.Dot,   new CircleRenderer());
		_map.put(MarkType.Line,  new PathRenderer());
	}
	
	public void init(GL gl, float[] trans)
	{
		this.gl = gl;
		this._zoom = (float) Math.sqrt(trans[0]*trans[0] + trans[4]*trans[4]);
		if (_zoom < 0.1f) _zoom = 0.1f;
	}
		
	public void render(GL gl, PanelItem layer) {
		renderPanel(gl, layer);
	}
	
	public void renderPanel(GL gl, PanelItem layer) {
		_curFill = null;
		//_tess.init(gl, _glu);
		//FBOTexture tex = null;
		
		// translate origin as needed
		boolean translate = layer.left != 0 || layer.top != 0;
		if (translate) {
			gl.glPushMatrix();
			gl.glTranslated(layer.left, layer.top, 0);
		}
		//gl.glGetFloatv(GL.GL_MODELVIEW, _matrix, 0);
		
		// draw panel background / border
		if (layer.fill != null || layer.stroke != null) {
			double l = layer.left, r = layer.left+layer.width;
			double t = layer.top,  b = layer.top+layer.height;
			clear();
			vertex(l, t); vertex(r, t); vertex(r, b); vertex(l, b);
			if (layer.fill != null) {
				fill(layer.fill, layer.alpha);
				shape(GL.GL_QUADS);
			}
			if (layer.stroke != null) {
				stroke(layer.stroke, layer.alpha);
				shape(GL.GL_LINE_LOOP);
			}
		}
		
		// recursively render groups
		List<GroupItem> items = preprocess(layer.items);
		GroupRenderer gr = null; String type = null;
		for (int i=0; i<items.size(); ++i)
		{
			GroupItem group = items.get(i);
			if (group == null) continue; // XXX revisit to prevent
			
			// lookup renderer if we switch types
			if (type != group.type) {
				type = group.type;
				gr = _map.get(type);
			}	
			if (group instanceof PanelItem) {
				renderPanel(gl, (PanelItem)group);
			} else if (group.type == MarkType.Panel) {
				List<GroupItem> layers = preprocess(group.items);
				for (Item item : layers) {
					renderPanel(gl, (PanelItem)item);
				}
				Objects.List.reclaim(layers);
				//if (group.interactive())
				//	group.computeBounds();
			} else if (group.visible) {
				gr.render(group, gl);
				//if (group.interactive())
				//	group.computeBounds();
			}
		}
		Objects.List.reclaim(items);
		
		
		layer.dirty(false);
		
		// translate origin back
		if (translate) {
			gl.glPopMatrix();
			//gl.glGetFloatv(GL.GL_MODELVIEW, _matrix, 0);
		}
		layer.computeBounds();
	}
	
	// -----
	
	private final void shape(final int type) {
		int count = vb.position()>>1;
		vb.position(0);
		gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
		gl.glVertexPointer(2, GL.GL_FLOAT, 0, vb);
		gl.glDrawArrays(type, 0, count);
		gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		vb.position(count<<1);
	}
	
	private final void paint(final int type) {
		int count = vb.position()>>1;
		vb.position(0); cb.position(0);
		gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL.GL_COLOR_ARRAY);
		gl.glVertexPointer(2, GL.GL_FLOAT, 0, vb);
		gl.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, cb);
		gl.glDrawArrays(type, 0, count);
		gl.glDisableClientState(GL.GL_COLOR_ARRAY);
		gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		vb.position(count<<1);
		cb.position(count);
	}
	
	private final void clear() {
		vb.clear(); cb.clear();
	}
	
	private final void vertex(final float x, final float y) {
		vb.put(x).put(y);
	}
	
	private final void vertex(final double x, final double y) {
		vb.put((float)x).put((float)y);
	}
	
	private final void fill(final Fill fill, final double alpha) {
		if (fill == _curFill && alpha == _curAlpha) return;
		_curFill = fill;
		_curAlpha = alpha;
		
		if (fill == null) {
			gl.glColor4f(1, 1, 1, 0);
			return;
		}
		Solid sf = (Solid)fill;
		int rgb = sf.color();
		float r = (0xFF & (rgb >> 16)) / 255f;
		float g = (0xFF & (rgb >> 8)) / 255f;
		float b = (0xFF & rgb) / 255f;
		float a = (float)(sf.alpha() * alpha);
		gl.glColor4f(r, g, b, a);
	}
	
	private final void fill(final Fill fill, final double alpha, final int reps) {
		Solid sf = (Solid) fill;
		int argb = (fill==null ? 0x00ffffff : sf.color());
		int abgr = (argb & 0xFF00FF00)
			| ((argb & 0x000000FF) << 16)
			| ((argb & 0x00FF0000) >> 16);
		if (alpha < 1) {
			int a = (int)(255 * sf.alpha() * alpha);
			abgr = (a << 24) | (0x00FFFFFF & abgr);
		}
		for (int i=0; i<reps; ++i) {
			cb.put(abgr);
		}
	}
	
	private final void stroke(Stroke s, double alpha) {
		gl.glLineWidth((float)s.width());
		fill(s.fill(), alpha);
	}
	
	private final void stroke(Stroke s, double alpha, int reps) {
		fill(s.fill(), alpha, reps);
	}
	
	private VertexCallback vc = new VertexCallback() {
		public void vertex(double x, double y) {
			vb.put((float)x).put((float)y);
		}
	};
	
	// -- Mark Renderers ------------------------------------------------------
	
	abstract class GroupRenderer {
		public abstract void render(Item group, GL gl);
	}
	
	class CardinalRenderer extends GroupRenderer {
		public void render(Item group, GL gl) {
			List<Item> items = group.items();
			int len = items.size();
			
			double[] p = new double[len<<1];
			for (int i=0; i<len; ++i) {
				Item item = items.get(i);
				p[(i<<1)  ] = item.left;
				p[(i<<1)+1] = item.top;
			}
			
			clear();
			int k = Geometry.cardinal(p, len, 0.15, false, vc);
			
			// RENDER
			gl.glColor3f(0, 0, 0.7f);
			gl.glLineWidth(1);
			
			vb.position(0);
			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			gl.glVertexPointer(2, GL.GL_FLOAT, 0, vb);
			gl.glDrawArrays(GL.GL_LINE_STRIP, 0, k);
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		}
	}
	
	class CircleRenderer extends GroupRenderer {
		protected int[] ib = new int[1000];
		protected int[] fb = new int[1000];
		protected int count;
		
		public void render(Item group, GL gl) {
			List<Item> items = group.items();
			fb[0] = 0;
			count = 0;
			clear();
			
			for (int i=0; i<items.size(); ++i)
			{
				DotItem item = (DotItem) items.get(i);
				if (!item.visible || item.alpha==0) continue;
				if (item.stroke == null) continue;
				
				float x = (float) item.left;
				float y = (float) item.top;
				float size = (float) item.radius;
				float scale = (float) 1.0f; // get display zoom
				
				int slices = (int)(2.0 * Math.PI / (Math.acos(1 - 0.25 / (scale*size))));
				float xx = size; // we start at angle = 0 
				float yy = 0; 
			    
				for (int j=0; j < slices; ++j) 
				{
					double a = 2*Math.PI*j / slices;
					xx = (float)(x + size * Math.cos(a));
					yy = (float)(y + size * Math.sin(a));
					vertex(xx, yy);
				}
				
				stroke(item.stroke, item.alpha, slices);
				ib[i] = slices;
				if (i > 0) fb[i] = fb[i-1] + ib[i-1];
				count += 1;
			}
			if (group.interactive()) {
				((GroupItem)group).computeBounds();
			}
			
			// RENDER
			vb.position(0);
			cb.position(0);
			
			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL.GL_COLOR_ARRAY);
			
			gl.glVertexPointer(2, GL.GL_FLOAT, 0, vb);
			gl.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, cb);
			
			gl.glMultiDrawArrays(GL.GL_LINE_LOOP, fb, 0, ib, 0, count);
			
			gl.glDisableClientState(GL.GL_COLOR_ARRAY);
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		}
	}
	class BarRenderer extends GroupRenderer {
		public void render(Item group, GL gl) {
			List<Item> items = group.items();
			boolean fb = false, sb = false;
			
			for (int i=0; i<items.size(); ++i) {
				Item item = items.get(i);
				if (!item.visible) continue;
				fb = item.fill != null;
				sb = item.stroke != null;
				if (!(fb || sb)) continue;
				
				double l = item.left, r = item.left+item.width;
				double t = item.top, b = item.top+item.height;
				vertex(l,t); vertex(r,t); vertex(r,b); vertex(l,b);
			}
			
			if (group.interactive())
				((GroupItem)group).computeBounds();
			
			vb.position(0);
			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			gl.glVertexPointer(2, GL.GL_FLOAT, 0, vb);
			
			for (int i=0, vi=0; i<items.size(); ++i, vi+=4) {
				Item item = items.get(i);
				if (!item.visible) continue;
				fb = item.fill != null;
				sb = item.stroke != null;
				if (!(fb || sb)) continue;
				
				if (fb) {
					fill(item.fill, item.alpha);
					gl.glDrawArrays(GL.GL_QUADS, vi, 4);
				}
				if (sb) {
					stroke(item.stroke, item.alpha);
					gl.glDrawArrays(GL.GL_LINE_LOOP, vi, 4);
				}
			}
			
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
			clear();
		}
	}
	class PathRenderer extends GroupRenderer {
		public void render(Item group, GL gl) {
			List<Item> items = group.items();
			if (items.size() == 0) return;
			
			Item first = items.get(0);
			boolean sb = first.stroke != null;
			
			if (sb) {
				GroupItem g = (GroupItem)group;
				int itp = interpolateCode(g.interpolate);
				miterJoin(gl, items, g.segmented, itp);
			}
		}
		public void miterJoin(GL gl, List<Item> items, boolean segment, int interp) {
			if (items.size() < 2) return;
			
			// set up width/color for non-segmented case
			double lw = 0; Fill lc = null;
			if (!segment) {
				Item first = items.get(0);
				if (first.stroke == null) return;
				lw = first.stroke.width() / 2;
				lc = first.stroke.fill();
				if (lw <= 0 || lc == null) return;
				fill(lc, first.alpha);
			}
			
			clear();
			for (int i=0, n=items.size()-1; i<n; ++i)
			{
				Item s1 = items.get(i), s2 = items.get(i+1), s = null;
				
				// visible
			    if (!s1.visible || !s2.visible) continue;
			    if (s1.stroke == null) return;
			    
			    // P1-P2 is the current line segment. V is a vector that is perpendicular to
			    // the line segment, and has length lineWidth / 2. ABCD forms the initial
			    // bounding box of the line segment (i.e., the line segment if we were to do
			    // no joins).
			    double px = s2.left - s1.left;
			    double py = s2.top - s1.top;
			    double vx = -py, vy = px;
			    double ss = 1.0 / Math.sqrt(vx*vx + vy*vy); vx *= ss; vy *= ss;
			    double l1, l2;
			    if (!segment) {
			    	l1 = l2 = lw;
			    } else if (interp == STEP_BEFORE) {
			    	l1 = l2 = s1.stroke.width()/2;
			    } else if (interp == STEP_AFTER) {
			    	l1 = l2 = s2.stroke.width()/2;
			    } else {
			    	l1 = s1.stroke.width()/2; l2 = s2.stroke.width()/2;
			    }
			    
			    double ax = s1.left + vx*l1, ay = s1.top + vy*l1;
			    double bx = s2.left + vx*l2, by = s2.top + vy*l2;
			    double cx = s2.left - vx*l2, cy = s2.top - vy*l2;
			    double dx = s1.left - vx*l1, dy = s1.top - vy*l1;
			    			  
			    // Start join. P0 is the previous line segment's start point. We define the
			    // cutting plane as the average of the vector perpendicular to P0-P1, and
			    // the vector perpendicular to P1-P2. This insures that the cross-section of
			    // the line on the cutting plane is equal if the line-width is unchanged.
			    // Note that we don't implement miter limits, so these can get wild.
			    if (i > 0) {
			    	Item s0 = items.get(i-1);
			    	if (s0.visible) {
			    		double v1x = -(s1.top - s0.top), v1y = s1.left - s0.left;
			    		ss = 1.0 / Math.sqrt(v1x*v1x + v1y*v1y);
			    		v1x = v1x*ss + vx; v1y = v1y*ss + vy;
			    		double d[] = lineIntersect(s1.left, s1.top, v1x, v1y, dx, dy, px, py);
			    		double a[] = lineIntersect(s1.left, s1.top, v1x, v1y, ax, ay, px, py);
			    		dx = d[0]; dy = d[1]; ax = a[0]; ay = a[1];
			    	}
			    }

			    // Similarly, for end join.
			    if (i < (n-1)) {
			    	Item s3 = items.get(i+2);
			    	if (s3.visible) {
			    		double v2x = -(s3.top - s2.top), v2y = s3.left - s2.left;
			    		ss = 1.0 / Math.sqrt(v2x*v2x + v2y*v2y);
			    		v2x = v2x*ss + vx; v2y = v2y*ss + vy;
			    		double c[] = lineIntersect(s2.left, s2.top, v2x, v2y, cx, cy, px, py);
			    		double b[] = lineIntersect(s2.left, s2.top, v2x, v2y, bx, by, px, py);
			    		cx = c[0]; cy = c[1]; bx = b[0]; by = b[1];
			    	}
			    }
			    
			    if (segment) {
			    	s = (interp==STEP_AFTER ? s2 : s1);
			    	stroke(s.stroke, s.alpha, 2);
			    }
			    vertex(dx, dy); vertex(ax, ay);
			    if (segment) {
			    	s = (interp==LINEAR ? s2 : s);
			    	stroke(s.stroke, s.alpha, 2);
			    }
			    vertex(bx, by); vertex(cx, cy);
			}
			if (segment) paint(GL.GL_QUADS); else shape(GL.GL_QUADS);
			clear();
		}
	}
	
	// -- Utility methods -----------------------------------------------------
	
	private final static int LINEAR = 0;
	private final static int STEP_BEFORE = 1;
	private final static int STEP_AFTER = 2;
	public static int interpolateCode(String interp) {
		return interp==Interpolate.StepAfter ? 2
				: interp==Interpolate.StepBefore ? 1 : 0;
	}
	
	public static double[] lineIntersect(double o1x, double o1y,
		double d1x, double d1y, double o2x, double o2y, double d2x, double d2y)
	{
		double denom = (d1x * -d2y) + (d1y * d2x);
		double ss = ((o2x-o1x) * -d2y) + ((o2y-o1y) * d2x); 
		double[] v = new double[2];
		v[0] = o1x + (d1x * ss) / denom;
		v[1] = o1y + (d1y * ss) / denom;
		return v;
	}
	
}