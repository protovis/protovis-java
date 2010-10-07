package pv.scene;

import java.awt.geom.Rectangle2D;
import java.util.Map;

import pv.mark.property.Property;

public class LinkItem extends Item {

	public Item source;
	public Item target;
	public double sourceX;
	public double sourceY;
	public double targetX;
	public double targetY;
	
	public void populate(Item item) {
		LinkItem litem = (LinkItem) item;
		source = litem.source;
		target = litem.target;
		super.populate(item);
	}
	
	public Rectangle2D bounds(Rectangle2D bounds)
	{
		double minX, maxX, minY, maxY;
		double s = stroke==null ? 0 : stroke.width();
		if (s >= 2) s /= 2;
		if (source.left < target.left) {
			minX = source.left;
			maxX = target.left;
		} else {
			minX = target.left;
			maxX = source.left;
		}
		if (source.top < target.top) {
			minY = source.top;
			maxY = target.top;
		} else {
			minY = target.top;
			maxY = source.top;
		}
		bounds.setRect(minX-s, minY-s, maxX-minX+s+s, maxY-minY+s+s);
		return bounds;
	}
	
	protected boolean inBounds(double x, double y)
	{
		return false; // TODO
	}
	
	public boolean hit(double x, double y)
	{
		return false; // TODO
	}
	
	public static int checkProperties(Map<String,Property> props) {
		return Item.checkProperties(props);
	}
	
}
