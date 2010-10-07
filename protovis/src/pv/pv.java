package pv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import pv.util.Objects;

import com.google.common.base.Function;

@SuppressWarnings("unchecked")
/**
 * The pv class provides a collection of common utility methods.
 */
public class pv {

	public static final Function<?,?> identity = new Function<Object,Object>()
	{
		public Object apply(Object from) { return from; }
	};
	
	public static final Comparator<Number> numberOrder = new Comparator<Number>() {
		public int compare(Number a, Number b) {
			double x = a.doubleValue(), y = b.doubleValue();
			return (x < y ? -1 : x > y ? 1 : 0);
		}
	};
	
	public static List<Double> range(double start, double end, double step) {
		List<Double> list = (List<Double>) Objects.List.get();
		if (step > 0) {
			for (double x = start; x < end; x += step)
				list.add(x);
		} else {
			for (double x = start; x > end; x += step)
				list.add(x);
		}
		return list;
	}
	
	public static <T> List<T> repeat(List<T> list, int n) {
		List<T> l = (List<T>) Objects.List.get();
		for (int i=0; i<n; ++i)
			l.addAll(list);
		return l;
	}
	
	public static <T> List<T> blend(List<T>... lists) {
		List<T> l = (List<T>) Objects.List.get();
		for (int i=0; i<lists.length; ++i)
			l.addAll(lists[i]);
		return l;
	}
	
	public static <F,T> List<T> map(Iterable<F> list, Function<? super F,T> f) {
		if (f == null) f = (Function<? super F,T>) identity;
		List<T> l = (List<T>) Objects.List.get();
		for (F obj : list) {
			l.add(f.apply(obj));
		}
		return l;
	}
	
	public static <F> List<Double> normalize(List<F> list, Function<? super F,Double> f) {
		List<Double> norm = map(list, f);
		double sum = sum(norm);
		for (int i=0; i<norm.size(); ++i) {
			norm.set(i, norm.get(i) / sum);
		}
		return norm;
	}
	
	public static double sum(Iterable<? extends Number> list) {
		double sum = 0;
		for (Number x : list) {
			sum += x.doubleValue();
		}
		return sum;
	}
	
	public static <F> double sum(Iterable<F> list, Function<? super F,? extends Number> f) {
		double sum = 0;
		for (F obj : list) {
			sum += f.apply(obj).doubleValue();
		}
		return sum;
	}
	
	public static <T extends Comparable> T max(Iterable<T> list) {
		T max = null;
		for (T obj : list) {
			if (max == null || obj.compareTo(max) > 0) {
				max = obj;
			}
		}
		return max;
	}
	
	public static <F,T extends Comparable> T max(Iterable<F> list, Function<F,T> f) {
		T max = null;
		for (F from : list) {
			T obj = f.apply(from);
			if (max == null || obj.compareTo(max) > 0) {
				max = obj;
			}
		}
		return max;
	}
	
	public static <T extends Comparable> int maxIndex(Iterable<T> list) {
		T max = null; int i = 0, maxi = -1;
		for (T obj : list) {
			if (max == null || obj.compareTo(max) > 0) {
				max = obj; maxi = i;
			}
			++i;
		}
		return maxi;
	}
	
	public static <F,T extends Comparable> int maxIndex(Iterable<F> list, Function<F,T> f) {
		T max = null; int i = 0, maxi = -1;
		for (F from : list) {
			T obj = f.apply(from);
			if (max == null || obj.compareTo(max) > 0) {
				max = obj; maxi = i;
			}
			++i;
		}
		return maxi;
	}
	
	public static <T extends Comparable> T min(Iterable<T> list) {
		T min = null;
		for (T obj : list) {
			if (min == null || obj.compareTo(min) < 0) {
				min = obj;
			}
		}
		return min;
	}
	
	public static <F,T extends Comparable> T min(Iterable<F> list, Function<F,T> f) {
		T min = null;
		for (F from : list) {
			T obj = f.apply(from);
			if (min == null || obj.compareTo(min) < 0) {
				min = obj;
			}
		}
		return min;
	}
	
	public static <T extends Comparable> int minIndex(Iterable<T> list) {
		T max = null; int i = 0, mini = -1;
		for (T obj : list) {
			if (max == null || obj.compareTo(max) < 0) {
				max = obj; mini = i;
			}
			++i;
		}
		return mini;
	}
	
	public static <F,T extends Comparable> int minIndex(Iterable<F> list, Function<F,T> f) {
		T max = null; int i = 0, mini = -1;
		for (F from : list) {
			T obj = f.apply(from);
			if (max == null || obj.compareTo(max) < 0) {
				max = obj; mini = i;
			}
			++i;
		}
		return mini;
	}
	
	public static double mean(Collection<Double> col) {
		return sum(col) / col.size();
	}
	
	public static <F> double mean(Collection<F> col, Function<? super F,Double> f) {
		return sum(col, f) / col.size();
	}
	
	public static <F,T extends Comparable> T median(Collection<F> col, Function<? super F,T> f) {
		List<T> list = map(col, f); Collections.sort(list);
		return list.get(list.size()/2);
	}
	
	public static <F> Double median(Collection<F> col, Function<? super F,Number> f) {
		List<Number> list = map(col, f); Collections.sort(list, numberOrder);
		int i = list.size() / 2;
		if (list.size() % 2 > 0) return list.get(i).doubleValue();
		return 0.5 * (list.get(i-1).doubleValue() + list.get(i).doubleValue());
	}
	
	public static <F> List<F> permute(List<F> list,
		List<? extends Number> indices)
	{
		List<F> l = new ArrayList<F>(list.size());
		for (int i=0; i<indices.size(); ++i) {
			l.set(i, list.get(indices.get(i).intValue()));
		}
		return l;
	}
	
	public static <F,T> List<T> permute(List<F> list,
		List<? extends Number> indices, Function<? super F,T> f)
	{
		List<T> l = new ArrayList<T>(list.size());
		for (int i=0; i<indices.size(); ++i) {
			l.set(i, f.apply(list.get(indices.get(i).intValue())));
		}
		return l;
	}

	// --
	
	public static double log(double x, double b) {
		return Math.log(x) / Math.log(b);
	}

	public static double logSymmetric(double x, double b) {
		return (x == 0) ? 0 : ((x < 0) ? -log(-x, b) : log(x, b));
	}
	
	public static double logAdjusted(double x, double b) {
		boolean negative = x < 0;
		if (x < b) x += (b - x) / b;
		return negative ? -log(x, b) : log(x, b);
	}
	
	public static double logFloor(double x, double b) {
		return (x > 0)
			? Math.pow(b, Math.floor(log(x, b)))
			: -Math.pow(b, -Math.floor(-log(-x, b)));
	}
	
	public static double logCeil(double x, double b) {
		return (x > 0)
			? Math.pow(b, Math.ceil(pv.log(x, b)))
			: -Math.pow(b, -Math.ceil(-pv.log(-x, b)));
	}
	
}
