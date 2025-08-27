package com.example;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Simulador {

    private static final String[] PORTAS_COORDENADORES = {"12345", "12346", "12347"};
    private static final Set<Integer> idsUsados = new HashSet<>(); // garante IDs únicos

    public static void main(String[] args) {
        // Inicia primeiro coordenador na primeira porta
        new Thread(() -> new CoordenadorServidor(Integer.parseInt(PORTAS_COORDENADORES[0])).iniciar()).start();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        // A cada 40s cria um novo processo cliente
        scheduler.scheduleAtFixedRate(() -> {
            int id;
            do {
                id = ThreadLocalRandom.current().nextInt(1, 10000); // ID randômico
            } while (idsUsados.contains(id));
            idsUsados.add(id);

            System.out.println("\n--- Lançando novo processo: " + id + " ---");
            new Thread(new ProcessoCliente(id, PORTAS_COORDENADORES)).start();
        }, 0, 40, TimeUnit.SECONDS);

        // A cada 60s cria um novo coordenador numa porta aleatória
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("\n--- Reiniciando o coordenador em uma porta aleatória ---");
            new Thread(() -> {
                int portaAleatoria = Integer.parseInt(
                    PORTAS_COORDENADORES[ThreadLocalRandom.current().nextInt(PORTAS_COORDENADORES.length)]
                );
                new CoordenadorServidor(portaAleatoria).iniciar();
            }).start();
        }, 60, 60, TimeUnit.SECONDS);
    }
}

