import io.nats.client.Connection;
import io.nats.client.Nats;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

// Implementação do serviço RPC
public class ECommerceServer implements ECommerce {
    private final Map<String, Integer> estoque = new HashMap<>();

    public ECommerceServer() {
        estoque.put("Notebook", 5);
        estoque.put("Celular", 10);
        estoque.put("Fone de Ouvido", 15);
    }

    @Override
    public boolean verificarEstoque(String produto, int quantidade) throws RemoteException {
        Integer qtd = estoque.get(produto);
        System.out.println("[RPC] Verificando estoque de " + produto + "...");
        return qtd != null && qtd >= quantidade;
    }

    @Override
    public String realizarCompra(String produto, int quantidade) throws RemoteException {
        System.out.println("[RPC] Pedido recebido: " + produto + " x" + quantidade);
        if (!verificarEstoque(produto, quantidade)) {
            return "Produto indisponível no estoque.";
        }

        // Atualiza estoque
        estoque.put(produto, estoque.get(produto) - quantidade);
        String msg = "Compra confirmada de " + quantidade + "x " + produto + " às " + LocalTime.now();

        // Publica mensagem no NATS (assíncrono)
        new Thread(() -> publicarMensagemNATS(msg)).start();

        return msg;
    }

    private void publicarMensagemNATS(String mensagem) {
        try {
            Connection nc = Nats.connect("nats://192.168.0.1:4222");
            nc.publish("pedidos.confirmados", mensagem.getBytes());
            System.out.println("[NATS] Mensagem publicada: " + mensagem);
            nc.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Inicializa o servidor RMI
    public static void main(String[] args) {
        try {
            
            System.setProperty("java.rmi.server.hostname", "192.168.0.2");

            ECommerceServer obj = new ECommerceServer();
            ECommerce stub = (ECommerce) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("ECommerceService", stub);

            System.out.println("[SERVIDOR RPC] Pronto e aguardando requisições...");
        } catch (Exception e) {
            System.err.println("Erro no servidor: " + e.toString());
            e.printStackTrace();
        }
    }
}
