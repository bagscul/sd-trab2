import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ECommerce extends Remote {
    boolean verificarEstoque(String produto, int quantidade) throws RemoteException;
    String realizarCompra(String produto, int quantidade) throws RemoteException;
}
