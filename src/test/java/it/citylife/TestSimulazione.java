package it.citylife;

import it.citylife.model.City;
import it.citylife.model.CityState;
import it.citylife.model.StateObserver;

public class TestSimulazione {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- AVVIO MOTORE DI TEST ---");

        // 1. Accendi la tua città
        City myCity = new City();

        // 2. Ti fingi l'interfaccia grafica per leggere i dati
        myCity.addObserver(new StateObserver() {
            @Override
            public void onStateChanged(CityState state) {
                // Se i soldi finiscono, scatta l'allarme!
                if (state.getBudget() < 0) {
                    System.out.println("⚠️ BANCAROTTA! La città è in debito!");
                }
            }
        });

        System.out.println("Città generata con 8 edifici. Faccio partire il tempo...");

        // 3. Il ciclo del tempo (Facciamo passare 10 turni)
        for (int i = 1; i <= 10; i++) {
            System.out.println("\n[ PREMO IL TASTO ADVANCE TICK - TURNO " + i + " ]");
            
            // Fai calcolare tutto al tuo motore
            myCity.advanceTick();
            
            // Pausa di 2 secondi per permetterti di leggere il terminale
            Thread.sleep(2000); 
        }

        System.out.println("\n--- TEST COMPLETATO ---");
    }
}