import java.util.*;
import java.io.*;
import java.net.*;

public class Network {
	private Random r;      //Random variable for deciding whether to pass segment

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
        String receiverMessage;     //message to be received from receiver
        String response;        //response to be sent to sender

        byte[] newline = {'\n'};    //newline byte array for marking end of byte stream

        if(args.length < 1) {
            //requires the user to supply the port number at runtime
            throw new Exception("Please supply the port number");
        }
        portNumber = Integer.parseInt(args[0]);     //get the port number from the arguments

        ServerSocket socket = new ServerSocket(portNumber);	//create socket for the sender
        ServerSocket socket2 = new ServerSocket(portNumber+1);  //another socket for the receiver

        while(true) {
            //socket waits for contact from sender:
         	Socket senderSocket = socket.accept();
            //get IP address of connecting sender:
            String senderIP = senderSocket.getInetAddress().getHostAddress();
            //print message stating connection made:
            System.out.println("get connection from sender: " + senderIP);
            //socket waits for contact for receiver:
            Socket receiverSocket = socket2.accept();
            //get IP address of connecting receiver:
            String receiverIP = receiverSocket.getInetAddress().getHostAddress();
            //print message stating connection made:
            System.out.println("get connection from receiver: " + receiverIP);

            //create input stream attached to sender socket:
            BufferedReader inFromSender = new BufferedReader(new InputStreamReader(senderSocket.getInputStream()));
            //create output stream attached to sender socket:
            DataOutputStream outToSender = new DataOutputStream(senderSocket.getOutputStream());
            //create input stream attached to receiver socket:
            BufferedReader inFromReceiver = new BufferedReader(new InputStreamReader(receiverSocket.getInputStream()));
            //create output stream attached to receiver socket:
            DataOutputStream outToReceiver = new DataOutputStream(receiverSocket.getOutputStream());

            //send initial "Hello" response to confirm connections:
            response = "Hello!";
            outToSender.writeBytes(response + '\n');
            outToReceiver.writeBytes(response + '\n');

            //loop until sender exits:
            while(true) {
                senderMessage = inFromSender.readLine();    //get the packet from the sender
                //if message is -1, then we're done:
                if(senderMessage.equals("-1")) {
                    //so send this to the receiver as well:
                    outToReceiver.writeBytes("-1\n");
                    //then close the sockets:
                    socket.close();
                    socket2.close();
                    //and end the program
                    System.exit(0);
                }
                //convert the String into array of bytes:
                byte[] bytes = new byte[senderMessage.length()];
                for(int i = 0; i < senderMessage.length(); i++) {
                    bytes[i] = (byte)senderMessage.charAt(i);
                }
                //print info about received packet:
                System.out.print("Received: Packet" + bytes[0] + ", " + bytes[1] + ", ");
                //figure out if we should pass, corrupt, or drop:
                double random = n.getRandomValue();
                //25% chance to drop the packet:
                if(random < 0.25) {
                    System.out.println("DROP");
                    byte[] ACKbytes = new byte[3];
                    ACKbytes[0] = 2;    //ACK2 to notify of drop
                    ACKbytes[1] = 0;    //checksum of 0
                    ACKbytes[2] = '\n';     //to mark end of byte stream
                    outToSender.write(ACKbytes, 0, 3);  //send false ACK to sender
                }
                //only forward message to receiver if not a drop:
                else {
                    //50% chance of passing:
                    if(random < 0.75) {
                        System.out.println("PASS");
                    }
                    //25% chance of corrupting:
                    else {
                        System.out.println("CORRUPT");
                        //add 1 to corrupt the checksum field:
                        bytes[5] += 1;
                    }

                    //now forward message to receiver:
                    outToReceiver.write(bytes, 0, bytes.length);
                    //send newline character to mark the end of it
                    outToReceiver.write(newline, 0, 1);
                    //wait for the ACK:
                    receiverMessage = inFromReceiver.readLine();
                    //forward ACK to sender:
                    outToSender.writeBytes(receiverMessage + '\n');
                }
            }
        }
	}
}