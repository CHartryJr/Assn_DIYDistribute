import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * This class file is one out of two files used to simulate a Distruibtaion application
 *  The other is DIYAppWorker.java
 * @author carlhartryjr
 */
public class DIYAppController extends Thread
{ 
	private static final int workAmount = 8000;// amount of work per packets
	private static final int MAX_WORK_LOAD = 50;// the amount of packets the shared que acan hold
	private static final int TIME = 500;// time for threads to sleep
	private volatile static Double  sum = 0.0d;// the storage value for the final amount
	private static AtomicInteger num = new AtomicInteger(0);// a count for the nummber of threads
	private static LinkedBlockingQueue<ArrayList<String>> queuedwork;// the shared que for data to be send ou
	private static Semaphore semi = new Semaphore(1);
	private ServerSocket sock;
	private Socket clientSocket;
	private String threadName;
	private int id;
		/**
		 * This constructor must be called first. This creates a thread that accepts new connectiond and creats worker threads.
		 * @param sock
		 * @param semi
		 */
		protected  DIYAppController(ServerSocket sock) 
		{
			this.id =0;
			this.sock = sock;
		}
		/**
		 * This constructor is created within the first constructors run() and will handle all message handleing between clinet and server. The 
		 * messages are Objects that containt arraylist
		 * @param clientSocket
		 * @param semi
		 * @param id
		 */
		private DIYAppController(Socket clientSocket, int id) 
		{
			this.clientSocket = clientSocket;
			this.id =id;
		}
		/**
		 * Run method for this class is seperated by Thread id if id is "0" then it creates threads and accept new connections from clients if a
		 * any other id it will handle message passing between the client and server.
		 */
		public void run()
		{
			if(id ==0) // boss thread
			{
				try
				{
					while(true)
					{
						int count =num.incrementAndGet();
						Socket sockIn = sock.accept();
						DIYAppController worker = new DIYAppController(sockIn,count);
						System.out.println("Boss thread has hired worker{" + count +"}");
						worker.start();
					}
				}
				catch(Exception e)
				{
					try 
					{
						sock.close();
						System.exit(0);
					} 
					catch (IOException e1)
					{
						e1.printStackTrace();
					}
				}
			}
			else// worker thread
			{
				threadName = "{Worker "+id+"} ";
				System.out.println(threadName + " is ready for work");
				String buffer="";
				Scanner in = null ;
				try 
				{
					while(true) 
					{
						buffer = "";
						in = new Scanner(clientSocket.getInputStream());
						buffer = in.nextLine();
						
						if(buffer.equals("Done")) 
						{
						System.out.println(threadName +"is done!");
						num.decrementAndGet();
						break;
						}
						else 
						{
						System.out.println(threadName +" Has returned  "+ buffer);
						sum += Double.parseDouble(buffer);
						}
						
						ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
						if(queuedwork.size() != 0) ///critical
						{
							System.out.println(threadName + " is waiting to aquire packet. ");
							semi.acquire();
							System.out.println(threadName + " is gathering packet.");
							out.writeObject(queuedwork.poll()); 
							semi.release();
							Thread.sleep(TIME);
							if(queuedwork.size() == 5) 
							{
								System.out.println(threadName +"is working on last bit of data ");
							}
						}
						else if(queuedwork.size() == 0) 
						{
							System.out.println(threadName + " has no more packets to work with");
							semi.acquire();
							out.writeObject(null); 
							semi.release();
							Thread.sleep(TIME);
						}
					}
				}
				catch(Exception e) 
				{
					in.close();
					try 
					{
						clientSocket.close();
					} 
					catch (IOException e1)
					{
						e1.printStackTrace();
					}
				}
			}
		}
/**
 * This file accepts a server number and creates a server thread open for conncetions.
 * The  user is then prompts for a file that contains nothing but floating point values seperatated by each line.
 * by the end of the file the user should have very close estimeate of the data given by partial summation. 
 * The partial summation is conducted by mutilple thread to handle large data.
 * The main threads job is to gather data from the file break it in to packets the size of "workAmount" and make sure the queue is not empty untill all data fromr the
 * file is obtained.
 * @param args
 * @throws Exception
 */
	public static void main(String [] args) throws Exception 
	{
		int port = 0;
		ServerSocket sock = null;
		
		queuedwork = new LinkedBlockingQueue<ArrayList<String>>(MAX_WORK_LOAD);

		if(args.length != 1) //getting server port ready
		{
			throw new Exception("Not a right amount of arguments");
		}
		else 
		{
			try 
			{
				port = Integer.parseInt(args[0]);
				if(port <= 1000) 
				{
				throw new Exception("not a useable port number portnumber > 1000");
				}
				sock = new ServerSocket(port);
			}
			catch(Exception e)
			{
				System.out.println("Error with Server creation Error:  "+ e);
				sock.close();
				System.exit(0);
			}
		}
		Scanner fileName = new Scanner(System.in);
		System.out.println("Enter a Floating-point .dat file for the Server to process!");
		try
		{
		File file = new File(fileName.nextLine());
		fileName.close();
		FileReader fr = new FileReader(file);
		BufferedReader input = new BufferedReader(fr);
		DIYAppController op = new DIYAppController(sock);
		System.out.println("\nSetting up data before opening server");
		while (input.ready()) 
		{
			if(queuedwork.size() < MAX_WORK_LOAD) 
			{
				ArrayList<String> packet = new ArrayList<String>();
				for(int i =0; i < workAmount;i++) 
				{
					String line = input.readLine();
					packet.add(line);
				}
				queuedwork.put(packet);
				if(queuedwork.size() == 4) 
				{
					System.out.println("Server is open");
					op.start();
				}
			}
		}
				
		while(true) 
		{
			if(num.get() == 1 && queuedwork.size() == 0) 
			{
				input.close();
				System.out.println("Final amount produced by workers is "+ sum);
				break;
			}
		}
		}
		catch(Exception e)
		{
			System.out.println("error withg reading file");
			e.printStackTrace();
		}
		
		Thread.sleep(TIME);
		System.exit(0);
	}//EOM
}//EOF