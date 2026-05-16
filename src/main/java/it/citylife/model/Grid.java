package it.citylife.model;

/**
 * Mappa di gioco della città: una griglia rettangolare di celle (WIDTH × HEIGHT).
 *
 * Gestisce la matrice bidimensionale di {@link Cell} e fornisce i metodi per
 * piazzare, rimuovere e accedere alle strutture per coordinate (x = colonna, y = riga).
 *
 * Le dimensioni sono fisse a 20×20 ma separate in due costanti distinte (WIDTH e HEIGHT)
 * per supportare facilmente mappe rettangolari in futuro senza modificare il codice chiamante.
 *
 * La griglia non conosce la logica di gioco (budget, politiche, effetti):
 * è un puro contenitore spaziale interrogato da City e GameController. 
 *
 * @see Cell
 * @see City
 * @see GameController
 */
public class Grid {

    // Matrice bidimensionale indicizzata come matrix[x][y] (colonna, riga)
    private Cell[][] matrix;

    // Numero di colonne della griglia (asse X)
    private static final int WIDTH = 20;

    // Numero di righe della griglia (asse Y)
    private static final int HEIGHT = 20;

    /**
     * Inizializza la griglia allocando e istanziando tutte le WIDTH × HEIGHT celle.
     * Ogni cella riceve le proprie coordinate (x, y) e parte vuota (structure = null).
     */
    public Grid() {
        this.matrix = new Cell[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                matrix[x][y] = new Cell(x, y);
            }
        }
    }

    /**
     * Restituisce la cella alle coordinate (x, y).
     *
     * Restituisce null se le coordinate sono fuori dai limiti della griglia,
     * evitando ArrayIndexOutOfBoundsException nei loop di attraversamento.
     *
     * @param x colonna (0-based, da 0 a WIDTH-1)
     * @param y riga (0-based, da 0 a HEIGHT-1)
     * @return la cella corrispondente, oppure null se le coordinate sono invalide
     */
    public Cell getCell(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            return null;
        }
        return matrix[x][y];
    }

    /**
     * Verifica se la cella alle coordinate (x, y) è vuota e valida.
     *
     * Restituisce false sia per coordinate fuori griglia (cella null)
     * sia per celle già occupate da una struttura.
     *
     * @param x colonna della cella
     * @param y riga della cella
     * @return true se la cella esiste ed è priva di strutture
     */
    public boolean isCellEmpty(int x, int y) {
        Cell cell = getCell(x, y);
        return (cell != null && cell.isEmpty());
    }

    /**
     * Piazza una struttura nella cella (x, y) se questa è vuota.
     *
     * Non esegue validazioni di budget o regole di gioco: quelle sono
     * responsabilità di GameController. Questo metodo gestisce solo
     * il posizionamento fisico nella griglia.
     *
     * @param s la struttura da piazzare
     * @param x colonna della cella target
     * @param y riga della cella target
     * @return true se il piazzamento è avvenuto, false se la cella è occupata o invalida
     */
    public boolean placeStructure(Structure s, int x, int y) {
        if (!isCellEmpty(x, y)) return false;
        getCell(x, y).setStructure(s);
        return true;
    }

    /**
     * Rimuove la struttura dalla cella (x, y), lasciandola vuota.
     * Non ha effetto se le coordinate sono invalide o la cella è già vuota.
     *
     * @param x colonna della cella target
     * @param y riga della cella target
     */
    public void removeStructure(int x, int y) {
        Cell cell = getCell(x, y);
        if (cell != null) cell.setStructure(null);
    }

    /**
     * Restituisce il numero di colonne della griglia (asse X).
     *
     * @return larghezza della griglia (WIDTH = 20)
     */
    public int getWidth() {
        return WIDTH;
    }

    /**
     * Restituisce il numero di righe della griglia (asse Y).
     *
     * @return altezza della griglia (HEIGHT = 20)
     */
    public int getHeight() {
        return HEIGHT;
    }
}
