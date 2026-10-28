import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface VetService extends Remote {

	List<String> listVets() throws RemoteException;

}
