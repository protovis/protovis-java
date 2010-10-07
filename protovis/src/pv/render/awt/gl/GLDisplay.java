package pv.render.awt.gl;

import java.awt.Component;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;

import pv.animate.Scheduler;
import pv.mark.Scene;
import pv.render.Display;
import pv.render.DisplayTask;
import pv.render.awt.AWTInputHandler;
import pv.scene.Item;
import pv.scene.PanelItem;
import pv.style.Color;

public class GLDisplay extends GLCanvas implements Display, GLEventListener {

	protected static final GLCapabilities s_caps = new GLCapabilities();
	static {
		//s_caps.setSampleBuffers(true);
		//s_caps.setNumSamples(2);
		s_caps.setRedBits(8);
		s_caps.setGreenBits(8);
		s_caps.setBlueBits(8);
		s_caps.setAlphaBits(8);
		s_caps.setDoubleBuffered(true);
		s_caps.setHardwareAccelerated(true);
	}
	
	private static final long serialVersionUID = 3864943312191013676L;
	protected List<Scene> _scenes = new CopyOnWriteArrayList<Scene>();
	protected List<Item> _items = new CopyOnWriteArrayList<Item>();
	protected GLRenderer _renderer = GLRenderer.instance();
	protected AWTInputHandler _handler;
	
	protected AffineTransform _transform = new AffineTransform();
	protected AffineTransform _itransform = new AffineTransform();
	protected float[] _matrix = new float[16];
	protected Point2D _pt = new Point2D.Double();
	
	protected float _frameTime = 0;
	protected float[] _bgcolor = {1,1,1,1};
	protected int _fpi = 0;
	protected long t0 = System.currentTimeMillis();
	
	public GLDisplay() {
		this(s_caps);
	}
	
	public GLDisplay(GLCapabilities caps) {
		super(caps);
		addGLEventListener(this);
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
		bgcolor(color, 1.0);
	}
	
	public void bgcolor(int color, double alpha) {
		_bgcolor[0] = Color.red(color) / 255f;
		_bgcolor[1] = Color.green(color) / 255f;
		_bgcolor[2] = Color.blue(color) / 255f;
		_bgcolor[3] = (float) alpha;
	}
	
	public boolean shouldDraw() {
		for (Item scene : _items) {
			if (scene.dirty()) return true;
		}
		return false;
	}
	
	public void preRender() {}
	public void postRender() {}
	
	public void render() {
		super.display();
	}
	
	public void repaint() {
		super.display();
	}
	
	public void display(GLAutoDrawable glDrawable) {
		//if (!shouldDraw()) return;
		
		// FRAME RATE CALCULATION
//		float t1 = (System.currentTimeMillis() - t0) / 1000f;
//		_frameTime += t1;
//		_fpi = (_fpi + 1);
//		if (_fpi+1 == 15) {
//			float fps = _fpi / _frameTime;
//			System.out.println("FRAME: "+fps+" fps");
//			_fpi = 0; _frameTime = 0;
//		}
//		t0 = System.currentTimeMillis();
		
		preRender();
		
		final GL gl = glDrawable.getGL();
		gl.glClearColor(_bgcolor[0], _bgcolor[1], _bgcolor[2], _bgcolor[3]);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		
		float[] matrix = getModelMatrix();
		gl.glLoadMatrixf(matrix, 0);
		_renderer.init(gl, matrix);
		
		for (Item scene : _items) {
			_renderer.render(gl, (PanelItem)scene);
		}
		postRender();
	}

	public void displayChanged(GLAutoDrawable gLDrawable,
		boolean modeChanged, boolean deviceChanged)
	{
		// DO NOTHING
	}

	public void init(GLAutoDrawable glDrawable) {
		_handler = new AWTInputHandler(this);
		glDrawable.addKeyListener(_handler);
		glDrawable.addMouseListener(_handler);
		glDrawable.addMouseMotionListener(_handler);
		glDrawable.addMouseWheelListener(_handler);
		
		final GL gl = glDrawable.getGL();
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glEnable(GL.GL_LINE_SMOOTH);
		//gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
		gl.glEnable(GL.GL_POLYGON_SMOOTH);
		//gl.glHint(GL.GL_POLYGON_SMOOTH_HINT, GL.GL_NICEST);
		//gl.glEnable(GL.GL_MULTISAMPLE);
	}

	public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
		doReshape(glDrawable, x, y, width, height);
	}
	
	protected void doReshape(GLAutoDrawable glDrawable, int x, int y, int width, int height)
	{
		final GL gl = glDrawable.getGL();
		_renderer.width(width);
		_renderer.height(height);
		
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, width, height, 0, 0, 1);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		//gl.glTranslatef(0.375f, 0.375f, 0f);
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
	
	private float[] getModelMatrix() {
		_matrix[ 0] = (float)_transform.getScaleX();
		_matrix[ 1] = (float)_transform.getShearY();
		_matrix[ 2] = 0;
		_matrix[ 3] = 0;
		_matrix[ 4] = (float)_transform.getShearX();
		_matrix[ 5] = (float)_transform.getScaleY();
		_matrix[ 6] = 0;
		_matrix[ 7] = 0;
		_matrix[ 8] = 0;
		_matrix[ 9] = 0;
		_matrix[10] = 1;
		_matrix[11] = 0;
		_matrix[12] = (float)_transform.getTranslateX();
		_matrix[13] = (float)_transform.getTranslateY();
		_matrix[14] = 0;
		_matrix[15] = 1;
		return _matrix;
	}

}
