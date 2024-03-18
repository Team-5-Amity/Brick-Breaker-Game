import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BrickBreakerGame extends JFrame implements KeyListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static int PADDLE_WIDTH = 100; // Modified to allow updates
    private static final int PADDLE_HEIGHT = 10;
    private static final int BALL_RADIUS = 10;
    private static final int BRICK_WIDTH = 60;
    private static final int BRICK_HEIGHT = 20;
    private static final int NUM_BRICKS_ROW = 8;
    private static final int NUM_BRICKS_COLUMN = 5;
    private static final int PADDLE_SPEED = 10;
    private static final int POWERUP_WIDTH = 20;
    private static final int POWERUP_HEIGHT = 20;

    private int paddleX = WIDTH / 2 - PADDLE_WIDTH / 2;
    private int paddleY = HEIGHT - PADDLE_HEIGHT - 35;
    private int ballX = WIDTH / 2;
    private int ballY = HEIGHT / 2;
    private int ballSpeedX = 5;
    private int ballSpeedY = -5;
    private boolean ballMoving = false;

    private int score = 0;
    private int level = 1;
    private int numBricksDestroyed = 0;

    private List<Brick> bricks = new ArrayList<>();
    private List<PowerUp> powerUps = new ArrayList<>();

    private boolean leftPressed = false;
    private boolean rightPressed = false;

    private Clip brickBreakClip;
    private Clip paddleHitClip;

    public BrickBreakerGame() {
        setTitle("Brick Breaker Game");
        setSize(WIDTH, HEIGHT);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel gamePanel = new GamePanel(); // Custom JPanel for game graphics
        add(gamePanel);

        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        initializeBricks();
        loadSoundEffects();

        Timer timer = new Timer(10, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move();
                gamePanel.repaint(); // Repaint the game panel
            }
        });
        timer.start();
    }

    // Custom JPanel for game graphics
    private class GamePanel extends JPanel {
        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setDoubleBuffered(true); // Enable double buffering
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Draw paddle
            g.setColor(Color.BLUE);
            g.fillRect(paddleX, paddleY, PADDLE_WIDTH, PADDLE_HEIGHT);

            // Draw ball
            g.setColor(Color.RED);
            g.fillOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS, BALL_RADIUS * 2, BALL_RADIUS * 2);

            // Draw bricks
            for (Brick brick : bricks) {
                if (!brick.isDestroyed()) {
                    g.setColor(brick.getColor());
                    g.fillRect((int) brick.getX(), (int) brick.getY(), BRICK_WIDTH, BRICK_HEIGHT);
                }
            }

            // Draw power-ups
            for (PowerUp powerUp : powerUps) {
                if (powerUp.isActive()) {
                    g.setColor(Color.GREEN);
                    g.fillRect((int) powerUp.getX(), (int) powerUp.getY(), POWERUP_WIDTH, POWERUP_HEIGHT);
                }
            }

            // Draw score and level
            g.setColor(Color.BLACK);
            g.drawString("Score: " + score, 10, 20);
            g.drawString("Level: " + level, 10, 40);
        }
    }

    private void initializeBricks() {
        bricks.clear();
        Random random = new Random();
        Color[] colors = { Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.ORANGE };
        for (int i = 0; i < NUM_BRICKS_COLUMN; i++) {
            for (int j = 0; j < NUM_BRICKS_ROW; j++) {
                bricks.add(new Brick(j * BRICK_WIDTH * 1.5, i * BRICK_HEIGHT * 1.5,
                        colors[random.nextInt(colors.length)]));
            }
        }
    }

    private void loadSoundEffects() {
        try {
            AudioInputStream brickBreakStream = AudioSystem.getAudioInputStream(new File("sounds/brick_break.mp3"));
            brickBreakClip = AudioSystem.getClip();
            brickBreakClip.open(brickBreakStream);

            AudioInputStream paddleHitStream = AudioSystem.getAudioInputStream(new File("sounds/paddle_hit.mp3"));
            paddleHitClip = AudioSystem.getClip();
            paddleHitClip.open(paddleHitStream);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void playBrickBreakSound() {
        if (brickBreakClip != null) {
            brickBreakClip.stop();
            brickBreakClip.setFramePosition(0);
            brickBreakClip.start();
        }
    }

    private void playPaddleHitSound() {
        if (paddleHitClip != null) {
            paddleHitClip.stop();
            paddleHitClip.setFramePosition(0);
            paddleHitClip.start();
        }
    }

    private void move() {
        if (ballMoving) {
            ballX += ballSpeedX;
            ballY += ballSpeedY;

            if (ballX - BALL_RADIUS <= 0 || ballX + BALL_RADIUS >= WIDTH) {
                ballSpeedX *= -1;
            }

            if (ballY - BALL_RADIUS <= 0) {
                ballSpeedY *= -1;
            }

            if (ballY + BALL_RADIUS >= HEIGHT - PADDLE_HEIGHT &&
                    ballX >= paddleX && ballX <= paddleX + PADDLE_WIDTH) {
                ballSpeedY *= -1;
                playPaddleHitSound();
            }

            for (Brick brick : bricks) {
                if (!brick.isDestroyed() &&
                        ballX + BALL_RADIUS >= brick.getX() && ballX - BALL_RADIUS <= brick.getX() + BRICK_WIDTH &&
                        ballY + BALL_RADIUS >= brick.getY() && ballY - BALL_RADIUS <= brick.getY() + BRICK_HEIGHT) {
                    brick.setDestroyed(true);
                    ballSpeedY *= -1;
                    score += 10;
                    numBricksDestroyed++;
                    playBrickBreakSound();
                    spawnPowerUp(brick.getX() + BRICK_WIDTH / 2, brick.getY() + BRICK_HEIGHT / 2);

                    // Check if all bricks are destroyed
                    if (numBricksDestroyed == NUM_BRICKS_ROW * NUM_BRICKS_COLUMN) {
                        level++;
                        numBricksDestroyed = 0;
                        initializeBricks();
                        increaseDifficulty();
                    }
                }
            }

            // Power-up effects
            for (PowerUp powerUp : powerUps) {
                if (powerUp.isActive() &&
                        ballX + BALL_RADIUS >= powerUp.getX() && ballX - BALL_RADIUS <= powerUp.getX() + POWERUP_WIDTH
                        &&
                        ballY + BALL_RADIUS >= powerUp.getY()
                        && ballY - BALL_RADIUS <= powerUp.getY() + POWERUP_HEIGHT) {
                    applyPowerUpEffect(powerUp.getType());
                    powerUp.setActive(false);
                }
            }

            if (ballY + BALL_RADIUS >= HEIGHT) {
                // Game over
                ballMoving = false;
                int choice = JOptionPane.showConfirmDialog(this,
                        "Game Over!\nYour score: " + score + "\nDo you want to restart?", "Game Over",
                        JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    restartGame();
                } else {
                    System.exit(0);
                }
            }
        }

        // Continuous paddle movement
        if (leftPressed && paddleX > 0) {
            paddleX -= PADDLE_SPEED;
        }
        if (rightPressed && paddleX < WIDTH - PADDLE_WIDTH) {
            paddleX += PADDLE_SPEED;
        }
    }

    private void increaseDifficulty() {
        ballSpeedX += level;
        ballSpeedY -= level;
    }

    private void spawnPowerUp(double x, double y) {
        Random random = new Random();
        int chance = random.nextInt(100); // 1% chance of power-up spawn
        if (chance < 1) {
            PowerUpType type = PowerUpType.values()[random.nextInt(PowerUpType.values().length)];
            powerUps.add(new PowerUp(x, y, type));
        }
    }

    private void applyPowerUpEffect(PowerUpType type) {
        switch (type) {
            case EXPAND_PADDLE:
                PADDLE_WIDTH += 20;
                break;
            case SHRINK_PADDLE:
                PADDLE_WIDTH -= 20;
                break;
            case SLOW_DOWN_BALL:
                ballSpeedX /= 2; // Reduce ball speed by half
                ballSpeedY /= 2;
                break;
            // Add more power-up effects as needed
        }
    }

    private void restartGame() {
        score = 0;
        level = 1;
        numBricksDestroyed = 0;
        ballSpeedX = 5;
        ballSpeedY = -5;
        PADDLE_WIDTH = 100; // Reset paddle width
        initializeBricks();
        ballX = WIDTH / 2;
        ballY = HEIGHT / 2;
        ballMoving = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_SPACE && !ballMoving) {
            ballMoving = true;
        }
        if (keyCode == KeyEvent.VK_LEFT) {
            leftPressed = true;
        }
        if (keyCode == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_LEFT) {
            leftPressed = false;
        }
        if (keyCode == KeyEvent.VK_RIGHT) {
            rightPressed = false;
        }
    }

    private static class Brick {
        private double x;
        private double y;
        private Color color;
        private boolean destroyed;

        public Brick(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.destroyed = false;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public Color getColor() {
            return color;
        }

        public boolean isDestroyed() {
            return destroyed;
        }

        public void setDestroyed(boolean destroyed) {
            this.destroyed = destroyed;
        }
    }

    private static class PowerUp {
        private double x;
        private double y;
        private PowerUpType type;
        private boolean active;

        public PowerUp(double x, double y, PowerUpType type) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.active = true;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public PowerUpType getType() {
            return type;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

    private enum PowerUpType {
        EXPAND_PADDLE,
        SHRINK_PADDLE,
        SLOW_DOWN_BALL // New power-up type
        // Add more power-up types as needed
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new BrickBreakerGame().setVisible(true);
            }
        });
    }
}
