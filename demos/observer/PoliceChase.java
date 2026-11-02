import java.util.ArrayList;
import java.util.List;

public class PoliceChase {

	interface Observer {

		void update(StolenCar car);

	}

	static class StolenCar {

		private final List<Observer> observers = new ArrayList<>();

		private final String plate;

		private int x;

		private int y;

		StolenCar(String plate) {
			this.plate = plate;
		}

		void attach(Observer observer) {
			this.observers.add(observer);
		}

		void detach(Observer observer) {
			this.observers.remove(observer);
		}

		void move(int dx, int dy) {
			this.x += dx;
			this.y += dy;
			notifyObservers();
		}

		private void notifyObservers() {
			for (Observer observer : this.observers) {
				observer.update(this);
			}
		}

		String getPlate() {
			return this.plate;
		}

		int getX() {
			return this.x;
		}

		int getY() {
			return this.y;
		}

	}

	static class PoliceCar implements Observer {

		private final String name;

		PoliceCar(String name) {
			this.name = name;
		}

		@Override
		public void update(StolenCar car) {
			System.out.printf("%s: carro %s visto em (%d, %d), indo para lá%n", this.name, car.getPlate(), car.getX(),
					car.getY());
		}

	}

	static class Helicopter implements Observer {

		@Override
		public void update(StolenCar car) {
			System.out.printf("Helicóptero: iluminando o carro %s em (%d, %d)%n", car.getPlate(), car.getX(), car.getY());
		}

	}

	public static void main(String[] args) {
		StolenCar car = new StolenCar("ROB-0001");
		PoliceCar one = new PoliceCar("Viatura 1");
		PoliceCar two = new PoliceCar("Viatura 2");

		car.attach(one);
		car.attach(two);
		car.attach(new Helicopter());
		car.move(1, 0);

		System.out.println("-- Viatura 2 sai da escuta do rádio --");
		car.detach(two);
		car.move(0, 1);
	}

}
