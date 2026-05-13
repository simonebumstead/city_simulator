package it.citylife.model;

/**
 * Rappresenta una singola cella della griglia 20×20 della città.
 *
 * Ogni cella è identificata dalle sue coordinate (x, y) e può contenere
 * al più una struttura ({@link Placeable}). Se la cella è vuota, il campo
 * structure è null e isEmpty() restituisce true.
 *
 * La cella non conosce la griglia che la contiene né le celle adiacenti:
 * tutta la logica di adiacenza e attraversamento è delegata a {@link Grid}
 * e a {@link City}.
 *
 * @see Grid
 * @see Placeable
 * @see Structure
 */
public class Cell {

    // Coordinate della cella nella griglia (x = colonna, y = riga)
    private int x, y;

    // Struttura attualmente piazzata in questa cella; null se la cella è vuota
    private Placeable structure;

    /**
     * Crea una cella vuota alle coordinate specificate.
     *
     * @param x colonna nella griglia (0-based, da 0 a Grid.WIDTH-1)
     * @param y riga nella griglia (0-based, da 0 a Grid.HEIGHT-1)
     */
    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
        this.structure = null;
    }

    /**
     * Indica se la cella è priva di strutture.
     *
     * @return true se non è piazzata alcuna struttura, false altrimenti
     */
    public boolean isEmpty() {
        return structure == null;
    }

    /**
     * Restituisce la struttura contenuta nella cella.
     *
     * @return la struttura piazzata, oppure null se la cella è vuota
     */
    public Placeable getStructure() { return structure; }

    /**
     * Imposta la struttura contenuta nella cella.
     * Passare null equivale a rimuovere la struttura (svuotare la cella).
     *
     * @param s la struttura da piazzare, oppure null per svuotare la cella
     */
    public void setStructure(Placeable s) { this.structure = s; }
}
