package us.wthr.jdem846.rasterdata;

import java.util.LinkedList;
import java.util.List;

import us.wthr.jdem846.DataContext;
import us.wthr.jdem846.DemConstants;
import us.wthr.jdem846.exception.DataSourceException;
import us.wthr.jdem846.logging.Log;
import us.wthr.jdem846.logging.Logging;
import us.wthr.jdem846.math.MathExt;
import us.wthr.jdem846.model.exceptions.ContextPrepareException;

public class RasterDataContext implements DataContext
{
	private static Log log = Logging.getLog(RasterDataContext.class);
	private static final double NOT_SET = DemConstants.ELEV_NO_DATA;
	
	
	private List<RasterData> rasterDataList = new LinkedList<RasterData>();
	private List<RasterDataRowColumnBox> rasterDataRowColumnBoxes = new LinkedList<RasterDataRowColumnBox>();
	
	private double east = DemConstants.COORDINATE_NOT_SET;
	private double west = DemConstants.COORDINATE_NOT_SET;
	private double north = DemConstants.COORDINATE_NOT_SET;
	private double south = DemConstants.COORDINATE_NOT_SET;
	
	private double latitudeResolution;
	private double longitudeResolution;
	
	private double effectiveLatitudeResolution = NOT_SET;
	private double effectiveLongitudeResolution = NOT_SET;
	
	private double dataMinimumValue = 0;
	private double dataMaximumValue = 0;
	
	private double metersResolution = NOT_SET;
	
	private boolean isDisposed = false;
	

	//private ElevationScaler elevationScaler;
	
	
	private boolean avgOfAllRasterValues = false;
	private boolean interpolate = false;
	private boolean scaled = true;
	
	
	public RasterDataContext()
	{
		
	}
	
	public void prepare() throws ContextPrepareException
	{
		
		east = -180.0;
		west = 180.0;
		north = -90.0;
		south = 90.0;
		
		latitudeResolution = Double.MAX_VALUE;
		longitudeResolution = Double.MAX_VALUE;
		
		rasterDataRowColumnBoxes.clear();
		
		for (RasterData rasterData : rasterDataList) {
			
			if (rasterData.getNorth() > north)
				north = rasterData.getNorth();
			
			if (rasterData.getSouth() < south)
				south = rasterData.getSouth();
			
			if (rasterData.getEast() > east)
				east = rasterData.getEast();
			
			if (rasterData.getWest() < west)
				west = rasterData.getWest();
			
			if (rasterData.getLatitudeResolution() < latitudeResolution)
				latitudeResolution = rasterData.getLatitudeResolution();
			
			if (rasterData.getLongitudeResolution() < longitudeResolution)
				longitudeResolution = rasterData.getLongitudeResolution();
			
			RasterDataRowColumnBox rowColBox = new RasterDataRowColumnBox(this.longitudeToColumn(rasterData.getWest()), this.latitudeToRow(rasterData.getNorth()), rasterData.getColumns(), rasterData.getRows());
			rasterDataRowColumnBoxes.add(rowColBox);
		}
		
		metersResolution = getMetersResolution(DemConstants.EARTH_MEAN_RADIUS);
		
		
		north = validateLatitude(north);
		south = validateLatitude(south);
		west = validateLongitude(west);
		east = validateLongitude(east);
		
		log.info("Prepared RasterDataProxy to region N/S/E/W: " + north + "/" + south + "/" + east + "/" + west);
		log.info("Prepared RasterDataProxy to lat/long resolutions: " + latitudeResolution + "/" + longitudeResolution);
		log.info("Prepared Meters Resolution: " + metersResolution);
	}
	
	protected double validateLongitude(double longitude)
	{
		if (longitude <= -180.0)
			longitude = -180.0;
		if (longitude >= 180.0)
			longitude = 180.0;
		return longitude;
	}
	
	protected double validateLatitude(double latitude)
	{
		if (latitude <= -90.0)
			latitude = -90.0;
		if (latitude >= 90.0)
			latitude = 90.0;
		return latitude;
	}
	
