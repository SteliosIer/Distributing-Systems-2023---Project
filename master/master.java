import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;



public class master extends Thread 
{
	
	// Save average statistics 
	static double average_excercise_time = 0;
	static double average_distance = 0;
	static double average_uphill = 0;
	
	// Save Individual statistics (Clients with their calculated data)
	protected ArrayList<Client> all_clients = new ArrayList<>();
		
	//Save Statistics from worker's work
	ArrayList<Client> clients = new ArrayList<>();
	
	//Number of workers 
	int no_of_workers;

	//Initialize String Builder with GPX data from parse
	StringBuilder str_build = new StringBuilder();
	
	//Initialize Mapped Data in Array
	ArrayList <ArrayList <waypoint>> mapped_waypoints = new ArrayList<ArrayList<waypoint>>();
	
	//Initialize ArrayList with objects responsible for Writing to Worker
	ArrayList <ActionsForMaster.WritingForMaster> worker_threads = new ArrayList<ActionsForMaster.WritingForMaster>();; 
	
	//Initialize writer responsible for WRITING to APP - class 
	WritingForAPP app_writer;
	
	
	//Saved Ports 
	int [] ports ;
	
	//default Constructor 
	public master(){;}
	
	//Master Constructor 
	public master(int [] ports , int size)
	{
		this.ports = ports;
		this.no_of_workers = size;
	}
	
	

	/* Master's Run , Continiously Checks For Available Work and Synchronization with Workers etc.. */
	public void run()
	{
		for(int i=0 ; i< this.ports.length ; i++)
		{
			//Create threads for Listening Server On Given Ports 
			Thread T = new ServerForMaster( ports[i] );
			T.start();
		}
		System.out.println("Master Thread Commenced");
		
		//Master Working Non-stop
		while(true)
		{

		try {
        Thread.sleep(1200);
      }
		catch (InterruptedException e) {
        e.printStackTrace();
      }


		// If raw data from parsing , Map Them 
		if(this.str_build.length()!=0)
			{
			//Create MAP Thread 
			Thread T = new map(this.str_build , 20);
			T.start();
			this.str_build = new StringBuilder();
			}	//End if MAP
			
			
		int counter=0;
		
		// If mapped data available , disperse them to workers
		if(!this.mapped_waypoints.isEmpty() && this.worker_threads.size()==no_of_workers)
		{
			/* Disperse Them To Workers */			
	
			//Need to implement RR 
			for(int i=0 ; i < no_of_workers ; i++)
			{
				if(!this.mapped_waypoints.isEmpty())
				{
									
				// create message using last chunk			
				message msg = new message (this.mapped_waypoints.remove( this.mapped_waypoints.size()-1) );
				// set write message for master to be sent to Worker 
				this.worker_threads.get(i).setMessage(msg);
					
				}
				
				// if no more work available , break the loop
				if(this.mapped_waypoints.isEmpty())
					break;
			}
			
			
			
		}	//end if mapped_waypoints empty 
		
		
		/* REDUCING RESULTS FROM WORKERS */		
		if(!this.clients.isEmpty())
		{
			/* Calculate Statistics for Variables saved in Master */
			// Create Thread , Parameters Current All Client Array + New List returned from workers
			Thread cal = new calculateStats(new ArrayList<Client>(this.clients) , new ArrayList<Client>(this.all_clients));
			cal.start();
			
			// Temporary useful ArrayLists 
			ArrayList <String> temp_names = new ArrayList<>();
			ArrayList <Client> temp_clients = new ArrayList<>();
			
			// Loop Clients from workers
			for(Client c : clients)
			{
				//Save Client to all_clients arraylist
				this.all_clients.add(c);
				
				// if temp array doesnt have client , add him
				if(!temp_names.contains(c.name))
				{
					// Add name found in templist 
					temp_names.add(c.name);
					// Create New Client Instance 
					temp_clients.add(new Client(c.name));
				}
				
				// if temp array has client , update his statistics 
				if(temp_names.contains(c.name))
				{
					for(Client cl : temp_clients)
					{
						if(cl.name.equals(c.name))
							cl.update(c.total_distance , c.average_speed , c.total_elevation , c.total_time);
					}
					
					
				}
				
				
				
			}
			// Clear ArrayList	
			clients.clear();
		
			// Give message to WriterForAPP to send it to APP
			this.app_writer.setMessage(new message(temp_clients));

			
		}	//End if Reduce 


		} // End While
	}
	



