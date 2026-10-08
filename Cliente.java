import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    private static final String HOST = "localhost"; // Troque para o IP do servidor se for remoto
    private static final int PORTA = 7777;

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORTA);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);
             Scanner teclado = new Scanner(System.in)) {

            System.out.println("Conectado ao servidor de Adivinhação!");

            // Thread que escuta as mensagens do servidor
            Thread threadEscuta = new Thread(() -> {
                try {
                    String msgServidor;
                    while ((msgServidor = entrada.readLine()) != null) {
                        System.out.println("\nServidor: " + msgServidor);

                        if (msgServidor.contains("TEMOS UM VENCEDOR") ||
                            msgServidor.contains("já foi encerrado")) {
                            Thread.sleep(5000);
                            System.exit(0);
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    System.out.println("Conexão com o servidor encerrada.");
                }
            });
            threadEscuta.start();

            // Loop principal - envio de palpites
            while (true) {
                System.out.print("Digite seu palpite: ");
                if (teclado.hasNextLine()) {
                    String palpite = teclado.nextLine();
                    saida.println(palpite);
                }
            }

        } catch (IOException e) {
            System.err.println("Não foi possível conectar ao servidor.");
        }
    }
}