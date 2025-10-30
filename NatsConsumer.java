import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NatsConsumer {
    public static void main(String[] args) {
        String natsUrl = System.getenv().getOrDefault("NATS_URL", "nats://192.168.0.1:4222");
        ExecutorService pool = Executors.newFixedThreadPool(4);

        try {
            Connection nc = Nats.connect(natsUrl);
            System.out.println("[NATS] Conectado em " + natsUrl + " e aguardando mensagens...");

            Dispatcher d = nc.createDispatcher((msg) -> {
                String conteudo = new String(msg.getData());
                pool.submit(() -> {
                    String worker = Thread.currentThread().getName();
                    System.out.println("[NATS-Worker-" + worker + "] Recebido: " + conteudo);
                    try { Thread.sleep(300 + (int)(Math.random()*700)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    System.out.println("[NATS-Worker-" + worker + "] Processado: " + conteudo);
                });
            });

            d.subscribe("pedidos.confirmados");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[NATS] Fechando consumer e pool...");
                pool.shutdownNow();
                try { nc.close(); } catch (Exception ex) {}
            }));

            while (true) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            pool.shutdownNow();
        }
    }
}
