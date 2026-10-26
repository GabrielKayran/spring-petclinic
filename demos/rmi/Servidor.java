import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

// Implementação real do serviço. Ela roda só no servidor.
public class Servidor implements VeterinarioService {

	// Referências estáticas impedem que o garbage collector remova o registry e o objeto
	private static Registry registry;

	private static Servidor servico;

	public List<String> listarVeterinarios() {
		System.out.println("Chamada remota recebida na thread " + Thread.currentThread().getName());
		return List.of("James Carter", "Helen Leary", "Linda Douglas", "Rafael Ortega", "Henry Stevens");
	}

	public static void main(String[] args) throws Exception {
		servico = new Servidor();
		// exportObject publica o objeto na rede e devolve o stub, o "procurador" que o cliente vai usar
		VeterinarioService stub = (VeterinarioService) UnicastRemoteObject.exportObject(servico, 0);
		// O registry funciona como uma lista telefônica: associa um nome a um stub
		registry = LocateRegistry.createRegistry(1099);
		registry.rebind("VeterinarioService", stub);
		System.out.println("Servidor RMI pronto na porta 1099. Ctrl+C para encerrar.");
	}

}
