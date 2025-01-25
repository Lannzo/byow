package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;
import tileengine.Tileset;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

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

    private static int bombX;
    private static int bombY;
    private static int bombTimer;
    private static boolean bombActive = false;
    private static List<ExplosionTile> explosionTiles = new ArrayList<>();

    public static void main(String[] args) {
        // initialize the tile rendering engine with a window of size WIDTH x HEIGHT
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH + 2, HEIGHT + 2, 1, 1);

        // initialize tiles
        TETile[][] world = new TETile[WIDTH][HEIGHT];

        // Enter interface
        // Take

        // Methods to generate the world
        randomWalk(world);
        smoothen(world, 3);

        // timer 10 seconds
        // Initialize bomb variables
        bombTimer = -1; // Not active initially


//        spawnCoins();
        spawnMonsters();
        placeLadders();

        Avatar player = createAvatar(CENTERX, CENTERY);
        updateAvatar(world, player);

        // Update world and avatar movement
        boolean gameOver = false;
        while (!gameOver) {
            String input = takeInput();

            // Turn off lights
            if (input.equals("L")) {
                changeLights(world, player);
            }

            makeMove(input, player, world);

            // Bomb logic
            if (!bombActive) {
                spawnBomb(world);
            }

            if (bombActive) {
                bombTimer--;
                if (bombTimer <= 0) {
                    explodeBomb(world);
                    bombActive = false;
                }
            }
            updateExplosions(world);

            ter.renderFrame(world);
        }
    }

    private static class ExplosionTile {
        int x;
        int y;
        int timer;

        public ExplosionTile(int x, int y, int timer) {
            this.x = x;
            this.y = y;
            this.timer = timer;
        }
    }

    private static void spawnBomb(TETile[][] world) {
        // Find a random floor tile
        while (true) {
            bombX = RANDOM.nextInt(WIDTH);
            bombY = RANDOM.nextInt(HEIGHT);
            if (world[bombX][bombY] == Tileset.FLOOR) {
                break;
            }
        }

        world[bombX][bombY] = Tileset.BOMB; // Use flower as bomb for now, replace with bomb image later
        bombTimer = 400; // Set timer to 50 frames (adjust for desired duration)
        bombActive = true;
    }

    private static void explodeBomb(TETile[][] world) {
        explosionTiles.clear(); // Clear previous explosion effects

        for (int x = bombX - 2; x <= bombX + 1; x++) {
            for (int y = bombY - 2; y <= bombY + 1; y++) {
                if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT && world[x][y] != Tileset.CELL && world[x][y] != Tileset.NOTHING) {
                    if(world[x][y] == Tileset.AVATAR){
                        System.out.println("Game Over");
                        System.exit(0);
                    }
                    explosionTiles.add(new ExplosionTile(x, y, 120)); // Add to explosion list with timer
                    world[x][y] = Tileset.FIRE; // Set to fire
                }
            }
        }
    }

    private static void updateExplosions(TETile[][] world) {

        for (ExplosionTile tile : explosionTiles) {
            tile.timer--;

            if (tile.timer <= 0) {
                world[tile.x][tile.y] = Tileset.FLOOR; // Revert to floor
            }
        }

    }


    private static void placeLadders() {
        // Generate new world, proceed to next level
    }

    private static void spawnMonsters() {
        // Spawn monster

        // Random walk monster at time intervals

        // If avatar walks into monster, dead end game
    }

    private static void spawnCoins() {
        // Logic where to spawn coins: find corner floors, spawn coins with chance p%

        // Update world to spawn coins
    }

    // Change Lights
    private static void changeLights(TETile[][] world, Avatar player) {
        int VISIONRADIUS = 5;

        TETile[][] noLightsWorld = world;

        // Dim the lights

        // Get player coordinates

        // Get all the tiles around player

        // Update temporary world: visible tiles around ,black the rest

        // Render

        // Render original world
    }


    private static boolean withinVision(Avatar player, TETile[][] world, int x, int y, int radius) {
        int playerPosX = player.x;
        int playerPosY = player.y;

        // Calculate if tile within radius
        return (x >= playerPosX - radius && x <= playerPosX + radius) && (y > playerPosY - radius || y < playerPosY + radius);
    }

    private static void makeMove(String move, Avatar player, TETile[][] world) {
        if (validMove(move, player, world)) {
            // Reverse the previous tile
            world[player.x][player.y] = Tileset.FLOOR;

            switch (move) {
                case "w" -> player.y += 1;
                case "a" -> player.x -= 1;
                case "s" -> player.y -= 1;
                case "d" -> player.x += 1;
            }

            updateAvatar(world, player);
        }
    }

    private static boolean validMove(String move, Avatar player, TETile[][] world) {
        int x;
        int y;

        switch (move) {
            case "w":
                x = player.x;
                y = player.y + 1;
                break;
            case "a":
                x = player.x - 1;
                y = player.y;
                break;
            case "s":
                x = player.x;
                y = player.y - 1;
                break;
            case "d":
                x = player.x + 1;
                y = player.y;
                break;
            default:
                return false;
        }

        // Outside of world
        if (x < 0 ||  x >= world.length || y < 0 || y >= world[0].length){
            return false;
        }

        // Attempted to move to a wall
        if (world[x][y] == Tileset.CELL){
            return false;
        }

        return true;
    }

    private static String takeInput() {
        StringBuilder s = new StringBuilder();
        if (StdDraw.hasNextKeyTyped()) {
            s.append(StdDraw.nextKeyTyped());
        }
        return s.toString();
    }

    public static void randomWalk(TETile[][] tiles) {
        int height = tiles[0].length;
        int width = tiles.length;
        int tileFloored = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = Tileset.CELL;
            }
        }

        int currentX = CENTERX;
        int currentY = CENTERY;
        while (tileFloored < TILE_COUNT * 0.5) {
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

            if (tiles[currentX][currentY] == Tileset.FLOOR) {
                continue;
            }
            tileFloored++;
        }
    }

    public static void smoothen(TETile[][] tiles, int iterations) {
        for (int i = 0; i < iterations; i++) {
            smoothen(tiles);
        }
    }

    public static void smoothen(TETile[][] tiles) {
        for (int y = 1; y < HEIGHT - 1; y++) {
            for (int x = 1; x < WIDTH - 1; x++) {
                if (tiles[x][y] == Tileset.CELL && isEdgeWalls(tiles, x, y)) {
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

    private static Avatar createAvatar(int positionX, int positionY) {
        return new Avatar(positionX, positionY);
    }

    private static void updateAvatar(TETile[][] world, Avatar player) {
        world[player.x][player.y] = Tileset.AVATAR;
    }
}