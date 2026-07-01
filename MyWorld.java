import greenfoot.*;

/**
 * Die Spielwelt des Space Shooters. Steuert das Spawnen von Aliens und
 * Power-ups, verwaltet Punkte und Leben, zeichnet ein bewegtes Sternenfeld
 * als Hintergrund (Geschwindigkeitsgefuehl), das HUD und den Game-Over-Screen.
 */
public class MyWorld extends World
{
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;

    private static final int START_LIVES = 3;
    private static final int START_SPAWN_INTERVAL = 100; // ~2 Sekunden bei speed=50
    private static final int MIN_SPAWN_INTERVAL = 30;    // schnellstes Spawn-Tempo
    private static final int DIFFICULTY_STEP = 250;      // alle N Acts schwerer
    private static final int POWERUP_INTERVAL = 450;     // ~9s bei speed=50

    private static final int STAR_COUNT = 80;

    private int actCount = 0;
    private int difficultyCount = 0;
    private int powerUpCount = 0;
    private int spawnInterval = START_SPAWN_INTERVAL;

    private int score = 0;
    private int lives = START_LIVES;
    private boolean gameOver = false;

    // Sternenfeld fuer den bewegten Hintergrund.
    private int[] starX = new int[STAR_COUNT];
    private int[] starY = new int[STAR_COUNT];
    private int[] starSpeed = new int[STAR_COUNT];

    public MyWorld()
    {
        super(WIDTH, HEIGHT, 1);
        initStars();
        addObject(new Spaceship(), 50, HEIGHT / 2);
        drawScene();
    }

    @Override
    public void act()
    {
        drawScene();

        if (gameOver)
        {
            // Im Game-Over-Zustand nur auf Neustart warten.
            if (Greenfoot.isKeyDown("r"))
            {
                Greenfoot.setWorld(new MyWorld());
            }
            return;
        }

        // Steigende Schwierigkeit: Spawn-Intervall über die Zeit verkürzen.
        difficultyCount++;
        if (difficultyCount >= DIFFICULTY_STEP)
        {
            difficultyCount = 0;
            if (spawnInterval > MIN_SPAWN_INTERVAL)
            {
                spawnInterval -= 5;
            }
        }

        // Aliens spawnen.
        actCount++;
        if (actCount >= spawnInterval)
        {
            spawnAlien();
            actCount = 0;
        }

        // Power-ups spawnen.
        powerUpCount++;
        if (powerUpCount >= POWERUP_INTERVAL)
        {
            spawnPowerUp();
            powerUpCount = 0;
        }
    }

    private void initStars()
    {
        for (int i = 0; i < STAR_COUNT; i++)
        {
            starX[i] = Greenfoot.getRandomNumber(WIDTH);
            starY[i] = Greenfoot.getRandomNumber(HEIGHT);
            starSpeed[i] = 1 + Greenfoot.getRandomNumber(5); // 1..5
        }
    }

    /**
     * Zeichnet den kompletten Hintergrund neu: schwarzes All, scrollende
     * Sterne (als Streifen -> Geschwindigkeit), HUD und ggf. Game-Over.
     */
    private void drawScene()
    {
        GreenfootImage bg = getBackground();
        bg.setColor(new Color(0, 0, 0));
        bg.fill();

        for (int i = 0; i < STAR_COUNT; i++)
        {
            int v = 80 + starSpeed[i] * 35; // schneller = heller
            if (v > 255) v = 255;
            bg.setColor(new Color(v, v, v));
            // Horizontaler Streifen simuliert Bewegungsunschaerfe/Speed.
            bg.drawLine(starX[i], starY[i], starX[i] + starSpeed[i] * 2, starY[i]);

            starX[i] -= starSpeed[i];
            if (starX[i] < 0)
            {
                starX[i] = WIDTH;
                starY[i] = Greenfoot.getRandomNumber(HEIGHT);
                starSpeed[i] = 1 + Greenfoot.getRandomNumber(5);
            }
        }

        drawHud(bg);

        if (gameOver)
        {
            drawGameOver(bg);
        }
    }

