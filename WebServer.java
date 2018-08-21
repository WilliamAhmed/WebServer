import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

public class WebServer
{
	public static void main(String[] args) throws Exception
	{
		if(args.length != 1)
		{
			System.err.println("You must supply a port number");
			System.exit(1);
		}
		
		int port = Integer.parseInt(args[0]);
		ServerSocket serverSock = new ServerSocket(port);

		while(true)
	        {
		    Socket sock = serverSock.accept();
		    Connection conThread = new Connection(sock);
			conThread.start();
	        }
		
	}
}

class Connection extends Thread
{
    Socket listenSock = new Socket();

	public Connection(Socket con)
	{
	    listenSock = con;
	}
	
	public void run()
	{
		try
		{
			while(true)
			{
				
				Scanner scanin = new Scanner(listenSock.getInputStream());
				String line = null;
				int nLines = 0;			
				String[] linesFromClient = new String[32];
				
				//Read request Lines into Array
				while(true)
				{
					line = scanin.nextLine();
					if(line.length() == 0) break;
						linesFromClient[nLines] = line;
						nLines++;	
				}
				
				//Print Request Lines from Array
				//for(int i = 0; i < linesFromClient.length; i++)
				//{
				//	if(linesFromClient[i] != null)
				//	{
				//		System.out.println("[Line " + i + "]: " + linesFromClient[i]);
				//	}
				//}
				
				//Date to supply with every response
				Date date = new Date();
				SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
				
				//Extract 'Command' and 'Resource' information from request line 0
				String command, resource;
				Scanner reqScan = new Scanner(linesFromClient[0]);
				command = reqScan.next();
				resource = reqScan.next();
				
				//Create FileOutputStream to write to a file
				FileOutputStream fOut = new FileOutputStream("Server_Log.txt", true);
				PrintStream outLog = new PrintStream(fOut);
				outLog.print("[" + dateFormat.format(date) + "] - " + "'" + command + " " + resource + "' " + listenSock.getInetAddress() + ", ");

				
				//Output stream to return information to the client
				OutputStream outs = listenSock.getOutputStream();
				String reply = "";
				
				//Check the resource information. If the request does not start with '/' then return 400
				//else continue
				if(resource.charAt(0) != '/')
				{
					reply ="HTTP/1.0 400 Bad Request\r\n" +
							"Date: " + dateFormat.format(date) + "\r\n" +
							"Connection: close\r\n";
							
					outs.write(reply.getBytes());
					
					//output log file
					outLog.print("400 Bad Request");
				}
				
				//Check Command Operation. If PUT, TRACE or DELETE then disallow and return 405 response
				//If GET command then proceed with GET operations. (Finding file type, returning file found/not found, returning file)
				//If HEAD command then proceed with GET but check before returning content body as for head this is not needed
				//If none of the above then return 501 response
				if(command.equals("PUT") || command.equals("DELETE") || command.equals("TRACE"))
				{
					//return 405
					reply="HTTP/1,0 405 Request Not Allowed\r\n" +
							"Allow: GET, HEAD\r\n" +
							"Date: " + dateFormat.format(date) + "\r\n" +
							"Connection: close\r\n";
							
					outs.write(reply.getBytes());
					
					//output log file
					outLog.print("405 Request Not Allowed");
				}
				else if(command.equals("GET") || command.equals("HEAD"))
				{
					//Create the File Name Directory
					String fileName = "www" + resource;
					
					//Create new File to manipulate the required Resource File
					File resourceFile = new File(fileName);


					//if resource file is a directory, look for index.htm in the directory and display
					if(resourceFile.isDirectory())
					{
						File testFile = new File(resourceFile.getPath() + "/index.html");
						if(testFile.exists())
						{
							resourceFile = testFile;
						}
						else
						{
							testFile = new File(resourceFile.getPath() + "/index.htm");
							if(testFile.exists())
							{
								resourceFile = testFile;
							}
						}
					}
					
					//return file
					//Get File Type
					String fileExtension = "";
					String fileType = "";
					
					int index = resource.lastIndexOf('.');
					fileExtension = resource.substring(index + 1);
					
					if(fileExtension.equals("html") || fileExtension.equals("htm"))
					{
					   fileType = "text/html";
					}
					else if(fileExtension.equals("txt"))
					{
					   fileType = "text/plain";
					}
					else if(fileExtension.equals(".jpg") || fileExtension.equals(".jpeg"))
					{
						fileType = "image/jpeg";
					}	
					
					//Check if file Exists
					if(!resourceFile.exists())
					{
						//404 Not Found Reply
						reply = "HTTP/1.0 404 Not Found\r\n" +
								"Connection: close\r\n" +
								"Content-Type: " + fileType + "\r\n" +
								"Date: " + dateFormat.format(date) + "\r\n" +
								"\r\n" +
								"<h1>404 Error, File not Found</h1>\r\n";
													
						//Write 404 Reply back to Client
						outs.write(reply.getBytes());
						
						//output log file
						outLog.print("404 Not Found");
					}
					else
					{
					
						//Only reutrn file if command was GET Command. This succesfully handles the HEAD command whereby content is not returned
						if(command.equals("GET"))
						{
							//200 OK File Found Reply
							reply = "HTTP/1.0 200 OK\r\n" +
									"Connection: close\r\n" +
									"Content-Length: " + resourceFile.length() + "\r\n" +
									"Content-Type: " + fileType + "\r\n" +
									"Date: " + dateFormat.format(date) + "\r\n" +
									"\r\n";
									
							outs.write(reply.getBytes());
							
							//output log file
							outLog.print("200 OK");
							
							//Reads file using a FileInputStream
							InputStream fileInStream = new FileInputStream(resourceFile);
							
							byte[] xfer = new byte[128];
							
							//Loop through the file and output the contents to the output stream
							while(true)
							{
								int len = fileInStream.read(xfer,0,128);
								if(len <=0) break;
									outs.write(xfer,0,len);
							}
							
							//Close the FileOutputStream
							outs.close();
						}
					}
				}
				else //Command unknown, return 501 response line
				{
					//return 501
					reply=	"HTTP/1.0 501 Command Unknown\r\n" +
							"Date: " + dateFormat.format(date) + "\r\n" +
							"Connection: close\r\n";
						
					outs.write(reply.getBytes());
					
					//output log file
					outLog.print("501 Command Unknown");
				}

				outLog.print("\r\n");
				outLog.close();
				outs.close();
				listenSock.close();
			}
		}
		catch(IOException e)
		{
			System.err.println("Error: error reading socket");
		}
	}
}
