
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
/**
 * This file represents the client side of the program. This files arguements are IP, socket
 * This file purpose is to connect to a server establish a connection and do a partial summation of  the multitude of packets given to it. 
 * @author carl hartry jr.
 *
 */

public class DIYAppWorker 
{
	public static void main(String[] args) throws Exception 
	{
		InetAddress	ip = null;
		int port = 0;
		Socket sock =null;
		String buffer = "";
		ObjectInputStream in = null;
		PrintWriter out;
		double partSum = 0.0d;
		int count =1;
		Double accumu = 0.0d;

		if(args.length != 2) // connect to server
		{
			throw new Exception("Not a right amount of arguments");
		}
		else 
		{
			try 
			{
				ip = InetAddress.getByName(args[0]);
				port = Integer.parseInt(args[1]);
			}
			catch(Exception e)
			{
				System.out.println("Error with arguments given  ip[0]  socket[1] Error:  "+ e);
				System.exit(0);
			}
		}
		System.out.println("Trying to connect to server");
		sock = new Socket(ip,port);
		while(!sock.isConnected()) 
		{
			sock.close();
			sock = new Socket(ip,port);
		}
		System.out.println("In line for work");

		while(true)// message passing get a obj with a array list loop the list if the list is null make obj "0" else print and send amount gather + amount sent in total.
		{
			try 
			{
				buffer ="";
				buffer += accumu;
				out = new PrintWriter(sock.getOutputStream(),true);
				out.println(buffer);
				in = new ObjectInputStream(sock.getInputStream());
				Object obj;

				if(!in.equals(null)) 
				{
					obj = in.readObject();
					if(obj == null) 
					{
						obj = "0";
					}
				}
				else
				{
					obj = "0";
				}
				System.out.println("Getting slice");
				if(!obj.equals("0")) 
				{
					@SuppressWarnings("unchecked")
					ArrayList<String> workPacket = (ArrayList<String>)obj;
					System.out.println("Have recived a packet and is processing it now!");
					accumu = 0.0d;
					for(String s:workPacket) 
					{
						if(s != null) 
						{
						accumu	+= Double.parseDouble(s);
						partSum += Double.parseDouble(s);
						}
					}
					System.out.println("Iteration{"+count+"} Acummulated: " + accumu+ " and has sent back in total : " + partSum);
					buffer += partSum;
				}
				else 
				{
					buffer="Done";
					out = new PrintWriter(sock.getOutputStream(),true);
					out.println(buffer);
					break;
				}
			}
			catch (Exception e) 
			{
				e.printStackTrace();
				in.close();
				sock.close();
				System.exit(0);
			}
			count ++;
		}
		System.out.println("Clocking Out!");
		in.close();
		out.close();
		sock.close();
	}//EOM
}
