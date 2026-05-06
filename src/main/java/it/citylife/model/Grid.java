package it.citylife.model;

/**
 * Grid rappresenta la mappa di gioco.
 * Gestisce la matrice di celle e fornisce i metodi per manipolare lo spazio.
 * È predisposta per supportare mappe rettangolari grazie alla separazione di larghezza e altezza.
 */
public class Grid {
    // La matrice bidimensionale di oggetti Cell
    private Cell[][] matrix;
    
    // Dimensioni separate: ottima pratica per la scalabilità futura
    private static final int WIDTH = 20;  // Asse X
    private static final int HEIGHT = 20; // Asse Y

    /**
     * Costruttore: Inizializza fisicamente la scacchiera.
     */
    public Grid() {
        // matrix[colonne][righe]
        this.matrix = new Cell[WIDTH][HEIGHT];
        
        // Ciclo annidato per creare ogni singola cella
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                // Ogni cella riceve le proprie coordinate (X, Y)
                matrix[x][y] = new Cell(x, y);
            }
        }
    }

    /**
     * Recupera una cella specifica in base alle coordinate.
     */
    public Cell getCell(int x, int y) {
        // Controllo di sicurezza aggiornato con WIDTH e HEIGHT
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            return null;
        }
        return matrix[x][y];
    }

    /**
     * Metodo di utilità per verificare se una posizione è libera.
     */
    public boolean isCellEmpty(int x, int y) {
        Cell cell = getCell(x, y);
        return (cell != null && cell.isEmpty());
    }

    // --- I NUOVI METODI RICHIESTI DA TE ---

    /**
     * Restituisce la larghezza della mappa (asse X).
     * Indispensabile per lo Sviluppatore 4 per disegnare le colonne.
     */
    public int getWidth() {
        return WIDTH;
    }

    /**
     * Restituisce l'altezza della mappa (asse Y).
     * Indispensabile per lo Sviluppatore 4 per disegnare le righe.
     */
    public int getHeight() {
        return HEIGHT;
    }
}