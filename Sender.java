import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class Sender {
	public static void main(String args[]) throws Exception {
		String input;		//String input from the file
		String response;	//String response from the server
		byte state = 0;		//current state in sender FSM - 0 or 1
        int numberSent = 0;     //total number of packets sent
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
        	//get the packet, the next word
        	String packet = words[i];
        	//packet length is 6 bytes plus # of chars in packet (+ 1 for \n):
        	int packetLength = 7 + packet.length();
        	//create byte array for the packet:
        	byte[] bytes = new byte[packetLength];
        	//first byte is the sequence number, which alternates with each packet:
        	bytes[0] = (byte)(i % 2);
        	//second byte is the ID - position in the whole message:
        	bytes[1] = (byte)(i + 1);
            //skip over ID of 10 and 13 b/c these are newline and carriage return characters:
            if((bytes[1] == 10) || (bytes[1] == 13)) {
                bytes[1] += 1;
            }

        	//find checksum:
        	int checksum = 0;
        	for(int j = 0; j < packet.length(); j++) {
        		int ansiValue = (int)packet.charAt(j);
        		checksum += ansiValue;
        	}
        	
        	//put checksum in packet:
        	bytes[2] = (byte)(checksum >> 24);
        	bytes[3] = (byte)(checksum >> 16);
        	bytes[4] = (byte)(checksum >> 8);
        	bytes[5] = (byte)checksum;
            //don't allow low byte to be negative:
        	if(bytes[5] < 0) {
        		bytes[5] &= 0x7F;
        	}

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


        //loop to send the packets to network:
        int i = 0;
        while(true) {
        	//send to the network:
        	outToNetwork.write(packets.get(i), 0, packets.get(i).length);
            numberSent++;       //increment number of packets sent
        	//wait for ACK:
        	response = inFromNetwork.readLine();
            //get sequence number of the ACK:
        	byte ACKsequence = (byte)response.charAt(0);
            //display appropriate message:
            System.out.print("Waiting ACK" + state + ", " + numberSent + ", ");
        	if(ACKsequence == 2) {
        		//must have dropped the packet
        		System.out.println("DROP, resend Packet" + state);
        	}
            //if ACK sequence number matches expected sequence number:
            else if(ACKsequence == state) {
                //move to the next sequence number:
                if(state == 0) {
                    state = 1;
                }
                else {
                    state = 0;
                }
                //print the ACK we received:
                System.out.print("ACK" + ACKsequence + ", ");
                i++;    //move to the next packet
                if(i >= packets.size()) {
                    //if we're at the end, print this:
                    System.out.println("no more packets to send");
                    //tell the network we're done:
                    outToNetwork.writeBytes("-1\n");
                    //and exit the loop
                    break;
                }
                //if not at the end, say what we're sending next:
                System.out.println("send Packet" + state);
        	}
            else {
                //wrong sequence number so resend the packet:
        	    System.out.println("ACK" + ACKsequence + ", resend Packet" + state);
        	}
        }
        senderSocket.close();	//disconnect the sender when we break from the loop
	}
}