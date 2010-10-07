package pv.render.awt.java2D;

import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import pv.mark.constants.Shape;
import pv.scene.WedgeItem;

public class Shapes {

	private static final Map<String,java.awt.Shape> SHAPE_MAP
		= new HashMap<String,java.awt.Shape>();
	static {
		double s = 0.5;
		shape(Shape.Circle, new Ellipse2D.Double(-s, -s, 2*s, 2*s));
		shape(Shape.Square, new Rectangle2D.Double(-s, -s, 2*s, 2*s));
		
		GeneralPath p;
		
		p = new GeneralPath();
		p.moveTo(0, -s);
		p.lineTo(0,  s);
		p.moveTo(-s, 0);
		p.lineTo( s, 0);
		shape(Shape.Cross, p);
		
		p = new GeneralPath();
		p.moveTo(-s, -s);
		p.lineTo(+s, +s);
		p.moveTo(+s, -s);
		p.lineTo(-s, +s);
		shape(Shape.X, p);
		
		p = new GeneralPath();
		p.moveTo(-s,  s);
		p.lineTo( s,  s);
		p.lineTo( 0, -s);
		p.lineTo(-s,  s);
		shape(Shape.Triangle, p);
		
		p = new GeneralPath();
		p.moveTo( 0,  s);
		p.lineTo(-s,  0);
		p.lineTo( 0, -s);
		p.lineTo( s,  0);
		p.lineTo( 0,  s);
		shape(Shape.Diamond, p);
	}
	
	public static java.awt.Shape shape(String name)
	{
		return SHAPE_MAP.get(name);
	}
	
	public static void shape(String name, java.awt.Shape s) {
		SHAPE_MAP.put(name, s);
	}
	
	// ------------------------------------------------------------------------
	
	public static void drawWedge(GeneralPath p, WedgeItem wi)
	{
		double cx = wi.left, cy = wi.top, x0=0, y0=0, x, y;
		double a0 = wi.startAngle, a1 = wi.endAngle;
		double inner = wi.innerRadius, outer = wi.outerRadius;
		
		double a = Math.abs(a1 - a0);
		int slices = (int)(2 * Math.PI * Math.PI * outer / a);
		boolean circle = (a >= 2*Math.PI - 0.001);
		if (slices <= 0) return;

		// pick starting point
		if (wi.innerRadius <= 0 && !circle) {
			p.moveTo(cx, cy);
		} else {
			x0 = cx + outer * Math.cos(a0);
			y0 = cy + -outer * Math.sin(a0);
			p.moveTo(x0, y0);
		}
			
		// draw outer arc
		for (int i=0; i <= slices; ++i) {
			a = a0 + i*(a1-a0)/slices;
			x = cx + outer * Math.cos(a);
			y = cy + -outer * Math.sin(a);
			p.lineTo(x,y);
		}

		if (circle) {
			// return to starting point
			p.lineTo(x0, y0);
		} else if (inner > 0) {
			// draw inner arc
			for (int i = slices+1; --i >= 0;) {
				a = a0 + i*(a1-a0)/slices;
				x = cx + inner * Math.cos(a);
				y = cy + -inner * Math.sin(a);
				p.lineTo(x,y);
			}
			p.lineTo(x0, y0);
		} else {
			// return to center
			p.lineTo(cx, cy);
		}
	}
	
}
