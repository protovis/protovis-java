package pv.benchmark;

import pv.mark.Scene;
import pv.render.awt.gl.GLDisplay;

public class SceneBenchmark implements Benchmark {
	private String name;
	private Scene scene;
	private GLDisplay display;
	
	public SceneBenchmark(GLDisplay display, String name, Scene scene) {
		this.display = display;
		this.name = name;
		this.scene = scene;
	}
	public String name() { return name; }
	public void setup() {
		scene.updateAndWait();
		if (display != null) display.addScene(scene);
	}
	public void takedown() {
		if (display != null) { 
			display.removeScene(scene);
			display.display();
		}
	}
	public long[] run(int iterations) {
		long[] t = new long[iterations];
		for (int i=0; i<iterations; ++i) {
			t[i] = System.currentTimeMillis();
			display.display();
			t[i] = System.currentTimeMillis() - t[i];
		}
		return t;
	}
}