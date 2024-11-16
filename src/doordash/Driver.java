package doordash;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Driver extends Thread {
	private BufferedReader br;
	private PrintWriter pw;
	
	public Driver(String hostname, int port) {	
		try {
			Socket s = new Socket(hostname, port);
			System.out.println("Connected to " + hostname + " @ " + port + ".\n");
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			pw = new PrintWriter(s.getOutputStream());
			this.start();
			while (true) {
				pw.flush();
			}
		} catch (IOException e) {
			System.out.println("Connection error: " + e.getMessage());
		}
	}
	
	public void run() {
		try {
			String line;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException e) {
			System.out.println("IOException: " + e.getMessage());
		} finally {
			System.exit(0);
		}
	}
	
	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		System.out.print("Welcome to DavidsTable!\nEnter the server hostname: ");
		String host = in.next();
		System.out.print("Enter the server port: ");
		int port = in.nextInt();
		in.close();
		Driver d = new Driver(host, port);
	}
}
