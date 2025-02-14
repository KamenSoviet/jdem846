package us.wthr.jdem846.graphics;

import us.wthr.jdem846.exception.GraphicsRenderException;
import us.wthr.jdem846.graphics.framebuffer.FrameBuffer;
import us.wthr.jdem846.graphics.framebuffer.FrameBufferModeEnum;
import us.wthr.jdem846.math.Matrix;
import us.wthr.jdem846.math.Vector;

public interface IRenderer
{

	public RenderCodesEnum getError();

	public void initialize(int width, int height);
	
	public void setBillboard(Vector cam, Vector objPos);
	
	public void setLighting(Vector position, double emission, double ambiant, double diffuse, double specular, double shininess);
	public void setLighting(Vector position, IColor emission, IColor ambiant, IColor diffuse, IColor specular, double shininess);
	public void disableLighting();
	public void enableLighting();
	
	public void setMaterial(double emission, double ambiant, double diffuse, double specular, double shininess);
	public void setMaterial(IColor emission, IColor ambiant, IColor diffuse, IColor specular, double shininess);
	public void disableMaterial();
	
	public void enableFog(IColor fogColor, FogModeEnum mode, double density, double start, double end);
	public void disableFog();
	
	public void normal(Vector normal);
	
	public void pushMatrix();

	public void popMatrix();

	public void setFrameBuffer(FrameBuffer frameBuffer);
	
	
	public int getMaximumTextureWidth();
	public int getMaximumTextureHeight();
	
	public void viewPort(int x, int y, int width, int height);

	public void viewPort(int x, int y, int width, int height, FrameBufferModeEnum bufferMode);

	public void matrixMode(MatrixModeEnum mode) throws GraphicsRenderException;

	public void multMatrix(Matrix m);
	public void loadIdentity();

	public void ortho(double left, double right, double bottom, double top, double nearval, double farval);

	public void perspective(double fov, double aspect, double near, double far);

	public void lookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ);

	public boolean bindTexture(Texture texture);
	public boolean bindTexture(Texture texture, TextureMapConfiguration configuration);
	
	public void unbindTexture();

	public boolean isTextureBound();

	public void clear(IColor backgroundColor);
	public void clear(int backgroundColor);
	
	public void clear();
	public void clearColorBuffer();
	public void clearColorBuffer(int backgroundColor);
	public void clearDepthBuffer();
	

	public void rotate(double angle, AxisEnum axis);

	public void translate(double x, double y, double z);

	public void scale(double x, double y, double z);

	public void begin(PrimitiveModeEnum mode);

	public void end();
	
	public void color(IColor color);
	public void color(int[] color);
	public void color(int color);

	public void texCoord(double left, double front);

	public void vertex(Vector v);
	public void vertex(double x, double y, double z);

	public boolean project(Vector v);

	public ImageCapture captureImage();
	
	
	
	public void finish();
	
	public void dispose();
}
