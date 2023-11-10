import java.util.ArrayList; // import the ArrayList class
import java.io.*;
import java.net.*;

	//Create Client Class
	public class Client extends Thread implements Serializable 
	{
		String name;
		double total_distance;
		double average_speed;
		double total_elevation;
		double total_time;
		
		/* Constructors */
		public Client(String user)
		{
			this.name=user;
			this.total_distance = 0;
			this.average_speed = 0;
			this.total_elevation = 0;
			this.total_time = 0;
			
		}

		
		/* Total_Distance - Average_speed - Total_Elevation - Total_Time */
		public Client(String user , double total_distance , double average_speed , double total_elevation , double total_time)
		{
			this.name=user;
			this.total_distance = total_distance;
			this.average_speed = average_speed;
			this.total_elevation = total_elevation;
			this.total_time=total_time;
			
		}
		
		// Update Clients Information-Data 
		public void update(double total_distance , double average_speed , double total_elevation , double total_time)
		{
			this.total_distance = this.total_distance + total_distance;
			this.average_speed = this.average_speed + average_speed;
			this.total_elevation = this.total_elevation + total_elevation;
			this.total_time = this.total_time + total_time;
		}


		public String toString()
		{
			return this.name+" : TD : "+this.total_distance+" : as : "+this.average_speed+" : te : "+this.total_elevation+" : TT : "+total_time ;
		}





	}
	