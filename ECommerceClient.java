import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ECommerceClient {
    public static void main(String[] args) {
        try {
            String rmiHost = (args.length > 0) ? args[0] : "192.168.0.2";
            int threads = (args.length > 1) ? Integer.parseInt(args[1]) : 3;
            int duracao = (args.length > 2) ? Integer.parseInt(args[2]) : 20;

            Registry registry = LocateRegistry.getRegistry(rmiHost, 1099);
            ECommerce stub = (ECommerce) registry.lookup("ECommerceService");

            String[] produtos = {"Notebook", "Mouse", "Teclado", "Monitor", "Fone"};
            Random rnd = new Random();

            System.out.println("[CLIENTE] Iniciando loop de compras com " + threads + " threads por " + duracao + "s...");

            ExecutorService pool = Executors.newFixedThreadPool(threads);

            long end = System.currentTimeMillis() + duracao * 1000L;

            for (int i = 0; i < threads; i++) {
                final int id = i;
                pool.submit(() -> {
                    while (System.currentTimeMillis() < end) {
                        String produto = produtos[rnd.nextInt(produtos.length)];
                        int qtd = 1 + rnd.nextInt(3); // 1 a 3 unidades
                        try {
                            System.out.println("[CLIENTE-" + id + "] Tentando comprar " + qtd + "x " + produto);
                            boolean disponivel = stub.verificarEstoque(produto, qtd);
                            if (disponivel) {
                                String resposta = stub.realizarCompra(produto, qtd);
                                System.out.println("[CLIENTE-" + id + "] Resultado: " + resposta);
                            } else {
                                System.out.println("[CLIENTE-" + id + "] Estoque insuficiente para " + produto);
                            }
                        } catch (Exception e) {
                            System.err.println("[CLIENTE-" + id + "] Erro: " + e);
                            e.printStackTrace();
                        }
                        // Pausa curta entre tentativas para tornar o demo dinâmico
                        try { Thread.sleep(500 + rnd.nextInt(800)); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                    }
                });
            }

            pool.shutdown();
            while (!pool.isTerminated()) {
                Thread.sleep(200);
            }
            System.out.println("[CLIENTE] Loop finalizado.");
        } catch (Exception e) {
            System.err.println("Erro no cliente: " + e.toString());
            e.printStackTrace();
        }
    }
}
