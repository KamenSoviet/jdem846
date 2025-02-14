package us.wthr.jdem846.image;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import us.wthr.jdem846.DemConstants;
import us.wthr.jdem846.buffers.BufferFactory;
import us.wthr.jdem846.buffers.IIntBuffer;
import us.wthr.jdem846.canvas.CanvasProjection;
import us.wthr.jdem846.exception.DataSourceException;
import us.wthr.jdem846.gis.CoordinateSpaceAdjuster;
import us.wthr.jdem846.gis.exceptions.MapProjectionException;
import us.wthr.jdem846.gis.projections.EquirectangularProjection;
import us.wthr.jdem846.gis.projections.MapPoint;
import us.wthr.jdem846.gis.projections.MapProjection;
import us.wthr.jdem846.graphics.Color;
import us.wthr.jdem846.graphics.Colors;
import us.wthr.jdem846.graphics.IColor;
import us.wthr.jdem846.graphics.Texture;
import us.wthr.jdem846.input.InputSourceData;
import us.wthr.jdem846.logging.Log;
import us.wthr.jdem846.logging.Logging;
import us.wthr.jdem846.math.MathExt;
import us.wthr.jdem846.util.ColorUtil;

public class SimpleGeoImage implements InputSourceData, ISimpleGeoImage
{
	@SuppressWarnings("unused")
	private static Log log = Logging.getLog(SimpleGeoImage.class);
	
	private enum RoundingType {
		ROUND,
		FLOOR,
		CEIL
	}
	
	private String imageFile;

	private IIntBuffer rasterBuffer = null;

	private IImageDefinition imageDefinition;
	
	private MapProjection mapProjection;
	private CanvasProjection canvasProjection;

	private CoordinateSpaceAdjuster coordinateSpaceAdjuster;

	private MapPoint mapPoint = new MapPoint();

	private boolean hasAlphaChannel = false;

	public SimpleGeoImage()
	{
		this.imageDefinition = new ImageDefinition();
	}
	
	public SimpleGeoImage(String imagePath, double north, double south, double east, double west) throws DataSourceException
	{
		this(imagePath, north, south, east, west, 1.0);
	}
	
	public SimpleGeoImage(String imagePath, double north, double south, double east, double west, double layerTransparency) throws DataSourceException
	{
		imageFile = imagePath;

		Dimension dimensions = null;

		try {
			dimensions = fetchImageDimensions();
		} catch (IOException ex) {
			throw new DataSourceException("Error determining image dimensions: " + ex.getMessage(), ex);
		}
		
		this.imageDefinition = new ImageDefinition();
		imageDefinition.setImageHeight(dimensions.height);
		imageDefinition.setImageWidth(dimensions.width);
		imageDefinition.setNorth(north);
		imageDefinition.setSouth(south);
		imageDefinition.setEast(east);
		imageDefinition.setWest(west);
		imageDefinition.setLayerTransparency(layerTransparency);
		
		update();
	}

	public void update()
	{
		if (imageDefinition.getLatitudeResolution() == 0) {
			imageDefinition.determineLatitudeResolution();
		}
		
		if (imageDefinition.getLongitudeResolution() == 0) {
			imageDefinition.determineLongitudeResolution();
		}

		mapProjection = new EquirectangularProjection(imageDefinition.getNorth()
													, imageDefinition.getSouth()
													, imageDefinition.getEast()
													, imageDefinition.getWest()
													, imageDefinition.getImageWidth()
													, imageDefinition.getImageHeight());
		
		canvasProjection = new CanvasProjection(mapProjection
												, imageDefinition.getNorth()
												, imageDefinition.getSouth()
												, imageDefinition.getEast()
												, imageDefinition.getWest()
												, imageDefinition.getImageWidth()
												, imageDefinition.getImageHeight());

		coordinateSpaceAdjuster = new CoordinateSpaceAdjuster(imageDefinition.getNorth()
												, imageDefinition.getSouth()
												, imageDefinition.getEast()
												, imageDefinition.getWest());

	}

	public boolean isLoaded()
	{
		return (rasterBuffer != null);
	}
	
	
	public void load() throws DataSourceException
	{
		load(false);
	}
	
