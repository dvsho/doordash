package doordash;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.*;

public class Order extends Thread {
	private long time;
	private String restaurant;
	private String item;
	private static Vector<ServerThread> drivers;
	private Lock orderLock;
	private Condition beingDelivered;
	private double lat, lon;
	
	public Order(long t, String r, String i, double la, double lo) {
		time = t;
		restaurant = r;
		item = i;
		orderLock = new ReentrantLock();
		beingDelivered = orderLock.newCondition();
		lat = la;
		lon = lo;
	}
	
	public long getTime() {
		return time;
	}
	
	public String getRestaurant() {
		return restaurant;
	}
	
	public String getItem() {
		return item;
	}
	
	public double getLatitude() {
		return lat;
	}
	
	public double getLongitude() {
		return lon;
	}
	
	public static void setDrivers(Vector<ServerThread> d) {
		drivers = d;
	}
	
	public static String getElapsedTime(long st) {
		long elapsed = System.currentTimeMillis() - st;
		long m = elapsed / 60000, s = (elapsed % 60000) / 1000, ms = elapsed % 1000;
		return "[00:" + String.format("%02d", m) + ":" + String.format("%02d", s) + "." + String.format("%03d", ms) + "] ";
	}
	
	public double getDistance(double la, double lo) {
		la = Math.toRadians(la);
		lo = Math.toRadians(lo);
		double tempLatitude = Math.toRadians(lat), tempLongitude = Math.toRadians(lon);
		double d = 3963.0 * Math.acos(Math.sin(la) * Math.sin(tempLatitude) + Math.cos(la) * Math.cos(tempLatitude) * Math.cos(tempLongitude - lo));
		return Math.round(d * 10) / 10.0;
	}
	
	public String startingDelivery(long start) {
		return getElapsedTime(start) + "Starting delivery of " + item + " to " + restaurant + ".";
	}
	
	public String continuingDelivery(long start) {
		return getElapsedTime(start) + "Continuing delivery to " + restaurant + ".";
	}
	
	public String finishedDelivery(long start) {	
		return getElapsedTime(start) + "Finished delivery of " + item + " to " + restaurant + ".";
	}
	
	public static String returning(long start) {
		return getElapsedTime(start) + "Finished all deliveries, returning to HQ.";
	}
	
	public static String returned(long start, List<Order> completed) {	
		for (Order o : completed) {
			try {
				o.orderLock.lock();
				o.beingDelivered.signal();
			} finally {
				o.orderLock.unlock();
			}
		}
		
		return getElapsedTime(start) + "Returned to HQ.";
	}
	
	public static String finishedAll(long start) {
		return getElapsedTime(start) + "Broadcast: All orders complete!\n";
	}
	
	public void run() {
		try {
			Thread.sleep(time * 1000);
		} catch (InterruptedException e) {}
		
		boolean assigned = false;
		while (!assigned) {
		    for (ServerThread d : drivers) {
                if (d.getWaiting()) {
                	if (d.getStartTime() == 0)
                		d.setStartTime(System.currentTimeMillis());
                	d.addOrder(this);
                	try {
						Thread.sleep(10);
					} catch (InterruptedException e) {}
                    d.startDelivery();
                    d.setWaiting(false);
                    assigned = true;
                    break;
                }		        
		    }
		}
		
		try {
			orderLock.lock();
			beingDelivered.await();
		} catch (InterruptedException e) {} finally {
			orderLock.unlock();
		}
		
	}
}
