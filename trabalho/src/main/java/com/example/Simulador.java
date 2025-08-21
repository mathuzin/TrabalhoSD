package com.example;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Simulador {

    private static final String[] PORTAS_COORDENADORES = {"12345", "12346", "12347"};
    private static AtomicInteger clienteId = new AtomicInteger(0);

    public static void main(String[] args) {
        new Thread(() -> {
            new CoordenadorServidor(Integer.parseInt(PORTAS_COORDENADORES[0])).iniciar();
        }).start();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        scheduler.scheduleAtFixedRate(() -> {
            int id = clienteId.incrementAndGet();
            System.out.println("\n--- Lançando novo processo: " + id + " ---");
            new Thread(new ProcessoCliente(id, PORTAS_COORDENADORES)).start();
        }, 0, 40, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("\n--- Reiniciando o coordenador em uma porta aleatória ---");
            new Thread(() -> {
                int portaAleatoria = Integer.parseInt(PORTAS_COORDENADORES[
                    java.util.concurrent.ThreadLocalRandom.current().nextInt(PORTAS_COORDENADORES.length)
                ]);
                new CoordenadorServidor(portaAleatoria).iniciar();
            }).start();
        }, 65, 60, TimeUnit.SECONDS); 
    }
}