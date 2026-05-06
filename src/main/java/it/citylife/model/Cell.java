package it.citylife.model;

/**
 * Rappresenta una singola coordinata sulla mappa.
 */
public class Cell {
    private int x, y;
    private Object structure; // Uso Object in attesa degli edifici

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
        this.structure = null; // Cella inizialmente vuota
    }

    public boolean isEmpty() {
        return structure == null;
    }

    public Object getStructure() { return structure; }
    public void setStructure(Object s) { this.structure = s; }
}