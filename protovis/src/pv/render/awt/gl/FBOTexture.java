package pv.render.awt.gl;

import javax.media.opengl.GL;

public class FBOTexture {

	public final int fid;
	public final int tid;
	public final int width;
	public final int height;
	public final int format = GL.GL_RGBA;
	
	public FBOTexture(int frameBufferID, int textureID, int width, int height) {
		this.fid = frameBufferID;
		this.tid = textureID;
		this.width = width;
		this.height = height;
	}
	
	public boolean init(GL gl) {
		//gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
		gl.glBindTexture(GL.GL_TEXTURE_2D, tid);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, format, width, height, 0, format,
			GL.GL_UNSIGNED_BYTE, null);
			//BufferUtil.newByteBuffer(width*height*4));
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT,
			GL.GL_COLOR_ATTACHMENT0_EXT, GL.GL_TEXTURE_2D, tid, 0);
		gl.glGenerateMipmapEXT(GL.GL_TEXTURE_2D);
		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
		
		int status = gl.glCheckFramebufferStatusEXT(GL.GL_FRAMEBUFFER_EXT);
		if (status == GL.GL_FRAMEBUFFER_COMPLETE_EXT) {
			gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
			return true;
		} else {
			return false;
		}
	}
	
	public void begin(GL gl, boolean clear) {
		gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, fid);
		//gl.glPushAttrib(GL.GL_VIEWPORT_BIT);
		//gl.glViewport(0, 0, width, height);
		if (clear) {
			gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
	        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		}
		
		GLRenderer r = GLRenderer.instance();
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, r.width(), 0, r.height(), 0, 1);
		gl.glMatrixMode(GL.GL_MODELVIEW);
	}

	public void end(GL gl) {
		GLRenderer r = GLRenderer.instance();
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, r.width(), r.height(), 0, 0, 1);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		
		//gl.glPopAttrib();
		gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
	}

	public void draw(GL gl, float x, float y) {
		draw(gl, x, y, width, height);
	}
	
	public void draw(GL gl, float x, float y, float w, float h) {
		gl.glColor4f(1,1,1,1);
		gl.glBindTexture(GL.GL_TEXTURE_2D, tid);
		gl.glBegin(GL.GL_QUADS);
		gl.glTexCoord2f(0, 0); gl.glVertex2f(x+0, y+0);
		gl.glTexCoord2f(1, 0); gl.glVertex2f(x+w, y+0);
		gl.glTexCoord2f(1, 1); gl.glVertex2f(x+w, y+h);
		gl.glTexCoord2f(0, 1); gl.glVertex2f(x+0, y+h);
		gl.glEnd();
		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
	}
	
	public void bind(GL gl) {
		gl.glBindTexture(GL.GL_TEXTURE_2D, tid);
		//gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, wrap_s);
		//gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, wrap_t);
		//gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, mag_filter);
		//gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, min_filter);
		//gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, mode);
		//gl.glEnable(GL.GL_TEXTURE_2D);
	}

	public void unbind(GL gl) {
		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
	}
	
	public void free(GL gl) {
		int[] ids = {tid};
		gl.glDeleteTextures(1, ids, 0);
	}
}
