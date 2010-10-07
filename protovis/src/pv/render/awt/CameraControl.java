package pv.render.awt;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import pv.render.Display;

public class CameraControl implements MouseListener,
	MouseMotionListener, MouseWheelListener
{
	private double _x, _y, _mx, _my, _ba;
	private Display _d;
	
	public void attach(Display d) {
		_d = d;
		Component c = d.asComponent();
		c.addMouseListener(this);
		c.addMouseMotionListener(this);
		c.addMouseWheelListener(this);
	}

	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mousePressed(MouseEvent e) {
		_mx = _x = e.getX();
		_my = _y = e.getY();
		_ba = Double.NaN;
	}
	public void mouseReleased(MouseEvent e) { }
	public void mouseDragged(MouseEvent e) {
		//System.err.println("MOUSE DRAG");
		if (e.isShiftDown()) {
			double z = Math.max(-0.9, Math.min(0.9, (e.getY()-_y)/100.0)); 
			_d.zoom(1 + z, _mx, _my);
		} else if (e.isAltDown()) {
            double angle = Math.atan2(e.getY() - _my, e.getX() - _mx);
            // only rotate once the base angle has been established
            if ( !Double.isNaN(_ba) ) {
            	_d.rotate(angle - _ba, _mx, _my);
            }
            _ba = angle;
		} else {
			_d.pan(e.getX() - _x, e.getY() - _y);
		}
		_x = e.getX();
		_y = e.getY();
		_d.render();
	}
	public void mouseMoved(MouseEvent e) { }
	public void mouseWheelMoved(MouseWheelEvent e) { }
}