package us.wthr.jdem846.gis.projections;

import us.wthr.jdem846.logging.Log;
import us.wthr.jdem846.logging.Logging;

public class MollweideProjectionTest extends BaseMapProjectionTest
{
	@SuppressWarnings("unused")
	private static Log log = Logging.getLog(MollweideProjectionTest.class);
	
	

	@Override
	protected void setUp() throws Exception
	{
		String saveImagesTo = "test-image-MollweideProjection-{test}.png";
		setUp(90, -90, 180, -180, 800, 403, saveImagesTo);
	}
	
	

	public void testProjection1()
	{
		MollweideProjection projection = new MollweideProjection(90, -90, 180, -180, width, height);
		this.__testProjection(projection, 0.0, 0.0, 400.0, 200.0);

		// Boston, MA, USA
		//double latitude = 42.357778;
		//double longitude = -71.061667;
	}
	

}
