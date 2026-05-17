package it.citylife.model;

/**
 * Utility statiche per interrogazioni topologiche sulla {@link Grid}.
 *
 * Consolida operazioni ripetute (adiacenza stradale, copertura elettrica,
 * iterazione celle) che prima erano duplicate in {@link City} e
 * {@link GameController}.
 */
public final class GridQueries {

    // Raggio (distanza di Chebyshev) entro cui una PowerPlant alimenta una cella
    public static final int POWER_RADIUS = 5;

    private static final int[][] CARDINAL_DIRS = {{0,1}, {0,-1}, {1,0}, {-1,0}};

    private GridQueries() {}

    /**
     * @return true se almeno una delle quattro celle cardinalmente adiacenti
     *         contiene una Road.
     */
    public static boolean hasAdjacentRoad(Grid grid, int x, int y) {
        for (int[] d : CARDINAL_DIRS) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (nx < 0 || nx >= grid.getWidth() || ny < 0 || ny >= grid.getHeight()) continue;
            Cell c = grid.getCell(nx, ny);
            if (c != null && c.getStructure() != null && c.getStructure().getType() == StructureType.ROAD) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true se la cella (x, y) è coperta da almeno una PowerPlant
     *         entro la distanza di Chebyshev {@link #POWER_RADIUS}.
     */
    public static boolean isPoweredAt(Grid grid, int x, int y) {
        for (int px = 0; px < grid.getWidth(); px++) {
            for (int py = 0; py < grid.getHeight(); py++) {
                Cell pc = grid.getCell(px, py);
                if (pc != null && pc.getStructure() instanceof PowerPlant
                        && Math.max(Math.abs(px - x), Math.abs(py - y)) <= POWER_RADIUS) {
                    return true;
                }
            }
        }
        return false;
    }
}
