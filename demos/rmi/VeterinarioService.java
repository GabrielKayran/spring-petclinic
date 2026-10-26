import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// Interface remota: é o contrato que cliente e servidor compartilham.
// Precisa estender Remote, e todo método declara RemoteException porque a rede pode falhar.
public interface VeterinarioService extends Remote {

	List<String> listarVeterinarios() throws RemoteException;

}
