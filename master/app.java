import java.util.*; 
import java.io.*;
import java.net.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class app extends Thread
{
	

	// Requests for Statistics to be Resolved 
	ArrayList<StringBuilder> requests = new ArrayList<>();	
	ArrayList<Client> clients = new ArrayList<>();
	// Dictionary with Users and Threads to them 
	Dictionary<String,  requestToAndroid.WritingForRequest> androids = new Hashtable<>();
	// Writer to Master Thread 
	RequestToMaster.WritingToMaster master_req_writer = null ;
	
	public static int counter = 0;
	
	public static int get_counter()
	{
		return counter++;
	}

	//Saved Ports 
	int [] ports ;
	
	//default Constructor 
	public app(){;}
	
	
	//Master Constructor 
	public app(int [] ports)
	{
		this.ports = ports;
	}
	

   public void set_writer_toMaster(RequestToMaster.WritingToMaster w)
   {
	   this.master_req_writer = w;
   }


	public parse get_parse(String str)
	{
		return new parse(str);
	}
	
	public void run() 
	{
		
		Socket requestSocket = null;
		ObjectInputStream in = null;

		//Try To Create A Connection With Server 
		try {
			
			// Open APP Server Ports for Communicating with Androids 
			for(int i=0 ; i< this.ports.length ; i++)
			{
				//Create threads for Listening Server On Given Ports 
				Thread T = new ServerForAPP( ports[i] );
				T.start();
			}


			/* Create socket for contacting the server on port 4320*/
			String host = "localhost";
			requestSocket= new Socket(host,  4320);	//Server Listening on 4320 created in Master's Class 

			/* Create the streams to send and receive data from server */
			in = new ObjectInputStream(requestSocket.getInputStream());
			//Create Read Threads 
			ReadingForAPP re = new ReadingForAPP(in,requestSocket);			
			Thread read = new Thread(re);
			read.start();


			host = "localhost";
			requestSocket= new Socket(host,  4561);	//Server Listening on 4561 created in Master's Class for Requests 
			RequestToMaster req_thread = new RequestToMaster(requestSocket);
			Thread t = new Thread(req_thread);
			t.start();
			
			
			/* Keep Threads read alive */
			while(requestSocket.isConnected())
			{
				try {
				Thread.sleep(1000);
					}
				catch (InterruptedException e) {
				e.printStackTrace();
					}
			
			// Handle Requests 

			if(!requests.isEmpty())
			{
				while(!requests.isEmpty())
				{
					master_req_writer.addRequest(requests.remove(0));
					
				}
			}
			
				
			
			}// End While 
				
			
			

		} catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} 
		finally {
			try {
				System.out.println("closed ..");
				in.close();
				requestSocket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}
	
	/* Class To communicate with Master to resolve request */
	public class RequestToMaster extends Thread{
		
		ObjectInputStream in;
		Socket socket;
		ObjectOutputStream out;
		
		
		public RequestToMaster(Socket socket)
		{
			try 
			{
				this.socket = socket;
				this.in = new ObjectInputStream(socket.getInputStream());
				this.out = new ObjectOutputStream(socket.getOutputStream());
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
				WritingToMaster wr = new WritingToMaster(out,socket);
				ReadingFromMaster re = new ReadingFromMaster(in,socket);
				
				// Commence Read - Write Threads 
				Thread write =  new Thread(wr);
				Thread read = new Thread(re);
				write.start();
				
				// Add Write Thread To Master's ArrayList  
				set_writer_toMaster(wr);
				
				// Start Read Thread
				read.start();
				
				// Keep Threads Alive so long as The program is Executing 
				while(socket.isConnected()){		
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
					System.out.println("closed");
					in.close();
					out.close();
					socket.close();
				}
				catch(IOException e){e.printStackTrace();}
					
			}
		}

		
		
		/* Write to Master for Requests */
		public class WritingToMaster extends Thread
		{
			
			Socket socket;
			ObjectOutputStream out;
			ArrayList <StringBuilder> message = new ArrayList <>();
			
			public WritingToMaster(ObjectOutputStream out , Socket socket)
			{
				this.out=out;
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
							// while Requests available , send them 				
							while(!message.isEmpty())
							{
								System.out.println("Request to be sent to Master "+message.get(0));
								out.writeObject(message.remove(0));
								
								out.flush();
								System.out.println("Request Flushed to Master");
							}
							
						}
					catch (IOException e) 
						{
						e.printStackTrace();
						} 	
											
					}
					/*try
					{
						System.out.println("closed out"); 
						out.close();
						}
					catch(IOException e){e.printStackTrace();}	
				*/
			}
			
			// Add Request-username to be written to Master for resolution
			public synchronized void addRequest(StringBuilder s)
			{
				this.message.add(s);
			}
		}
			
		
		
		/* Read From Master result of Request */
		public class ReadingFromMaster extends Thread
		{
			
			ObjectInputStream in;
			Socket socket;
			ArrayList<StringBuilder> message = new ArrayList<>();
		
			public ReadingFromMaster(ObjectInputStream in , Socket socket) 
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
				// Client Information Read 
				System.out.println("message APP received from request Master :"+mes.client.name);
				
				
				//To be sent back to Android 
				System.out.println("app sends back to android Request ..");
				androids.get(mes.client.name).setmessage(mes);
				
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
			try{in.close();System.out.println("closed in "); }
			catch(IOException e){e.printStackTrace();}	
			}
		}

		
	}//End RequestToMaster
	
	
	
		
	/* Input Stream Class For Master Class */
	public class ReadingForAPP extends Thread{
		ObjectInputStream in;
		Socket socket;

		public ReadingForAPP(ObjectInputStream in , Socket socket) 
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
				// Message read 
				System.out.println("message APP received : \n"+mes.toString(1));
				
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
	
	/* Read Class for App to read GPX files from Android */
	public class readGPXfromAndroid extends Thread{
		ObjectInputStream in;
		Socket socket;

		public readGPXfromAndroid(Socket socket) 
		{
				this.socket = socket;
		}
		

		public void run() 
		{
			try 
			{
				in = new ObjectInputStream(socket.getInputStream());
				this.in = in;
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			
			
			while(socket.isConnected())
			{
			//Read Message from Android
			try
				{
				//Sleep For 1s
				try {
					Thread.sleep(1000);
					  }
				catch (InterruptedException e) {
				e.printStackTrace();
					  }
				
				StringBuilder mes = (StringBuilder) in.readObject();

					  			  
				// GPX file read
				//System.out.println("message APP received stb : \n"+mes);
		
				
				// GPX read , now use Class Parse to edit it and send it to Master 
				Thread t = new parse(mes);
				t.start();
				
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
		
	}
	

		/* Class that Sends StringBuilder of parsed .gpx file to Master */
	public class parse extends Thread
	{

		//Initialize Sockets
		Socket requestSocket = null;
		ObjectOutputStream out = null;
		
		//Initialize Data 
		String filename;
		StringBuilder Builder;
		StringBuilder file;

		public parse(String filename)
		{
			this.filename = filename;
			
		}
		
		public parse(StringBuilder file)
		{
			this.file=file;			
		}
		
		
		public void run()
		{
				//Connect With Master 
				try 
				{
				/* Create socket for contacting the server on port 4321*/
				String host = "localhost";
				requestSocket = new Socket(host,  4322);	//Server Listening on 4322 created in Master's Class 

				/* Create the streams to send and receive data from server */
				out = new ObjectOutputStream(requestSocket.getOutputStream());
				
				}
				catch (UnknownHostException unknownHost) {
				System.err.println("You are trying to connect to an unknown host!");
			} catch (IOException ioException) {
				ioException.printStackTrace();
			} 
			
			


			/* String Builder */
			StringBuilder builder = new StringBuilder();
				
			try {			
				// Creating the temporary file
				this.filename = ("temp"+String.valueOf(get_counter())+".gpx");
				System.out.println(this.filename);
				File f = new File(this.filename);
				FileWriter myWriter = new FileWriter(f);
				myWriter.write(this.file.toString());
				myWriter.close();
				
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(f);
				doc.getDocumentElement().normalize();
				
				builder.append(this.filename+"\n");

				/* get all gpx tags (each gpx file has a single gpx tag) */
				NodeList gpxs = doc.getElementsByTagName("gpx");
				Node gpxtag = gpxs.item(0);

				/* creator always at position 0 */
				Node creator = gpxtag.getAttributes().item(0);
				builder.append(creator.getNodeName() + ": "
						+ creator.getTextContent()+"\n");

				/* get all wpt nodes */
				NodeList waypoints = doc.getElementsByTagName("wpt");

				for (int i = 0; i < waypoints.getLength(); i++) {
					Node waypoint = waypoints.item(i);

					/* get attributes of each wpt tag (lat and lon) */
					NamedNodeMap wptAttributes = waypoint.getAttributes();
					for (int j = 0; j < wptAttributes.getLength(); j++) {
						Node attribute = wptAttributes.item(j);
						builder.append(attribute.getNodeName() + ": "
								+ attribute.getNodeValue() + ", ");
					}
					builder.append("\n");

					/* get child nodes of wpt tag (ele and time) */
					NodeList wptChildren = waypoint.getChildNodes();
					for (int j = 0; j < wptChildren.getLength(); j++) {
						Node childNode = wptChildren.item(j);

						/* ignore hidden #text tags */
						if (!childNode.getNodeName().equals("#text")) {
							builder.append(childNode.getNodeName() + ": "
									+ childNode.getTextContent() + ", ");
						}
					}
					builder.append("\n");
				}
				f.delete();
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			
			
			// Save StringBuilder And Send it To Master 
			this.Builder = builder;
			//System.out.println(Builder);
			//System.out.println("Parsed file : "+this.filename);

			try
			{
			out.writeObject(new message(this.Builder));
			out.flush();
			}
			
			catch (IOException e) {e.printStackTrace();} 			
			try{out.close();}
			catch(IOException e){e.printStackTrace();}	
			
			
			
			
		}
		
		
		
		
	}


	// ServerForMaster.java - Class
	// Open Server Ports For Master 
	public class ServerForAPP extends Thread  {
			
			int port;
			
			public ServerForAPP(int port)
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

					while (true) 
					{
						/* Accept the connection */
						providerSocket = serverSocket.accept();


						System.out.println("Connection Accepted");
				
						/* Handle ANDROID requests using required function (Read/Write) */
						if(this.port==4311)
							{
								requestToAndroid T = new requestToAndroid(providerSocket);
								T.start();						
							}
							
						/* Parse GPX file from ANDROID port : 4310 */	
						if(this.port==4310)
							{
								readGPXfromAndroid T = new readGPXfromAndroid(providerSocket);
								T.start();
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
		
		
	/* Class that Sends/Receives REQUESTS FROM/TO ANDROID */
	public class requestToAndroid extends Thread
	{

		//Initialize Sockets
		Socket requestSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		

		public requestToAndroid(Socket s)
		{
			this.requestSocket = s;
		}
		
		
		public void run()
		{
				//Connect With Master 
				try 
				{

				
				/* Create the streams to send and receive data from server */
				out = new ObjectOutputStream(requestSocket.getOutputStream());
				in = new ObjectInputStream(requestSocket.getInputStream());

				WritingForRequest wr = new WritingForRequest(out,requestSocket);
				ReadingForRequest re = new ReadingForRequest(in , requestSocket , wr);
				

				// Commence Read - Write Threads 
				Thread read = new Thread(re);
				Thread write = new Thread(wr);
										
				read.start();
				write.start();
				
				
				}
				
				catch (UnknownHostException unknownHost) {
				System.err.println("You are trying to connect to an unknown host!");
			} catch (IOException ioException) {
				ioException.printStackTrace();
			} 
			
			
			/* Keep Threads read alive */
			while(requestSocket.isConnected())
			{
				try {
				Thread.sleep(1000);
					}
				catch (InterruptedException e) {
				e.printStackTrace();
					}								
			}// End While 
	
			
			
		}
		
/* Reading for Request Data-User from ANDROID */		
	public class ReadingForRequest extends Thread{
		ObjectInputStream in;
		Socket socket;
		WritingForRequest wrs;
		
		public ReadingForRequest(ObjectInputStream in , Socket socket , WritingForRequest wr ) 
		{
				this.in = in;
				this.socket = socket;
				this.wrs = wr;
		}
		

		public void run() 
		{

			//Read Message from Master
			try
				{
				while(socket.isConnected())
				{
					StringBuilder mes = (StringBuilder) in.readObject();
					// Message read 
					System.out.println("message APP-Request received : "+mes.toString());
					// Save user's name and Thread responsible for writing to User-Android 
					androids.put(mes.toString() , this.wrs);
					// Save name of User that requested 
					requests.add(mes);

		
					
				}
				
				}
			catch (ClassNotFoundException e)
				{
				throw new RuntimeException(e);
				}
			catch (IOException e) 
				{
				//Catch If Nothing Found To Read 
				} 

		
			try{in.close();}
			catch(IOException e){e.printStackTrace();}	
		}
		
		

	}	//End ReadingForRequest

		
	

	/* Writing For Request Data-User */
	public class WritingForRequest extends Thread {

		ObjectOutputStream out;
		Socket socket;
		message mes = null;
		
		public WritingForRequest(ObjectOutputStream out , Socket connection) 
		{
			this.out = out;
			this.socket = connection;
		} 
			

		public void run() 
		{
			
			//Send Message to Worker 
			try
				{
				while(socket.isConnected()){
				//Sleep For 1s
				try {
					Thread.sleep(1000);
					  }
				catch (InterruptedException e) {
				e.printStackTrace();
					  }
					  if(mes!=null){
						
						//Send Request For User-Data To Master Holding User Title 
						out.writeObject(new StringBuilder(mes.client.toString()));
						out.flush();
						this.mes=null;
					  }
					}
				}
			catch (IOException e) 
				{
				//e.printStackTrace();
				} 	
									
			
			try{out.close();}
			catch(IOException e){e.printStackTrace();}	
		
		
		}
		
		
	public synchronized void setmessage(message mes){
		this.mes = mes;
		}
	
		
		
	}//End WritingForRequest 



	
	} //End Request User-Data  	
		
		
		
		
		



	/* Main Application Running */
	public static void main(String args[])
	{
		
		// Number of workers 
		int x = 4;
		
		int [] port_array = {4321 , 4322 , 4320 , 4561};
		//Create Master 
		Thread Master = new master(port_array , x); 
		//Start Master Thread 
		Master.start();
	
	
		//Ports For Server To Create Listen Channel On 
		int [] port_array2 = {4310 , 4311};
		//Create Master 
		app app = new app(port_array2); 
		//Start Master Thread 
		app.start();
	
	
		parse parser;
		//Read GPX Files from Args
		StringBuilder builder = new StringBuilder();
		for (String arg: args) 
		{
			// For Each File , create a parse Thread 
			parser = app.get_parse(arg);
			parser.start();
		}
	
	
		System.out.println("DONE");
	
				
		
	}	
	






	
}













