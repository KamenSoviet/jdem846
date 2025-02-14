package us.wthr.jdem846.model;

import us.wthr.jdem846.ModelContext;
import us.wthr.jdem846.ModelDimensions;
import us.wthr.jdem846.canvas.CanvasProjection;
import us.wthr.jdem846.canvas.CanvasProjection3d;
import us.wthr.jdem846.canvas.CanvasProjectionGlobe;
import us.wthr.jdem846.canvas.CanvasProjectionTypeEnum;
import us.wthr.jdem846.canvas.LatLonResolution;
import us.wthr.jdem846.image.ImageDataContext;
import us.wthr.jdem846.image.SimpleGeoImage;
import us.wthr.jdem846.logging.Log;
import us.wthr.jdem846.logging.Logging;
import us.wthr.jdem846.math.MathExt;
import us.wthr.jdem846.modelgrid.IModelGrid;
import us.wthr.jdem846.rasterdata.RasterData;
import us.wthr.jdem846.rasterdata.RasterDataContext;

public class ModelGridDimensions extends ModelDimensions
{
	@SuppressWarnings("unused")
	private static Log log = Logging.getLog(ModelGridDimensions.class);

	
	public ModelGridDimensions()
	{
		
	}
	
	public ModelGridDimensions(ModelContext modelContext)
	{
		if (modelContext.getModelGridContext().getGridLoadedFrom() == null) {
			initNoModelGrid(modelContext);
		} else {
			initModelGrid(modelContext);
		}
	}
	
