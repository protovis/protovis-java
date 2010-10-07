package pv.benchmark.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.Callable;

import javax.media.opengl.GL;

import pv.mark.constants.Interpolate;
import pv.render.awt.gl.Images;
import pv.scene.DotItem;
import pv.scene.GroupItem;
import pv.scene.ImageItem;
import pv.scene.Item;
import pv.scene.LinkItem;
import pv.style.Fill;
import pv.style.Stroke;
import pv.style.Fill.Solid;
import pv.util.Geometry;
import pv.util.Geometry.VertexCallback;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;

public class RenderTask implements Callable<RenderTask>, VertexCallback {

	// items to process
	protected GroupItem group;
	protected int start, end;
	
	// manage geometry
	protected FloatBuffer vb = BufferUtil.newFloatBuffer(1024*8*2);
	protected IntBuffer cb = BufferUtil.newIntBuffer(1024*8);
	protected FloatBuffer tb;
	
	// pre-process, collect geometry
	public RenderTask call() throws Exception {
		process();
		return this;
	}
	
	public void process() {}
	
	// send data to graphics card
	public void render(GL gl) {}
	
	public RenderTask init(GroupItem group, int start, int end) {
		vb.position(0);
		cb.position(0);
		this.group = group;
		this.start = start;
		this.end = end;
		return this;
	}
	
	// -----
	
	public void clear() { this.group = null; }
	
	protected void vertex(float x, float y) { vb.put(x).put(y); }
	public void vertex(double x, double y) { vb.put((float)x).put((float)y); }
	
	protected void tcoord(float x, float y) { tb.put(x).put(y); }
	protected void tcoord(double x, double y) { tb.put((float)x).put((float)y); }
	
	protected static void fill(Fill fill, double alpha, GL gl) {
		float r=1, g=1, b=1, a=0;
		if (fill != null) {
			Solid sf = (Solid) fill;
			int rgb = sf.color();
			r = (0xFF & (rgb >> 16)) / 255f;
			g = (0xFF & (rgb >> 8)) / 255f;
			b = (0xFF & rgb) / 255f;
			a = (float)(sf.alpha() * alpha);
		}
		gl.glColor4f(r, g, b, a);
	}

	protected void fill(Fill fill, double alpha) {
		cb.put(color(fill, alpha));
	}
	
	protected void fill(Fill fill, double alpha, int reps) {
		int c = color(fill, alpha);
		while (--reps >= 0) cb.put(c);
	}
	
	protected static void stroke(Stroke s, double alpha, GL gl) {
		gl.glLineWidth((float) s.width());
		fill(s.fill(), alpha, gl);
	}
	
	protected void stroke(Stroke stroke, double alpha) {
		cb.put(color(stroke.fill(), alpha));
	}
	
	protected void stroke(Stroke stroke, double alpha, int reps) {
		int c = color(stroke.fill(), alpha);
		while (--reps >= 0) cb.put(c);
	}
	
	protected static int color(Fill fill, double alpha) {
		Solid sf = (Solid) fill;
		int argb = (fill==null ? 0x00ffffff : sf.color());
		int abgr = (argb & 0xFF00FF00)
			| ((argb & 0x000000FF) << 16)
			| ((argb & 0x00FF0000) >> 16);
		if (alpha < 1) {
			int a = (int)(255 * sf.alpha() * alpha);
			abgr = (a << 24) | (0x00FFFFFF & abgr);
		}
		return abgr;
	}
	
	// ------------------------------------------------------------------------
	
	private final static int LINEAR = 0;
	private final static int STEP_BEFORE = 1;
	private final static int STEP_AFTER = 2;
	public static int interpolateCode(String interp) {
		return interp==Interpolate.StepAfter ? 2
				: interp==Interpolate.StepBefore ? 1 : 0;
	}
	
