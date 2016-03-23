import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class Receiver {
	public static void main(String args[]) throws Exception {
		String fullMessage;		//String to hold the entire message and display once done
		String networkMessage;	//String input from the network
		int state = 0;		//current state in sender FSM - 0 or 1
		ArrayList<byte[]> packets = new ArrayList<byte[]>();	//array list to hold all packets

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


		//create receiver socket and connect to server:
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
			//split into individual bytes:
			byte[] bytes = new byte[networkMessage.length()];
			for(int i = 0; i < networkMessage.length(); i++) {
                bytes[i] = (byte)networkMessage.charAt(i);
            }
            System.out.println("Received from network: " + bytes[1]);

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
        	System.out.println("Checksum: " + checksum);
        	if(checksum != calculatedChecksum) {
        		System.out.println("\tCrap! It's corrupted! " + calculatedChecksum);
        	}
            if(bytes[bytes.length-1] == '.') {
				System.out.println("We're done!");
				outToNetwork.writeBytes("terminate\n");		//tell Network we're done
		        break;
			}
            byte[] ACKbytes = new byte[3];
            ACKbytes[0] = 0;
            ACKbytes[1] = 0;
			ACKbytes[2] = '\n';
			outToNetwork.write(ACKbytes, 0, 3);
		}
		receiverSocket.close();		//disconnect the receiver when we break from the loop
	}
}