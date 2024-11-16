package doordash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DavidsTable {
	private Vector<ServerThread> serverThreads;
	private List<Order> orders;
	
	public DavidsTable(int numDrivers, List<Order> o, double la, double lo) {
		orders = o;
		ServerSocket ss;
		try {
			ss = new ServerSocket(3456);
			System.out.println("\nListening on port 3456.\nWaiting for drivers...\n");
			serverThreads = new Vector<ServerThread>();
			for (int i = 1; i <= numDrivers; i++) {
					Socket s = ss.accept();
					System.out.println("Connection from " + s.getInetAddress() + ". Waiting for " + (numDrivers - i) + " more driver(s)...");
					serverThreads.add(new ServerThread(s, la, lo));
					if (numDrivers - i != 0)
						broadcast((numDrivers - i) + " more driver(s) is/are needed before service can begin. Waiting...");
				
			}
			
			Order.setDrivers(serverThreads);
			System.out.println("Starting service.\n");
			broadcast("All drivers have arrived! Starting service.\n");
			startService();
			ss.close();
		} catch (IOException e) {
			if (e.getMessage() != null)
				System.out.println("IOException: " + e.getMessage());
			else
				System.exit(0);
		}
	}
	
	public void startService() throws IOException {
		ExecutorService exe = Executors.newCachedThreadPool();
		long startStart = System.currentTimeMillis();
		for (Order o : orders) {
			exe.execute(o);
		}
		
		exe.shutdown();
		while (!exe.isTerminated()) {
			Thread.yield();
		}
		
		System.out.println("All orders complete!");
		broadcast(Order.finishedAll(startStart));
		for (ServerThread d : serverThreads) {
			d.setMoreOrders(false);
			d.startDelivery();
		}
		
		System.exit(0);
	}
	
	public void broadcast(String message) {
		if (message != null) {
			for(ServerThread threads : serverThreads) {
				threads.sendMessage(message);
			}
		}
	}

	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		List<List<String>> schedule = new ArrayList<>();
		Gson gson = new Gson();
		
		while (schedule.isEmpty()) {
			System.out.print("What is the name of the schedule file? ");
			String f = in.nextLine();
			
			try {
				BufferedReader br = new BufferedReader(new FileReader(f));
				String line;
				while ((line = br.readLine()) != null) {
					schedule.add(new ArrayList<>(Arrays.asList(line.split(","))));
				}
				
				br.close();
			} catch (FileNotFoundException e) {
				System.out.println("The file " + f + " could not be found.\n");
			} catch (IOException e) {
				System.out.println("The file " + f + " could not be properly read.\n");
			}
		}
		
		System.out.println("The schedule file has been properly read.\n");
		System.out.print("What is your latitude? ");
		final double latitude = in.nextDouble();
		System.out.print("What is your longitude? ");
		final double longitude = in.nextDouble();		
		System.out.print("How many drivers will be in service today? ");
		int num = in.nextInt();
		in.close();
		
		System.out.println("\nQuerying Yelp API for restaurants' locations...");
		List<Order> orders = new ArrayList<>();
		for (List<String> s : schedule) {
			String rTerm = s.get(1).replace(" ", "%20").replace("'", "%27");
			// Lines 126-139: Yelp Developer Portal API Reference.
			HttpRequest request = HttpRequest.newBuilder()
				    .uri(URI.create("https://api.yelp.com/v3/businesses/search?latitude=" + latitude + "&longitude=" + longitude + "&term=" + rTerm + "&sort_by=best_match&limit=1"))
				    .header("accept", "application/json")
				    .header("Authorization", "Bearer HCcdDSoCfuUihFxl9gHtzjknadnUebsZHEbTg7NX-eI59X8afJ4Lz8-JVDfRCG5jemq20y1IeNmmZ7EZMX1VnYHq31wYIRrLNauchCSkhttIdlO8EmqcgDoA7U5BZXYx")
				    .method("GET", HttpRequest.BodyPublishers.noBody())
				    .build();
			HttpResponse<String> response = null;
			try {
				response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
			} catch (IOException e) {
				System.out.println("IOException: " + e.getMessage());
			} catch (InterruptedException e) {
				System.out.println("InterruptedException: " + e.getMessage());
			}
			
			String json = response.body();
			BusinessList bl = gson.fromJson(json, BusinessList.class);
			double olat = 0.0, olong = 0.0;
			for (Business b : bl.getBusinesses()) {
				olat = b.getCoordinates().getLatitude();
		        olong = b.getCoordinates().getLongitude();
			}
	        
			orders.add(new Order(Long.parseLong(s.get(0)), s.get(1).trim(), s.get(2).trim(), olat, olong));
		}
		
		System.out.println("Done.");
		DavidsTable dt = new DavidsTable(num, orders, latitude, longitude);
	}
}
