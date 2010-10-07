package pv.render.awt;

import java.awt.Color;

import pv.util.IntObjectHashMap;

/**
 * This class provides methods for mapping ARGB color values to
 * Java {@link java.awt.Color} instances. A cache is maintained for
 * quick-lookups, avoiding the need to repeatedly allocate new Color
 * instances.
 */
public class Colors extends pv.style.Color {

	private static final IntObjectHashMap colorMap = new IntObjectHashMap();
    private static int misses = 0;
    private static int lookups = 0;
	
	// ------------------------------------------------------------------------
    // java.awt.Color Lookup Methods
    
    /**
     * Get a Java Color object for the given red, green, blue, and alpha values
     * as floating point numbers in the range 0-1.0.
     * @param r the red color component (in the range 0-1.0)
     * @param g the green color component (in the range 0-1.0)
     * @param b the blue color component (in the range 0-1.0)
     * @param a the alpha (transparency) component (in the range 0-1.0)
     * @return a Java Color object
     */
    public static Color getColor(float r, float g, float b, float a) {
        return getColor(rgba(r,g,b,a));
    }

    /**
     * Get a Java Color object for the given red, green, and blue values
     * as floating point numbers in the range 0-1.0.
     * @param r the red color component (in the range 0-1.0)
     * @param g the green color component (in the range 0-1.0)
     * @param b the blue color component (in the range 0-1.0)
     * @return a Java Color object
     */
    public static Color getColor(float r, float g, float b) {
        return getColor(r,g,b,1.0f);
    }
    
    /**
     * Get a Java Color object for the given red, green, and blue values.
     * @param r the red color component (in the range 0-255)
     * @param g the green color component (in the range 0-255)
     * @param b the blue color component (in the range 0-255)
     * @param a the alpa (transparency) component (in the range 0-255)
     * @return a Java Color object
     */
    public static Color getColor(int r, int g, int b, int a) {
        return getColor(rgba(r,g,b,a));
    }
    
    /**
     * Get a Java Color object for the given red, green, and blue values.
     * @param rgb an rgb integer
     * @param a the alpa (transparency) component (in the range 0-1)
     * @return a Java Color object
     */
    public static Color getColor(int rgb, double alpha) {
        return getColor(rgba(rgb, alpha));
    }
    
    /**
     * Get a Java Color object for the given red, green, and blue values.
     * @param r the red color component (in the range 0-255)
     * @param g the green color component (in the range 0-255)
     * @param b the blue color component (in the range 0-255)
     * @return a Java Color object
     */
    public static Color getColor(int r, int g, int b) {
        return getColor(r,g,b,255);
    }
    
    /**
     * Get a Java Color object for the given grayscale value.
     * @param v the grayscale value (in the range 0-255, 0 is
     * black and 255 is white)
     * @return a Java Color object
     */
    public static Color getGrayscale(int v) {
        return getColor(v,v,v,255);
    }
    
    /**
     * Get a Java Color object for the given color code value.
     * @param rgba the integer color code containing red, green,
     * blue, and alpha channel information
     * @return a Java Color object
     */
    public static Color getColor(int rgba) {
        Color c = null;
        if ( (c=(Color)colorMap.get(rgba)) == null ) {
            c = new Color(rgba,true);
            colorMap.put(rgba,c);
            misses++;
        }
        lookups++;
        return c;
    }
    
    // ------------------------------------------------------------------------
    // ColorLib Statistics and Cache Management
    
    /**
     * Get the number of cache misses to the Color object cache.
     * @return the number of cache misses
     */
    public static int getCacheMissCount() {
        return misses;
    }

    /**
     * Get the number of cache lookups to the Color object cache.
     * @return the number of cache lookups
     */
    public static int getCacheLookupCount() {
        return lookups;
    }
    
    /**
     * Clear the Color object cache.
     */
    public static void clearCache() {
        colorMap.clear();
    }
	
}