	private static double[] lineIntersect(double o1x, double o1y,
		double d1x, double d1y, double o2x, double o2y, double d2x, double d2y)
	{
		double denom = (d1x * -d2y) + (d1y * d2x);
		double ss = ((o2x-o1x) * -d2y) + ((o2y-o1y) * d2x); 
		double[] v = new double[2];
		v[0] = o1x + (d1x * ss) / denom;
		v[1] = o1y + (d1y * ss) / denom;
		return v;
	}
	
	// ------------------------------------------------------------------------
	
	public static class Cardinal extends RenderTask {		
		public Cardinal() {
			vb = BufferUtil.newFloatBuffer(1024*8*2*50);
		}
		
		public void process() {
			List<Item> items = group.items();
			int len = items.size();
			
			double[] p = new double[len<<1];
			for (int i=0; i<len; ++i) {
				Item item = items.get(i);
				p[(i<<1)  ] = item.left;
				p[(i<<1)+1] = item.top;
			}
			vb.position(0);
			Geometry.cardinal(p, len, 0.15, false, this);
		}
		public void render(GL gl) {
			int count = vb.position() >> 1;
			vb.position(0);
			gl.glColor3f(0, 0, 0.7f);
			gl.glLineWidth(1);
			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			gl.glVertexPointer(2, GL.GL_FLOAT, 0, vb);
			gl.glDrawArrays(GL.GL_LINE_STRIP, 0, count);
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		}
	}
	
	public static class LayerBegin extends RenderTask {
		public void process() {
		}
		public void render(GL gl) {
			if (group.left != 0 || group.top != 0) {
				gl.glPushMatrix();
				gl.glTranslated(group.left, group.top, 0);
			}
			//gl.glGetFloatv(GL.GL_MODELVIEW, _matrix, 0);
			
			// draw panel background / border
			if (group.fill != null || group.stroke != null) {
				double l = group.left, r = group.left+group.width;
				double t = group.top,  b = group.top+group.height;
				vertex(l, t); vertex(r, t);
				vertex(r, b); vertex(l, b);
				
				vb.position(0);
				gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
				gl.glVertexPointer(2, GL.GL_FLOAT, 0, vb);
				
				if (group.fill != null) {
					fill(group.fill, group.alpha, gl);
					gl.glDrawArrays(GL.GL_QUADS, 0, 4);
				}
				if (group.stroke != null) {
					stroke(group.stroke, group.alpha, gl);
					gl.glDrawArrays(GL.GL_LINE_LOOP, 0, 4);
				}
				gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
			}
			
		}
	}

	public static class LayerFinish extends RenderTask {
		public void process() {
		}
		public void render(GL gl) {	
			group.dirty(false);
			
			// translate origin back
			if (group.left != 0 || group.top != 0) {
				gl.glPopMatrix();
				//gl.glGetFloatv(GL.GL_MODELVIEW, _matrix, 0);
			}
			group.computeBounds();
		}
	}

	public static class Bar extends RenderTask {
		public void process() {
			List<Item> items = group.items;
			for (int i=start; i<end; ++i) {
				Item item = items.get(i);
				if (!item.visible || item.alpha==0) continue;
				boolean fb = item.fill != null;
				boolean sb = item.stroke != null;
				if (!(fb || sb)) continue;
				
				double l = item.left, r = item.left+item.width;
				double t = item.top, b = item.top+item.height;
				vertex(l,t); vertex(r,t); vertex(r,b); vertex(l,b);
			}
			if (group.size()==end && group.interactive())
				group.computeBounds();
		}
		public void render(GL gl) {			
			vb.position(0);
			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			gl.glVertexPointer(2, GL.GL_FLOAT, 0, vb);
			
			List<Item> items = group.items;
			for (int i=start, vi=0; i<end; ++i, vi+=4) {
				Item item = items.get(i);
				if (!item.visible || item.alpha==0)
					continue;
				if (item.fill != null) {
					fill(item.fill, item.alpha, gl);
					gl.glDrawArrays(GL.GL_QUADS, vi, 4);
				}
				if (item.stroke != null) {
					stroke(item.stroke, item.alpha, gl);
					gl.glDrawArrays(GL.GL_LINE_LOOP, vi, 4);
				}
			}
			
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		}
	}
	