    // Pixel-Art-Herz (7x6 Raster) fuer die Lebensanzeige.
    private static final int[][] HEART = {
        {0, 1, 1, 0, 1, 1, 0},
        {1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1},
        {0, 1, 1, 1, 1, 1, 0},
        {0, 0, 1, 1, 1, 0, 0},
        {0, 0, 0, 1, 0, 0, 0}
    };

    /**
     * Zeichnet das HUD im Pixel-Art-Stil: Punkte-Panel mit Muenze, Boost-Leiste
     * und Herzen fuer die Leben. Wird jeden Frame auf den Hintergrund gezeichnet.
     */
    private void drawHud(GreenfootImage bg)
    {
        Spaceship ship = getShip();

        // --- Punkte-Panel (oben links) ---
        drawPanel(bg, 8, 8, 170, 30);
        bg.setColor(new Color(250, 210, 60));      // Muenze
        bg.fillOval(16, 15, 16, 16);
        bg.setColor(new Color(180, 130, 20));
        bg.drawOval(16, 15, 16, 16);
        bg.setColor(new Color(255, 245, 190));
        bg.setFont(new Font("Monospaced", true, false, 16));
        bg.drawString(String.format("%06d", score), 40, 29);

        // --- Boost-Panel (darunter) ---
        drawPanel(bg, 8, 42, 170, 24);
        bg.setColor(new Color(120, 220, 255));
        bg.setFont(new Font("Monospaced", true, false, 12));
        bg.drawString("BOOST", 14, 59);
        double frac = (ship != null) ? ship.getBoostFraction() : 0;
        boolean ready = frac >= 1.0;
        Color barColor = (ship != null && ship.isBoosting()) ? new Color(255, 120, 60)
                        : ready ? new Color(90, 240, 120)
                                : new Color(80, 180, 240);
        drawBar(bg, 66, 47, 104, 14, frac, barColor);

        // --- Leben als Pixel-Herzen (oben rechts) ---
        int hearts = Math.min(lives, 6);
        int hx = WIDTH - 12 - hearts * 24;
        for (int i = 0; i < hearts; i++)
        {
            drawPixelHeart(bg, hx + i * 24, 12, 3);
        }
        if (lives > 6)
        {
            bg.setColor(new Color(255, 255, 255));
            bg.setFont(new Font("Monospaced", true, false, 14));
            bg.drawString("x" + lives, WIDTH - 40, 40);
        }

        // --- Schnellfeuer-Anzeige (oben mittig) ---
        if (ship != null && ship.isRapidFireActive())
        {
            drawPanel(bg, WIDTH / 2 - 72, 8, 144, 24);
            bg.setColor(new Color(250, 220, 40));
            bg.setFont(new Font("Monospaced", true, false, 13));
            bg.drawString("SCHNELLFEUER!", WIDTH / 2 - 60, 25);
        }
    }

    private void drawGameOver(GreenfootImage bg)
    {
        int pw = 340, ph = 150;
        int px = WIDTH / 2 - pw / 2;
        int py = HEIGHT / 2 - ph / 2;
        drawPanel(bg, px, py, pw, ph);

        bg.setColor(new Color(255, 80, 80));
        bg.setFont(new Font("Monospaced", true, false, 40));
        bg.drawString("GAME OVER", WIDTH / 2 - 118, HEIGHT / 2 - 20);

        bg.setColor(new Color(255, 255, 255));
        bg.setFont(new Font("Monospaced", true, false, 18));
        bg.drawString("Punkte: " + String.format("%06d", score), WIDTH / 2 - 70, HEIGHT / 2 + 18);

        bg.setColor(new Color(180, 220, 255));
        bg.setFont(new Font("Monospaced", true, false, 14));
        bg.drawString("Druecke R fuer Neustart", WIDTH / 2 - 92, HEIGHT / 2 + 48);
    }

    private Spaceship getShip()
    {
        java.util.List<Spaceship> ships = getObjects(Spaceship.class);
        return ships.isEmpty() ? null : ships.get(0);
    }