	public void dispose() throws DataSourceException
	{
		log.info("Disposing raster data context");
		
		if (isDisposed()) {
			throw new DataSourceException("Raster data proxy already disposed.");
		}
		
		clearBuffers();
		
		for (RasterData rasterData : rasterDataList) {
			if (!rasterData.isDisposed()) {
				rasterData.dispose();
			}
		}
		
		rasterDataRowColumnBoxes.clear();
		
		// TODO: Finish
		isDisposed = true;
	}

	
	public boolean isDisposed()
	{
		return isDisposed;
	}
	
	/*
	public void calculateElevationMinMax(boolean full) throws DataSourceException
	{
		log.info("Calculating elevation minimums & Maximums");
		
		dataMinimumValue = Double.MAX_VALUE;
		dataMaximumValue = Double.MIN_VALUE;
		
		for (RasterData rasterData : rasterDataList) {
			double minValue = rasterData.getDataMinimum();
			double maxValue = rasterData.getDataMaximum();
			
			if (minValue == DemConstants.ELEV_NO_DATA || maxValue == DemConstants.ELEV_NO_DATA) {
				if (full) {
					rasterData.calculateMinAndMax();
				} else {
					continue;
				}
			}
			
			
			
			
			
			if (rasterData.getDataMinimum() < dataMinimumValue)
				dataMinimumValue = rasterData.getDataMinimum();
			
			if (rasterData.getDataMaximum() > dataMaximumValue)
				dataMaximumValue = rasterData.getDataMaximum();
			
		}
		
		if (dataMinimumValue == Double.MAX_VALUE) {
			dataMinimumValue = DemConstants.ELEV_NO_DATA;
		}
		
		if (dataMaximumValue == Double.MIN_VALUE) {
			dataMaximumValue = DemConstants.ELEV_NO_DATA;
		}
	}
	*/

	public double  getMetersResolution()
	{
		return metersResolution;
	}
	
	public double getMetersResolution(double meanRadius)
	{
		
		double lat = (getNorth() - getSouth()) / 2.0;
		double lon = (getEast() - getWest()) / 2.0;
		return getMetersResolution(meanRadius, lat, lon, getLatitudeResolution(), getLongitudeResolution());

	}
	
	public static double getMetersResolution(double meanRadius, double latitude, double longitude, double latitudeResolution, double longitudeResolution)
	{
		//double lat1 = latitude;
		//double lon1 = longitude;
		//double lat2 = lat1 + latitudeResolution;
		//double lon2 = lon1 + longitudeResolution;
		//double R = meanRadius;
		//double dLat = Math.toRadians(lat2 - lat1);
		//double dLon = Math.toRadians(lon2 - lon1);
		//double dLat = MathExt.radians(latitudeResolution);
		//double dLon = MathExt.radians(longitudeResolution);

		
		
		double a = MathExt.sqr(MathExt.sin(MathExt.radians(latitudeResolution)/2)) + MathExt.cos(MathExt.radians(latitude)) * MathExt.cos(MathExt.radians(latitude + latitudeResolution)) * MathExt.sqr(MathExt.sin(MathExt.radians(longitudeResolution)/2)); 
		double d = meanRadius * (2 * MathExt.atan2(MathExt.sqrt(a), MathExt.sqrt(1-a))) * 1000;
		
		/*
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2); 
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double d = R * c * 1000;
		*/
		return d;
	}
	
	public void addRasterData(RasterData rasterData) throws DataSourceException
	{
		rasterDataList.add(rasterData);
		try {
			prepare();
		} catch (ContextPrepareException ex) {
			throw new DataSourceException("Failed to prepare with raster data: " + ex.getMessage(), ex);
		}
	}

	public void removeRasterData(RasterData rasterData) throws DataSourceException
	{
		if (rasterDataList.remove(rasterData)) {
			try {
				prepare();
			} catch (ContextPrepareException ex) {
				throw new DataSourceException("Failed to prepare with raster data: " + ex.getMessage(), ex);
			}
		}
	}
	
