package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;
import tileengine.Tileset;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.Color;
import java.awt.Font;

public class Main {
    private static final int WIDTH = 60;
    private static final int HEIGHT = 45;
    private static final int TILE_COUNT = WIDTH * HEIGHT;
    private static final int CENTERX =  WIDTH / 2;
    private static final int CENTERY =  HEIGHT / 2;
    private static int score = 0; // Keep track of the score

    private static Clip backgroundMusicClip;
    private static Clip coinCollectClip;
    private static Clip monsterCollisionClip;

    private static final long SEED = 2873123;
    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        // initialize the tile rendering engine with a window of size WIDTH x HEIGHT
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH + 4, HEIGHT + 6, 2, 3);

        TETile[][] world = new TETile[WIDTH][HEIGHT];

        randomWalk(world);
        smoothen(world, 3);
        spawnCoins(world);

        List<Monster> monsters = new ArrayList<>();

        int minMonster = 5;
        int maxMonster = 10;

        int numMon = RANDOM.nextInt(maxMonster) + minMonster;
        spawnMonsters(world, monsters,numMon);

        Avatar player = createAvatar(CENTERX, CENTERY);
        updateAvatar(world, player);

        // Board for displaying score (replace with desired dimensions)
        char[][] board = new char[1][15]; // 3 rows, 10 columns (adjust as needed)

        // Initialize board with empty spaces or borders
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = ' '; // Replace with border character if desired
            }
        }

        // Update world and avatar movement
        boolean gameOver = false;
        int monsterMoveCounter =0;

        // Load and play background music
        loadBackgroundMusic("src/sounds/bg.wav");
        playBackgroundMusic();

        // Load sound effects
        loadSoundEffect("coin", "src/sounds/coin.wav");
        loadSoundEffect("monster", "src/sounds/ack.wav");

        while (!gameOver) {
            String input = takeInput();

            makeMove(input, player, world);

            // Make the movement of monster slower
            if (monsterMoveCounter % 50 == 0) {
                moveMonsters(world, monsters);
            }
            monsterMoveCounter++;

            ter.renderFrame(world);

            if (checkCollision(player, monsters)) {
                playSoundEffect("monster");
                System.out.println("Game Over! You were caught by a monster.");
                gameOver = true;

            }
            drawHUD(score, board); // Draw HUD including the board
            StdDraw.show(); // Essential for displaying the HUD
        }

        // Update board (if score changes)
        if (score > 0) {
            score++;
            updateBoard(board, score); // Call to update board with new score
        }
    }

    private static void loadBackgroundMusic(String filePath) {
        try {
            File musicPath = new File(filePath);

            if (musicPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
                backgroundMusicClip = AudioSystem.getClip();
                backgroundMusicClip.open(audioInput);
            } else {
                System.out.println("Can't find file");
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private static void playBackgroundMusic() {
        if (backgroundMusicClip != null) {
            backgroundMusicClip.start();
            backgroundMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private static void loadSoundEffect(String type, String filePath) {
        try {
            File soundFile = new File(filePath);
            if (soundFile.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundFile);
                if ("coin".equals(type)) {
                    coinCollectClip = AudioSystem.getClip();
                    coinCollectClip.open(audioInput);
                } else if ("monster".equals(type)) {
                    monsterCollisionClip = AudioSystem.getClip();
                    monsterCollisionClip.open(audioInput);
                }
            } else {
                System.out.println("Can't find file: " + filePath);
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private static void playSoundEffect(String type) {
        if ("coin".equals(type) && coinCollectClip != null) {
            coinCollectClip.setFramePosition(0); // Reset to start
            coinCollectClip.start();
        } else if ("monster".equals(type) && monsterCollisionClip != null) {
            monsterCollisionClip.setFramePosition(0); // Reset to start
            monsterCollisionClip.start();
        }
    }

    private static void drawHUD(int score, char[][] board) {
        // Adjust positioning based on ter.initialize parameters
        int offsetX = 2; // Adjust based on the x offset in ter.initialize
        int offsetY = HEIGHT + 5; // Adjusted y-position (one less than total height + top offset)

        // SCORE LABEL
        StdDraw.setPenColor(Color.GREEN);
        Font font = new Font(" ", Font.BOLD, 16);
        // Position of Score Label (adjusted for offset)
        StdDraw.textLeft(offsetX, 49.5, "Score: " + score);

        // Border around the board
        StdDraw.rectangle(offsetX + board[0].length / 2.0, offsetY -0.5 , board[0].length / 2.0 + 0.5, 1.0);
        StdDraw.setPenRadius(0.004); // Adjust thickness as desired
    }

    private static void updateBoard(char[][] board, int score) {
        String scoreStr = String.valueOf(score);

        // Clear previous score on the board
        for (int j = 0; j < board[0].length; j++) {
            board[0][j] = ' ';
        }

        // Place the new score on the board
        for (int i = 0; i < scoreStr.length(); i++) {
            if (i < board[0].length) { // Prevent index out of bounds
                board[0][i] = scoreStr.charAt(i);

            }
        }
    }

    private static void spawnMonsters(TETile[][] world, List<Monster> monsters, int numberOfMonsters) {
        // Spawn monster
        for (int i = 0; i < numberOfMonsters; i++){
            int x,y;
            do {
                x = RANDOM.nextInt(WIDTH);
                y = RANDOM.nextInt(HEIGHT);
            } while (world[x][y] != Tileset.FLOOR); // to spawn monsters only in floor tiles

            world[x][y] = Tileset.MONSTER;
            monsters.add(new Monster(x,y));
        }
    }

    private static void moveMonsters(TETile[][] world, List<Monster> monsters) {
        for (Monster monster : monsters) {
            // Save current position
            int oldX = monster.x;
            int oldY = monster.y;

            // Randomly choose a direction
            switch (RANDOM.nextInt(4)) {
                case 0 -> monster.x++; // Move right
                case 1 -> monster.x--; // Move left
                case 2 -> monster.y++; // Move up
                case 3 -> monster.y--; // Move down
            }

            // Ensure the monster doesn't move into walls or out of bounds
            if (monster.x < 0 || monster.x >= WIDTH || monster.y < 0 || monster.y >= HEIGHT ||
                    world[monster.x][monster.y] == Tileset.CELL) {
                monster.x = oldX;
                monster.y = oldY;
            } else {
                // Allow monster walking into avatar
                if (world[oldX][oldY] != Tileset.AVATAR) {
                    world[oldX][oldY] = Tileset.FLOOR;
                }

                world[monster.x][monster.y] = Tileset.MONSTER;
            }
        }
    }

    private static boolean checkCollision(Avatar player, List<Monster> monsters) {
        for (Monster monster : monsters) {
            if (player.x == monster.x && player.y == monster.y) {
                return true; // Player collided with a monster
            }
        }
        return false;
    }

    // SPAWN COINS UPDATED RANDOM CORNERS AND EVERYWHERE
    private static void spawnCoins(TETile[][] world) {
        Random random = new Random();
        double cornerSpawnChance = 0.1;
        double randomSpawnChance = 0.02; // Lower chance for random spawns

        for (int x = 1; x < WIDTH - 1; x++) {
            for (int y = 1; y < HEIGHT - 1; y++) {
                if (world[x][y] == Tileset.FLOOR) {
                    int adjacentWalls = 0;
                    if (world[x + 1][y] == Tileset.CELL) adjacentWalls++;
                    if (world[x - 1][y] == Tileset.CELL) adjacentWalls++;
                    if (world[x][y + 1] == Tileset.CELL) adjacentWalls++;
                    if (world[x][y - 1] == Tileset.CELL) adjacentWalls++;

                    if (adjacentWalls >= 2 && random.nextDouble() < cornerSpawnChance) {
                        world[x][y] = Tileset.COIN;
                    } else if (random.nextDouble() < randomSpawnChance) { // Random spawn if not a corner
                        world[x][y] = Tileset.COIN;
                    }
                }
            }
        }

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
        if (world[x][y] == Tileset.COIN) {
            playSoundEffect("coin");
            world[x][y] = Tileset.FLOOR;
            score++; // Increment the score when a coin is picked up
            System.out.println("Coin Collected! Score: " + score);
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