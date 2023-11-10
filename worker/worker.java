import java.util.*; 
import java.io.*;
import java.net.*;
import java.lang.*;



//worker Class 
public class worker extends Thread 
{
	
	worker.WritingForWorkers writer;

	message workload = new message();
	String str;
	int id;
	
	//worker Constructor 
	public worker()
	{
	
	}
	
	public void run() 
	{
		
		Socket requestSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		//Try To Create A Connection With Server 
		try {

			/* Create socket for contacting the server on port 4321 and IP = host */
			String host = "localhost";
			requestSocket= new Socket(host,  4321);	//Server Listening on 4321 created in Master's Class 


			/* Create the streams to send and receive data from server */
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			in = new ObjectInputStream(requestSocket.getInputStream());

			//Create Read Write Threads 
			WritingForWorkers wr =  new WritingForWorkers(out,requestSocket);
			ReadingForWorkers re = new ReadingForWorkers(in,requestSocket);
			this.writer = wr;
			
			Thread write = new Thread(wr);
			Thread read = new Thread(re);
			write.start();
			read.start();

			
			/* Keep Threads read/write alive And Check for Available WORK */
			while(requestSocket.isConnected())
			{

				try {
				Thread.sleep(1000);
					}
				catch (InterruptedException e) {
				e.printStackTrace();
					}
				
				/* Check if workload available to calculate */
				if(!this.workload.getArr().isEmpty())
				{
					Thread T = new worker_calculation(new ArrayList<waypoint>(this.workload.getArr()));
					T.start();
					System.out.println("Worker Started Calculating");
					// Clear message and ArrayList
					this.workload.getArr().clear();
					this.workload= new message();
				}
				
			}// End While 
				
			
			

		} catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} 
		