	public static class Circle extends RenderTask {
		protected int[] ib = new int[100000]; //new int[1000];
		protected int[] fb = new int[100000]; //new int[1000];
		protected int count;
		
		public Circle() {
			vb = BufferUtil.newFloatBuffer(1024*100*2*30);
			cb = BufferUtil.newIntBuffer(1024*100*30);
		}
		
		public void process() {
			List<Item> items = group.items;
			fb[0] = 0; count = 0;
			
			for (int i=start; i<end; ++i)
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
			if (group.size()==end && group.interactive())
				group.computeBounds();
		}
		public void render(GL gl) {
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
	
	public static class Link extends RenderTask {
		public Link() {
			vb = BufferUtil.newFloatBuffer(1024*1024*2*30);
			cb = BufferUtil.newIntBuffer(1024*1024*30);
		}
		
		public void process() {
			List<Item> items = group.items;
			for (int i=start; i<end; ++i) {
				LinkItem link = (LinkItem) items.get(i);
				if (!link.visible || link.alpha==0 || link.stroke==null)
					return;
				
				double px = link.targetX - link.sourceX; 
			    double py = link.targetY - link.sourceY;
			    double vx = -py, vy = px;
			    double ss = 1.0 / Math.sqrt(vx*vx + vy*vy);
			    double w = link.stroke.width() / 2;
			    vx *= w*ss; vy *= w*ss;
			    
			    stroke(link.stroke, link.alpha, 4);
			    vertex(link.sourceX+vx, link.sourceY+vy);
			    vertex(link.sourceX-vx, link.sourceY-vy);
			    vertex(link.targetX-vx, link.targetY-vy);
			    vertex(link.targetX+vx, link.targetY+vy);
			}
		}
		public void render(GL gl) {
			int count = vb.position() >> 1;
			
			vb.position(0);
			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			gl.glVertexPointer(2, GL.GL_FLOAT, 0, vb);
			
			cb.position(0);
			gl.glEnableClientState(GL.GL_COLOR_ARRAY);
			gl.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, cb);
			
			gl.glDrawArrays(GL.GL_QUADS, 0, count);
			
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL.GL_COLOR_ARRAY);
		}
	}
	
	public static class Path extends RenderTask {
		public void process() {
			List<Item> items = group.items();
			if (items.size() < 2) return;
			boolean segment = group.segmented;
			int interp = interpolateCode(group.interpolate);
			
			// set up width/color for non-segmented case
			double lw = 0; Fill lc = null;
			if (!segment) {
				Item first = items.get(start);
				if (first.stroke == null) return;
				lw = first.stroke.width() / 2;
				lc = first.stroke.fill();
				if (lw <= 0 || lc == null) return;
			}
			
			vb.clear();
			cb.clear();
			
			int n = items.size() - 1;
			for (int i=start; i<(end-1); ++i)
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
		}
		public void render(GL gl) {
			int count = vb.position()>>1;
			vb.position(0); cb.position(0);
			
			gl.glColor3f(0, 0, 0.6f);
			
			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL.GL_COLOR_ARRAY);
			
			gl.glVertexPointer(2, GL.GL_FLOAT, 0, vb);
			gl.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, cb);
			
			gl.glDrawArrays(GL.GL_QUADS, 0, count);
			