	public void load(boolean useHeap) throws DataSourceException
	{
		if (isLoaded()) {
			throw new DataSourceException("Image data already loaded");
		}
		
		BufferedImage image = null;
		try {
			image = (BufferedImage) ImageIcons.loadImage(this.imageFile);
		} catch (IOException ex) {
			throw new DataSourceException("Failed to load image: " + ex.getMessage(), ex);
		}

		Raster raster = image.getRaster();
		Raster alphaRaster = image.getAlphaRaster();
		
		if (alphaRaster != null) {
			hasAlphaChannel = true;
		} else {
			hasAlphaChannel = false;
		}
		
		int[] rgba = {0, 0, 0, 0};

		long capacity = raster.getWidth() * raster.getHeight();
		
		if (useHeap && ((int)capacity) > 0) { // If we're being asked to locate the data in heap memory and the capacity values doesn't overflow an integer type.
			this.rasterBuffer = BufferFactory.allocateStandardCapacityIntBuffer((int)capacity);
		} else {
			this.rasterBuffer = BufferFactory.allocateIntBuffer(capacity);
		}
		
		for (int y = 0; y < raster.getHeight(); y++) {
			for (int x = 0; x < raster.getWidth(); x++) {
				
				raster.getPixel(x, y, rgba);
				if (!hasAlphaChannel) {
					rgba[3] = 0xFF;
				} 
				
				long index = ((long) y * (long)raster.getWidth()) + ((long)x);
				int c = ColorUtil.rgbaToInt(rgba);
				rasterBuffer.put(index, c);
				
			}
		}
		
		image = null;
		raster = null;	
		
	}

	public void unload() throws DataSourceException
	{
		if (!isLoaded()) {
			throw new DataSourceException("Image data not loaded");
		}
		
		//rasterBuffer.dispose(); // Cannot dispose, the buffer may be shared with another copy
		rasterBuffer = null;
	}

	public boolean contains(double latitude, double longitude)
	{
		return coordinateSpaceAdjuster.contains(latitude, longitude);
	}

	protected Dimension fetchImageDimensions() throws IOException
	{
		ImageInputStream in = ImageIO.createImageInputStream(new File(this.imageFile));

		try {
			final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			if (readers.hasNext()) {
				ImageReader reader = readers.next();
				try {
					reader.setInput(in);
					return new Dimension(reader.getWidth(0), reader.getHeight(0));
				} finally {
					reader.dispose();
				}
			}
		} finally {
			if (in != null)
				in.close();
		}

		return null;
	}
	
	
	public IColor getColor(double latitude, double longitude) throws DataSourceException
	{
		return getColor(latitude, longitude, false);
	}

	public IColor getColor(double latitude, double longitude, boolean nearestNeighbor) throws DataSourceException
	{
		if (!isLoaded()) {
			throw new DataSourceException("Image data not loaded");
		}
		return getColor(latitude, longitude, imageDefinition.getLatitudeResolution(), imageDefinition.getLongitudeResolution(), nearestNeighbor);
	}

	public IColor getColor(double latitude, double longitude, double effectiveLatitudeResolution, double effectiveLongitudeResolution) throws DataSourceException
	{
		return getColor(latitude, longitude, effectiveLatitudeResolution, effectiveLongitudeResolution, false);
	}

	public IColor getColor(double latitude, double longitude, double effectiveLatitudeResolution, double effectiveLongitudeResolution, boolean nearestNeighbor) throws DataSourceException
	{

		double adjLatitude = 0;
		double adjLongitude = 0;

		if ((adjLatitude = coordinateSpaceAdjuster.adjustLatitude(latitude)) == DemConstants.ELEV_NO_DATA) {
			return null;
		}

		if ((adjLongitude = coordinateSpaceAdjuster.adjustLongitude(longitude)) == DemConstants.ELEV_NO_DATA) {
			return null;
		}

		return _getColor(adjLatitude, adjLongitude, effectiveLatitudeResolution, effectiveLongitudeResolution, nearestNeighbor);

	}

	protected IColor _getColor(double latitude, double longitude, double effectiveLatitudeResolution, double effectiveLongitudeResolution) throws DataSourceException
	{
		return _getColor(latitude, longitude, effectiveLatitudeResolution, effectiveLongitudeResolution, false);
	}

