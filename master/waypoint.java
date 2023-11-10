import java.io.Serializable;
	// Class To Save Waypoint Data 
	// waypoint.java
	public class waypoint implements Serializable 
	{
		
		String creator; 
		String lat; 
		String lon; 
		String ele;
		String time;
		String file;
		
		public waypoint(String file , String creator , String lat , String lon , String ele , String time)
		{
			this.file = file;
			this.creator = creator;
			this.lat = lat;
			this.lon = lon;
			this.ele = ele;
			this.time = time;
		}
		

		
		@Override
		public String toString() 
		{
		return this.file+" : "+this.creator+" : "+this.lat +" : "+this.lon+" : "+this.ele+" : "+this.time; 
		}
		
	}//End Waypoint