	//Set GPX files Master Read by parse class 
	public synchronized void set_gpx(message builder)
	{
		this.str_build.append(builder.builder);
		//System.out.println("appended");
	} 
	
	// Map Class Sets Mapped Waypoints for Master to Use 
	public synchronized void set_mapped_waypoints(ArrayList <ArrayList <waypoint>> mapped_waypoint)
	{
		for(ArrayList <waypoint> wp : mapped_waypoint)
			this.mapped_waypoints.add(wp);
	}

	// Set Amount of workers 
	public void setWorkers(int size)
	{
		this.no_of_workers=size;
	}

	// Add Worker Thread to ArrayList , Threads that help Master to Write To Workers 
	public synchronized void add_worker_thread(ActionsForMaster.WritingForMaster t)
	{
		this.worker_threads.add(t);
	}

	// Add to Client arraylist the results of worker's work
	public synchronized void add_workers_results(Client cl)
	{
		this.clients.add(cl);
	}

	// Set APP Writer Object to Master , for Returning the Results to the APP class 
	public void setAppWriter(WritingForAPP t){
		this.app_writer = t;
	}

	// Return All Client's Data Read So Far , To count The Average statistics 
	public ArrayList<Client> getClients(){
		return this.all_clients;}


// ServerForMaster.java - Class
// Open Server Ports For Master 
public class ServerForMaster extends Thread  {
		
		int port;
		
		public ServerForMaster(int port)
		{
			this.port=port;
		}
		
		/* Define the socket that receives requests */
		ServerSocket serverSocket;

		/* Define the socket that is used to handle the connection */
		Socket providerSocket;


		public void run() 
		{
			try {

				/* Create Server Socket */
				serverSocket = new ServerSocket(this.port);

				while (true) {
					/* Accept the connection */
					providerSocket = serverSocket.accept();


					System.out.println("Connection Accepted");
					/* Handle the APP writing */
					if(this.port==4320)
						{
							WritingForAPP wr = new WritingForAPP(providerSocket);
							setAppWriter(wr);
							Thread T = new Thread(wr); 
							T.start();
						}
					
					
					/* Handle the workers' requests using required function (Read/Write) */
					if(this.port==4321)
						{
							Thread T = new ActionsForMaster(providerSocket);
							T.start();
						}
						
					/* Connection with Parse Class (parse GPX File) */	
					if(this.port==4322)
						{
							//Functions for Read of Class Parse messages 
							Thread T = new ParserForMaster(providerSocket);
							T.start();
						}	
						
					/* Receive Requests from APP */
					if(this.port==4561)
						{
							//Functions for Read of Request Messages  
							Thread T = new ActionsForRequest(providerSocket);
							T.start();
							//System.out.println("4561 master");
						}	
						
					


						
				}

			} catch (IOException ioException) {
				ioException.printStackTrace();
			} finally {
				try {
					providerSocket.close();
				} catch (IOException ioException) {
					ioException.printStackTrace();
				}
			}
		}
		
		
		
		
		
		
	}// End ServerForMaster 


// ActionsForMaster.java
// Functions For Read/Write With Workers
public class ActionsForMaster extends Thread {
	
	ObjectInputStream in;
	ObjectOutputStream out;
	Socket socket;
	
