package pv.mark.update;

import pv.animate.Scheduler;
import pv.animate.Transition;
import pv.mark.Mark;
import pv.scene.GroupItem;
import pv.scene.PanelItem;

public abstract class MarkUpdater {

	private static MarkUpdater s_updater = new SerialUpdater();
	public static MarkUpdater instance() {
		return s_updater;
	}
	
	public abstract void update(Mark mark, GroupItem proto, PanelItem panel, Transition t);
	
	public void submit(Mark mark, GroupItem proto, PanelItem panel, Transition t) {
		if (Scheduler.isCurrentThread()) {
			//long t0 = System.currentTimeMillis();
			
			update(mark, proto, panel, t);
			
			//long t1 = System.currentTimeMillis();
			//System.out.println((t1-t0)/1000f+"s");
		} else {
			Scheduler.instance().add(new Task(mark, proto, panel, t, this));
		}
	}
	
	public static class Task implements Scheduler.Task {
		Mark mark;
		GroupItem proto;
		PanelItem panel;
		Transition trans;
		MarkUpdater exec;
		
		public String id() { return "update-"+mark.toString(); }
		
		public Task(Mark mark, GroupItem proto, PanelItem layer, Transition t,
			MarkUpdater exec)
		{
			this.mark = mark;
			this.proto = proto;
			this.panel = layer;
			this.trans = t;
			this.exec = exec;
		}
		
		public long evaluate(long t) {
			//long t0 = System.currentTimeMillis();

			exec.update(mark, proto, panel, trans);
			
			//long t1 = System.currentTimeMillis();
			//System.out.println((t1-t0)/1000f+"s");
			
			return 0;
		}
	}
	
}
