import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Servidor {
    private static final int PORTA = 7777;
    private static int numeroSecreto;
    private static volatile boolean jogoAtivo = true;
    private static final List<AtendimentoCliente> clientes = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        Random random = new Random();
        numeroSecreto = random.nextInt(1001); // 0 a 1000
        System.out.println("=========================================");
        System.out.println("Servidor iniciado! Número secreto: " + numeroSecreto);
        System.out.println("Aguardando jogadores na porta " + PORTA + "...");
        System.out.println("=========================================");

        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + socketCliente.getInetAddress().getHostAddress());

                AtendimentoCliente atendimento = new AtendimentoCliente(socketCliente);
                clientes.add(atendimento);
                new Thread(atendimento).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    public static int getNumeroSecreto() {
        return numeroSecreto;
    }

    public static boolean isJogoAtivo() {
        return jogoAtivo;
    }

    public static synchronized void anunciarVencedor(String ipVencedor) {
        if (!jogoAtivo) return;
        jogoAtivo = false;

        String mensagem = "\n=========================================\n" +
                          "🏆 TEMOS UM VENCEDOR! 🏆\n" +
                          "Vencedor: " + ipVencedor + "\n" +
                          "Número sorteado: " + numeroSecreto + "\n" +
                          "=========================================\n";

        System.out.println(mensagem);

        for (AtendimentoCliente c : clientes) {
            c.enviarMensagem(mensagem);
        }
    }

    // =========================================================
    // CLASSE INTERNA - A Thread que atende cada cliente
    // =========================================================
    static class AtendimentoCliente implements Runnable {
        private Socket socket;
        private BufferedReader entrada;
        private PrintWriter saida;
        private String ipCliente;

        public AtendimentoCliente(Socket socket) {
            this.socket = socket;
            this.ipCliente = socket.getInetAddress().getHostAddress();
            try {
                this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.saida = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.err.println("Erro ao criar streams para " + ipCliente);
            }
        }

        @Override
        public void run() {
            try {
                enviarMensagem("Bem-vindo ao Jogo de Adivinhação! Digite um número entre 0 e 1000:");

                String linha;
                while ((linha = entrada.readLine()) != null) {
                    if (!jogoAtivo) {
                        enviarMensagem("O jogo já foi encerrado por outro jogador.");
                        continue;
                    }

                    try {
                        int palpite = Integer.parseInt(linha.trim());

                        if (palpite < 0 || palpite > 1000) {
                            enviarMensagem("⚠️ Entrada inválida! Digite um número entre 0 e 1000.");
                            continue;
                        }

                        // LÓGICA CONFORME O ENUNCIADO:
                        // MAIOR = o número secreto é maior que o palpite
                        // MENOR = o número secreto é menor que o palpite
                        if (palpite == numeroSecreto) {
                            enviarMensagem("ACERTOU");
                            anunciarVencedor(ipCliente);
                            break;
                        } else if (numeroSecreto > palpite) {
                            enviarMensagem("MAIOR"); // o número secreto é MAIOR que o palpite
                        } else {
                            enviarMensagem("MENOR"); // o número secreto é MENOR que o palpite
                        }

                    } catch (NumberFormatException e) {
                        enviarMensagem("⚠️ Entrada inválida! Digite apenas números inteiros.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Cliente " + ipCliente + " desconectou-se.");
            } finally {
                fecharConexao();
            }
        }

        public synchronized void enviarMensagem(String msg) {
            if (saida != null) {
                saida.println(msg);
            }
        }

        public void fecharConexao() {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) { /* ignora */ }
        }
    }
}