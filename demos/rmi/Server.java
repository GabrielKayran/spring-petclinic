import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class Server implements VetService {

	// Static references keep the registry and the exported object from being garbage collected
	private static Registry registry;

	private static Server service;

	public List<String> listVets() {
		System.out.println("Chamada remota recebida na thread " + Thread.currentThread().getName());
		return List.of("James Carter", "Helen Leary", "Linda Douglas", "Rafael Ortega", "Henry Stevens");
	}

	public static void main(String[] args) throws Exception {
		service = new Server();
		VetService stub = (VetService) UnicastRemoteObject.exportObject(service, 0);
		registry = LocateRegistry.createRegistry(1099);
		registry.rebind("VetService", stub);
		System.out.println("Servidor RMI pronto na porta 1099. Ctrl+C para encerrar.");
	}

}