	public ActionsForMaster(Socket connection) {
		try 
		{
			out = new ObjectOutputStream(connection.getOutputStream());
			in = new ObjectInputStream(connection.getInputStream());
			this.socket = connection;
			
		} 
		
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public void run() 
	{
		try
		{
			WritingForMaster wr = new WritingForMaster(out,socket);
			ReadingForMaster re = new ReadingForMaster(in,socket);
			
			// Commence Read - Write Threads 
			Thread write =  new Thread(wr);
			Thread read = new Thread(re);
			write.start();
			
			// Add Write Thread To Master's ArrayList  
			add_worker_thread(wr);
			
			// Start Read Thread
			read.start();
			
			// Keep Threads Alive so long as The program is Executing 
			while(write.isAlive()){		
			try {Thread.sleep(1000);}
				catch (InterruptedException e) {e.printStackTrace();}}
			while(read.isAlive()){		
			try {Thread.sleep(1000);}
				catch (InterruptedException e) {e.printStackTrace();}}
		}
		
		finally
		{
			try
			{
				in.close();
				out.close();
				socket.close();
			}
			catch(IOException e){e.printStackTrace();}
				
		}
	}
	


/* Class Responsible For Reading From Workers To Master */
// ReadingForMaster.java
public class ReadingForMaster extends Thread {
	
	ObjectInputStream in;
	Socket socket;
	
	public ReadingForMaster(ObjectInputStream in ,Socket connection) 
	{
			this.in = in;
			this.socket = connection;			
	}

	public void run() {
		
		while(socket.isConnected())
			{
			//Read Message from Worker
			try
				{
				
				// read message 
				message mes = (message) in.readObject();
				// set the message 
				add_workers_results(mes.client);
				
				
				}
			catch (IOException e) 
				{
				//Catch Exception if Nothing Found to Read 
				}
			catch (ClassNotFoundException e)
				{
				throw new RuntimeException(e);
				}
		
			
			}
			try{in.close();}
			catch(IOException e){e.printStackTrace();}	
		

	}
	


}

/* Class Responsible For Writing from Master to Workers */
// WritingForMaster.java
public class WritingForMaster extends Thread {

	ObjectOutputStream out;
	Socket socket;
	message aMessage;	//Message to be sent to Worker
	
	public  WritingForMaster(ObjectOutputStream out , Socket connection) 
	{
		this.out = out;
		this.socket = connection;
	} 
		

	public void run() 
	{
		
		while(socket.isConnected())
			{
			
			//Send Message to Worker 
			try
				{
				//Sleep For 1s
				try {
					Thread.sleep(1000);
					  }
				catch (InterruptedException e) {
				e.printStackTrace();
					  }
					  
					if(this.aMessage!=null)
					{
						
						message mes = aMessage;		//Create New message with DATA of old message 
						out.writeObject(mes);
						out.flush();
						this.aMessage=null;
					}
				}
			catch (IOException e) 
				{
				//e.printStackTrace();
				} 	
									
			}
			try{out.close();}
			catch(IOException e){e.printStackTrace();}	
		
	}
	
	//Set Message to be sent 
	public synchronized void setMessage(message mes)
	{
		this.aMessage=mes;
	}
	

	
	
	
}








}  //End ActionsForMaster.java 


// ParserForMaster.java
// Functions For Parsing the GPX Files From APP 
public class ParserForMaster extends Thread {
	
	ObjectInputStream in;
	Socket socket;
	