	protected IColor _getColor(double latitude, double longitude, double effectiveLatitudeResolution, double effectiveLongitudeResolution, boolean nearestNeighbor) throws DataSourceException
	{
		if (!isLoaded()) {
			throw new DataSourceException("Image data not loaded");
		}

		if (effectiveLatitudeResolution == DemConstants.ELEV_UNDETERMINED) {
			effectiveLatitudeResolution = imageDefinition.getLatitudeResolution();
		}

		if (effectiveLongitudeResolution == DemConstants.ELEV_UNDETERMINED) {
			effectiveLongitudeResolution = imageDefinition.getLongitudeResolution();
		}

		if (latitude >= imageDefinition.getSouth() && latitude <= imageDefinition.getNorth() && longitude >= imageDefinition.getWest() && longitude <= imageDefinition.getEast()) {

			if (nearestNeighbor) {
				return getColorNearestNeighbor(latitude, longitude);
			} else {

				double north = latitude + (effectiveLatitudeResolution / 2.0);
				double south = latitude - (effectiveLatitudeResolution / 2.0);

				double west = longitude - (effectiveLongitudeResolution / 2.0);
				double east = longitude + (effectiveLongitudeResolution / 2.0);

				double samples = 0;

				double rows = (north - south) / imageDefinition.getLatitudeResolution();
				double columns = (east - west) / imageDefinition.getLongitudeResolution();

				if (rows < 1 && columns < 1) {
					return getColorBilinear(latitude, longitude);
				} else {
					
					IColor c = null;
					int[] rgba = {0, 0, 0, 0};
					
					for (double x = west; x <= east; x += imageDefinition.getLongitudeResolution()) {
						for (double y = north; y >= south; y -= imageDefinition.getLatitudeResolution()) {
							if ((c = getColorBilinear(y, x)) != null) {
								rgba[0] += c.getRed();
								rgba[1] += c.getGreen();
								rgba[2] += c.getBlue();
								rgba[3] += c.getAlpha();
								samples++;
							}
						}
					}

					if (samples > 0) {
						rgba[0] = (int) MathExt.round((double) rgba[0] / samples);
						rgba[1] = (int) MathExt.round((double) rgba[1] / samples);
						rgba[2] = (int) MathExt.round((double) rgba[2] / samples);
						rgba[3] = (int) MathExt.round((double) rgba[3] / samples);
					}
					
					return new Color(rgba);
				}
			}

		} else {
			return null;
		}
	}

	public IColor getColorBilinear(double latitude, double longitude) throws DataSourceException
	{
		if (!isLoaded()) {
			throw new DataSourceException("Image data not loaded");
		}
		
		PixelValue p00 = getColorNearestNeighbor(latitude, RoundingType.FLOOR, longitude, RoundingType.FLOOR, true, true);
		PixelValue p01 = getColorNearestNeighbor(latitude, RoundingType.FLOOR, longitude, RoundingType.CEIL, true, true);
		PixelValue p10 = getColorNearestNeighbor(latitude, RoundingType.CEIL, longitude, RoundingType.FLOOR, true, true);
		PixelValue p11 = getColorNearestNeighbor(latitude, RoundingType.CEIL, longitude, RoundingType.CEIL, true, true);
		
		p00 = getFirstNotNull(p00, p01, p10, p11);
		p01 = getFirstNotNull(p01, p11, p00, p10);
		p10 = getFirstNotNull(p10, p00, p11, p01);
		p11 = getFirstNotNull(p11, p01, p10, p00);
		
		if (p00 == null) {
			return null;
		}
		
		IColor c00 = p00.getColor();
		IColor c01 = p01.getColor();
		IColor c10 = p10.getColor();
		IColor c11 = p11.getColor();
		
		c00 = getFirstNotNull(c00, c01, c10, c11, Colors.TRANSPARENT);
		c01 = getFirstNotNull(c01, c11, c00, c10, Colors.TRANSPARENT);
		c10 = getFirstNotNull(c10, c00, c11, c01, Colors.TRANSPARENT);
		c11 = getFirstNotNull(c11, c01, c10, c00, Colors.TRANSPARENT);

		double xFrac = p01.getColumnDecimal() - p00.getColumn();
		double yFrac = p10.getRowDecimal() - p00.getRow();
		
		return ColorUtil.interpolateColor(c00, c01, c10, c11, xFrac, yFrac);
		
	}
	
	protected <T> T getFirstNotNull(T ... values)
	{
		if (values == null) {
			return null;
		}
		
		for (T t : values) {
			if (t != null) {
				return t;
			}
		}
		
		return null;
	}

	public IColor getColorNearestNeighbor(double latitude, double longitude) throws DataSourceException
	{
		PixelValue pv = getColorNearestNeighbor(latitude, RoundingType.ROUND, longitude, RoundingType.ROUND, true, true);
		return (pv != null) ? pv.getColor() : null;
	}
	
