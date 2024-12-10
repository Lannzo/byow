package core;

import tileengine.TERenderer;
import tileengine.TETile;
import tileengine.Tileset;

import java.util.ArrayDeque;
import java.util.Random;

public class Main {
    private static final int WIDTH = 60;
    private static final int HEIGHT = 45;
    private static final int TILE_COUNT = WIDTH * HEIGHT;
    private static final int CENTERX =  WIDTH / 2;
    private static final int CENTERY =  HEIGHT / 2;



    private static final long SEED = 2873123;
    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        // initialize the tile rendering engine with a window of size WIDTH x HEIGHT
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);

        // initialize tiles
        TETile[][] world = new TETile[WIDTH][HEIGHT];
        randomWalk(world);

        for (int i = 0; i < 3; i++) {
        smoothen(world);
        }

        // draws the world to the screen
        ter.renderFrame(world);
    }

    public static void randomWalk(TETile[][] tiles) {
        int height = tiles[0].length;
        int width = tiles.length;
        int tileFloored = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = Tileset.WALL;
            }
        }

        int currentX = CENTERX;
        int currentY = CENTERY;
        while (tileFloored++ < TILE_COUNT * 0.5) {
            tiles[currentX][currentY] = Tileset.FLOOR;

            // Randomly move to adjacent tile
            switch (RANDOM.nextInt(4)) {
                case 0 -> currentX++;
                case 1 -> currentX--;
                case 2 -> currentY++;
                default -> currentY--;
            }

            // restrict currentX and currentY within bounds
            currentX = Math.max(0, Math.min(currentX, width - 1));
            currentY = Math.max(0, Math.min(currentY, height - 1));
        }
    }
    public static void smoothen(TETile[][] tiles) {
        for (int y = 1; y < HEIGHT - 1; y++) {
            for (int x = 1; x < WIDTH - 1; x++) {
                if (tiles[x][y] == Tileset.WALL && isEdgeWalls(tiles, x, y)) {
                    tiles[x][y] = Tileset.FLOOR;
                }
            }

        }
    }

    private static boolean isEdgeWalls(TETile[][] tiles, int x, int y) {
        int floorCount = 0;
        if (tiles[x + 1][y] == Tileset.FLOOR) { floorCount++; }
        if (tiles[x - 1][y] == Tileset.FLOOR) { floorCount++; }
        if (tiles[x][y + 1] == Tileset.FLOOR) { floorCount++; }
        if (tiles[x][y - 1] == Tileset.FLOOR) { floorCount++; }

        return floorCount > 2;
    }
}