			gl.glDisableClientState(GL.GL_COLOR_ARRAY);
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		}
	}
	
	public static class Rule extends RenderTask {
		public void process() {
			List<Item> items = group.items();
			for (int i=start; i<end; ++i) {
				Item item = items.get(i);
				if (!item.visible || item.alpha==0 || item.stroke==null)
					continue;
				stroke(item.stroke, item.alpha, 2);
				vertex(item.left, item.top);
				vertex(item.left + item.width, item.top + item.height);
			}
		}
		public void render(GL gl) {
			int count = vb.position() >> 1;
			
			vb.position(0);
			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			gl.glVertexPointer(2, GL.GL_FLOAT, 0, vb);
			
			cb.position(0);
			gl.glEnableClientState(GL.GL_COLOR_ARRAY);
			gl.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, cb);
			
			gl.glDrawArrays(GL.GL_LINES, 0, count);
			
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL.GL_COLOR_ARRAY);
		}
	}
	
	public static class Image extends RenderTask {
		private GL gl;
		public void process() {
			Images imgs = Images.instance();
			List<Item> items = group.items;
			
			for (int i=start; i<end; ++i) {
				ImageItem item = (ImageItem) items.get(i);
				if (!item.visible || item.alpha==0) continue;
				
				Texture x = imgs.getTexture(item.url, gl);
				if (x == null) continue;
				
				// calculate image dimensions
				float w, h;
				if (item.width > 0 && item.height > 0) {
					w = (float) item.width;
					h = (float) item.height;
				} else if (item.width > 0) {
					w = (float) item.width;
					h = (w * x.getImageHeight()) / x.getImageWidth();
				} else if (item.height > 0) {
					h = (float) item.height;
					w = (h * x.getImageWidth()) / x.getImageHeight();
				} else {
					w = x.getImageWidth();
					h = x.getImageHeight();
				}
				
				// render the image
				float l = (float)(item.left - 0.5*w);
				float r = (float)(item.left + 0.5*w);
				float t = (float)(item.top - 0.5*h);
				float b = (float)(item.top + 0.5*h);
				vertex(l, b); vertex(r, b); vertex(r, t); vertex(l, t);
				
				TextureCoords c = x.getImageTexCoords();
				l = c.left(); r = c.right();
				t = c.top();  b = c.bottom();
				tcoord(l, b); tcoord(r, b); tcoord(r, t); tcoord(l, t);
			}
		}
		public void render(GL gl) {
			Images imgs = Images.instance();
			List<Item> items = group.items;
			
			gl.glColor4f(1, 1, 1, 1);
			gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
			
			for (int i=start, j=(start<<2); i<end; ++i, j+=4) {
				ImageItem item = (ImageItem) items.get(i);
				if (!item.visible || item.alpha==0) continue;
				Texture x = imgs.getTexture(item.url, gl);
				x.bind();
				
				gl.glDrawArrays(GL.GL_QUADS, j, 4);
			}
			
			gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
			gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
			
		}
//		public void render(Item group, GL gl) {
//			List<Item> items = group.items();
//			
//			gl.glColor4f(1, 1, 1, 1);
//			for (int i=0; i<items.size(); ++i) {
//				ImageItem item = (ImageItem) items.get(i);
//				if (!item.visible || item.alpha==0) continue;
//				
//				// get image texture, skip if none
//				Texture x = _img.getTexture(item.url, gl);
//				if (x == null) continue;
//				
//				// calculate image dimensions
//				float w, h;
//				if (item.width > 0 && item.height > 0) {
//					w = (float) item.width;
//					h = (float) item.height;
//				} else if (item.width > 0) {
//					w = (float) item.width;
//					h = (w * x.getImageHeight()) / x.getImageWidth();
//				} else if (item.height > 0) {
//					h = (float) item.height;
//					w = (h * x.getImageWidth()) / x.getImageHeight();
//				} else {
//					w = x.getImageWidth();
//					h = x.getImageHeight();
//				}
//				
//				// render the image
//				float l = (float)(item.left - 0.5*w);
//				float r = (float)(item.left + 0.5*w);
//				float t = (float)(item.top - 0.5*h);
//				float b = (float)(item.top + 0.5*h);
//				TextureCoords c = x.getImageTexCoords();
//				x.bind();
//				gl.glBegin(GL.GL_QUADS);
//				gl.glTexCoord2f(c.left(),  c.bottom()); gl.glVertex2f(l,b);
//				gl.glTexCoord2f(c.right(), c.bottom()); gl.glVertex2f(r,b);
//				gl.glTexCoord2f(c.right(), c.top());    gl.glVertex2f(r,t);
//				gl.glTexCoord2f(c.left(),  c.top());    gl.glVertex2f(l,t);
//				gl.glEnd();
//			}
//			gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
//		}
	}
}
