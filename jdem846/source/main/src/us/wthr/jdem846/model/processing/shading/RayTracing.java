package us.wthr.jdem846.model.processing.shading;

import us.wthr.jdem846.DemConstants;
import us.wthr.jdem846.ModelContext;
import us.wthr.jdem846.ModelDimensions;
import us.wthr.jdem846.exception.RayTracingException;
import us.wthr.jdem846.logging.Log;
import us.wthr.jdem846.logging.Logging;
import us.wthr.jdem846.math.MathExt;
import us.wthr.jdem846.math.Spheres;
import us.wthr.jdem846.scaling.ElevationScaler;

public class RayTracing
{
	@SuppressWarnings("unused")
	private static Log log = Logging.getLog(RayTracing.class);
	
	
	private RasterDataFetchHandler rasterDataFetchHandler;

	private double longitudeResolution;
	private double latitudeResolution;
	private double radiusInterval;
	
	private double modelRadiusMeters;
	
	private double northLimit;
	private double southLimit;
	private double eastLimit;
	private double westLimit;

	private double maximumElevation;
	private ElevationScaler elevationScaler;
	
	private double[] points;

	
	public RayTracing(
					double latitudeResolution, 
					double longitudeResolution, 
					double modelRadiusMeters, 
					double northLimit,
					double southLimit,
					double eastLimit,
					double westLimit,
					double maximumElevation,
					ElevationScaler elevationScaler,
					RasterDataFetchHandler rasterDataFetchHandler)
	{
		this.rasterDataFetchHandler = rasterDataFetchHandler;
		this.latitudeResolution = latitudeResolution;
		this.longitudeResolution = longitudeResolution;
		this.modelRadiusMeters = modelRadiusMeters;
		this.northLimit = northLimit;
		this.southLimit = southLimit;
		this.eastLimit = eastLimit;
		this.westLimit = westLimit;
		this.maximumElevation = maximumElevation;
		this.elevationScaler = elevationScaler;
		
		this.points = new double[3];
		
		radiusInterval = MathExt.sqrt(MathExt.sqr(latitudeResolution) + MathExt.sqr(longitudeResolution));
	}
	
	
	/** Ray trace from coordinate to the source of light. If there is another
	 * higher elevation that blocks the way, then return true. Return false if
	 * the trace either not blocked or it exceeds the maximum data elevation and 
	 * it can then be assumed to not be blocked. Points with no data are skipped
	 * and the loop continues on. Note: This initial implementation
	 * assumes a flat Earth and is therefore not technically accurate.
	 * 
	 * @param centerLatitude Geographic latitude of the point being tested for shadow
	 * @param centerLongitude Geographic longitude of the point being tested for shadow
	 * @param centerElevation Elevation of the test point.
	 * @return True if the ray's path is blocked, otherwise returns false.
	 * @throws RayTracingException Thrown if an error is detected when fetching an elevation along the ray path.
	 */
	public double isRayBlocked(double remoteElevationAngle, double remoteAzimuth, double centerLatitude, double centerLongitude, double centerElevation) throws RayTracingException
	{

		double isBlocked = 0.0;
		
		
		// Variables for use during each pass
		double radius = radiusInterval;
		

		while (true) {
			
			// Fetch points in space following the path of the azimuth and elevation angles
			// at the current radius.
			Spheres.getPoint3D(remoteAzimuth, remoteElevationAngle, radius, points);
			
			// Latitude/Longitude pair for the path at the current radius
			double latitude = centerLatitude + points[0];
			double longitude = centerLongitude - points[2];
			
			// Check if the latitude/longitude point is outside of the dataset.
			// If we get to this point, we assume that the ray is not blocked because
			// we are unable to prove otherwise.
			if (latitude > northLimit ||
					latitude < southLimit ||
					longitude > eastLimit ||
					longitude < westLimit) {
				break;
			}
			
			// Calculate the elevation of the path at the current radius.
			double metersResolution = ModelDimensions.getMetersResolution(modelRadiusMeters / 1000.0, longitude, latitude, latitudeResolution, longitudeResolution);
			double resolution = (points[1] / radiusInterval);
			double _rayElevation = centerElevation + (resolution * metersResolution);
			double rayElevation = _rayElevation;//getScaledElevation(_rayElevation);
			

			
			// Fetch the elevation value
			double pointElevation = 0;
			try {
				pointElevation = rasterDataFetchHandler.getRasterData(latitude, longitude);
			} catch (Exception ex) {
				throw new RayTracingException("Failed to get elevation for point: " + ex.getMessage(), ex);
			}

			
			
			// Increment for the next pass radius
			radius += radiusInterval;
			
			// If the elevation at the current point is invalid, we skip it and continue. Given this condition
			// we assume the path to not be blocked since we cannot prove otherwise.
			if (pointElevation == DemConstants.ELEV_NO_DATA) {
				continue;
			}
			
			// If the elevation at the current point exceeds the elevation of the ray path
			// then the ray is blocked. 
			if (pointElevation > rayElevation) {
				isBlocked = (radius / radiusInterval) * metersResolution;
				break;

			}
			
			// If the elevation of the ray path at the current radius exceeds the maximum dataset
			// elevation then we can safely assume that the ray is not blocked.
			if (rayElevation > this.maximumElevation) {
				break;
			}
			
		}
		

		return isBlocked;
		
	}
	
	
	protected double getScaledElevation(double elevation)
	{

		if (elevationScaler != null) {
			return elevationScaler.scale(elevation);
		} else {
			return elevation;
		}
	}


	public double getNorthLimit()
	{
		return northLimit;
	}

	public void setNorthLimit(double northLimit)
	{
		this.northLimit = northLimit;
	}

	public double getSouthLimit()
	{
		return southLimit;
	}

	public void setSouthLimit(double southLimit)
	{
		this.southLimit = southLimit;
	}

	public double getEastLimit()
	{
		return eastLimit;
	}

	public void setEastLimit(double eastLimit)
	{
		this.eastLimit = eastLimit;
	}

	public double getWestLimit()
	{
		return westLimit;
	}

	public void setWestLimit(double westLimit)
	{
		this.westLimit = westLimit;
	}

	public double getLongitudeResolution()
	{
		return longitudeResolution;
	}

	public void setLongitudeResolution(double longitudeResolution)
	{
		this.longitudeResolution = longitudeResolution;
	}

	public double getLatitudeResolution()
	{
		return latitudeResolution;
	}

	public void setLatitudeResolution(double latitudeResolution)
	{
		this.latitudeResolution = latitudeResolution;
	}

	public double getModelRadiusMeters()
	{
		return modelRadiusMeters;
	}

	public void setModelRadiusMeters(double modelRadiusMeters)
	{
		this.modelRadiusMeters = modelRadiusMeters;
	}


	public RasterDataFetchHandler getRasterDataFetchHandler()
	{
		return rasterDataFetchHandler;
	}

	public void setRasterDataFetchHandler(
			RasterDataFetchHandler rasterDataFetchHandler)
	{
		this.rasterDataFetchHandler = rasterDataFetchHandler;
	}
	
	public interface RasterDataFetchHandler 
	{
		public double getRasterData(double latitude, double longitude) throws Exception;
	}
	
	
}
