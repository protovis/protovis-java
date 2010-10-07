package pv.render.awt;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;

import pv.mark.MarkEvent;
import pv.mark.constants.Events;
import pv.render.Display;
import pv.render.InputHandler;
import pv.scene.Item;

public class AWTInputHandler extends InputHandler implements KeyListener,
	MouseListener, MouseMotionListener, MouseWheelListener, Events
{
	private Display _display;
	private double _x, _y;
	private int _sx, _sy;
	private boolean _mouseDown;
	
	private Point2D _pt = new Point2D.Double();
	private Item _active;
	
	public AWTInputHandler(Display display) {
		_display = display;
	}
	
	// TODO? keep track of mouse event to populate key events
	
	public void keyPressed(KeyEvent e) {
		if (_active == null) return;
		MarkEvent.fire(event(keyPress, e), _active);
	}
	public void keyReleased(KeyEvent e) {
		if (_active == null) return;
		MarkEvent.fire(event(keyRelease, e), _active);
	}
	public void keyTyped(KeyEvent e) {
		if (_active == null) return;
		MarkEvent.fire(event(keyType, e), _active);
	}
	public void mouseClicked(MouseEvent e) {
		if (_active == null) return;
		MarkEvent.fire(event(mouseClick, e), _active);
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
		if (_active == null) return;
		if (!_mouseDown) {
            // we've left the component and an item 
            // is active but not being dragged, deactivate it
			MarkEvent.fire(event(mouseExit, e), _active);
            _active = null;
        }
	}
	public void mousePressed(MouseEvent e) {
		if (_active == null) return;
		MarkEvent.fire(event(mousePress, e), _active);
	}
	public void mouseReleased(MouseEvent e) {
		if (_active == null) return;
		MarkEvent.fire(event(mouseRelease, e), _active);
		if (_mouseDown && isOffComponent(e)) {
            // mouse was dragged off of the component, 
            // then released, so register an exit
			MarkEvent.fire(event(mouseExit, e), _active);
            _active = null;
        }
        _mouseDown = false;
	}
	public void mouseDragged(MouseEvent e) {
		if (_active == null) return;
		MarkEvent.fire(event(mouseDrag, e), _active);
	}
	public void mouseMoved(MouseEvent e) {
		_sx = e.getX(); _sy = e.getY();
		_pt.setLocation(_sx, _sy);
		_display.getInverseTransform().transform(_pt, _pt);
		_x = _pt.getX(); _y = _pt.getY();
		
		Item item = pick(_display.items(), _x, _y);
		if (_active != item) {
			if (_active != null) {
				MarkEvent.fire(event(mouseExit, e), _active);
			}
			_active = item;
			if (_active != null) {
				MarkEvent.fire(event(mouseEnter, e), _active);
			}
		} else if (_active != null) {
			MarkEvent.fire(event(mouseMove, e), _active);
		}
	}
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (_active == null) return;
		MarkEvent.fire(event(mouseWheel, e), _active);
	}
	
	private boolean isOffComponent(MouseEvent e) {
		int x = e.getX(), y = e.getY();
        return ( x<0 || x>_display.getWidth() || y<0 || y>_display.getHeight() );
	}
	
	protected MarkEvent event(String type, MouseEvent e) {
		MarkEvent m = MarkEvent.create(type);
		m.x = m.screenX = e.getX();
		m.y = m.screenY = e.getY();
		m.button = e.getButton();
		m.clickCount = e.getClickCount();
		m.modifiers = e.getModifiers();
		m.x = _x;
		m.y = _y;
		return m;
	}
	
	protected MarkEvent event(String type, MouseWheelEvent e) {
		MarkEvent m = MarkEvent.create(type);
		m.x = m.screenX = e.getX();
		m.y = m.screenY = e.getY();
		m.button = e.getButton();
		m.clickCount = e.getClickCount();
		m.modifiers = e.getModifiers();
		m.wheelRotation = e.getWheelRotation();
		m.x = _x;
		m.y = _y;
		return m;
	}
	
	protected MarkEvent event(String type, KeyEvent e) {
		MarkEvent m = MarkEvent.create(type);
		m.keyChar = e.getKeyChar();
		m.keyCode = e.getKeyCode();
		m.modifiers = e.getModifiers();
		m.screenX = _sx;
		m.screenY = _sy;
		m.x = _x;
		m.y = _y;
		return m;
	}
	
}
