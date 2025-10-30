import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ECommerceClient {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("<IP_SERVIDOR_RPC>", 1099);
            ECommerce stub = (ECommerce) registry.lookup("ECommerceService");

            String produto = "Notebook";
            int qtd = 2;

            System.out.println("[CLIENTE] Verificando disponibilidade de " + produto + "...");
            boolean disponivel = stub.verificarEstoque(produto, qtd);

            if (disponivel) {
                System.out.println("[CLIENTE] Produto disponível! Realizando compra...");
                String resposta = stub.realizarCompra(produto, qtd);
                System.out.println("[CLIENTE] " + resposta);
            } else {
                System.out.println("[CLIENTE] Estoque insuficiente para " + produto + ".");
            }

        } catch (Exception e) {
            System.err.println("Erro no cliente: " + e.toString());
            e.printStackTrace();
        }
    }
}