	public ParserForMaster(Socket connection) {
		try 
		{
			//Input Connections-Streams
			in = new ObjectInputStream(connection.getInputStream());
			this.socket = connection;
			
		} 
		
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	/* Parser Thread reads incoming *.gpx files from Main as long as the Program is executing  */
	public void run() 
	{
		try
		{
			Thread read = new ReadingForMaster(in,socket);
			read.start();
			while(read.isAlive()){		
			try {Thread.sleep(1000);}
				catch (InterruptedException e) {e.printStackTrace();}}
		}
		
		finally
		{
			try
			{
				in.close();
				//out.close();
				socket.close();
			}
			catch(IOException e){e.printStackTrace();}
				
		}
	}
	




public class ReadingForMaster extends Thread  {
	
	ObjectInputStream in;
	Socket socket;
	
	public ReadingForMaster(ObjectInputStream in ,Socket connection) 
	{
			this.in = in;
			this.socket = connection;			
	}

	public void run() {
		
		while(socket.isConnected())
			{
			//Read Message from Parse Class 
			try
				{
				
				message mes = (message) in.readObject();

				set_gpx(mes);
							
				//System.out.println("message server received : "+mes);
				
				}
			catch (IOException e) 
				{
				//Catch Exception if Nothing Found to Read 
				}
			catch (ClassNotFoundException e)
				{
				throw new RuntimeException(e);
				}
		
			
			}
			try{in.close();}
			catch(IOException e){e.printStackTrace();}	
		

	}
	


}





}


// map.java
// Mapping the Data 
public class map extends Thread
{

		StringBuilder stringbuilder;
		int N;


		//Map Constructor 
		public map(StringBuilder stb , int N)
		{
			this.stringbuilder = stb;
			this.N = N;	
			
		}
		
		/* Map Thread , Edits the data . 
		Edits The Data , Creates Waypoints with them 
		And divides them in chunks .
		When finished it Sets them for use from Master.
		*/
		
		public void run()
		{
			
			/* Map Waypoints Given From Master (N waypoints) */
			
			//Split StringBuilder by line
			String[] lines = this.stringbuilder.toString().split("\n");
			String[][] new_lines = new String[lines.length][2];
			
			
			// Replace COMMA with ""
			for(int i=0 ; i<lines.length ; i++)
			{
				lines[i] = lines[i].replaceAll(",","");
			}

			// Split String Where Empty Space " " .
			for(int i=0 ; i<lines.length ; i++)
			{
				new_lines[i] = lines[i].split(" "); 
			}

			// Create ArrayList for Saving Elements 
			ArrayList<String> str_array = new ArrayList<String>();
			
			// Put Contents To 1 ArrayList
			
			for(String[] i : new_lines)
			{
				for(String j : i)
				{
					//System.out.println(j);
					str_array.add(j);		
				}
			}
			
			// ArrayList to Array Convertion
			String[] arr = new String[str_array.size()];
			arr = str_array.toArray(arr);
			
			
			/* Get Element Values && Save Them As Waypoints */
			
			// Save Temp Data 
			String creator = null;
			String lat = null;
			String lon = null;
			String ele = null;
			String time = null;
			String this_file = null;
			
			// Save Waypoints ArrayList
			ArrayList <waypoint> waypoints = new ArrayList<waypoint>();
			
			// Loop Through Array with Elements  
			for(int i=0 ; i < arr.length ; i++)
			{
				if(arr[i].contains(".gpx"))
				{
					this_file=arr[i];
				}
				if(arr[i].equals("creator:"))
				{
					creator = arr[i+1];
				}
				if(arr[i].equals("lat:"))
				{
					lat = arr[i+1];
				}
				if(arr[i].equals("lon:"))
				{
					lon = arr[i+1];
				}
				if(arr[i].equals("ele:"))
				{
					ele = arr[i+1];
				}
				if(arr[i].equals("time:"))
				{
					time = arr[i+1];
					// Add waypoint to ArrayList 
					waypoints.add(new waypoint(this_file,creator,lat,lon,ele,time));
				}
				
			}
			
		
			/* Split in each Chunk with N waypoints */
			int counter=0;
			int chunk_size = this.N;
			String last_file="";
			ArrayList<ArrayList<waypoint> > arr_wp = new ArrayList<ArrayList<waypoint>>();
			ArrayList<waypoint> temp_wp = new ArrayList<waypoint>();
					
		
			for(waypoint wp : waypoints)
			{
				// Chunk filled (N) 				
				if(counter==chunk_size)
				{
					arr_wp.add(temp_wp);
					temp_wp = new ArrayList<waypoint>();
					counter=0;
				}
				
				// New User name found in Data , Break the chunk And Create A new one 
				if(!last_file.equals(wp.file) && counter!=0)
				{
					arr_wp.add(temp_wp);
					temp_wp = new ArrayList<waypoint>();
					counter=0;
				}

				// Add to ArrayList the waypoint , ++ the counter , save last file user-name
				temp_wp.add(wp);
				last_file = wp.file;
				counter++;

			}
			//If no more elements to add and chunk not filled
			if(!temp_wp.isEmpty() || counter<chunk_size)
				arr_wp.add(temp_wp);
			
			
			/* Send Multi-Dimensional ArrayList To Master */
			set_mapped_waypoints(arr_wp);
			
			/*
			ArrayList of ArrayLists N - waypoints each
			*/
			
			
			
			
		}
	

	
	
}


// WritingForAPP.java
// Writing Socket - Connection to APP class , after gpx file parsed successfully
public class WritingForAPP extends Thread {

