package it.citylife.model;

/**
 * Rappresenta una singola coordinata sulla mappa.
 */
public class Cell {
    private int x, y;
    private Placeable structure;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
        this.structure = null;
    }

    public boolean isEmpty() {
        return structure == null;
    }

    public Placeable getStructure() { return structure; }
    public void setStructure(Placeable s) { this.structure = s; }
}