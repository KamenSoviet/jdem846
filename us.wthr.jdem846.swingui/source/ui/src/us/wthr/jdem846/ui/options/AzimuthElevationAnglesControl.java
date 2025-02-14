package us.wthr.jdem846.ui.options;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import us.wthr.jdem846.logging.Log;
import us.wthr.jdem846.logging.Logging;
import us.wthr.jdem846.model.AzimuthElevationAngles;
import us.wthr.jdem846.model.OptionModelPropertyContainer;
import us.wthr.jdem846.model.exceptions.MethodContainerInvokeException;
import us.wthr.jdem846.ui.base.Panel;


@SuppressWarnings("serial")
public class AzimuthElevationAnglesControl extends Panel implements ChangeListener, OptionModelUIControl
{
	private static Log log = Logging.getLog(AzimuthElevationAnglesControl.class);
	
	private final OptionModelPropertyContainer propertyContainer;
	
	public AzimuthElevationAnglesControl(OptionModelPropertyContainer propertyContainer)
	{
		this.propertyContainer = propertyContainer;
		this.setToolTipText(propertyContainer.getTooltip());
		
		refreshUI();

		
	}
	
	public void refreshUI()
	{
		try {
			AzimuthElevationAngles initialValue = (AzimuthElevationAngles) propertyContainer.getValue();
			if (initialValue != null) {
				//this.setSolarAzimuth(initialValue.getAzimuthAngle());
				//this.setSolarElevation(initialValue.getElevationAngle());
			}
		} catch (MethodContainerInvokeException ex) {
			log.error("Error setting initial control value for property " + propertyContainer.getPropertyName());
		}
	}
	
	@Override
	public void stateChanged(ChangeEvent e)
	{
		log.info("Azimuth/Elevation angles changed");
		
		AzimuthElevationAngles angles = new AzimuthElevationAngles();
		//angles.setAzimuthAngle(this.getSolarAzimuth());
		//angles.setElevationAngle(this.getSolarElevation());
		
		try {
			propertyContainer.setValue(angles);
		} catch (MethodContainerInvokeException ex) {
			log.error("Error setting value for property " + propertyContainer.getPropertyName());
		}
		
	}
	
}