	ObjectOutputStream out;
	Socket socket;
	message aMessage;	//Message to be sent to APP
	
	public WritingForAPP(Socket connection)  
		{
			//Create Output Stream connection 
			try
			{
			this.out = new ObjectOutputStream(connection.getOutputStream());
			this.socket = connection;	
			}
			catch (IOException e) 
			{
			e.printStackTrace();
			}
		} 
		

	/* Thread Sends Message to APP class */
	public void run() 
	{
		
		while(socket.isConnected())
			{
			
			//Send Message to Worker 
			try
				{
				//Sleep For 1s
				try {
					Thread.sleep(1000);
					  }
				catch (InterruptedException e) {
				e.printStackTrace();
					  }
					  
					if(this.aMessage!=null)
					{
						
						message mes = aMessage;		//Create message for APP 
						out.writeObject(mes);
						out.flush();
						this.aMessage=null;
					}
				}
			catch (IOException e) 
				{
				//e.printStackTrace();
				} 	
									
			}
			try{out.close();}
			catch(IOException e){e.printStackTrace();}	
		
	}
	
	//Set Message , used by MASTER for sending message to APP 
	public synchronized void setMessage(message mes)
	{
		this.aMessage=mes;
	}
	

	
	
	
}



// Calculate Stats 
// calculateStats.java
public class calculateStats extends Thread
{
	ArrayList<Client> new_clients;
	ArrayList<Client> all_clients;
	
	public calculateStats(ArrayList<Client> ar , ArrayList<Client> ar2){
		this.new_clients=ar;
		this.all_clients=ar2;
	}
	
	/* Calculate Average Data from All_Clients Throughout the programs life */
	// Only 1 Thread to be Executed Here , Since We are Using Critical Master's Data
	public synchronized void run()
	{

		int counter=0;
		double sum_distance = 0;
		double sum_av_speed = 0;
		double sum_elevation = 0;
		double sum_time = 0;
		
		/* Loop all Clients submitted and Calculate Average Data For Master */
		for(Client c : this.new_clients)
		{
			sum_distance = sum_distance + c.total_distance;
			sum_av_speed = sum_av_speed + c.average_speed;
			sum_elevation = sum_elevation + c.total_elevation;
			sum_time = sum_time + c.total_time;
			counter++;
		}
		
		for(Client c : this.all_clients)
		{
			sum_distance = sum_distance + c.total_distance;
			sum_av_speed = sum_av_speed + c.average_speed;
			sum_elevation = sum_elevation + c.total_elevation;
			sum_time = sum_time + c.total_time;
			counter++;
		}
		
		
		// Use Functions to Set The Average Data for Master
		set_average_distance(sum_distance/counter);
		set_average_speed(sum_av_speed/counter);
		set_average_elevation(sum_elevation/counter);
	
	}
	
}



// ActionsForRequest.java 
// Resolve Requests , Read Write 

public class ActionsForRequest extends Thread
{
	
	
	ObjectInputStream in;
	ObjectOutputStream out;
	Socket socket;
	
