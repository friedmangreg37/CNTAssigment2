import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class Sender {
	public static void main(String args[]) throws Exception {
		String input;		//String input from the file
		String response;	//String response from the server
		int answer;		//integer answer to the command
		byte sequenceNumber = 0;		//current state in sender FSM - 0 or 1
		ArrayList<byte[]> packets = new ArrayList<byte[]>();	//array list to hold all packets

		//command line inputs:
		String url;		//the server url
		int portNumber;		//the port number
		String filename;	//name of the file to read from

		if(args.length < 3) {
			//requires user the supply url and port number at runtime
			throw new Exception("Please supply IP address, port number, and file name");
		}
		url = args[0];		//get the url from the first argument supplied
		portNumber = Integer.parseInt(args[1]);	//get the port number from the second argument
		filename = args[2];


		//input stream from file:
		BufferedReader inFromFile = new BufferedReader(new FileReader(filename));
		//create sender socket and connect to server:
		Socket senderSocket = new Socket(url, portNumber);
		//create output stream attacked to socket:
		DataOutputStream outToNetwork = new DataOutputStream(senderSocket.getOutputStream());
		//create input stream attacked to socket:
		BufferedReader inFromNetwork = new BufferedReader(new InputStreamReader(senderSocket.getInputStream()));


		response = inFromNetwork.readLine(); 	//read line from server (should be "Hello!")
		System.out.println("receive: " + response);		//print the message


		//read the whole message from the file:
		input = inFromFile.readLine();
		//get an array of the words read from file:
        String words[] = input.split(" ");


        //for loop to split message into packets:
        for(int i = 0; i < words.length; i++) {
        	System.out.println("Gonna send: " + words[i]);
        	//get the packet, the next word
        	String packet = words[i];
        	//packet length is 6 bytes plus # of chars in packet (+ 1 for \n):
        	int packetLength = 7 + packet.length();
        	//create byte array for the packet:
        	byte[] bytes = new byte[packetLength];
        	//first byte is the sequence number:
        	bytes[0] = sequenceNumber;
        	//second byte is the ID - position in the whole message:
        	bytes[1] = (byte)(i + 1);

        	//find checksum:
        	int checksum = 0;
        	for(int j = 0; j < packet.length(); j++) {
        		int ansiValue = (int)packet.charAt(j);
        		checksum += ansiValue;
        	}
        	
        	//put checksum in packet:
        	bytes[2] = (byte)(checksum >>> 24);
        	bytes[3] = (byte)(checksum >>> 16);
        	bytes[4] = (byte)(checksum >>> 8);
        	bytes[5] = (byte)checksum;

        	//add in the actual packet content:
        	for(int j = 0; j < packet.length(); j++) {
        		bytes[6+j] = (byte)packet.charAt(j);
        	}
        	bytes[packetLength-1] = '\n';	//newline to tell network packet is done

        	//append this packet to the list of packets:
        	packets.add(bytes);

        	if((packet.charAt(packet.length()-1) == '.')) {
        		//found period so we're done with the message
        		break;
        	}
        }


        //for loop to send the packets to network:
        for(int i = 0; i < packets.size(); i++) {
        	System.out.println(packets.get(i));

        	//send to the network:
        	outToNetwork.write(packets.get(i), 0, packets.get(i).length);
        	response = inFromNetwork.readLine();
        	System.out.println(response);
        }


        outToNetwork.writeBytes("terminate\n");		//tell Network we're done
        senderSocket.close();	//disconnect the sender when we break from the loop
	
		/*
		//loop until user says to quit:
		while( (input = inFromFile.readLine()) != null) {

			outToNetwork.writeBytes(input + '\n');	//send this input to the server
			response = inFromNetwork.readLine();		//get the response from the server
			answer = Integer.parseInt(response);	//convert to an int
			if(answer >= 0) {		//if >= 0 then not an error so just print result
				System.out.println("receive: " + answer);
			}
			//error codes:
			else if(answer == -5) {
				//error code for exit, so print this, then close the socket:
				System.out.println("receive: exit");
				break;
			}
		}*/
	}
}