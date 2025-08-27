package com.example;

import java.io.*;
import java.net.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ProcessoCliente implements Runnable {

    private final String clienteId;
    private final String[] possiveisCoordenadores;

    // Agenda execuções futuras para novas tentativas
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ProcessoCliente(int id, String... possiveisCoordenadores) {
        this.clienteId = "Cliente-" + id;
        this.possiveisCoordenadores = possiveisCoordenadores;
    }

    @Override
    public void run() {
        tentarConsumir();
    }

    private void tentarConsumir() {
        try {
            // escolhe coordenador aleatório
            int portaCoordenador = Integer.parseInt(
                possiveisCoordenadores[ThreadLocalRandom.current().nextInt(possiveisCoordenadores.length)]
            );

            System.out.println(clienteId + ": Tentando conectar ao coordenador na porta " + portaCoordenador);
            try (Socket socket = new Socket("localhost", portaCoordenador);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                System.out.println(clienteId + ": Conectado. Enviando solicitação...");
                out.println("SOLICITAR");

                String resposta = in.readLine();
                if ("CONCEDER".equals(resposta)) {
                    secaoCritica(); // recurso de 5–15s
                    out.println("LIBERAR");
                    System.out.println(clienteId + ": Seção crítica finalizada e recurso liberado.");
                }
            }
        } catch (IOException e) {
            // Coordenador pode ter morrido → tenta de novo em 2s
            System.err.println(clienteId + ": Coordenador falhou ou conexão perdida. Tentando novamente em 2s...");
            scheduler.schedule(this::tentarConsumir, 2, TimeUnit.SECONDS);
            return;
        }

        // agenda nova tentativa (10–25s)
        long tempoEspera = ThreadLocalRandom.current().nextLong(10000, 25001);
        System.out.println(clienteId + ": Aguardando " + (tempoEspera / 1000) + "s para próxima solicitação.");
        scheduler.schedule(this::tentarConsumir, tempoEspera, TimeUnit.MILLISECONDS);
    }

    private void secaoCritica() {
        try {
            long duracaoMs = ThreadLocalRandom.current().nextLong(5000, 15001);
            System.out.println(clienteId + ": Acesso concedido. Usando recurso por " + (duracaoMs / 1000) + "s.");
            Thread.sleep(duracaoMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

