package pv.render.awt.java2D;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import pv.animate.Scheduler;
import pv.mark.Scene;
import pv.render.Display;
import pv.render.DisplayTask;
import pv.render.awt.AWTInputHandler;
import pv.render.awt.Colors;
import pv.scene.Item;
import pv.scene.PanelItem;

public class Java2DDisplay extends JComponent implements Display {

	private static final long serialVersionUID = 3917179431350332689L;

	protected List<Scene> _scenes = new ArrayList<Scene>();
	protected List<Item> _items = new ArrayList<Item>();
	protected Java2DRenderer _renderer = Java2DRenderer.instance();
	protected AWTInputHandler _handler;
	
    protected BufferedImage _offscreen;
	protected AffineTransform _transform = new AffineTransform();
	protected AffineTransform _itransform = new AffineTransform();
	protected Point2D _pt = new Point2D.Double();
	
	protected Runnable _paint = new Runnable() {
		public void run() { paintImmediately(0, 0, getWidth(), getHeight()); }
	};
	
	protected float[] _frameTimes = new float[10];
	protected int _fpi = 0;
		
	// ----
	
	public Java2DDisplay() {
		setDoubleBuffered(false);
        setBackground(Color.WHITE);
        setSize(500,500);
        
        _handler = new AWTInputHandler(this);
        addKeyListener(_handler);
		addMouseListener(_handler);
		addMouseMotionListener(_handler);
		addMouseWheelListener(_handler);
		setFocusable(true);
		
		Scheduler.instance().addPostTask(new DisplayTask(this));
	}
	
	public Component asComponent() {
		return this;
	}
	
	public void addScene(Scene scene) {
		_scenes.add(scene);
		_items.add(scene.items());
	}
	
	public boolean removeScene(Scene scene) {
		if (_scenes.remove(scene)) {
			_items.remove(scene.items());
			return true;
		} else {
			return false;
		}
	}
	
	public List<Scene> scenes() {
		return Collections.unmodifiableList(_scenes);
	}
	
	public List<Item> items() {
		return Collections.unmodifiableList(_items);
	}
	
	public void bgcolor(int color) {
		setBackground(Colors.getColor(color));
	}
	
	public void bgcolor(int color, double alpha) {
		setBackground(Colors.getColor(color, alpha));
	}
	
	/**
     * Set the size of the Display.
     * @param width the width of the Display in pixels
     * @param height the height of the Display in pixels
     * @see java.awt.Component#setSize(int, int)
     */
    public void setSize(int width, int height) {
    	_offscreen = null;
        setPreferredSize(new Dimension(width, height));
        super.setSize(width, height);
    }
    
    /**
     * Set the size of the Display.
     * @param d the dimensions of the Display in pixels
     * @see java.awt.Component#setSize(java.awt.Dimension)
     */
    public void setSize(Dimension d) {
    	_offscreen = null;
        setPreferredSize(d);
        super.setSize(d);
    }
	
    public void preRender() {}
	public void postRender() {}
    
	public void render() {
		if (SwingUtilities.isEventDispatchThread()) {
			_paint.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(_paint);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void paintComponent(Graphics g) {
		preRender();
		
//		long t0 = System.currentTimeMillis();
		
		if (_offscreen == null) {
            _offscreen = getNewOffscreenBuffer(getWidth(), getHeight());
        }
        Graphics2D g2D = (Graphics2D) g;
        Graphics2D buf_g2D = (Graphics2D) _offscreen.getGraphics();
		
        buf_g2D.setColor(getBackground());
        buf_g2D.fillRect(0, 0, getWidth(), getHeight());
        
		buf_g2D.setTransform(_transform);
		
		for (Item scene : _items) {
			_renderer.render(buf_g2D, (PanelItem)scene);
		}
		buf_g2D.dispose();
		g2D.drawImage(_offscreen, 0, 0, null);
		
		postRender();
		
		// FRAME RATE CALCULATION
//		float t1 = (System.currentTimeMillis() - t0) / 1000f;
//		_frameTimes[_fpi] = t1;
//		_fpi = (_fpi + 1) % _frameTimes.length;
//		float fps = 0;
//		for (int i=0; i<_frameTimes.length; ++i)
//			fps += _frameTimes[i];
//		fps = _frameTimes.length / fps;
//		if (_fpi+1 == _frameTimes.length)
//			System.out.println("FRAME: "+fps+" fps");
	}
	
	// ----------
	
	public AffineTransform getTransform() {
		return _transform;
	}
	
	public AffineTransform getInverseTransform() {
		return _itransform;
	}
	
	public void reset() {
		_transform.setToIdentity();
		_itransform.setToIdentity();
	}
	
	public void pan(double dx, double dy) {
		_pt.setLocation(dx, dy);
		_itransform.transform(_pt, _pt);
		double panx = _pt.getX();
        double pany = _pt.getY();
        _pt.setLocation(0, 0);
        _itransform.transform(_pt, _pt);
        panx -= _pt.getX();
        pany -= _pt.getY();
        _transform.translate(panx, pany);
        try {
            _itransform = _transform.createInverse();
        } catch ( Exception e ) { /*will never happen here*/ }
	}
	
	/**
     * Creates a new buffered image to use as an offscreen buffer.
     */
    protected BufferedImage getNewOffscreenBuffer(int width, int height) {
        BufferedImage img = null;
        if ( !GraphicsEnvironment.isHeadless() ) {
            try {
                img = (BufferedImage)createImage(width, height);
            } catch ( Exception e ) {
                img = null;
            }
        }
        if ( img == null ) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }
        return img;
    }
	
	// -----
	
	public void zoom(double s, double x, double y) {
		_pt.setLocation(x, y);
		_itransform.transform(_pt, _pt);
		double zx = _pt.getX(), zy = _pt.getY();
        _transform.translate(zx, zy);
        _transform.scale(s, s);
        _transform.translate(-zx, -zy);
        try {
            _itransform = _transform.createInverse();
        } catch ( Exception e ) { /*will never happen here*/ }
	}
	
	public void rotate(double theta, double x, double y) {
		_pt.setLocation(x, y);
		_itransform.transform(_pt, _pt);
        double zx = _pt.getX(), zy = _pt.getY();
        _transform.translate(zx, zy);
        _transform.rotate(theta);
        _transform.translate(-zx, -zy);
        try {
            _itransform = _transform.createInverse();
        } catch ( Exception e ) { /*will never happen here*/ }
	}

}
