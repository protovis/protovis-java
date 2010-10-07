package pv.render.awt.gl;


import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;

import pv.mark.constants.Interpolate;
import pv.mark.constants.MarkType;
import pv.mark.constants.Shape;
import pv.mark.constants.TextAlign;
import pv.mark.constants.TextBaseline;
import pv.render.AbstractRenderer;
import pv.render.awt.Fonts;
import pv.scene.DotItem;
import pv.scene.GroupItem;
import pv.scene.ImageItem;
import pv.scene.Item;
import pv.scene.LabelItem;
import pv.scene.PanelItem;
import pv.scene.LinkItem;
import pv.scene.RuleItem;
import pv.scene.WedgeItem;
import pv.style.Fill;
import pv.style.Font;
import pv.style.Stroke;
import pv.style.Fill.Solid;
import pv.util.Objects;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;

public class GLRenderer extends AbstractRenderer {
	
	private static GLRenderer s_instance = null;
	public static GLRenderer instance() {
		if (s_instance == null) s_instance = new GLRenderer();
		return s_instance;
	}
	
	private final Map<String,GroupRenderer> _map
		= new HashMap<String,GroupRenderer>();
	
	// Allocate 1MB for vertices and 0.5MB for colors
	private FloatBuffer vb = BufferUtil.newFloatBuffer(1024*1024);
	private IntBuffer cb = BufferUtil.newIntBuffer(512*1024);
	
	public float _zoom;
	private float[] _matrix = {1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
	private Fill _curFill = null;
	private double _curAlpha = Double.NaN;
	private FrameBuffer _fbo;
	
	private GL gl;
	private GLU _glu = new GLU();
	private Tessellator _tess = new Tessellator();
	
	public GLRenderer() {
		_map.put(MarkType.Area,  new AreaRenderer());
		_map.put(MarkType.Bar,   new BarRenderer());
		_map.put(MarkType.Dot,   new DotRenderer());
		_map.put(MarkType.Image, new ImageRenderer());
		_map.put(MarkType.Label, new LabelRenderer());
		_map.put(MarkType.Link,  new LinkRenderer());
		_map.put(MarkType.Line,  new LineRenderer());
		_map.put(MarkType.Rule,  new RuleRenderer());
		_map.put(MarkType.Wedge, new WedgeRenderer());
	}
	
	public void init(GL gl, float[] trans)
	{
		this.gl = gl;
		this._zoom = (float) Math.sqrt(trans[0]*trans[0] + trans[4]*trans[4]);
		if (_zoom < 0.1f) _zoom = 0.1f;
	}
		
	public void render(GL gl, PanelItem panel) {
		renderPanel(gl, panel);
	}
	
	public void renderPanel(GL gl, PanelItem panel) {
		_curFill = null;
		_tess.init(gl, _glu);
		FBOTexture tex = null;
		
		// translate origin as needed
		boolean translate = panel.left != 0 || panel.top != 0;
		if (translate) {
			gl.glPushMatrix();
			gl.glTranslated(panel.left, panel.top, 0);
		}
		gl.glGetFloatv(GL.GL_MODELVIEW, _matrix, 0);
		
		if (panel.cache) {
			// TODO handle dirty bit, type-safety of texture
			if (!panel.dirty() && panel.cacheData != null) {
				((FBOTexture)panel.cacheData).draw(gl, 0, 0);
				if (translate) {
					gl.glPopMatrix();
					gl.glGetFloatv(GL.GL_MODELVIEW, _matrix, 0);
				}
				return;
			} else {
				// setup render to texture
				if (_fbo == null) {
					_fbo = new FrameBuffer(gl);
				}
				if (panel.cacheData == null) {
					tex = _fbo.createTexture(gl,
						(int)(panel.width+0.5), (int)(panel.height+0.5));
					tex.init(gl);
					panel.cacheData = tex;
				} else {
					tex = (FBOTexture) panel.cacheData;
				}
				tex.begin(gl, true);
				gl.glMatrixMode(GL.GL_MODELVIEW);
				gl.glPushMatrix();
				gl.glLoadIdentity();
			}
		}
		
		// draw panel background / border
		if (panel.fill != null || panel.stroke != null) {
			double l = panel.left, r = panel.left+panel.width;
			double t = panel.top,  b = panel.top+panel.height;
			clear();
			vertex(l, t); vertex(r, t); vertex(r, b); vertex(l, b);
			if (panel.fill != null) {
				fill(panel.fill, panel.alpha);
				shape(GL.GL_QUADS);
			}
			if (panel.stroke != null) {
				stroke(panel.stroke, panel.alpha);
				shape(GL.GL_LINE_LOOP);
			}
		}
		
		// recursively render groups
		List<GroupItem> items = preprocess(panel.items);
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
				List<GroupItem> panels = preprocess(group.items);
				for (Item item : panels) {
					renderPanel(gl, (PanelItem)item);
				}
				Objects.List.reclaim(panels);
				if (group.interactive())
					group.computeBounds();
			} else if (group.visible) {
				gr.render(group, gl);
				if (group.interactive())
					group.computeBounds();
			}
		}
		Objects.List.reclaim(items);
		