	public void initModelGrid(ModelContext modelContext)
	{
		IModelGrid modelGrid = modelContext.getModelGridContext().getModelGrid();
		GlobalOptionModel globalOptionModel = modelContext.getModelProcessManifest().getGlobalOptionModel();
		
		
		north = modelGrid.getNorth();
		south = modelGrid.getSouth();
		east = modelGrid.getEast();
		west = modelGrid.getWest();
		
		dataRows = modelGrid.getHeight();
		dataColumns = modelGrid.getWidth();
		latitudeResolution = modelGrid.getLatitudeResolution();
		longitudeResolution = modelGrid.getLongitudeResolution();
		
		outputHeight = globalOptionModel.getHeight();
		outputWidth = globalOptionModel.getWidth();
		
		double scaleX = globalOptionModel.getViewAngle().getZoom();
		
		
		LatLonResolution latLonOutputRes = null;
		if(!modelContext.getModelGridContext().isUserProvided()) {
			
			CanvasProjectionTypeEnum canvasProjectionType = CanvasProjectionTypeEnum.getCanvasProjectionEnumFromIdentifier(globalOptionModel.getRenderProjection());
			
			
			if (canvasProjectionType == CanvasProjectionTypeEnum.PROJECT_3D) {
				latLonOutputRes = CanvasProjection3d.calculateOutputResolutions(outputWidth,
																outputHeight,
																dataColumns,
																dataRows,
																latitudeResolution,
																longitudeResolution,
																scaleX);
			} else if (canvasProjectionType == CanvasProjectionTypeEnum.PROJECT_SPHERE) {
				latLonOutputRes = CanvasProjectionGlobe.calculateOutputResolutions(outputWidth,
																outputHeight,
																dataColumns,
																dataRows,
																latitudeResolution,
																longitudeResolution,
																scaleX);
			} else  {
				latLonOutputRes = CanvasProjection.calculateOutputResolutions(outputWidth,
																outputHeight,
																dataColumns,
																dataRows,
																latitudeResolution,
																longitudeResolution,
																scaleX);
			}
		} else {
			latLonOutputRes = new LatLonResolution(latitudeResolution, longitudeResolution);
		}
		
		modelLatitudeResolution = latLonOutputRes.latitudeResolution;
		modelLongitudeResolution = latLonOutputRes.longitudeResolution;
		modelLatitudeResolutionTrue = latLonOutputRes.latitudeResolution;
		modelLongitudeResolutionTrue = latLonOutputRes.longitudeResolution;
		
		textureLatitudeResolution = latLonOutputRes.latitudeResolution;
		textureLongitudeResolution = latLonOutputRes.longitudeResolution;
		textureLatitudeResolutionTrue = latLonOutputRes.latitudeResolution;
		textureLongitudeResolutionTrue = latLonOutputRes.longitudeResolution;
		
		double modelQuality = globalOptionModel.getModelQuality();
		double textureQuality = globalOptionModel.getTextureQuality();
		
		if (modelQuality > textureQuality) {
			modelQuality = textureQuality;
		}
		
		if (modelContext.getModelGridContext().isUserProvided()) {
			textureQuality = 1.0;
		}
		
		modelLatitudeResolution /= modelQuality;
		modelLongitudeResolution /= modelQuality;
		
		textureLatitudeResolution /= textureQuality;
		textureLongitudeResolution /= textureQuality;

	}
	
	
	public void initNoModelGrid(ModelContext modelContext)
	{
		GlobalOptionModel globalOptionModel = modelContext.getModelProcessManifest().getGlobalOptionModel();
		RasterDataContext rasterDataContext = modelContext.getRasterDataContext();
		ImageDataContext imageDataContext = modelContext.getImageDataContext();
		
		north = -90;
		south = 90;
		east = -180;
		west = 180;
		
		latitudeResolution = Double.MAX_VALUE;
		longitudeResolution = Double.MAX_VALUE;
		
		if (rasterDataContext != null) {
			for (RasterData rasterData : rasterDataContext.getRasterDataList()) {
				latitudeResolution = MathExt.min(latitudeResolution, rasterData.getLatitudeResolution());
				longitudeResolution = MathExt.min(longitudeResolution, rasterData.getLongitudeResolution());
				
				north = MathExt.max(north, rasterData.getNorth());
				south = MathExt.min(south, rasterData.getSouth());
				east = MathExt.max(east, rasterData.getEast());
				west = MathExt.min(west, rasterData.getWest());
			}
		}
		
		if (imageDataContext != null) {
			for (SimpleGeoImage image : imageDataContext.getImageList()) {
				latitudeResolution = MathExt.min(latitudeResolution, image.getLatitudeResolution());
				longitudeResolution = MathExt.min(longitudeResolution, image.getLongitudeResolution());
			
				north = MathExt.max(north, image.getNorth());
				south = MathExt.min(south, image.getSouth());
				east = MathExt.max(east, image.getEast());
				west = MathExt.min(west, image.getWest());
			}
		}

		if (north > 90) {
			north = 90;
		}
		
		if (south < -90) {
			south = -90;
		}
		
		if (east > 180) {
			east = 180;
		}
		
		if (west < -180) {
			west = -180;
		}

		
		
		south = south += latitudeResolution;
		east = east -= longitudeResolution;
		
		dataRows = (int) MathExt.ceil((north - south) / latitudeResolution);
		dataColumns = (int) MathExt.ceil((east - west) / longitudeResolution);
		
		outputHeight = globalOptionModel.getHeight();
		outputWidth = globalOptionModel.getWidth();

		//double scaleX = 1.0;//modelContext.getModelOptions().getProjection().getZoom();
		double scaleX = globalOptionModel.getViewAngle().getZoom();
		
		
		LatLonResolution latLonOutputRes = null;
		CanvasProjectionTypeEnum canvasProjectionType = CanvasProjectionTypeEnum.getCanvasProjectionEnumFromIdentifier(globalOptionModel.getRenderProjection());
		
		if (canvasProjectionType == CanvasProjectionTypeEnum.PROJECT_3D) {
			latLonOutputRes = CanvasProjection3d.calculateOutputResolutions(outputWidth,
															outputHeight,
															dataColumns,
															dataRows,
															latitudeResolution,
															longitudeResolution,
															scaleX);
		} else if (canvasProjectionType == CanvasProjectionTypeEnum.PROJECT_SPHERE) {
			latLonOutputRes = CanvasProjectionGlobe.calculateOutputResolutions(outputWidth,
															outputHeight,
															dataColumns,
															dataRows,
															latitudeResolution,
															longitudeResolution,
															scaleX);
		} else  {
			latLonOutputRes = CanvasProjection.calculateOutputResolutions(outputWidth,
															outputHeight,
															dataColumns,
															dataRows,
															latitudeResolution,
															longitudeResolution,
															scaleX);
		}
		
		modelLatitudeResolution = latLonOutputRes.latitudeResolution;
		modelLongitudeResolution = latLonOutputRes.longitudeResolution;
		modelLatitudeResolutionTrue = latLonOutputRes.latitudeResolution;
		modelLongitudeResolutionTrue = latLonOutputRes.longitudeResolution;
		
		textureLatitudeResolution = latLonOutputRes.latitudeResolution;
		textureLongitudeResolution = latLonOutputRes.longitudeResolution;
		textureLatitudeResolutionTrue = latLonOutputRes.latitudeResolution;
		textureLongitudeResolutionTrue = latLonOutputRes.longitudeResolution;
		
		double modelQuality = globalOptionModel.getModelQuality();
		double textureQuality = globalOptionModel.getTextureQuality();
		
		modelLatitudeResolution /= modelQuality;
		modelLongitudeResolution /= modelQuality;
		
		textureLatitudeResolution /= textureQuality;
		textureLongitudeResolution /= textureQuality;

	}
	
	
	public static ModelGridDimensions getModelDimensions(ModelContext modelContext)
	{
		ModelGridDimensions modelGridDimensions = new ModelGridDimensions(modelContext);
		return modelGridDimensions;
	}
	
	
	public ModelGridDimensions copy()
	{
		ModelGridDimensions copy = new ModelGridDimensions();
		copy.dataColumns = this.dataColumns;
		copy.dataRows = this.dataRows;
		copy.north = this.north;
		copy.south = this.south;
		copy.east = this.east;
		copy.west = this.west;
		copy.latitudeResolution = this.latitudeResolution;
		copy.longitudeResolution = this.longitudeResolution;
		copy.outputHeight = this.outputHeight;
		copy.outputWidth = this.outputWidth;
		copy.modelLatitudeResolution = this.modelLatitudeResolution;
		copy.modelLongitudeResolution = this.modelLongitudeResolution;
		copy.textureLatitudeResolution = this.textureLatitudeResolution;
		copy.textureLongitudeResolution = this.textureLongitudeResolution;

		return copy;
	}
}