	public RasterData removeRasterData(int index) throws DataSourceException
	{
		RasterData removed = rasterDataList.remove(index);
		try {
			prepare();
		} catch (ContextPrepareException ex) {
			throw new DataSourceException("Failed to prepare with raster data: " + ex.getMessage(), ex);
		}
		return removed;
	}
	
	public List<RasterData> getRasterDataList()
	{
		return rasterDataList;
	}
	
	public int getRasterDataListSize()
	{
		return rasterDataList.size();
	}
	

	public List<RasterDataRowColumnBox> getRasterDataRowColumnBoxes()
	{
		return rasterDataRowColumnBoxes;
	}

	
	public boolean dataOverlaps(RasterDataRowColumnBox bounds)
	{
		for (RasterDataRowColumnBox inputBounds : rasterDataRowColumnBoxes) {
			if (inputBounds.overlaps(bounds))
				return true;
		}
		return false;
	}
	
	public void fillBuffers() throws DataSourceException
	{
		fillBuffers(north, south, east, west);
	}
	
	public void fillBuffers(double north, double south, double east, double west) throws DataSourceException
	{
		int fillCount = 0;
		for (RasterData rasterData : rasterDataList) {
			if (rasterData.fillBuffer(north, south, east, west)) {
				fillCount++;
			}
		}

		log.info("" + fillCount + " data rasters matched the caching range");
		
	}
	
	public void clearBuffers() throws DataSourceException
	{
		for (RasterData rasterData : rasterDataList) {
			rasterData.clearBuffer();
		}
	}
	
	public double getData(double latitude, double longitude) throws DataSourceException
	{
		return getData(latitude, longitude, scaled);
	}
	
	public double getData(double latitude, double longitude, boolean scaled) throws DataSourceException
	{
		return getData(latitude, longitude, avgOfAllRasterValues, interpolate, scaled);
	}
	
	
	public double getData(double latitude, double longitude, boolean avgOfAllRasterValues, boolean interpolate) throws DataSourceException
	{
		return getData(latitude, longitude, avgOfAllRasterValues, interpolate, scaled);
	}
	
	public double getData(double latitude, double longitude, boolean avgOfAllRasterValues, boolean interpolate, boolean scaled) throws DataSourceException
	{
		if (effectiveLatitudeResolution == NOT_SET && effectiveLongitudeResolution == NOT_SET || !interpolate) {
			return getDataStandardResolution(latitude, longitude, avgOfAllRasterValues, interpolate, scaled);
		} else {
			return getDataAtEffectiveResolution(latitude, longitude, avgOfAllRasterValues, interpolate, scaled);
		}
	}
	
	public double getDataStandardResolution(double latitude, double longitude, boolean avgOfAllRasterValues, boolean interpolate) throws DataSourceException
	{
		return getDataStandardResolution(latitude, longitude, avgOfAllRasterValues, interpolate, scaled);
	}
	
	public double getDataStandardResolution(double latitude, double longitude, boolean avgOfAllRasterValues, boolean interpolate, boolean scaled) throws DataSourceException
	{
		double value = 0;
		double dataMatches = 0;
		
		for (RasterData rasterData : rasterDataList) {
			if (rasterData.contains(latitude, longitude)) {
				double rasterValue = rasterData.getData(latitude, longitude, interpolate);
				
				if (!avgOfAllRasterValues) {
					return rasterValue;
				}
				
				if (rasterValue != DemConstants.ELEV_NO_DATA && !Double.isNaN(rasterValue)) {
					value += rasterValue;
					dataMatches++;
				}

			}
		}
		

		if (dataMatches > 0) {
			double data = (value / dataMatches);
			return data;
		} else {
			return DemConstants.ELEV_NO_DATA;
		}

		
		
	}
	
	
	