	public PixelValue getColorNearestNeighbor(double latitude, RoundingType latitudeRoundType, double longitude, RoundingType longitudeRoundType, boolean adjustCoordinates, boolean wrap) throws DataSourceException
	{
		if (!isLoaded()) {
			throw new DataSourceException("Image data not loaded");
		}
		
		if (adjustCoordinates) {
			if ((latitude = coordinateSpaceAdjuster.adjustLatitude(latitude)) == DemConstants.ELEV_NO_DATA) {
				return null;
			}
	
			if ((longitude = coordinateSpaceAdjuster.adjustLongitude(longitude)) == DemConstants.ELEV_NO_DATA) {
				return null;
			}
		}
		
		if (latitude >= imageDefinition.getSouth() && latitude <= imageDefinition.getNorth() && longitude >= imageDefinition.getWest() && longitude <= imageDefinition.getEast()) {
			try {
				canvasProjection.getPoint(latitude, longitude, 0.0, mapPoint);
			} catch (MapProjectionException ex) {
				throw new DataSourceException("Error getting x/y point from coordinates: " + ex.getMessage(), ex);
			}

			int column = 0;
			int row = 0;
			
			if (latitudeRoundType == RoundingType.ROUND) {
				row = (int) Math.round(mapPoint.row);
			} else if (latitudeRoundType == RoundingType.CEIL) {
				row = (int) Math.ceil(mapPoint.row);
			} else if (latitudeRoundType == RoundingType.FLOOR) {
				row = (int) Math.floor(mapPoint.row);
			}
			
			if (longitudeRoundType == RoundingType.ROUND) {
				column = (int) Math.round(mapPoint.column);
			} else if (longitudeRoundType == RoundingType.CEIL) {
				column = (int) Math.ceil(mapPoint.column);
			} else if (longitudeRoundType == RoundingType.FLOOR) {
				column = (int) Math.floor(mapPoint.column);
			}
			
			int useColumn = column;
			int useRow = row;
			
			if (wrap && useRow >= getHeight()) {
				useRow = useRow - getHeight();
			}
			
			if (wrap && useColumn >= getWidth()) {
				useColumn = useColumn - getWidth();
			}
			
			
			IColor color = getPixel(useColumn, row);
			
			PixelValue pixelValue = new PixelValue(color, row, mapPoint.row, column, mapPoint.column);
			return pixelValue;
		} else {
			return null;
		}
	}

	public IColor getPixel(int x, int y) throws DataSourceException
	{
		if (!isLoaded()) {
			throw new DataSourceException("Image data not loaded");
		}

		if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
			
			return null; // Throw?
		}

		long index = (y * imageDefinition.getImageWidth()) + x;
		if (index < 0 || index >= rasterBuffer.capacity()) {
			return null; // Throw?
		}
		
		int c = rasterBuffer.get(index);
		
		IColor color = new Color(c);
		
		if (!hasAlphaChannel) {
			color = new Color(color.getRed(), color.getGreen(), color.getBlue(), getTransparency(0xFF));
		} else {
			color = new Color(color.getRed(), color.getGreen(), color.getBlue(), getTransparency(color.getAlpha()));
		}

		return color;
	}
	
	protected int getTransparency(int alpha)
	{
		return (int) MathExt.round((double)alpha * imageDefinition.getLayerTransparency());
	}
	
	
	public String getImageFile()
	{
		return imageFile;
	}

	@Override
	public double getNorth()
	{
		return imageDefinition.getNorth();
	}

	@Override
	public void setNorth(double north)
	{
		imageDefinition.setNorth(north);
		update();
	}

	@Override
	public double getSouth()
	{
		return imageDefinition.getSouth();
	}
	
	@Override
	public void setSouth(double south)
	{
		imageDefinition.setSouth(south);
		update();
	}

	@Override
	public double getEast()
	{
		return imageDefinition.getEast();
	}

	@Override
	public void setEast(double east)
	{
		imageDefinition.setEast(east);
		update();
	}

	@Override
	public double getWest()
	{
		return imageDefinition.getWest();
	}

	@Override
	public void setWest(double west)
	{
		imageDefinition.setWest(west);
		update();
	}

	@Override
	public int getHeight()
	{
		return imageDefinition.getImageHeight();
	}

	@Override
	public int getWidth()
	{
		return imageDefinition.getImageWidth();
	}

	@Override
	public double getLatitudeResolution()
	{
		return imageDefinition.getLatitudeResolution();
	}

	@Override
	public double getLongitudeResolution()
	{
		return imageDefinition.getLongitudeResolution();
	}
	
	@Override
	public double getLayerTransparency()
	{
		return imageDefinition.getLayerTransparency();
	}
	
	public Texture getAsTexture()
	{
		Texture tex = new Texture(imageDefinition.getImageWidth()
								, imageDefinition.getImageHeight()
								, imageDefinition.getNorth()
								, imageDefinition.getSouth()
								, imageDefinition.getEast()
								, imageDefinition.getWest()
								, rasterBuffer);
		return tex;
	}

	
	public IImageDefinition getImageDefinition()
	{
		return imageDefinition;
	}
	
	/**
	 * Creates copy.
	 * 
	 * @return
	 */
	public SimpleGeoImage copy() throws DataSourceException
	{
		SimpleGeoImage copy = new SimpleGeoImage(imageFile
												, imageDefinition.getNorth()
												, imageDefinition.getSouth()
												, imageDefinition.getEast()
												, imageDefinition.getWest()
												, imageDefinition.getLayerTransparency());

		if (this.isLoaded()) {
			copy.rasterBuffer = this.rasterBuffer;
			//copy.image = this.image;
			//copy.raster = this.raster;
			copy.hasAlphaChannel = this.hasAlphaChannel;
		}

		return copy;
	}

}
