package edu.stanford.vis.examples;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pv.animate.Scheduler;
import pv.mark.Mark;
import pv.mark.Scene;
import pv.mark.Variable;
import pv.mark.constants.MarkType;
import pv.mark.constants.Shape;
import pv.mark.constants.TextBaseline;
import pv.render.Display;
import pv.render.awt.gl.GLDisplay;
import pv.style.Fill;
import pv.style.Stroke;

/**
 * This class computes and visualizes a Gaussian Mixture Model to perform
 * clustering of usage data collected from Mechnical Turk. The data represents
 * the average task completion percentage of Turkers who participated in online
 * experiments. The goal is to find the model that best clusters Turkers into
 * different categories of participation.
 * 
 * <p>The data comes from the experiments described in the paper <a
 * href="http://vis.stanford.edu/papers/crowdsourcing-graphical-perception">
 * Crowdsourcing Graphical Perception: Using Mechanical Turk to Assess
 * Visualization Design</a> by Heer and Bostock, ACM CHI 2010.</p>
 * 
 * <p>Click the visualization and hit the space bar to commence an animated
 * run of the model. Type the 'R' key to reset the model.</p>
 */
public class TurkModel {

	// variables for storing model data
	static double[] X, u, s, p;
	
	public static void main(String[] args) {
		System.out.println("Hit the space bar to toggle model fitting.");
		System.out.println("Type 'R' to reset the calculation.");
		System.out.println("Drag the slider to rescale the plots.");
		
		// read in the raw data as a list of doubles
		X = read("data/percdata.txt");
		List<Double> raw = new ArrayList<Double>();
		for (int i=0; i<X.length; ++i) {
			X[i] = Math.max(0, Math.min(1, X[i]));
			raw.add(X[i]);
		}
		
		// create models of increasing mixture count
		final List<GMM> gmm = new ArrayList<GMM>();
		for (int k=1; k<=7; ++k) {
			gmm.add(new GMM(k, X));
		}
		
		// reusable property definition for plotting density functions
		Mark dist = Mark.create()
			.datatype(Point2D.class)
			.left("{{index/5.0}}");
		// a variable to control vertical scaling of density functions
		final Variable scalar = new Variable(10);
		
		// create the root of the visualization
		final Mark vis = new Scene()
			.data(gmm) // the list of models --> 1 layer per model
			.datatype(GMM.class)
			.left(10)
			.top("{{-50 + 90*index}}") // vertically offset the layers
			.height(100)
			;
		
		// add area chart of the total probability density function
		vis.add(MarkType.Area)
			.extend(dist)
			.def("s", scalar) // the vertical scaling parameter
			.data("{{data.density()}}")
			.bottom(0)
			.height("{{s*data.getY()}}")
			.fill(Fill.solid(0xccccff))
			;

		// add one layer for each Gaussian mixture component
		vis.add(MarkType.Panel)
			.height(100)
			.data("{{data.mixtures()}}")
		// draw the density as a stroked line
		   .add(MarkType.Line)
			.extend(dist)
			.def("s", scalar) // the vertical scaling parameter
			.bottom("{{s*data.getY()}}")
			.stroke(Stroke.solid(1, 0xcc8888, 0.5))
			;
		
		// add a visualization of the raw data distribution
		// as translucent dots beneath the chart
		vis.add(MarkType.Dot)
			.data(raw)
			.datatype(Double.class)
			.left("{{200*data}}")
			.bottom(-4)
			.shape(Shape.Circle)
			.size(4)
			.fill(Fill.solid(0x0000ff, 0.1))
			.stroke(Stroke.solid(1,0,0))
			;
		
		// add labels showing model selection criteria
		// See Wikipedia articles on 'AIC' and 'BIC' for more.
		vis.add(MarkType.Label)
			.datatype(GMM.class)
			.visible("{{ parent.index < 9 }}")
			.text("{{'BIC = '+data.bic()}}")
			.bottom(0)
			.left(210)
			.textMargin(0)
			.textBaseline(TextBaseline.Bottom)
		   .add(MarkType.Label)
		    .text("{{'AIC = '+data.aic()}}")
		    .bottom(20)
			;

		vis.update();
		
		final AtomicBoolean run = new AtomicBoolean(false);
		final AtomicBoolean reset = new AtomicBoolean(false);
		Scheduler.instance().add(new Scheduler.TaskAdapter() {
			public long evaluate(long t) {
				if (reset.compareAndSet(true, false)) {
					for (GMM model : gmm) model.reset();
					vis.update();
					return 1;
				}
				if (run.get()) {
					vis.update();
					for (GMM model : gmm)
						model.run(1);
					return 1;
				}
				return 200;
			}
		});
		
		final Display display = new GLDisplay();
		display.setSize(400, 625);
		display.addScene(vis.scene());
		
		// Space bar toggles animated run of model learning
		// 'R' key resets the model to the initial configuration
		// 'P' key prints out current densities
		display.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent k) {
				switch (k.getKeyCode()) {
				case KeyEvent.VK_SPACE:
					run.set(!run.get());
					break;
				case KeyEvent.VK_R:
					reset.set(true);
					break;
				case KeyEvent.VK_P:
					synchronized (display) {
						GMM best = null; double min = Double.MAX_VALUE;
						for (GMM model : gmm) {
							if (model.bic < min) {
								min = model.bic;
								best = model;
							}
						}
						best.printDensities();
					}
					break;
				}
			}
		});
		
		// Add a slider to control the vertical scale parameter
		final JSlider slider = new JSlider(10, 100);
		slider.setOrientation(JSlider.HORIZONTAL);
		slider.setValue((Integer)scalar.value());
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				scalar.value(slider.getValue());
				vis.update();
				display.render();
			}
		});
		
		// Show the visualization
		JFrame frame = new JFrame("Mixture Model Fitting of MTurk Data");
		Container con = frame.getContentPane();
		con.add(slider, BorderLayout.NORTH);
		con.add(display.asComponent(), BorderLayout.CENTER);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setResizable(false);
	}
	
	/**
	 * Read in an array of numbers.
	 */
	public static double[] read(String file) {
		ArrayList<Double> list = new ArrayList<Double>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			
			while ((line=br.readLine()) != null) {
				double x = Double.parseDouble(line);
				list.add(x);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		double[] a = new double[list.size()];
		for (int i=0; i<list.size(); ++i) {
			a[i] = list.get(i);
		}
		return a;
	}
	
	/**
	 * This class computes a Gaussian Mixture Model and is used to
	 * provide data to the visualizations.
	 */
	public static class GMM
	{
		public int k, N; // # of mixtures, # of data points
		public double[] u, s, p; // mean, variance, mixing proportions
		public double[] X; // raw data (observations)
		public double aic, bic, loglik; // AIC, BIC, and log-likelihood values
		
		private List<List<Point2D>> densities = new ArrayList<List<Point2D>>();
		private double wk[], ws[], w[][]; // intermediate variables
		
		public String aic() {
			NumberFormat fmt = NumberFormat.getNumberInstance();
			fmt.setMaximumFractionDigits(8);
			return fmt.format(aic);
		}
		
		public String bic() {
			NumberFormat fmt = NumberFormat.getNumberInstance();
			fmt.setMaximumFractionDigits(8);
			return fmt.format(bic);
		}
		
		public List<Point2D> density() {
			return densities.get(k);
		}
		
		public List<List<Point2D>> mixtures() {
			return densities.subList(0, k);
		}
		
		public GMM(int k, double[] X) {
			this.k = k;
			u = new double[k];
			s = new double[k];
			p = new double[k];
			init(X);
		}
		
		public void init(double[] X) {
			this.X = X;
			this.N = X.length;
			double variance = 0.001;
			for (int i=0; i<k; ++i) {
				u[i] = (i + 1.0) / (k+1);
				s[i] = variance;
				p[i] = 1.0 / k;
			}
			w = new double[k][N];
			wk = new double[N];
			ws = new double[k];
			update();
		}
		
		// updates measures after optimization iterations
		private void update() {
			loglik = loglik(X,u,s,p);
			aic = 2*(3*k-1) - 2*loglik;
			bic = (3*k-1)*Math.log(N) - 2*loglik;
			density(0, 1, 0.001, densities);
		}
		
		public void reset() {
			init(X);
		}
		
		// runs iterations of the E-M algorithm to learn the model
		public double run(int iters) {
			double ll = -1;
			
			for (int iter=0; iter < iters; ++iter) {
				// -- E-step ---
				zero(wk); ll = 0;
				for (int i=0; i < k; ++i) {
					double max = Double.MIN_VALUE;
					for (int j=0; j < N; ++j) {
						w[i][j] = gaussian(X[j], u[i], s[i]) * p[i];
						wk[j] += w[i][j];
						if (w[i][j] > max) max = w[i][j];
					}
					ll += Math.log(max);
				}
				for (int j=0; j < N; ++j) {
					for (int i=0; i < k; ++i) {
						w[i][j] /= wk[j];
					}
				}
				
				// -- M-step ---
				
				// compute new means and class probabilities
				zero(ws); zero(u); zero(s); zero(p);
				for (int i=0; i < k; ++i) {
					for (int j=0; j < N; ++j) {
						u[i] += w[i][j] * X[j];
						ws[i] += w[i][j];
					}
				}
				for (int i=0; i < k; ++i) {
					u[i] /= ws[i];
					p[i] = ws[i] / N;
				}
				
				// compute new variance
				for (int i=0; i < k; ++i) {
					for (int j=0; j < N; ++j) {
						s[i] += w[i][j] * (X[j] - u[i]) * (X[j] - u[i]);
					}
				}
				for (int i=0; i < k; ++i) {
					s[i] = s[i] / ws[i];
				}
			}
			update(); // recalculate measures and densities
			return loglik;
		}
		
		public static void zero(double[] x) {
			for (int i=0; i<x.length; ++i) { x[i] = 0; }
		}
		
		public static double gaussian(double x, double u, double v) {
			x = x - u;
			v = 2 * v;
			return (1 / Math.sqrt(Math.PI*v)) * Math.exp(-(x*x) / v);
		}
		
		public static double loglik(double[] X, double[] u, double[] s, double[] p) {
			double ll = 0, n = X.length;
			for (int i=0; i<n; ++i) {
				int c = -1;
				double max = Double.MIN_VALUE, v;
				for (int j=0; j<u.length; ++j) {
					v = gaussian(X[i], u[j], s[j]);
					if (v > max) { max = v; c = j; }
				}
				if (c == -1) {
					ll = Double.NaN;
				} else {
					ll += Math.log(p[c] * max);
				}
			}
			return ll;  
		}
		
		public void density(double a, double b, double step,
			List<List<Point2D>> list)
		{
			int len = (int)((b-a) / step);
			
			list.clear();
			for (int j=0; j<=k; ++j) {
				list.add(new ArrayList<Point2D>());
			}
			for (int i=0; i < len; ++i, a += step) {
				double y = 0, ys = 0;
				for (int j=0; j<k; ++j) {
					y = p[j] * gaussian(a, u[j], s[j]);
					ys += y;
					list.get(j).add(new Point2D.Double(a, y));
				}
				list.get(k).add(new Point2D.Double(a, ys));
			}
		}
		
		public void printDensities() {
			int i = 0;
			System.out.println("{");
			List<Point2D> list = densities.get(0);
			//for(List<Point2D> list : densities) {
				if (i > 0) System.out.println(",");
				System.out.print("\""+i+"\":[");
				int j = 0;
				for (Point2D p : list) {
					if (j > 0) System.out.print(",");
					System.out.print("["+p.getX()+","+p.getY()+"]");
					++j;
				}
				System.out.print("]");
				++i;
			//}
			System.out.println("\n}");
		}
	}
}
