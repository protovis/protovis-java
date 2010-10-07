package pv.render.awt.gl;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL;

import pv.mark.constants.Shape;

public class Shapes {

	private static final Map<String,ShapeRenderer> SHAPE_MAP
		= new HashMap<String,ShapeRenderer>();
	static {
		shape(Shape.Circle, new CircleRenderer());
		shape(Shape.Square, new SquareRenderer());
		shape(Shape.Cross, new CrossRenderer());
		shape(Shape.X, new XRenderer());
		shape(Shape.Triangle, new TriangleRenderer());
		shape(Shape.Diamond, new DiamondRenderer());
		shape(Shape.Point, new PointRenderer());
	}
	
	public static ShapeRenderer shape(String name)
	{
		return SHAPE_MAP.get(name);
	}
	
	public static void shape(String name, ShapeRenderer sr) {
		SHAPE_MAP.put(name, sr);
	}
	
	// ------------------------------------------------------------------------
	
	public static interface ShapeRenderer {
		public int draw(FloatBuffer vb, float x, float y, float radius, float scale);
		public int fill(FloatBuffer vb, float x, float y, float radius, float scale);
	}
	
	public static class CircleRenderer implements ShapeRenderer {		
		public int draw(FloatBuffer vb, float x, float y, float size, float scale) {
			int slices = (int)(2.0 * Math.PI / (Math.acos(1 - 0.25 / (scale*size))));
			double theta = 2 * Math.PI / slices; 
			float tan_factor = (float)Math.tan(theta); // tangential factor 
			float rad_factor = (float)Math.cos(theta); // radial factor 
			float xx = size; // we start at angle = 0 
			float yy = 0; 
		    
			for (int i=0; i < slices; ++i) 
			{ 
				vb.put(xx + x).put(yy + y);
				float tx = -yy; 
				float ty = xx; 
				xx += tx * tan_factor; 
				yy += ty * tan_factor; 
				// correct using the radial factor 
				xx *= rad_factor; 
				yy *= rad_factor; 
			} 
			return GL.GL_LINE_LOOP;
		}
		public int fill(FloatBuffer vb, float x, float y, float size, float scale) {
			int slices = (int)(2.0 * Math.PI / (Math.acos(1 - 0.25 / (scale*size))));
			double theta = 2 * Math.PI / slices; 
			float tan_factor = (float)Math.tan(theta); // tangential factor 
			float rad_factor = (float)Math.cos(theta); // radial factor 
			float xx = size; // we start at angle = 0 
			float yy = 0; 
		    
			vb.put(x).put(y); 
			for (int i=0; i < slices; ++i) 
			{ 
				vb.put(xx + x).put(yy + y);
				float tx = -yy; 
				float ty = xx; 
				xx += tx * tan_factor; 
				yy += ty * tan_factor; 
				// correct using the radial factor 
				xx *= rad_factor; 
				yy *= rad_factor; 
			}
			vb.put(x + size).put(y);
			return GL.GL_TRIANGLE_FAN;
		}
	}
	
	public static class SquareRenderer implements ShapeRenderer {
		public int draw(FloatBuffer vb, float x, float y, float size, float scale) {
			vb.put(x-size).put(y-size);
			vb.put(x-size).put(y+size);
			vb.put(x+size).put(y+size);
			vb.put(x+size).put(y-size);
			return GL.GL_LINE_LOOP;
		}
		public int fill(FloatBuffer vb, float x, float y, float size, float scale) {
			vb.put(x-size).put(y-size);
			vb.put(x-size).put(y+size);
			vb.put(x+size).put(y+size);
			vb.put(x+size).put(y-size);
			return GL.GL_QUADS;
		}
	}
	
	public static class CrossRenderer implements ShapeRenderer {
		public int draw(FloatBuffer vb, float x, float y, float size, float scale) {
			vb.put(x).put(y-size);
			vb.put(x).put(y+size);
			vb.put(x-size).put(y);
			vb.put(x+size).put(y);
			return GL.GL_LINES;
		}
		public int fill(FloatBuffer vb, float x, float y, float size, float scale) {
			return -1;
		}
	}
	
	public static class XRenderer implements ShapeRenderer {
		public int draw(FloatBuffer vb, float x, float y, float size, float scale) {
			vb.put(x-size).put(y-size);
			vb.put(x+size).put(y+size);
			vb.put(x-size).put(y+size);
			vb.put(x+size).put(y-size);
			return GL.GL_LINES;
		}
		public int fill(FloatBuffer vb, float x, float y, float size, float scale) {
			return -1;
		}
	}
	
