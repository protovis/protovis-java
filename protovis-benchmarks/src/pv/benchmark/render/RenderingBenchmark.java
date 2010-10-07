package pv.benchmark.render;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import pv.benchmark.Benchmark;
import pv.benchmark.BenchmarkRunner;
import pv.benchmark.SceneBenchmark;
import pv.mark.Scene;
import pv.mark.constants.MarkType;
import pv.render.awt.CameraControl;
import pv.render.awt.gl.GLDisplay;
import pv.render.awt.gl.GLRenderer;
import pv.style.Fill;
import pv.style.Stroke;

public class RenderingBenchmark {

	public static void main(String[] args) {
		new RenderingBenchmark();
	}
	
	private static class PDisplay extends GLDisplay {
		private static final long serialVersionUID = -5204253651992453979L;
		public void setRenderer(GLRenderer r) {
			_renderer = r;
		}
	}

	private List<Integer> data, layers;
	private PDisplay display;
	
	public RenderingBenchmark()
	{	
		int NDATA = 1000, NLAYERS = 50;
		
		data = new ArrayList<Integer>();
		for (int i=0; i<NDATA; ++i) data.add(i);
		
		layers = new ArrayList<Integer>();
		for (int i=0; i<NLAYERS; ++i) layers.add(i);
		
		display = new PDisplay();
		display.setSize(1024, 768);
		new CameraControl().attach(display);
		
		JFrame frame = new JFrame("Parallel Rendering Benchmark");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(display);
		frame.pack();
		frame.setVisible(true);
		
		BenchmarkRunner.instance.iterations(101);
		
		display.setRenderer(GLRendererS.instance());
		for (Benchmark b : benchmarks()) {
			BenchmarkRunner.instance.run("SEQ", b);
		}
		
		display.setRenderer(GLRendererP.instance());
		for (Benchmark b : benchmarks()) {
			BenchmarkRunner.instance.run("PAR", b);
		}
		
		System.exit(0);
	}
	
	public List<Benchmark> benchmarks() {
		List<Benchmark> list = new ArrayList<Benchmark>();
		
		list.add(new SceneBenchmark(display, "BARS",
			new Scene()
				.top(5).left(5)
				.width(1024).height(768)
			   .add(MarkType.Panel)
				.data(layers)
				.height(768)
			   .add(MarkType.Bar)
			    .data(data)
			   	.left("{{ index }}")
			   	.width(1)
			   	.height("{{7 - (index % 7)}}")
			   	.top("{{ cousin==null ? 0 : cousin.top + cousin.height }}")
			   	.fill(Fill.solid(0x4455aa))
			   .scene()
		));
		
		list.add(new SceneBenchmark(display, "PATH",
			new Scene()
				.top(5).left(5)
				.width(1024).height(768)
			.add(MarkType.Panel)
				.data(layers)
				.top("{{2*index}}")
			   .add(MarkType.Line)
			    .segmented(true)
			    .data(data)
			   	.left("{{ 10*index }}")
			   	.top("{{ 7*Math.random() }}")//item.sibling()==null ? 7*Math.random() : Math.min(7, Math.max(0, item.sibling().top + 2*(0.5 - Math.random()))) }}")
			   	.stroke("{{ Stroke.solid(1 + Math.round(2*Math.random()), index==500 ? 0xff0000 : 0x3344aa) }}")
			   .scene()
		));
		
		list.add(new SceneBenchmark(display, "DOTS",
			new Scene()
				.top(50).left(50)
				.width(1024).height(768)
			   .add(MarkType.Panel)
				.data(layers)
				.height(768)
			   .add(MarkType.Dot)
			    .data(data)
			   	.left("{{ 924 * Math.random() }}")
			   	.top("{{ 668 * Math.random() }}")
			   	.shape("circle")
			   	.size(25)
			   	.stroke(Stroke.solid(1, 0x3344ff))
			   .scene()
		));
		
		return list;
	}
	
}
