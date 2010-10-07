package pv.render.awt.gl;

import javax.media.opengl.GL;

public class FrameBuffer {

	public final int id;
	public final int[] ids = { 0 };
	
	public FrameBuffer(GL gl) {
		gl.glGenFramebuffersEXT(1, ids, 0); id = ids[0];
	}
	
	public FBOTexture createTexture(GL gl, int width, int height) {
		gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, id);
		gl.glGenTextures(1, ids, 0);
		int tid = ids[0];
		return new FBOTexture(id, tid, width, height);
	}
}
