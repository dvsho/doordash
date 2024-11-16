package doordash;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerThread extends Thread {
	private PrintWriter pw;
	private boolean isWaiting, moreOrders;
	private List<Order> orders;
	private Lock driverLock;
	private Condition waiting;
	private double currLat, currLong, hqLat, hqLong;
	private long startTime = 0;
	
	public ServerThread(Socket s, double la, double lo) {
		isWaiting = true;
		moreOrders = true;
		orders = Collections.synchronizedList(new ArrayList<>());
		driverLock = new ReentrantLock();
		waiting = driverLock.newCondition();
		currLat = la;
		currLong = lo;
		hqLat = la;
		hqLong = lo;
		try {
			pw = new PrintWriter(s.getOutputStream());
			this.start();
		} catch (IOException e) {
			System.out.println("IOException: " + e.getMessage());
		}
	}

	public boolean getWaiting() {
		return isWaiting;
	}
	
	public void setWaiting(boolean b) {
		isWaiting = b;
	}
	
	public boolean hasMoreOrders() {
		return moreOrders;
	}
	
	public void setMoreOrders(boolean b) {
		moreOrders = b;
	}
	
	public void addOrder(Order order) {
		orders.add(order);
	}
	
	public long getStartTime() {
		return startTime;
	}
	
	public void setStartTime(long st) {
		startTime = st;
	}
	
	public void startDelivery() {
		try {
			driverLock.lock();
			waiting.signal();
		} finally {
			driverLock.unlock();
		}
	}
	
	public double getDistance(double la, double lo) {
		la = Math.toRadians(la);
		lo = Math.toRadians(lo);
		double tempLatitude = Math.toRadians(currLat), tempLongitude = Math.toRadians(currLong);
		double d = 3963.0 * Math.acos(Math.sin(la) * Math.sin(tempLatitude) + Math.cos(la) * Math.cos(tempLatitude) * Math.cos(tempLongitude - lo));
		return Math.round(d * 10) / 10.0;
	}
	
	public void updateLocation(double la, double lo) {
		currLat = la;
		currLong = lo;
	}
	
	public void sendMessage(String message) {
		pw.println(message);
		pw.flush();
	}
	
	public void run() {
		while (moreOrders) {
			while (!orders.isEmpty()) {
				List<Order> trip = new ArrayList<>(), completed = new ArrayList<>();
				synchronized (this) {
					while (!orders.isEmpty()) {
						trip.add(orders.remove(0));
					}
				}
				
				Set<Order> ogTrip = new HashSet<>(trip);
				while (!trip.isEmpty()) {
					Collections.sort(trip, new OrderComp(currLat, currLong));
					Order order = trip.get(0);
					List<Order> destination = new ArrayList<>();
					for (Order o : trip) {
						if (o.getRestaurant().equals(order.getRestaurant())) {
							destination.add(o);
						}
					}
					
					for (Order o : trip) {
						Set<Order> currTrip = new HashSet<>(trip);
						// I got the idea to use sets from ChatGPT, but made the implementation myself.
						if (currTrip.equals(ogTrip))
							sendMessage(o.startingDelivery(startTime));
						else
							sendMessage(o.continuingDelivery(startTime));
					}
					
					try {
						Thread.sleep((long) (getDistance(order.getLatitude(), order.getLongitude()) * 1000));
					} catch (InterruptedException e) {}
					
					for (Order o : destination) {
						sendMessage(o.finishedDelivery(startTime));
						completed.add(o);
						trip.remove(o);
					}
					
					updateLocation(order.getLatitude(), order.getLongitude());
				}
				
				sendMessage(Order.returning(startTime));
				try {
					Thread.sleep((long) (getDistance(hqLat, hqLong) * 1000));
				} catch (InterruptedException e) {}
				sendMessage(Order.returned(startTime, completed));
				updateLocation(hqLat, hqLong);
			}
			
			try {
				driverLock.lock();
				setWaiting(true);				
				waiting.await();
				setWaiting(false);
			} catch (InterruptedException e) {} finally {
				driverLock.unlock();
			}
		}
	}
}

class OrderComp implements Comparator<Order> {
	private double la, lo;
	
	public OrderComp(double la, double lo) {
		this.la = la;
		this.lo = lo;
	}
	
    public int compare(Order order1, Order order2) {
        return Double.compare(order1.getDistance(la, lo), order2.getDistance(la, lo));
    }
}
