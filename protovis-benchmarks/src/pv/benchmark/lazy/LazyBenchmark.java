package pv.benchmark.lazy;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pv.benchmark.Benchmark;
import pv.benchmark.BenchmarkRunner;
import pv.mark.Mark;
import pv.mark.MarkEvent;
import pv.mark.Scene;
import pv.mark.constants.Events;
import pv.mark.constants.MarkType;
import pv.mark.constants.Shape;
import pv.mark.eval.EvaluatorBuilder;
import pv.scene.DotItem;
import pv.scene.GroupItem;
import pv.scene.Item;
import pv.scene.PanelItem;
import pv.style.Fill;
import pv.util.Objects;

public class LazyBenchmark {

	static int N = 1000000;
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		BenchmarkRunner.instance.iterations(101);
		
		List<Point2D> d = data();
		EvaluatorBuilder.instance().cache(false); // do not cache compiled evaluators
		BenchmarkRunner.instance.run("UPDATE", new Updater("NORMAL", dynamicScene(d)));
		BenchmarkRunner.instance.run("UPDATE", new Updater("LAZY", lazyScene(d)));
	}
	
	public static List<Point2D> data() {
		ArrayList<Point2D> data = new ArrayList<Point2D>();
		for (int i=0; i<N; ++i) {
			data.add(new Point2D.Double(Math.random(), Math.random()));
		}
		return data;
	}
	
	public static Scene dynamicScene(List<Point2D> data) {
		Scene scene = new Scene()
			.width(900)
			.height(400)
			.left(50)
			.top(50)
			.scene();
		scene.add(MarkType.Dot)
			.datatype(Point2D.class)
			.data(data)
			.left("{{(int)(parent.width * (data.getX() + 0.05*(0.5 - Math.random())))}}")
			.bottom("{{(int)(parent.height * (data.getY() + 0.05*(0.5 - Math.random())))}}")
			.shape(Shape.Point)
			.fill(Fill.solid(0xaa5555, 1));
		return scene;
	}
	
	public static Scene lazyScene(List<Point2D> data) {
		Scene scene = new Scene();
		Mark dot = scene.add(MarkType.Dot)
			.datatype(Point2D.class)
			.data(data)
			.left("{{(int)(parent.width * (data.getX() + 0.05*(0.5 - Math.random())))}}")
			.bottom("{{(int)(parent.height * (data.getY() + 0.05*(0.5 - Math.random())))}}");
		
		scene.updateAndWait();
		
		DotEvaluator deval = new DotEvaluator();
		deval._data = data;
		dot.evaluator(deval);
		// Re-evaluates only left & bottom.
		// Comment these out to test with no property updates at all.
		deval.check[0] = true;
		deval.check[1] = true;

		return scene;
	}
	
	public static class Updater implements Benchmark
	{
		private Scene scene;
		private String name;
		
		public Updater(String name, Scene scene) {
			this.scene = scene;
			this.name = name;
		}
		
		@Override
		public String name() { return name; }
		
		@Override
		public void setup() { scene.clear(); System.gc(); }
		
		@Override
		public void takedown() { scene.clear(); System.gc(); }
		
		@Override
		public long[] run(int iterations) {
			long[] tt = new long[iterations];
			for (int i=0; i<iterations; ++i) {
				long t = System.currentTimeMillis();
				scene.updateAndWait();
				tt[i] = System.currentTimeMillis() - t;
			}
			return tt;
		}
	}

	public static class DotEvaluator extends pv.mark.eval.ItemEvaluator {

		public int _props;
		public boolean _hasKey = true;
		
		// DECLARATIONS
		public List<?> _data;


		public Class<?> datatype() { return Point2D.class; }
		
		public Object key(Point2D data, int index) {
				return index;
		}
		
		protected Object getData(GroupItem item) {
			return _data;
		}
		
		GroupItem group;
		
		public GroupItem build(Mark mark, GroupItem proto, PanelItem layer, boolean animate) {
			if (group == null) {
				return build2(mark, proto, layer, animate);
			} else {
				MarkEvent.fire(MarkEvent.create(Events.build), group, group);
				return group;
			}
		}
		
		@SuppressWarnings("unchecked")
		private final GroupItem build2(Mark mark, GroupItem proto, PanelItem layer, boolean animate) {

			group = getGroup(mark, proto, layer);
			group.modified(false);
			Iterable<?> _data_ = data(group);
					
			// BUILD LOOKUP TABLE
			Map<Object,Item> map = null;
			if (animate && _hasKey) {
				map = (Map<Object,Item>) Objects.Map.get();
				for (int i=0; i<group.size(); ++i) {
					Item item = group.item(i);
					Object key = key((Point2D)item.data, item.index);
					map.put(key, item);
				}
				group.items.clear();
			}
		
			// GENERATE ITEMS
			int index = 0;
			for (Object datum : _data_) {
				Point2D data = (Point2D) datum;
				
				// GET SCENEGRAPH ITEM
				DotItem item = null;
				if (map != null) {
					item = (DotItem) map.remove(key(data, index));
				}
				if (item != null) {
					group.add(item);
					item.index = index;
				} else if (group.items.size() > index) {
					item = (DotItem) group.item(index);
				} else {
					item = new DotItem();
					group.add(item);
					item.group = group;
					item.index = index;
					item.born(true);
				}
		
				if (animate) {
					Item prev = item.next, next;
					if (prev == null) {
						prev = (item.next = new DotItem()); prev.group = group;
						next = (prev.next = new DotItem()); next.group = group;
					} else {
						next = prev.next;
					}
					prev.index = item.index;
					prev.populate(item);
				}
				item.zombie(false);
				item.dead(false);
				if (item.data != data) {
					item.data = data;
					group.modified(true);
				}
				index += 1;
			}
			if (index != group.size())
				group.dirty(true);
			
			// PRESIZE LAYER INSTANCES
			if (mark.markType() == MarkType.Panel) {
				int layerSize = mark.panelSize();
				for (int i=0; i<group.size(); ++i) {
					PanelItem item = (PanelItem) group.item(i);
					while (item.size() < layerSize) {
						item.add(null);
					}
				}
			}
			
			// PROCESS ZOMBIE ITEMS
			int idx = index;
			if (map != null) {
				for (Item item : map.values()) {
					if (item.dead()) {
						group.modified(true);
						continue;
					}
					item.zombie(true);
					if (item.next == null) {
						item.next = new DotItem();
						item.next.next = new DotItem();
						item.next.group = group;
						item.next.next.group = group;
					}
					item.next.populate(item);
					group.add(item);
					idx += 1;
				}
				Objects.Map.reclaim(map);
			} 
			
			// remove extra items if length changes
			group.discard(idx);
			
			// EVALUATE GROUP PROPERTIES
			{
				GroupItem item = group;
				item.handlers = mark.propertySet().handlers;

				// FIRE BUILD EVENT
				MarkEvent.fire(MarkEvent.create(Events.build), item, item);
			}
			return group;
		}
		
		public boolean[] check = new boolean[10];
		
		public void evaluate(GroupItem group, int start, int end, boolean animate) {
			for (int i=start; i<end; ++i) {
				DotItem item = (DotItem) group.item(i);
				Point2D data = (Point2D) item.data;
				Item parent = item.parent();
				boolean zombie = item.zombie();
				
				// EVALUATE PROPERTIES
				if (check[0]) item.left = (int)(parent.width * (data.getX() + 0.05*(0.5 - Math.random())));
				if (check[1]) item.bottom = (int)(parent.height * (data.getY() + 0.05*(0.5 - Math.random())));
				if (check[2]) item.visible = true;
				if (check[3]) item.alpha = 1;
				if (check[4]) item.fill = null;
				if (check[5]) item.stroke = null;
				if (check[6]) item.shape = null;
				if (check[7]) item.size = 0;

				if (zombie) {
					item.alpha = 0;
				}
				item.buildImplied(_props);
				
				if (animate) {
					Item orig = item;
					item.next.next.populate(item);
					if (item.born()) {
						item.born(false);
						item.next.populate(item);
						item = (DotItem) item.next;
						item.alpha = 0;
						item.buildImplied(_props);
					} else if (!zombie) {
						item.populate(item.next);
					}
					group.props = orig.checkInterpolatedProperties(group.props); 
				} else {
					item.born(false);
				}
			}
		}
		
	}
	
}
