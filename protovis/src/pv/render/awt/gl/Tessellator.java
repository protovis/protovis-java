package pv.render.awt.gl;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.media.opengl.glu.GLUtessellatorCallbackAdapter;

public class Tessellator extends GLUtessellatorCallbackAdapter {

	GL gl; GLU glu;
	GLUtessellator _tess;
	double vertices[][] = new double[64][6];
	int vertexIndex = 0; 
	
	public void init(GL gl, GLU glu) {
		this.gl = gl;
		this.glu = glu;
	}
	
	public GLUtessellator tessellator() {
		if (_tess == null) {
			GLUtessellator tess = glu.gluNewTess();
			glu.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, this);
			glu.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, this);
			glu.gluTessCallback(tess, GLU.GLU_TESS_END, this);
			glu.gluTessCallback(tess, GLU.GLU_TESS_ERROR, this);
			//glu.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, this);
			_tess = tess;
		}
		return _tess;
	}
	
	public void free() {
		if (_tess != null) glu.gluDeleteTess(_tess);
	}
	
	public void begin(int type) {
		gl.glBegin(type);
	}

	public void end() {
		gl.glEnd();
	}
	
	public void vertex(Object vertexData) {
    	double[] pointer;
    	if (vertexData instanceof double[]) {
    		pointer = (double[]) vertexData;
    		if (pointer.length == 6)
    			gl.glColor3dv(pointer, 3);
    		gl.glVertex3dv(pointer, 0);
    	}
    }
	
	public void combine(double[] newVertex, Object[] neighborVertex, float[] weight, Object[] outData)
    {
		// copy new intersect vertex to local array
		// Because newVertex is temporal and cannot be hold by tessellator until next
		// vertex callback called, it must be copied to the safe place in the app.
		// Once gluTessEndPolygon() called, then you can safely deallocate the array.
		vertices[vertexIndex][0] = newVertex[0];
		vertices[vertexIndex][1] = newVertex[1];
		vertices[vertexIndex][2] = newVertex[2];
		
		if (neighborVertex != null && neighborVertex.length>3 &&
			((double[])neighborVertex[0]).length > 5)
		{
			double[] n0 = (double[])neighborVertex[0];
			double[] n1 = (double[])neighborVertex[1];
			double[] n2 = (double[])neighborVertex[2];
			double[] n3 = (double[])neighborVertex[3];
	
			// compute vertex color with given weights and colors of 4 neighbors
			// the neighborVertex[4] must hold required info, in this case, color.
			// neighborVertex was actually the third param of gluTessVertex() and is
			// passed into here to compute the color of the intersect vertex.
			vertices[vertexIndex][3] = weight[0] * n0[3] + weight[1] * n1[3] +
									   weight[2] * n2[3] + weight[3] * n3[3];
			vertices[vertexIndex][4] = weight[0] * n0[4] + weight[1] * n1[4] +
									   weight[2] * n2[4] + weight[3] * n3[4];
			vertices[vertexIndex][5] = weight[0] * n0[5] + weight[1] * n1[5] +
									   weight[2] * n2[5] + weight[3] * n3[5];
		}
		
		// return output data (vertex coords and others)
		outData[0] = vertices[vertexIndex];   // assign the address of new intersect vertex
		++vertexIndex;  // increase index for next vertex
    }
	
	public void error(int errnum) {
		//String estring = glu.gluErrorString(errnum);
		//System.err.println("Tessellation Error: " + estring);
	}

}
