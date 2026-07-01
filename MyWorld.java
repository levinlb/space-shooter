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

    private void drawHud(GreenfootImage bg)
    {
        bg.setColor(new Color(255, 255, 255));
        bg.setFont(new Font(true, false, 18));
        bg.drawString("Punkte: " + score, 12, 24);
        bg.drawString("Leben: " + lives, WIDTH - 110, 24);

        if (isRapidFireActive())
        {
            bg.setColor(new Color(240, 200, 30));
            bg.setFont(new Font(true, false, 14));
            bg.drawString("SCHNELLFEUER!", WIDTH / 2 - 55, 24);
        }
    }

    private void drawGameOver(GreenfootImage bg)
    {
        bg.setColor(new Color(255, 255, 255));
        bg.setFont(new Font(true, false, 44));
        bg.drawString("GAME OVER", WIDTH / 2 - 130, HEIGHT / 2 - 20);
        bg.setFont(new Font(false, false, 22));
        bg.drawString("Punkte: " + score, WIDTH / 2 - 55, HEIGHT / 2 + 20);
        bg.drawString("Druecke R fuer Neustart", WIDTH / 2 - 110, HEIGHT / 2 + 55);
    }

    private boolean isRapidFireActive()
    {
        java.util.List<Spaceship> ships = getObjects(Spaceship.class);
        return !ships.isEmpty() && ships.get(0).isRapidFireActive();
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