    /**
     * Zeichnet ein halbtransparentes Pixel-Panel mit chunkigem Rahmen und
     * hellen Eck-Pixeln (Retro-Look).
     */
    private void drawPanel(GreenfootImage bg, int x, int y, int w, int h)
    {
        bg.setColor(new Color(15, 20, 45, 210));
        bg.fillRect(x, y, w, h);
        bg.setColor(new Color(90, 130, 220));
        bg.drawRect(x, y, w - 1, h - 1);
        bg.drawRect(x + 1, y + 1, w - 3, h - 3);
        bg.setColor(new Color(180, 220, 255));
        bg.fillRect(x, y, 2, 2);
        bg.fillRect(x + w - 2, y, 2, 2);
        bg.fillRect(x, y + h - 2, 2, 2);
        bg.fillRect(x + w - 2, y + h - 2, 2, 2);
    }

    /**
     * Zeichnet eine segmentierte Pixel-Fortschrittsleiste (0..1).
     */
    private void drawBar(GreenfootImage bg, int x, int y, int w, int h, double frac, Color color)
    {
        if (frac < 0) frac = 0;
        if (frac > 1) frac = 1;

        bg.setColor(new Color(10, 15, 30));
        bg.fillRect(x, y, w, h);
        bg.setColor(new Color(70, 90, 140));
        bg.drawRect(x, y, w - 1, h - 1);

        int segs = 10;
        int filled = (int) Math.round(frac * segs);
        int segW = (w - 4) / segs;
        for (int i = 0; i < segs; i++)
        {
            bg.setColor(i < filled ? color : new Color(30, 40, 70));
            bg.fillRect(x + 2 + i * segW, y + 2, segW - 1, h - 4);
        }
    }

    /**
     * Zeichnet ein rotes Pixel-Herz anhand des HEART-Rasters.
     */
    private void drawPixelHeart(GreenfootImage bg, int x, int y, int scale)
    {
        for (int r = 0; r < HEART.length; r++)
        {
            for (int c = 0; c < HEART[r].length; c++)
            {
                if (HEART[r][c] == 1)
                {
                    bg.setColor(new Color(225, 45, 65));
                    bg.fillRect(x + c * scale, y + r * scale, scale, scale);
                }
            }
        }
        // Glanzpunkt fuer den Pixel-Look.
        bg.setColor(new Color(255, 150, 170));
        bg.fillRect(x + scale, y + scale, scale, scale);
    }

    private void spawnAlien()
    {
        int randomX = 450 + Greenfoot.getRandomNumber(WIDTH - 450 + 1);
        int randomY = Greenfoot.getRandomNumber(HEIGHT);
        addObject(new Alien(), randomX, randomY);
    }

    private void spawnPowerUp()
    {
        int type = Greenfoot.getRandomNumber(PowerUp.TYPE_COUNT);
        int y = 30 + Greenfoot.getRandomNumber(HEIGHT - 60);
        addObject(new PowerUp(type), WIDTH - 20, y);
    }

    /**
     * Erhöht den Punktestand (Aufruf durch Bullet oder Punkte-Power-up).
     */
    public void addScore(int points)
    {
        score += points;
    }

    /**
     * Gibt ein zusätzliches Leben (Extra-Leben-Power-up).
     */
    public void addLife()
    {
        lives++;
    }

    /**
     * Zieht ein Leben ab (Alien ist links entkommen). Bei 0 Leben endet das Spiel.
     */
    public void loseLife()
    {
        lives--;
        if (lives <= 0)
        {
            lives = 0;
            endGame();
        }
    }

    /**
     * Bildschirm-Bombe: vernichtet alle Aliens und schreibt Punkte gut.
     */
    public void bombAliens()
    {
        java.util.List<Alien> aliens = getObjects(Alien.class);
        addScore(aliens.size() * 10);
        removeObjects(aliens);
    }

    public boolean isGameOver()
    {
        return gameOver;
    }

    /**
     * Beendet das Spiel: entfernt alle Objekte. Der Game-Over-Screen wird in
     * drawScene() gezeichnet.
     */
    private void endGame()
    {
        gameOver = true;
        removeObjects(getObjects(Alien.class));
        removeObjects(getObjects(Bullet.class));
        removeObjects(getObjects(PowerUp.class));
        removeObjects(getObjects(Spaceship.class));
    }
}
