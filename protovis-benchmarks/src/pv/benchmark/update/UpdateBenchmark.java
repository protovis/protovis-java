package pv.benchmark.update;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.ShapeAction;
import prefuse.data.Table;
import prefuse.visual.VisualItem;
import pv.benchmark.Benchmark;
import pv.benchmark.BenchmarkRunner;
import pv.mark.Scene;
import pv.mark.constants.MarkType;
import pv.mark.constants.Shape;
import pv.mark.eval.EvaluatorBuilder;
import pv.mark.property.AbstractProperty;
import pv.mark.property.Property;
import pv.scene.DotItem;
import pv.scene.Item;
import pv.style.Fill;

public class UpdateBenchmark {

	static int N = 100000;
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		BenchmarkRunner.instance.iterations(101);
		
		List<Point2D> d = data();
		EvaluatorBuilder.instance().cache(false);
		
		EvaluatorBuilder.instance().compile(true);
		BenchmarkRunner.instance.run("UPDATE", new Updater("COMPILE", dynamicScene(d)));
		
		EvaluatorBuilder.instance().compile(false);
		BenchmarkRunner.instance.run("UPDATE", new Updater("STATIC", staticScene(d)));
		
		EvaluatorBuilder.instance().compile(false);
		BenchmarkRunner.instance.run("UPDATE", new Updater("INTERPRET", interpretScene(d)));
		
		BenchmarkRunner.instance.run("UPDATE", new PrefuseUpdater(d));
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
			.size("{{(index%20)*(index%20) + 16}}")
			.fill(Fill.solid(0xaa5555, 1));
		return scene;
	}
	
	public static Scene staticScene(List<Point2D> data) {
		Scene scene = new Scene()
			.data(Property.None)
			.width(900)
			.height(400)
			.left(50)
			.top(50)
			.scene();
		scene.add(MarkType.Dot)
			.datatype(Point2D.class)
			.data(data)
			.left(new AbstractProperty() {
				public double number(Item x) {
					return (int)(x.parent().width *
						(((Point2D)x.data).getX() + 0.05*(0.5 - Math.random())));
				}
			})
			.bottom(new AbstractProperty() {
				public double number(Item x) {
					return (int)(x.parent().height *
						(((Point2D)x.data).getY() + 0.05*(0.5 - Math.random())));
				}
			})
			.shape(Shape.Point)
			.size(new AbstractProperty() {
				public double number(Item x) {
					return (x.index%20)*(x.index%20) + 16;
				}
			})
			.set("radius",new AbstractProperty() {
				public double number(Item x) {
					return Math.sqrt(((DotItem)x).size);
				}
			})
			.fill(Fill.solid(0xaa5555, 1))
			.stroke(Property.None);
		return scene;
	}
	
	public static Scene interpretScene(List<Point2D> data) {
		Scene scene = new Scene()
			.data(Property.None)
			.width(900)
			.height(400)
			.left(50)
			.top(50)
			.scene();
		scene.add(MarkType.Dot)
			.datatype(Point2D.class)
			.data(data)
			.left(Expression.Left())
			.bottom(Expression.Bottom())
			.shape(Shape.Point)
			.size(Expression.Size())
			.set("radius",new AbstractProperty() {
				public double number(Item x) {
					return Math.sqrt(((DotItem)x).size);
				}
			})
			.fill(Fill.solid(0xaa5555, 1))
			.stroke(Property.None);
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
	
	public static class PrefuseUpdater implements Benchmark
	{
		private ActionList action;
		
		public PrefuseUpdater(List<Point2D> data) {
			Table t = new Table();
			t.addColumn("X", double.class);
			t.addColumn("Y", double.class);
			t.addRows(data.size());
			for (int i=0; i<data.size(); ++i) {
				t.setDouble(i, "X", data.get(i).getX());
				t.setDouble(i, "Y", data.get(i).getY());
			}
			
			Visualization vis = new Visualization();
			vis.add("data", t);
			
			action = new ActionList();
			action.add(new Action() {
				public void run(double arg0) {
					int index = 0;
					Table t = (Table) m_vis.getGroup("data");
					int xi = t.getColumnNumber("X");
					int yi = t.getColumnNumber("Y");
					
					Iterator<?> i = m_vis.getGroup("data").tuples();
					while (i.hasNext()) {
						VisualItem vi = (VisualItem) i.next();
						vi.setX(900 * (vi.getDouble(xi) + 0.05*(0.5 - Math.random())));
						vi.setY(400 * (vi.getDouble(yi) + 0.05*(0.5 - Math.random())));
						vi.setSize(Math.sqrt((index%20)*(index%20) + 16));
						index += 1;
					}
				}
			});
			action.add(new ShapeAction("data"));
			action.add(new Action() {
				public void run(double arg0) {
					int index = 0;
					Iterator<?> i = m_vis.getGroup("data").tuples();
					while (i.hasNext()) {
						VisualItem vi = (VisualItem) i.next();
						vi.setSize(Math.sqrt((index%20)*(index%20) + 16));
						index += 1;
					}
				}
			});
			action.add(new ColorAction("data", VisualItem.FILLCOLOR, 0xffaa5555));
			vis.putAction("update", action);
		}
		
		@Override
		public String name() { return "PREFUSE"; }
		
		@Override
		public void setup() { }
		
		@Override
		public void takedown() { }
		
		@Override
		public long[] run(int iterations) {
			long[] tt = new long[iterations];
			for (int i=0; i<iterations; ++i) {
				long t = System.currentTimeMillis();
				action.run(0);
				tt[i] = System.currentTimeMillis() - t;
			}
			return tt;
		}
	}

}
