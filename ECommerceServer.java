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
import java.util.Random;

public class ECommerceServer implements ECommerce {
    private final Map<String, Integer> estoque = new HashMap<>();
    private final Random rnd = new Random();

    public ECommerceServer() {
        // Estoque inicial de exemplo
        estoque.put("Notebook", 5);
        estoque.put("Mouse", 20);
        estoque.put("Teclado", 15);
        estoque.put("Monitor", 6);
        estoque.put("Fone", 10);
    }

    @Override
    public synchronized boolean verificarEstoque(String produto, int quantidade) throws RemoteException {
        Integer q = estoque.get(produto);
        return q != null && q >= quantidade;
    }

    @Override
    public String realizarCompra(String produto, int quantidade) throws RemoteException {
        // Simula latência/execução lenta (entre 800ms e 2200ms)
        int sleepMs = 800 + rnd.nextInt(1400);
        try {
            System.out.println("[SERVIDOR] Processando compra de " + quantidade + "x " + produto +
                    " (demora simulada " + sleepMs + "ms) - thread: " + Thread.currentThread().getName());
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Erro: processamento interrompido.";
        }

        synchronized (this) {
            if (!verificarEstoque(produto, quantidade)) {
                return "Produto indisponível no estoque.";
            }
            // Atualiza estoque
            estoque.put(produto, estoque.get(produto) - quantidade);
        }

        String msg = "Compra confirmada de " + quantidade + "x " + produto + " às " + LocalTime.now()
                + " (thread: " + Thread.currentThread().getName() + ")";

        // Publica mensagem no NATS de forma assíncrona (para não bloquear retorno)
        // publicacao com retry interna
        new Thread(() -> publicarMensagemNATSComRetry(msg)).start();

        return msg;
    }

    private void publicarMensagemNATSComRetry(String mensagem) {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://192.168.0.1:4222");
        int maxAttempts = 3;
        int attempt = 0;
        while (attempt < maxAttempts) {
            attempt++;
            try (Connection nc = Nats.connect(natsUrl)) {
                nc.publish("pedidos.confirmados", mensagem.getBytes());
                System.out.println("[NATS] Mensagem publicada: " + mensagem + " (attempt " + attempt + ")");
                return;
            } catch (IOException | InterruptedException e) {
                System.err.println("[NATS] Falha ao publicar (attempt " + attempt + "): " + e.getMessage());
                if (attempt >= maxAttempts) {
                    System.err.println("[NATS] Excedeu tentativas ao publicar: " + mensagem);
                } else {
                    // backoff simples
                    try {
                        Thread.sleep(300 * attempt);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    // Inicializa o servidor RMI
    public static void main(String[] args) {
        try {
            String rmiHost = (args.length > 0) ? args[0] : "192.168.0.2";
            String natsUrl = (args.length > 1) ? args[1] : System.getenv().getOrDefault("NATS_URL", "nats://192.168.0.1:4222");

            System.setProperty("java.rmi.server.hostname", rmiHost);
            System.out.println("[SERVIDOR] RMI hostname = " + rmiHost + " | NATS = " + natsUrl);

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
