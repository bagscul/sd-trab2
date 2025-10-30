import java.rmi.Remote;
import java.rmi.RemoteException;

// Interface remota — define os métodos acessíveis via RPC
public interface ECommerce extends Remote {
    boolean verificarEstoque(String produto, int quantidade) throws RemoteException;
    String realizarCompra(String produto, int quantidade) throws RemoteException;
}
