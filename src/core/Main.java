package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;
import tileengine.Tileset;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import java.io.*;
import java.util.ArrayDeque;
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
    private static int highScore = 0;

    private static final String HIGH_SCORE_FILE = "highscore.txt";
    private static Clip backgroundMusicClip;
    private static Clip coinCollectClip;
    private static Clip monsterCollisionClip;
    private static Clip bombCollisionClip;

    private static final long SEED = 2873123;
    private static final Random RANDOM = new Random();

    private static int bombX;
    private static int bombY;
    private static int bombTimer;
    private static boolean bombActive = false;
    private static List<ExplosionTile> explosionTiles = new ArrayList<>();
    private static int coinCount = 0;

    public static void main(String[] args) {
        // Load and play background music
        loadBackgroundMusic("src/sounds/bg.wav");
        playBackgroundMusic();

        // Load sound effects
        loadSoundEffect("coin", "src/sounds/coin.wav");
        loadSoundEffect("monster", "src/sounds/ack.wav");
        loadSoundEffect("bomb", "src/sounds/bomb.wav");

        showMenu(); // Game interface

        loadHighScore();

        // initialize the tile rendering engine with a window of size WIDTH x HEIGHT
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH + 4, HEIGHT + 6, 2, 3);

        TETile[][] world = new TETile[WIDTH][HEIGHT];

        randomWalk(world);
        smoothen(world, 3);
        spawnCoins(world, 5);


        // timer 10 seconds
        // Initialize bomb variables
        bombTimer = -1; // Not active initially
        List<Monster> monsters = new ArrayList<>();

        int minMonster = 12;
        int maxMonster = 18;

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

        while (!gameOver) {
            String input = takeInput();

            // Make the movement of monster slower
            if (monsterMoveCounter % 50 == 0) {
                moveMonsters(world, monsters);
            }
            monsterMoveCounter++;

            makeMove(input, player, world);

            // Bomb logic
            if (!bombActive) {
                spawnBomb(world);
            }

            if (bombActive) {
                bombTimer--;
                if (bombTimer <= 0) {
                    explodeBomb(world);
                    playSoundEffect("bomb");
                    gameOver = avatarBombed(player);
                    if (gameOver) {
                        playSoundEffect("monster");
                        showGameOverScreen(score);
                    }
                    bombActive = false;
                }
            }

            if (!gameOver) {
                updateExplosions(world);
            }

            ter.renderFrame(world);

            if (checkCollision(player, monsters)) {
                playSoundEffect("monster");
                stopBackgroundMusic();
                System.out.println("Game Over! You were caught by a monster.");
                gameOver = true;
                showGameOverScreen(score);

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

    
    //Menu
    private static void showMenu() {
        StdDraw.setCanvasSize(800, 600); // Set menu screen size
        boolean redraw = true; // Flag to track whether to redraw the screen

        while (true) { // Keep showing the menu until the game starts or the program exits
            if (redraw) {
                StdDraw.clear(StdDraw.BLACK);   // Clear the screen to black
                StdDraw.picture(0.5, 0.5, "src/image/bg asset.png", 1.0, 1.0);

                // Display menu text
                StdDraw.setPenColor(StdDraw.WHITE);
                StdDraw.setFont(new Font("Monospaced", Font.BOLD, 50));
                StdDraw.text(0.5, 0.7, "GAME TITLE");

                StdDraw.setFont(new Font("Monospaced", Font.BOLD, 30));
                StdDraw.text(0.5, 0.5, "(1) New Game");
                StdDraw.text(0.5, 0.4, "(2) Instructions");
                StdDraw.text(0.5, 0.3, "(3) Exit");

                StdDraw.show(); // Render the menu
                redraw = false; // Set redraw to false to avoid unnecessary redrawing
            }

            // Wait for user input
            if (StdDraw.hasNextKeyTyped()) {
                char input = StdDraw.nextKeyTyped();
                if (input == '1') {
                    return; // Start the game
                } else if (input == '2') {
                    showInstructions(); // Show the instructions
                    redraw = true; // Redraw the menu after instructions
                } else if (input == '3') {
                    System.exit(0); // Exit the game
                }
            }
        }
    }

    //Instructions
    private static void showInstructions() {
        StdDraw.setCanvasSize(800, 600); // Set instructions screen size
        StdDraw.picture(0.5, 0.5, "src/image/bg asset2.png", 1.0, 1.0);

        // Display instructions text
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setFont(new Font("Monospaced", Font.BOLD, 20));
        StdDraw.text(0.5, 0.8, "HOW TO PLAY:");
        StdDraw.text(0.5, 0.7, "1. Use WASD keys to move.");
        StdDraw.text(0.5, 0.6, "2. Collect coins to increase your score.");
        StdDraw.text(0.5, 0.5, "3. Avoid monsters and bombs. Or else you will die.");
        StdDraw.text(0.5, 0.4, "4. Press (B) to go back to the menu to start a new game.");


        StdDraw.show(); // Render the instructions

        // Wait for user input to go back
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char input = StdDraw.nextKeyTyped();
                if (input == 'b' || input == 'B') {
                    return; // Exit instructions and go back to the menu
                }
            }
        }
    }

    //Method to stop bg music
    private static void stopBackgroundMusic() {
        if (backgroundMusicClip != null && backgroundMusicClip.isRunning()) {
            backgroundMusicClip.stop();
        }
    }

    private static void showGameOverScreen(int finalScore) {
        StdDraw.clear(StdDraw.BLACK); // Clear the screen
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setXscale(0, 1);
        StdDraw.setYscale(0, 1);
        StdDraw.setFont(new Font("Monospaced", Font.BOLD, 50));
        StdDraw.text(0.5, 0.8, "GAME OVER!");

        StdDraw.setFont(new Font("Monospaced", Font.BOLD, 30));
        StdDraw.text(0.5, 0.6, "Your Score: " + finalScore);

        // Check if the player beat the high score
        if (finalScore > highScore) {
            highScore = finalScore;
            saveHighScore(); // Save the new high score
            StdDraw.text(0.5, 0.5, "New High Score!");
        } else {
            StdDraw.text(0.5, 0.5, "High Score: " + highScore);
        }

        // Add more spacing for the Restart and Quit options
        StdDraw.text(0.5, 0.35, "(R) Restart");
        StdDraw.text(0.5, 0.25, "(Q) Quit");


        StdDraw.show();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char input = StdDraw.nextKeyTyped();
                if (input == 'r' || input == 'R') {
                    resetGame(); // Reset the game state
                    return;
                } else if (input == 'q' || input == 'Q') {
                    System.exit(0); // Exit the game
                }
            }
        }
    }

    private static void loadHighScore() {
        File file = new File(HIGH_SCORE_FILE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                if (line != null) {
                    highScore = Integer.parseInt(line);
                }
            } catch (IOException | NumberFormatException e) {
                System.out.println("Error loading high score: " + e.getMessage());
            }
        }
    }

    private static void saveHighScore() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HIGH_SCORE_FILE))) {
            writer.write(String.valueOf(highScore));
        } catch (IOException e) {
            System.out.println("Error saving high score: " + e.getMessage());
        }
    }



    // Method to reset the game state
    private static void resetGame() {
        score = 0; // Reset the score to 0
        main(null); // Restart the game by calling the main method
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
                    if (world[x][y] == Tileset.COIN) {
                        destroyCoin(x, y, world);
                    }
                    explosionTiles.add(new ExplosionTile(x, y, 60)); // Add to explosion list with timer
                    world[x][y] = Tileset.FIRE; // Set to fire
                }
            }
        }
    }

    private static boolean avatarBombed(Avatar player) {
        for (ExplosionTile tile : explosionTiles) {
            if (player.x == tile.x && player.y == tile.y) {
                return true;
            }
        }
        return false;
    }

    private static void updateExplosions(TETile[][] world) {

        for (ExplosionTile tile : explosionTiles) {
            tile.timer--;

            if (tile.timer <= 0) {
                world[tile.x][tile.y] = Tileset.FLOOR; // Revert to floor
            }
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
                } else if ("bomb".equals(type)) {
                    bombCollisionClip = AudioSystem.getClip();
                    bombCollisionClip.open(audioInput);
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
        } else if ("bomb".equals(type) && bombCollisionClip != null) {
            bombCollisionClip.setFramePosition(0); // Reset to start
            bombCollisionClip.start();
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
        StdDraw.textRight(WIDTH + 2, 49.5, "High Score: " + highScore);

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

                if (world[monster.x][monster.y] == Tileset.COIN) {
                    destroyCoin(monster.x, monster.y, world);
                }

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
    public static void spawnCoins(TETile[][] world, int numCoins) {
        Random random = new Random();
        coinCount = 0; // Reset coin count

        // Count existing coins
        for (int x = 0; x < world.length; x++) {
            for (int y = 0; y < world[0].length; y++) {
                if (world[x][y] == Tileset.COIN) {
                    coinCount++;
                }
            }
        }

        // If all coins are gone, spawn a new batch
        if (coinCount == 0) {
            System.out.println("Respawning coins...");
            for (int i = 0; i < numCoins; i++) {
                int x, y;
                do {
                    x = random.nextInt(world.length);
                    y = random.nextInt(world[0].length);
                } while (world[x][y] != Tileset.FLOOR); // Ensure valid tile

                world[x][y] = Tileset.COIN; // Place a coin
                coinCount++; // Increase count as coins are placed
            }
        }
    }

    public static void destroyCoin(int x, int y, TETile[][] world) {
        if (world[x][y] == Tileset.COIN) {
            world[x][y] = Tileset.FLOOR; // Replace coin with floor
            coinCount--; // Reduce coin count
            System.out.println("Coin at (" + x + ", " + y + ") destroyed! Remaining: " + coinCount);

            if (coinCount == 0) {
                System.out.println("All coins destroyed! Attempting to respawn...");
                spawnCoins(world, 10); // Respawn 5 coins if count is 0
            }
        } else {
            System.out.println("Tried to destroy a non-coin tile at (" + x + ", " + y + ")");
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
            destroyCoin(x, y, world);
            score++; // Increment the score when a coin is picked up
            System.out.println("Coin Collected! Score: " + score);

            if (coinCount == 0) {
                spawnCoins(world, 10);  // respawn 5 coins if coinCount is 0
            }
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