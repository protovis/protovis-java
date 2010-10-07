package pv.render;

import java.lang.ref.WeakReference;

import pv.animate.Scheduler;

public class DisplayTask implements Scheduler.Task {

	private WeakReference<Display> _display;
	
	public DisplayTask(Display d) {
		_display = new WeakReference<Display>(d);
	}
	
	public long evaluate(long t) {
		Display display = _display.get();
		if (display == null) return -1;
		display.render();
		return 1;
	}

	public String id() {
		return "display-task-"+_display.get();
	}

}
