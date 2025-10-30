import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import java.io.IOException;

public class NatsConsumer {
    public static void main(String[] args) {
        try {
            Connection nc = Nats.connect("nats://192.168.0.1:4222");
            System.out.println("[NATS] Conectado e aguardando mensagens...");

            Dispatcher d = nc.createDispatcher((msg) -> {
                String conteudo = new String(msg.getData());
                System.out.println("[NATS] Nova mensagem recebida: " + conteudo);
            });

            d.subscribe("pedidos.confirmados");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