		finally {
			try {
				in.close();
				out.close();
				requestSocket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}
	
	public synchronized void set_workload(message msg)
	{
		this.workload = msg;
	}
	
	
	/* Input Stream Class For Worker Class */
	public class ReadingForWorkers extends Thread /****/ {
		ObjectInputStream in;
		Socket socket;

		public ReadingForWorkers(ObjectInputStream in , Socket socket) 
		{
				this.in = in;
				this.socket = socket;
		}
		

		public void run() 
		{
			while(socket.isConnected())
			{
			//Read Message from Master
			try
				{
				//Sleep For 1s
				try {
					Thread.sleep(1000);
					  }
				catch (InterruptedException e) {
				e.printStackTrace();
					  }
				
				message mes = (message) in.readObject();
				// Message read , Commence Edit
				set_workload(mes);
				//System.out.println("message server received : \n"+mes.toString("!"));
				
				}
			catch (ClassNotFoundException e)
				{
				throw new RuntimeException(e);
				}
			catch (IOException e) 
				{
				//Catch If Nothing Found To Read 
				} 

			}
			try{in.close();}
			catch(IOException e){e.printStackTrace();}	
		}
		
		

	}	//End Input 
	
	
	/* Output Stream Class For Worker Class */
	public class WritingForWorkers extends Thread /****/ {

		ObjectOutputStream out;
		Socket socket;
		Client aMessage=null;

		//Constructor
		public WritingForWorkers(ObjectOutputStream out,Socket socket) 
		{
				this.out = out;	
				this.socket=socket;
		}

		
		public void run() 
		{
			while(socket.isConnected())
			{
					//Send Message to Master 
					try
						{
						//Sleep For 1s
						try {
							Thread.sleep(1000);
							  }
						catch (InterruptedException e) {
						e.printStackTrace();
							  }
							
							//If exists message to be sent (a client) , send it
							if(aMessage!=null)
							{
							message mes = new message(aMessage);
							out.writeObject(mes);
							out.flush();
							aMessage=null;
							}
						}
					catch (IOException e) 
						{
						//
						} 		
				
			}
				try{out.close();}
				catch(IOException e){e.printStackTrace();}	
			
		}
		
	//Set Message to be sent to Master (Client data) 
	public synchronized void setMessage(Client mes)
	{
		this.aMessage=mes;
	}
	
	
	

	}	//End Write 
	
	
	
	public class worker_calculation extends Thread{
		
		ArrayList<waypoint> arrs_wp;
		
		public worker_calculation(ArrayList <waypoint> wp)
		{
			this.arrs_wp = wp;
		}
		
		
		
	/* Calculate Distance */
	public double distance(double lat1, double lon1, double lat2, double lon2 , String unit) {
		if ((lat1 == lat2) && (lon1 == lon2)) {
			return 0;
		}
		else {
			double theta = lon1 - lon2;
			double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
			dist = Math.acos(dist);
			dist = Math.toDegrees(dist);
			dist = dist * 60 * 1.1515;
			if (unit.equals("K")) {
				dist = dist * 1.609344;
			} else if (unit.equals("N")) {
				dist = dist * 0.8684;
			}
			return (dist);
			}
		}
	
	/* Unpack Time String && Split it */
	public String[] getTime(String time)
	{
		String [] time_arr;
		String [] clock_arr;
			
			
		time_arr = time.split("T") ;	//Split Time in Date / Time 
		time_arr[1] = time_arr[1].replaceAll("Z","");
		clock_arr = time_arr[1].split(":");
		return clock_arr;
	}

			
		public void run()
		{
			//Save Whole Statistics 
			int average_excercise_time = 0;
			int average_distance = 0;
			int average_uphill = 0;
			
			String [] last_time_arr = {};
			String [] time_temp;
			
			/* Calculate Results */
			// set user = waypoint's creator
			String user = this.arrs_wp.get(0).creator;
			
			double last_lat=0;
			double last_lon=0;
			String time;
			double [] total_time = {0,0,0};
			
			double total_distance=0;
			double total_elevation=0;
			double distance=0;
			
			//Loop Through Mapped Data to Get Distance 
			for(waypoint a : this.arrs_wp)
			{
					
			if(last_lat!=0 || last_lon!=0)
			{
				//Calculate Distance from point A to B
				total_distance = distance + distance(last_lat , last_lon , Double.parseDouble(a.lat) , Double.parseDouble(a.lon), "K");
				//Calculate Elevation from A to B 
				total_elevation = total_elevation + Double.parseDouble(a.ele);
				
				//Edit Time String 
				time_temp = getTime(a.time);
				//Hours - Minutes - Seconds 
				total_time[0] = total_time[0] + (Double.parseDouble(time_temp[0]) - Double.parseDouble(last_time_arr[0]));
				total_time[1] = total_time[1] + (Double.parseDouble(time_temp[1]) - Double.parseDouble(last_time_arr[1]));
				total_time[2] = total_time[2] + (Double.parseDouble(time_temp[2]) - Double.parseDouble(last_time_arr[2]));
				
			
			}
			
			/* Save Cords for next Loop (first cords to second cords distance) */
			last_lat = Double.parseDouble(a.lat) ;
			last_lon = Double.parseDouble(a.lon) ;
			/* get time */
			last_time_arr = getTime(a.time);
			
			}
			
		
			
			/* Final Data Calculated */
			
			/* Total_Distance(KM) - Average_speed - Total_Elevation - Total_Time(MIN) */
			// Calculate Time (Minutes)
			double total_tim=0;
			total_tim = total_tim + total_time[0] * 60;
			total_tim = total_tim + total_time[1] * 1;
			total_tim = total_tim + total_time[2] / 60;
			
			double average_speed = total_distance / total_tim;


			/* Commence Sending To Master */
			Client this_client = new Client(user , total_distance , average_speed , total_elevation , total_tim);
			// Send To Writer Class to be sent to master 
			writer.setMessage(this_client);
			
			
		}
		
	}
	
	
public static void main(String args[])
{
		int x = 4;
		for(int i=0 ; i<x ; i++)
		{
			worker Worker = new worker();
			Worker.start();
		}

}
	
	
	
	
	
	
}//End Worker 
	
	
























































