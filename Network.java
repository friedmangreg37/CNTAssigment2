import java.util.*;
import java.io.*;
import java.net.*;

public class Network {
	private Random r;

	public Network() {
		r = new Random();
	}

	//function to get random double value between 0 and 1
	private double getRandomValue() {
		return r.nextDouble();
	}

	public static void main(String args[]) throws Exception {
		Network n = new Network();		//Network object for random number later
		
		int portNumber;         //the port number

		String senderMessage;     //message to be received from sender
        String response;        //response to be sent to sender

        if(args.length < 1) {
            //requires the user to supply the port number at runtime
            throw new Exception("Please supply the port number");
        }
        portNumber = Integer.parseInt(args[0]);     //get the port number from the arguments

        ServerSocket socket = new ServerSocket(portNumber);	//create socket at given port number

        while(true) {
            //socket waits for contact from sender:
         	Socket senderSocket = socket.accept();
            //get IP address of connecting sender:
            String senderIP = senderSocket.getInetAddress().getHostAddress();
            //print message stating connection made:
            System.out.println("get connection from " + senderIP);
            //create input stream attached to socket:
            BufferedReader inFromSender = new BufferedReader(new InputStreamReader(senderSocket.getInputStream()));
            //create output stream attached to socket:
            DataOutputStream outToSender = new DataOutputStream(senderSocket.getOutputStream());

            //send initial "Hello" response to confirm connection:
            response = "Hello!";
            outToSender.writeBytes(response + '\n');

            //loop until sender exits:
            while(true) {
                senderMessage = inFromSender.readLine();    //get the packet from the sender

                double random = n.getRandomValue();
                System.out.println(random);
                if(random < 0.5) {
                    System.out.println("PASS");
                }else if(random < 0.75) {
                    System.out.println("CORRUPT");
                }else {
                    System.out.println("DROP");
                }
                
                System.out.println("get: " + senderMessage);

                if(senderMessage.equals("terminate")) {   //sender is done
                    //so close the socket
                    socket.close();     //close the server socket
                    System.exit(0);     //end all processes
                }
                else {
	                response = "1";
	                outToSender.writeBytes(response + '\n');
	            }

                //get an array of the words send from sender:
                String words[] = senderMessage.split(" ");
                
            }
        }
	}
}