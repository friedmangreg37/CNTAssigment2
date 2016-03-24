import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class Receiver {
	public static void main(String args[]) throws Exception {
		StringBuilder fullMessage = new StringBuilder();		//String to hold the entire message and display once done
		String networkMessage;	//String input from the network
		byte state = 0;		//current state in sender FSM - 0 or 1
		int numberPackets = 0;	//counter for number of packets received

		//command line inputs:
		String url;		//the server url
		int portNumber;		//the port number
		String filename;	//name of the file to read from

		if(args.length < 2) {
			//requires user the supply url and port number at runtime
			throw new Exception("Please supply IP address and port number");
		}
		url = args[0];		//get the url from the first argument supplied
		portNumber = Integer.parseInt(args[1]);	//get the port number from the second argument
		portNumber += 1;	//change port number so not the same as sender one


		//create receiver socket and connect to network:
		Socket receiverSocket = new Socket(url, portNumber);
		//create output stream attacked to socket:
		DataOutputStream outToNetwork = new DataOutputStream(receiverSocket.getOutputStream());
		//create input stream attacked to socket:
		BufferedReader inFromNetwork = new BufferedReader(new InputStreamReader(receiverSocket.getInputStream()));


		networkMessage = inFromNetwork.readLine(); 	//read line from server (should be "Hello!")
		System.out.println("receive: " + networkMessage);		//print the message


		//loop for receiving packets and answering:
		while(true) {
			networkMessage = inFromNetwork.readLine();	//get packet from network
			//if message is -1 then we're done:
			if(networkMessage.equals("-1")) {
				//so break out of the loop:
				break;
			}
			numberPackets++;	//increment number of packets received

			//split into individual bytes:
			byte[] bytes = new byte[networkMessage.length()];
			for(int i = 0; i < networkMessage.length(); i++) {
                bytes[i] = (byte)networkMessage.charAt(i);
            }

            //create array for the ACK:
			byte[] ACKbytes = new byte[3];
            ACKbytes[0] = bytes[0];	//sequence number of ACK matches that of packet received
            ACKbytes[1] = 0;		//ACK checksum
            ACKbytes[2] = '\n';		//newline to mark end of ACK packet

            //get the checksum field of packet:
           	byte[] checksumBytes = new byte[4];
           	for(int i = 0; i < 4; i++) {
           		checksumBytes[i] = bytes[i+2];
           	}
           	int checksum = java.nio.ByteBuffer.wrap(checksumBytes).getInt();
           	//calculate what checksum should be:
        	int calculatedChecksum = 0;
        	for(int j = 6; j < bytes.length; j++) {
        		int ansiValue = (int)bytes[j];
        		calculatedChecksum += ansiValue;
        	}
        	//don't let low byte be negative, like we did with sender:
        	byte lastByte = (byte)calculatedChecksum;
        	if(lastByte < 0) {
        		calculatedChecksum &= 0xFFFFFF7F;
        	}

        	//print message about current state:
        	System.out.print("Waiting " + state + ", " + numberPackets + ", ");
        	System.out.print(bytes[0] + " " + bytes[1] + " " + checksum + " ");

        	//if the packet was corrupted:
        	if(checksum != calculatedChecksum) {
        		//then invert sequence number of the ACK packet
        		if(ACKbytes[0] == 0) {
        			ACKbytes[0] = 1;
        		}
        		else {
        			ACKbytes[0] = 0;
        		}
        	}
        	//if not corrupted:
        	else {
        		//extract the data and add to the full message:
        		for(int i = 6; i < bytes.length; i++) {
	        		fullMessage.append((char)bytes[i]);	
	        	}
	        	fullMessage.append(' ');	//add space in between each packet
        		//move to the next state:
        		if(state == 0) {
        			state = 1;
        		}
        		else {
        			state = 0;
        		}
        	}

        	//print the packet:
        	for(int i = 6; i < bytes.length; i++) {
        		System.out.print((char)bytes[i]);
        	}
        	//print the ACK we're sending:
        	System.out.println(", ACK" + ACKbytes[0]);
			//send ACK back to the network:
			outToNetwork.write(ACKbytes, 0, 3);
		}
		System.out.println(fullMessage.toString());		//print the full message
		receiverSocket.close();		//disconnect the receiver when we break from the loop
	}
}