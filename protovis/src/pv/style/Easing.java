package pv.style;

import pv.util.IntObjectHashMap;

/*
Disclaimer for Robert Penner's Easing Equations license:

TERMS OF USE - EASING EQUATIONS

Open source under the BSD License.

Copyright © 2001 Robert Penner
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    * Neither the name of the author nor the names of contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/**
 * Collection of easing functions to control animation rates.
 * 
 * <p>Many of these easing functions are adapted from Robert Penner's
 * <a href="http://www.robertpenner.com/">collection of easing functions</a>.
 * </p>
 */
public class Easing {

	public static final Easing None = new Easing();
	public static final Easing Sigmoid = new Sigmoid();
	public static final Easing Sine = new Sine();
	public static final Easing Exponential = new Exponential();
	public static final Easing Circular = new Circular();
	
	private static final IntObjectHashMap map_poly = new IntObjectHashMap();
	public static final Easing Poly(double exp) {
		float e = (float)exp;
		int key = Float.floatToIntBits(e);
		Easing poly = (Easing) map_poly.get(key);
		if (poly == null) {
			map_poly.put(key, (poly = new Polynomial(e)));
		}
		return poly;
	}
	
	public float ease(float t) { return t; }
	
	public static class Polynomial extends Easing {
		private float _exp;
		public Polynomial(float exp) { _exp = exp; }
		public float ease(float t) {
			if (t < 0)   return 0;
        	if (t > 1)   return 1;
        	if (t < .5f) return 0.5f * (float)Math.pow(2*t, _exp);
        	else         return 0.5f * (float)(2 - Math.pow(2*(1-t), _exp));
		}
	}
	
	public static class Sigmoid extends Easing {
		public float ease(float t) {
	        return 1f / (float)(1.0 + Math.exp(6 - 12*t));
		}
	}
	
	/**
	 * Easing equation function for a sinusoidal (sin(t)) easing in/out: acceleration until halfway, then deceleration.
	 * @param t		Current time (an animation fraction between 0 and 1).
	 * @return		The correct value.
	 */
	public static class Sine extends Easing {
		public float ease(float t) {
			return -0.5f * (float)(Math.cos(Math.PI*t) - 1);
		}
	}
		
	/**
	 * Easing equation function for an exponential (2^t) easing in/out: acceleration until halfway, then deceleration.
	 *
	 * @param t		Current time (an animation fraction between 0 and 1).
	 * @return		The correct value.
	 */
	public static class Exponential extends Easing {
		public float ease(float t) {
			if (t==0 || t==1) return t;
			if (t < 0.5) return 0.5f * (float)Math.pow(2, 10*(2*t-1)) - 0.0005f;
			return 0.5f * 1.0005f * (float)(-Math.pow(2, -10 * (2*t-1)) + 2);
		}
	}
	
	/**
	 * Easing equation function for a circular (sqrt(1-t^2)) easing in/out: acceleration until halfway, then deceleration.
	 *
	 * @param t		Current time (an animation fraction between 0 and 1).
	 * @return		The correct value.
	 */
	public static class Circular extends Easing {
		public float ease(float t) {
			if (t < 0.5) return -0.5f * (float)(Math.sqrt(1 - 4*t*t) - 1);
			return 0.5f * (float)(Math.sqrt(1 - (t=2*t-2)*t) + 1);
		}
	}

	/**
	 * Easing equation function for an elastic (exponentially decaying sine wave) easing in/out: acceleration until halfway, then deceleration.
	 *
	 * @param t		Current time (an animation fraction between 0 and 1).
	 * @param a		Amplitude.
	 * @param p		Period.
	 * @return		The correct value.
	 */
	public static class Elastic extends Easing {
		public float a = Float.NaN, p = Float.NaN;
		public Elastic() { }
		public Elastic(float a, float p) { this.a = a; this.p = p; }
		public float easeIn(float t) {
			if (t<=0 || t>=1) return t;  if (Float.isNaN(p)) p=0.45f;
			double s;
			if (Float.isNaN(a) || a < Math.abs(1)) { a=1; s=p/4; }
			else s = p/(2*Math.PI) * Math.asin (1/a);
			return (float) -(a*Math.pow(2,10*(t-=1)) * Math.sin( (t-s)*(2*Math.PI)/p ));
		}
		public float ease(float t) {
			if (t < 0.5) return 0.5f * (float)easeIn(2*t);
			return 0.5f * (float)(1 + 1 - easeIn(1-(2*t-1)));
			
		}
	}
		
}