		if (panel.cache) {
			// finish render to texture
			gl.glPopMatrix();
			tex.end(gl);
			tex.draw(gl, 0, 0);
		}
		panel.dirty(false);
		
		// translate origin back
		if (translate) {
			gl.glPopMatrix();
			gl.glGetFloatv(GL.GL_MODELVIEW, _matrix, 0);
		}
		panel.computeBounds();
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
	
	public static final java.awt.Font font(Font f) {
		int style = java.awt.Font.PLAIN;
		if (f.bold()) style |= java.awt.Font.BOLD;
		if (f.italic()) style |= java.awt.Font.ITALIC;
		return Fonts.getFont(f.name(), style, f.size());
	}
		
	// -- Mark Renderers ------------------------------------------------------
	
	abstract class GroupRenderer {
		public abstract void render(Item group, GL gl);
	}
	class AreaRenderer extends GroupRenderer {
		public void render(Item group, GL gl) {
			List<Item> items = group.items();
			if (items.size() == 0) return;
			
			Item first = items.get(0);
			boolean fb = first.fill != null;
			boolean sb = first.stroke != null;
			if (!(fb || sb)) return;
			
			GroupItem g = (GroupItem) group;
			int itp = interpolateCode(g.interpolate);
			clear();
			
			if (fb) {
				if (g.segmented && itp != LINEAR) {
					int len = items.size()-1;
					for (int i=0; i<len; ++i) {
						Item s = items.get(i), t = items.get(i+1);
						Item f = itp==STEP_BEFORE ? s : t;
						fill(f.fill, f.alpha, 4);
						
						vertex(s.left + s.width, s.top + s.height);
						vertex(t.left + t.width, t.top + t.height);

						if (itp == STEP_BEFORE) {
							vertex(t.left, t.top);
							vertex(s.left, t.top);
						} else if (itp == STEP_AFTER) {
							vertex(t.left, s.top);
							vertex(s.left, s.top);
						}
					}
					paint(GL.GL_QUADS); clear();
				} else {
					fill(first.fill, first.alpha);
					for (int i=0; i<items.size(); ++i) {
						Item item = items.get(i);
						if (g.segmented) fill(item.fill, item.alpha, 2);
						vertex(item.left, item.top);
						vertex(item.left + item.width, item.top + item.height);
					}
					if (g.segmented) paint(GL.GL_QUAD_STRIP);
					else shape(GL.GL_QUAD_STRIP);
					clear();
				}
			}
			if (sb) {
				int len = items.size()-1;
				stroke(first.stroke, first.alpha);
				for (int i=0; i<=len; ++i) {
					Item item = items.get(i);
					vertex(item.left, item.top);
					if (itp != LINEAR && i < len) {
						Item item1 = items.get(i+1);
						if (itp == STEP_BEFORE) {
							vertex(item.left, item1.top);
						} else if (itp == STEP_AFTER) {
							vertex(item1.left, item.top);
						}
					}
				}
				for (int i=len+1; --i>=0;) {
					Item item = items.get(i);
					vertex(item.left + item.width, item.top + item.height);
					if (itp != LINEAR && i > 0) {
						Item item0 = items.get(i-1);
						if (itp == STEP_BEFORE) {
							vertex(item0.left + item0.width, item.top + item.height);
						} else if (itp == STEP_AFTER) {
							vertex(item.left + item.width, item0.top + item0.height);
						}
					}
				}
				vertex(first.left, first.top);
				shape(GL.GL_LINE_STRIP);
				clear();
			}
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
				
				if (fb) {
					fill(item.fill, item.alpha);
					shape(GL.GL_QUADS);
				}
				if (sb) {
					stroke(item.stroke, item.alpha);
					shape(GL.GL_LINE_LOOP);
				}
				clear();
			}
		}
	}
	class DotRenderer extends GroupRenderer {
		public void render(Item group, GL gl) {
			List<Item> items = group.items();
			boolean fb = false, sb = false, p = false, pp = false;
			Shapes.ShapeRenderer sr = null; String shape = null;
			int cap = vb.capacity(); clear();
			
			for (int i=0, k=0; i<items.size(); ++i) {
				DotItem item = (DotItem) items.get(i);
				if (!item.visible) continue;
				
				p = (item.shape == Shape.Point);
				fb = item.fill != null;
				sb = item.stroke != null && !p;
				if (!(fb || sb) || item.shape==null) continue;
				if (item.shape != shape || k >= cap) {
					shape = item.shape;
					sr = Shapes.shape(shape);
					if (k > 0) {
						paint(GL.GL_POINTS); k = 0;
						clear();
					}
					if (p) {
						pp = true; 
					} else if (pp) {
						pp = false;
					}
				}
				
				if (p) {
					k += 2;
					fill(item.fill, item.alpha, 1);
					vertex(item.left, item.top);
				} else {
					if (fb) {
						fill(item.fill, item.alpha);
						shape(sr.fill(vb, (float)item.left,
							(float)item.top, (float)item.radius, _zoom));
						clear();
					}
					if (sb) {
						stroke(item.stroke, item.alpha);
						shape(sr.draw(vb, (float)item.left,
							(float)item.top, (float)item.radius, _zoom));
						clear();
					}
				}
			}
			if (pp) paint(GL.GL_POINTS);
		}
	}
	class ImageRenderer extends GroupRenderer {
		private Images _img = new Images();
		public void render(Item group, GL gl) {			
			List<Item> items = group.items();
			
			gl.glColor4f(1, 1, 1, 1);
			for (int i=0; i<items.size(); ++i) {
				ImageItem item = (ImageItem) items.get(i);
				if (!item.visible || item.alpha==0) continue;
				
				// get image texture, skip if none
				Texture x = _img.getTexture(item.url, gl);
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
				TextureCoords c = x.getImageTexCoords();
				x.bind();
				gl.glBegin(GL.GL_QUADS);
				gl.glTexCoord2f(c.left(),  c.bottom()); gl.glVertex2f(l,b);
				gl.glTexCoord2f(c.right(), c.bottom()); gl.glVertex2f(r,b);
				gl.glTexCoord2f(c.right(), c.top());    gl.glVertex2f(r,t);
				gl.glTexCoord2f(c.left(),  c.top());    gl.glVertex2f(l,t);
				gl.glEnd();
			}
			gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
		}
	}
	class LabelRenderer extends GroupRenderer {
		private Labels _lab = new Labels();
		public void render(Item group, GL gl) {
			gl.glPushMatrix();
			gl.glLoadIdentity();
			
			List<Item> items = group.items();
			for (int i=0; i<items.size(); ++i) {
				LabelItem item = (LabelItem) items.get(i);
				if (!item.visible ||
					 item.fill == null ||
					 item.font == null ||
					 item.text == null ||
					 item.text.length()==0)
					continue;
					
				// get label texture, skip if none
				Texture tx = _lab.getTexture(item, gl);
				if (tx == null) continue;
				double w = tx.getImageWidth();
				double h = tx.getImageHeight();
				
				fill(item.fill, item.alpha);
				
				double x = item.left*_matrix[0] + item.top*_matrix[4] + _matrix[12];
				double y = item.left*_matrix[1] + item.top*_matrix[5] + _matrix[13];
				
				// Vertical alignment
				if (item.textBaseline == TextBaseline.Middle) {
					y -= h / 2;
				} else if (item.textBaseline == TextBaseline.Bottom) {
					y -= 0.8*h + item.textMargin;
				} else {
					y += item.textMargin - 0.1*h;
				}
				// Horizontal alignment
				if (item.textAlign == TextAlign.Center) {
					x -= w / 2;
				} else if (item.textAlign == TextAlign.Right) {
					x -= w + item.textMargin;
				} else {
					x += item.textMargin;
				}
				// TODO textAngle
				
				// render the label
				float l = (float)(x);//(item.left);
				float r = (float)(l + w);
				float t = (float)(y);//(item.top);
				float b = (float)(t+h);
				TextureCoords c = tx.getImageTexCoords();
				tx.bind();
				gl.glBegin(GL.GL_QUADS);
				gl.glTexCoord2f(c.left(),  c.bottom()); gl.glVertex2f(l,b);
				gl.glTexCoord2f(c.right(), c.bottom()); gl.glVertex2f(r,b);
				gl.glTexCoord2f(c.right(), c.top());    gl.glVertex2f(r,t);
				gl.glTexCoord2f(c.left(),  c.top());    gl.glVertex2f(l,t);
				gl.glEnd();
			}
			gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
			gl.glPopMatrix();
		}
	}
	class LinkRenderer extends GroupRenderer {
		public void render(Item group, GL gl) {
			List<Item> items = group.items();
			int len = items.size(), cap = vb.capacity()-7;
			if (len == 0) return;
			
			clear();
			for (int i=0; i<len;) {
				for (int k=0; k<cap && i<len; k+=8, ++i) {
					LinkItem edge = (LinkItem) items.get(i);
					if (!edge.visible || edge.alpha==0 || edge.stroke==null)
						continue;
					
					Item s1 = edge.source, s2 = edge.target;
					double px = s2.left - s1.left;
				    double py = s2.top - s1.top;
				    double vx = -py, vy = px;
				    double ss = 1.0 / Math.sqrt(vx*vx + vy*vy);
				    double w = edge.stroke.width() / 2;
				    vx *= w*ss; vy *= w*ss;
				    
				    stroke(edge.stroke, edge.alpha, 4);
				    vertex(s1.left+vx, s1.top+vy);
				    vertex(s1.left-vx, s1.top-vy);
				    vertex(s2.left-vx, s2.top-vy);
				    vertex(s2.left+vx, s2.top+vy);
				}
				paint(GL.GL_QUADS); clear();
			}
			/*
			gl.glBegin(GL.GL_LINES);
			for (int i=0; i<len; ++i) {
				LinkItem edge = (LinkItem) items.get(i);
				if (!edge.visible || edge.stroke==null) continue;
				setStroke(edge.stroke, edge.alpha, gl);
				gl.glVertex2f((float)edge.source.left, (float)edge.source.top);
				gl.glVertex2f((float)edge.target.left, (float)edge.target.top);
			}
			gl.glEnd();
			*/
		}
	}
	class LineRenderer extends GroupRenderer {
		public void render(Item group, GL gl) {
			List<Item> items = group.items();
			if (items.size() == 0) return;
			
			Item first = items.get(0);
			boolean fb = first.fill != null;
			boolean sb = first.stroke != null;
			if (!(fb || sb)) return;
			
			if (fb) {
				fill(first.fill, first.alpha);
				GLUtessellator tess = _tess.tessellator();
				_glu.gluBeginPolygon(tess);
				for (int i=0; i<items.size(); ++i) {
					Item item = items.get(i);
					double[] vec = Objects.leaseVector(item.left, item.top, 0);
					_glu.gluTessVertex(tess, vec, 0, vec);
				}
				_glu.gluEndPolygon(tess);
				Objects.reclaimLeasedVectors();
			}
			if (sb) {
				GroupItem g = (GroupItem)group;
				int itp = interpolateCode(g.interpolate);
				if (!g.segmented && (first.stroke.width() <= 2 || itp != LINEAR)) {
					stroke(first.stroke, first.alpha);
					int len = items.size()-1, cap = vb.capacity()-4;
					for (int i=0, k=0; i<=len; ++i, k+=2) {
						if (k >= cap) {
							// flush buffer
							shape(GL.GL_LINE_STRIP);
							float x = vb.get(cap-2), y = vb.get(cap-1);
							clear(); vertex(x, y); k = 0;
						}
						Item item = items.get(i);
						vertex(item.left, item.top);
						if (itp != LINEAR && i < len) {
							Item item1 = items.get(i+1);
							if (itp == STEP_BEFORE) {
								vertex(item.left, item1.top);
							} else if (itp == STEP_AFTER) {
								vertex(item1.left, item.top);
							}
							k += 2;
						}
					}
					shape(GL.GL_LINE_STRIP);
					clear();
				} else {
					miterJoin(gl, items, g.segmented, itp);
				}
			}
		}
		public void miterJoin(GL gl, List<Item> items, boolean segment, int interp) {
			if (items.size() < 2) return;
			int cap = vb.capacity() - 8;
			
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
			for (int i=0, k=0, n=items.size()-1; i<n; ++i) {
				if (k >= cap) {
					if (segment) paint(GL.GL_QUADS);
					else shape(GL.GL_QUADS);
					clear(); k = 0;
				}
				
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
	    		Objects.reclaimLeasedVectors();
	    		k += 8;
			}
			if (segment) paint(GL.GL_QUADS); else shape(GL.GL_QUADS);
			clear();
		}
	}
	class RuleRenderer extends GroupRenderer {
		public void render(Item group, GL gl) {
			List<Item> items = group.items();
			for (int i=0; i<items.size(); ++i) {
				RuleItem item = (RuleItem) items.get(i);
				stroke(item.stroke, item.alpha, 2);
				vertex(item.left, item.top);
				vertex(item.left + item.width, item.top + item.height);
			}
			paint(GL.GL_LINES);
			clear();
		}
	}
	class WedgeRenderer extends GroupRenderer {
		public void render(Item group, GL gl) {
			List<Item> items = group.items();
			boolean fb = false, sb = false;
			
			for (int i=0; i<items.size(); ++i) {
				WedgeItem item = (WedgeItem) items.get(i);
				fb = item.fill != null;
				sb = item.stroke != null;
				if (!(fb || sb)) continue;
				
				float cx = (float)item.left, cy = (float)item.top;
				float a0 = (float)item.startAngle, a1 = (float)item.endAngle;
				float inner = (float)item.innerRadius;
				float outer = (float)item.outerRadius;
				
				if (fb) {
					fill(item.fill, item.alpha);
					shape(Shapes.fillWedge(vb, cx, cy,
						a0, a1, inner, outer, _zoom));
					clear();
				}
				if (sb) {
					stroke(item.stroke, item.alpha);
					shape(Shapes.drawWedge(vb, cx, cy,
						a0, a1, inner, outer, _zoom));
					clear();
				}
			}
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
		double x = o1x + (d1x * ss) / denom;
		double y = o1y + (d1y * ss) / denom;
		return Objects.leaseVector(x, y, 0);
	}
	
}