	public ActionsForRequest(Socket connection) {
		try 
		{
			this.out = new ObjectOutputStream(connection.getOutputStream());
			this.in = new ObjectInputStream(connection.getInputStream());
			this.socket = connection;
			
		} 
		
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public void run() 
	{
		try
		{
			
			ReadingForRequest re = new ReadingForRequest(in,socket , out);
			Thread read = new Thread(re);
			read.start();
			
			while(read.isAlive()){		
			try {Thread.sleep(1000);}
				catch (InterruptedException e) {e.printStackTrace();}}
		}
		
		finally
		{
			try
			{
				in.close();
				socket.close();
			}
			catch(IOException e){e.printStackTrace();}
				
		}
	}
	


/* Class Responsible For Reading From app to Master */
// ReadingForRequest.java
public class ReadingForRequest extends Thread {
	
	ObjectInputStream in;
	Socket socket;
	ObjectOutputStream out;
	ArrayList<Client> clients = new ArrayList<>();
	
	public ReadingForRequest(ObjectInputStream in ,Socket connection ,ObjectOutputStream out) 
	{
			this.out = out;
			this.in = in;
			this.socket = connection;			
	}

	public void run() {
		
		while(socket.isConnected())
			{
			//Read Message from Worker
			try
				{
				
				try
				{
				Thread.sleep(1000);}
				catch(Exception e){}
				
				// read message 
				StringBuilder mes = (StringBuilder) in.readObject();
				System.out.println("Inside Master , Request Received :"+mes);
				// set the message 
				for(Client i : all_clients){
				if(i.name.equals(mes.toString())){
					this.clients.add(i);
					}
				}
				
				double sum_distance = 0;
				double sum_av_speed = 0;
				double sum_elevation = 0;
				double sum_time = 0;
				
				/* Loop all Clients submitted and Calculate-Add up Values of all files */
				for(Client c : this.clients)
				{
					sum_distance = sum_distance + c.total_distance;
					sum_av_speed = sum_av_speed + c.average_speed;
					sum_elevation = sum_elevation + c.total_elevation;
					sum_time = sum_time + c.total_time;
				}
				
				
				Client client = new Client(mes.toString() , sum_distance , sum_av_speed , sum_elevation , sum_time);
				WritingForRequest wr = new WritingForRequest(this.out,this.socket , client);
				Thread write =  new Thread(wr);
				write.start();
				
				
				}
			catch (IOException e) 
				{
				//Catch Exception if Nothing Found to Read 
				}
			catch (ClassNotFoundException e)
				{
				throw new RuntimeException(e);
				}
		
			
			}
			try{in.close();}
			catch(IOException e){e.printStackTrace();}	
		
	}
	
	}

	/* Class Responsible For Writing from Master to app */
	// WritingForRequest.java
	public class WritingForRequest extends Thread {

		ObjectOutputStream out;
		Socket socket;
		Client cl;	//Message to be sent to Worker
		
		public  WritingForRequest(ObjectOutputStream out , Socket connection , Client cl) 
		{
			this.cl = cl;
			this.out = out;
			this.socket = connection;
		} 
			

		public void run() 
		{
			
		
				//Send User Data to APP
				try
					{
					// create message and send information
					System.out.println("Master completed Request and Flushes it back to APP");
					out.writeObject(new message(this.cl));
					out.flush();
					}
					
				catch (IOException e) 
					{
					//e.printStackTrace();
					} 	

				//try{out.close();}
				//catch(IOException e){e.printStackTrace();}	
			
		}
	
	
	}
	
	
}







public synchronized void set_average_distance(double x)
{
	this.average_excercise_time=x;
}
public synchronized void set_average_speed(double x)
{
	this.average_distance=x;
}
public synchronized void set_average_elevation(double x)
{
	this.average_uphill=x;
}



	
}//End Master
	
	






























