	public static class TriangleRenderer implements ShapeRenderer {
		public int draw(FloatBuffer vb, float x, float y, float size, float scale) {
			vb.put(x-size).put(y+size);
			vb.put(x+size).put(y+size);
			vb.put(x).put(y-size);
			return GL.GL_LINE_LOOP;
		}
		public int fill(FloatBuffer vb, float x, float y, float size, float scale) {
			vb.put(x-size).put(y+size);
			vb.put(x+size).put(y+size);
			vb.put(x).put(y-size);
			return GL.GL_TRIANGLES;
		}
	}
	
	public static class DiamondRenderer implements ShapeRenderer {
		public int draw(FloatBuffer vb, float x, float y, float size, float scale) {
			vb.put(x).put(y+size);
			vb.put(x-size).put(y);
			vb.put(x).put(y-size);
			vb.put(x+size).put(y);
			return GL.GL_LINE_LOOP;
		}
		public int fill(FloatBuffer vb, float x, float y, float size, float scale) {
			vb.put(x).put(y+size);
			vb.put(x-size).put(y);
			vb.put(x).put(y-size);
			vb.put(x+size).put(y);
			return GL.GL_QUAD_STRIP;
		}
	}
	
	public static class PointRenderer implements ShapeRenderer {
		public int draw(FloatBuffer vb, float x, float y, float size, float scale) {
			return -1;
		}
		public int fill(FloatBuffer vb, float x, float y, float size, float scale) {
			vb.put(x).put(y);
			return GL.GL_POINTS;
		}
	}
	
	// ------------------------------------------------------------------------
	
	public static int drawWedge(FloatBuffer vb, float cx, float cy,
		float a0, float a1, float inner, float outer, float scale)
	{		
		float a = (float)Math.abs(a1 - a0);
		int slices = (int)(a / (Math.acos(1 - 0.25 / (scale*outer))));
		double theta = 2 * Math.PI / slices; 
		float tan_factor = (float)Math.tan(theta); // tangential factor 
		float rad_factor = (float)Math.cos(theta); // radial factor 
		float xx = outer * (float)Math.cos(a0); 
		float yy = -outer * (float)Math.sin(a0); 
	    
		for (int i=0; i < slices; ++i) 
		{ 
			vb.put(xx + cx).put(yy + cy);
			float tx = -yy; 
			float ty = xx; 
			xx += tx * tan_factor; 
			yy += ty * tan_factor; 
			// correct using the radial factor 
			xx *= rad_factor; 
			yy *= rad_factor; 
		}
		if (a >= 2*Math.PI - 0.001) {
			// do nothing
		} else if (inner == 0) {
			vb.put(cx).put(cy);
		} else {
			xx = inner * (float)Math.cos(a1); 
			yy = -inner * (float)Math.sin(a1);
			for (int i=slices; --i >= 0; ++i) 
			{ 
				vb.put(xx + cx).put(yy + cy);
				float tx = -yy; 
				float ty = xx; 
				xx -= tx * tan_factor; 
				yy -= ty * tan_factor; 
				// correct using the radial factor 
				xx *= rad_factor; 
				yy *= rad_factor; 
			} 
		}
		return GL.GL_LINE_LOOP;
	}
	
	public static int fillWedge(FloatBuffer vb, float cx, float cy,
		float a0, float a1, float inner, float outer, float scale)
	{		
		float a = (float)Math.abs(a1 - a0);
		int slices = (int)(a / (Math.acos(1 - 0.25 / (scale*outer))));
		double theta = 2 * Math.PI / slices; 
		float tan_factor = (float)Math.tan(theta); // tangential factor 
		float rad_factor = (float)Math.cos(theta); // radial factor 
		float x1 = outer * (float)Math.cos(a0); 
		float y1 = -outer * (float)Math.sin(a0);
		float x2 = inner * (float)Math.cos(a0); 
		float y2 = -inner * (float)Math.sin(a0);
	    
		if (inner <= 0) {
			// pie, use triangle fan
			vb.put(cx).put(cy);
			for (int i=0; i < slices; ++i) 
			{ 
				vb.put(x1 + cx).put(y1 + cy);
				float tx = -y1; 
				float ty = x1; 
				x1 += tx * tan_factor; 
				y1 += ty * tan_factor;  
				x1 *= rad_factor; 
				y1 *= rad_factor;
			}
			return GL.GL_TRIANGLE_FAN;
		} else {
			// wedge, use quad strip
			for (int i=0; i < slices; ++i) 
			{ 
				vb.put(x1 + cx).put(y1 + cy);
				float tx = -y1; 
				float ty = x1; 
				x1 += tx * tan_factor; 
				y1 += ty * tan_factor;  
				x1 *= rad_factor; 
				y1 *= rad_factor;
				
				vb.put(x2 + cx).put(y2 + cy);
				tx = -y2; 
				ty = x2; 
				x2 -= tx * tan_factor; 
				y2 -= ty * tan_factor;  
				x2 *= rad_factor; 
				y2 *= rad_factor;
			}
			return GL.GL_QUAD_STRIP;
		}
	}
}
