package com.example;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CoordenadorServidor {

    private static boolean recursoOcupado = false;                  // controla acesso ao recurso
    private static final Queue<String> filaDeRequisicoes = new LinkedList<>();
    private static final Lock lock = new ReentrantLock();
    private static final Map<String, PrintWriter> clienteSaidas = new ConcurrentHashMap<>();
    private static int porta;

    private ServerSocket serverSocket;

    public CoordenadorServidor(int porta) {
        CoordenadorServidor.porta = porta;
    }

    public void iniciar() {
        try {
            serverSocket = new ServerSocket(porta);
            System.out.println("Coordenador iniciado na porta " + porta + ". Aguardando conexões...");

            long inicio = System.currentTimeMillis();

            while (true) {
                // mata o coordenador após 60s
                if (System.currentTimeMillis() - inicio > 60000) {
                    System.out.println("\nCoordenador encerrado por tempo de vida expirado.");
                    filaDeRequisicoes.clear();
                    serverSocket.close();
                    break; // sai do loop sem matar o programa
                }

                Socket clienteSocket = serverSocket.accept();
                new Thread(new GerenciadorDeRequisicoes(clienteSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Coordenador na porta " + porta + " finalizado: " + e.getMessage());
        }
    }

    static class GerenciadorDeRequisicoes implements Runnable {
        private final Socket socket;
        private final String clienteId;
        private final PrintWriter out;

        public GerenciadorDeRequisicoes(Socket socket) throws IOException {
            this.socket = socket;
            this.clienteId = socket.getRemoteSocketAddress().toString();
            this.out = new PrintWriter(socket.getOutputStream(), true);
            clienteSaidas.put(clienteId, this.out);
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String mensagem;
                while ((mensagem = in.readLine()) != null) {
                    lock.lock();
                    try {
                        if ("SOLICITAR".equals(mensagem)) {
                            if (!recursoOcupado) {
                                recursoOcupado = true;
                                out.println("CONCEDER");
                            } else {
                                filaDeRequisicoes.add(clienteId);
                            }
                        } else if ("LIBERAR".equals(mensagem)) {
                            recursoOcupado = false;
                            if (!filaDeRequisicoes.isEmpty()) {
                                String proximoClienteId = filaDeRequisicoes.poll();
                                PrintWriter proximoClienteOut = clienteSaidas.get(proximoClienteId);
                                if (proximoClienteOut != null) {
                                    recursoOcupado = true;
                                    proximoClienteOut.println("CONCEDER");
                                }
                            }
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (IOException e) {
            } finally {
                clienteSaidas.remove(clienteId);
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}