	public double getDataAtEffectiveResolution(double latitude, double longitude, boolean avgOfAllRasterValues, boolean interpolate) throws DataSourceException
	{
		return getDataAtEffectiveResolution(latitude, longitude, avgOfAllRasterValues, interpolate, scaled);
	}
	
	public double getDataAtEffectiveResolution(double latitude, double longitude, boolean avgOfAllRasterValues, boolean interpolate, boolean scaled) throws DataSourceException
	{
		
		double north = latitude + (effectiveLatitudeResolution / 2.0);
		double south = latitude - (effectiveLatitudeResolution / 2.0);
		
		double west = longitude - (effectiveLongitudeResolution / 2.0);
		double east = longitude + (effectiveLongitudeResolution / 2.0);
		
		double samples = 0;
		
		double data = 0;
		
		// Still not quite as I would like it, but works for now...
		for (double x = west; x <= east; x+=longitudeResolution) {
			for (double y = north; y >= south; y-=latitudeResolution) {
				double _data = getDataStandardResolution(y, x, avgOfAllRasterValues, interpolate, false);
				
				if (_data != DemConstants.ELEV_NO_DATA) {
					data += _data;
					samples++;
				}
				
			}
		}
		
		if (samples > 0) {
			data = (data / samples);
			return data;
		} else {
			return DemConstants.ELEV_NO_DATA;
		}
		
	}
	
	/*
	protected double getScaledDataValue(double data)
	{
		double _scaled = data;
		if (elevationScaler != null && data != DemConstants.ELEV_NO_DATA) {
			_scaled = elevationScaler.scale(data);
		}
		return _scaled;
	}
	*/
	
	public RasterDataContext getSubSet(double north, double south, double east, double west) throws DataSourceException
	{
		RasterDataLatLongBox subsetLatLongBox = new RasterDataLatLongBox(north, south, east, west);
		// TODO: This isn't working properly.

		RasterDataContext newDataProxy = new RasterDataContext();
		
		for (RasterData rasterData : rasterDataList) {
			if (rasterData.intersects(subsetLatLongBox)) {
				newDataProxy.getRasterDataList().add(rasterData);
			}
			
		}
		
		try {
			newDataProxy.prepare();
		} catch (ContextPrepareException ex) {
			throw new DataSourceException("Failed to prepare with raster data: " + ex.getMessage(), ex);
		}
		
		newDataProxy.setEffectiveLatitudeResolution(effectiveLatitudeResolution);
		newDataProxy.setEffectiveLongitudeResolution(effectiveLongitudeResolution);
		
		return newDataProxy;
	}
	
	
	public int latitudeToRow(double latitude)
	{
		// Nearest neighbor
		return (int) Math.floor((north - latitude) / this.getLatitudeResolution());
	}
	
	public double rowToLatitude(int row)
	{
		return (north - ((double)row * this.getLatitudeResolution()));
	}
	
	public int longitudeToColumn(double longitude)
	{
		// Nearest neighbor
		return (int) Math.floor((longitude - west) / this.getLongitudeResolution());
	}
	
	public double columnToLongitude(int column)
	{
		return west + ((double)column * this.getLongitudeResolution());
	}
	
	
	public double getNorth()
	{
		return validateLatitude(north); 
	}
	
	public double getSouth()
	{
		return validateLatitude(south);
	}
	
	public double getEast()
	{
		return validateLongitude(east);
	}
	
	public double getWest()
	{
		return validateLongitude(west);
	}
	
	public int getDataRows()
	{
		return getDataRows(north, south);
	}
	
	public int getDataRows(double north, double south)
	{
		return (int) Math.floor((north - south) / this.getLatitudeResolution());
	}
	
	public int getDataColumns()
	{
		return getDataColumns(east, west);
	}
	
	public int getDataColumns(double east, double west)
	{
		return (int) Math.floor((east - west) / this.getLongitudeResolution());
	}

	
	public double getLatitudeResolution() 
	{
		return latitudeResolution;
	}
	
