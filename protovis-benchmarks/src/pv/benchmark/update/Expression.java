package pv.benchmark.update;

import java.awt.geom.Point2D;

import pv.mark.property.AbstractProperty;
import pv.scene.Item;

public class Expression extends AbstractProperty {

	//.left("{{(int)(parent.width * (data.getX() + 0.05*(0.5 - Math.random())))}}")
	//.bottom("{{(int)(parent.height * (data.getY() + 0.05*(0.5 - Math.random())))}}")
	//.size("{{(index%20)*(index%20) + 16}}")
	
	public static Expression Left() {
		return new Floor(
			   new Mult(new Width(new Parent()),
						new Add(new X(new Data()),
								new Mult(new Num(0.05),
										 new Sub(new Num(0.5), new Random())))));
	}
	public static Expression Bottom() {
		return new Floor( 
			   new Mult(new Height(new Parent()),
						new Add(new Y(new Data()),
								new Mult(new Num(0.05),
										 new Sub(new Num(0.5), new Random())))));
	}
	public static Expression Size() {
		return new Add(new Num(16),
					   new Mult(new Mod(new Index(), new Num(20)),
					   			new Mod(new Index(), new Num(20))));
	}
	
	public double number(Item x) {
		return 0;
	}
	
	public static class Index extends Expression {
		public double number(Item x) { return x.index; }
	}
	public static class Data extends Expression {
		public Object object(Item x) { return x.data; }
	}
	public static class Parent extends Expression {
		public Object object(Item x) { return x.parent(); }
	}
	public static class Width extends Expression {
		private Expression e;
		public Width(Expression e) { this.e = e; }
		public double number(Item x) { return ((Item)e.object(x)).width; }
	}
	public static class Height extends Expression {
		private Expression e;
		public Height(Expression e) { this.e = e; }
		public double number(Item x) { return ((Item)e.object(x)).height; }
	}
	public static class X extends Expression {
		private Expression e;
		public X(Expression e) { this.e = e; }
		public double number(Item x) { return ((Point2D)e.object(x)).getX(); }
	}
	public static class Y extends Expression {
		private Expression e;
		public Y(Expression e) { this.e = e; }
		public double number(Item x) { return ((Point2D)e.object(x)).getY(); }
	}
	public static class Floor extends Expression {
		private Expression e;
		public Floor(Expression e) { this.e = e; }
		public double number(Item x) { return (int)e.number(x); }
	}
	public static class Random extends Expression {
		public double number(Item x) { return Math.random(); }
	}
	public static class Num extends Expression {
		private double d;
		public Num(double d) { this.d = d; }
		public double number(Item x) { return d; }
	}
	
	public static class BinaryExpression extends Expression {
		public Expression left;
		public Expression right;
		
		public BinaryExpression(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}
	}
	
	public static class Mod extends BinaryExpression {
		public Mod(Expression left, Expression right) { super(left, right); }
		public double number(Item x) {
			return ((int)left.number(x)) % ((int)right.number(x));
		}
	}
	
	public static class Mult extends BinaryExpression {
		public Mult(Expression left, Expression right) { super(left, right); }
		public double number(Item x) {
			return left.number(x) * right.number(x);
		}
	}
	
	public static class Add extends BinaryExpression {
		public Add(Expression left, Expression right) { super(left, right); }
		public double number(Item x) {
			return left.number(x) + right.number(x);
		}
	}
	
	public static class Sub extends BinaryExpression {
		public Sub(Expression left, Expression right) { super(left, right); }
		public double number(Item x) {
			return left.number(x) - right.number(x);
		}
	}
	
}
