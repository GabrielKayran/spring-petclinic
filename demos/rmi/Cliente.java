import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Cliente {

	public static void main(String[] args) throws Exception {
		String host = args.length > 0 ? args[0] : "localhost";
		// Procura o serviço pelo nome no registry do servidor
		Registry registry = LocateRegistry.getRegistry(host, 1099);
		VeterinarioService servico = (VeterinarioService) registry.lookup("VeterinarioService");
		// Parece uma chamada local, mas o stub serializa a chamada e a envia pela rede
		for (String veterinario : servico.listarVeterinarios()) {
			System.out.println(" - " + veterinario);
		}
	}

}