	public void setLatitudeResolution(double latitudeResolution)
	{
		this.latitudeResolution = latitudeResolution;
	}

	public double getLongitudeResolution() 
	{
		return longitudeResolution;
	}
	
	public void setLongitudeResolution(double longitudeResolution)
	{
		this.longitudeResolution = longitudeResolution;
	}

	public double getDataMinimumValue()
	{
		return (!Double.isNaN(dataMinimumValue)) ? dataMinimumValue : 0.0;
	}

	public void setDataMinimumValue(double dataMinimumValue)
	{
		this.dataMinimumValue = (!Double.isNaN(dataMinimumValue)) ? dataMinimumValue : 0.0;
	}
	
	public double getDataMaximumValue()
	{
		return dataMaximumValue;
	}
	
	
	public void setDataMaximumValue(double dataMaximumValue)
	{
		this.dataMaximumValue = (!Double.isNaN(dataMaximumValue)) ? dataMaximumValue : 0.0;
	}
	
	
	
	public double getEffectiveLatitudeResolution()
	{
		if (effectiveLatitudeResolution != NOT_SET)
			return effectiveLatitudeResolution;
		else
			return getLatitudeResolution();
	}

	public void setEffectiveLatitudeResolution(double effectiveLatitudeResolution)
	{
		this.effectiveLatitudeResolution = effectiveLatitudeResolution;
	}

	public double getEffectiveLongitudeResolution()
	{
		if (effectiveLongitudeResolution != NOT_SET)
			return effectiveLongitudeResolution;
		else
			return getLongitudeResolution();
	}

	public void setEffectiveLongitudeResolution(double effectiveLongitudeResolution)
	{
		this.effectiveLongitudeResolution = effectiveLongitudeResolution;
	}
	
	
	
	public boolean isAvgOfAllRasterValues() 
	{
		return avgOfAllRasterValues;
	}

	public void setAvgOfAllRasterValues(boolean avgOfAllRasterValues) 
	{
		this.avgOfAllRasterValues = avgOfAllRasterValues;
	}

	public boolean getInterpolate() 
	{
		return interpolate;
	}

	public void setInterpolate(boolean interpolate)
	{
		this.interpolate = interpolate;
	}

	public RasterDataContext copy() throws DataSourceException
	{
		if (isDisposed()) {
			throw new DataSourceException("Cannot copy object: already disposed");
		}
		return copy(new RasterDataContext());
		
	}
	
	public RasterDataContext copy(RasterDataContext clone) throws DataSourceException
	{
		if (isDisposed()) {
			throw new DataSourceException("Cannot copy object: already disposed");
		}
		

		clone.north = getNorth();
		clone.south = getSouth();
		clone.east = getEast();
		clone.west = getWest();
		clone.latitudeResolution = getLatitudeResolution();
		clone.longitudeResolution = getLongitudeResolution();
		clone.effectiveLatitudeResolution = getEffectiveLatitudeResolution();
		clone.effectiveLongitudeResolution = getEffectiveLongitudeResolution();
		clone.dataMaximumValue = getDataMaximumValue();
		clone.dataMinimumValue = getDataMinimumValue();
		clone.isDisposed = isDisposed(); // Should be false at this point...		
		clone.metersResolution = getMetersResolution();
		clone.avgOfAllRasterValues = this.avgOfAllRasterValues;
		clone.interpolate = this.interpolate;
		clone.scaled = this.scaled;
		//clone.elevationScaler = (this.elevationScaler != null) ? this.elevationScaler.copy() : null;
		for (RasterData rasterData : rasterDataList) {
			clone.rasterDataList.add(rasterData.copy());
		}
		
		//List<RasterDataRowColumnBox> rasterDataRowColumnBoxes
		
		for (RasterDataRowColumnBox box : rasterDataRowColumnBoxes) {
			clone.rasterDataRowColumnBoxes.add(box.copy());
		}
		
		return clone;
	}
}
