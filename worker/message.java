// Message.java
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.lang.*;
import java.io.Serializable;



/* Message Class Holds StringBuilder Attritbute */
public class message implements Serializable{
	
	public StringBuilder builder;
	public ArrayList <waypoint> arr_wp;
	public List <Client> client_arr;
	public Client client;
	public String string;

	public message(String str , Client cl)
	{
		this.client = cl;
		this.string = str;
	}



	public message(StringBuilder str1)
	{
		this.builder = str1;
	}
	
	public message(String str1)
	{
		this.builder = new StringBuilder(str1);
	}

	public message(ArrayList <waypoint> wp)
	{
		this.arr_wp = wp;
	}
	
	public message()
	{
		this.arr_wp = new ArrayList<waypoint>();
	}

	public ArrayList<waypoint> getArr()
	{
		return this.arr_wp;
	}
	
	public message(List<Client> ar)
	{
		this.client_arr = ar;
	}

	public message(Client cl)
	{
		this.client = cl;
	}



	@Override
	public String toString() 
	{
	return this.builder.toString(); 
	
	}


	public String toString(String st) 
	{
	StringBuilder strb = new StringBuilder();
	for(waypoint wp : arr_wp)
		strb.append(wp.toString()+"\n");
	
	return strb.toString(); 
	
	}
	
	public String toString(int a)
	{
	StringBuilder strb = new StringBuilder();
	for(Client cl : this.client_arr)
		strb.append(cl.toString()+"\n");
	
	return strb.toString(); 
	}





}
