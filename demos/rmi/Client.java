import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

	public static void main(String[] args) throws Exception {
		String host = args.length > 0 ? args[0] : "localhost";
		Registry registry = LocateRegistry.getRegistry(host, 1099);
		VetService service = (VetService) registry.lookup("VetService");
		for (String vet : service.listVets()) {
			System.out.println(" - " + vet);
		}
	}

}
