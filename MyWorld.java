import greenfoot.*;

/**
 * Die Spielwelt des Space Shooters. Steuert das Spawnen der Aliens,
 * verwaltet Punkte und Leben, zeichnet das HUD und den Game-Over-Screen.
 */
public class MyWorld extends World
{
    private static final int START_LIVES = 3;
    private static final int START_SPAWN_INTERVAL = 100; // ~2 Sekunden bei speed=50
    private static final int MIN_SPAWN_INTERVAL = 30;    // schnellstes Spawn-Tempo
    private static final int DIFFICULTY_STEP = 250;      // alle N Acts schwerer

    private int actCount = 0;
    private int difficultyCount = 0;
    private int spawnInterval = START_SPAWN_INTERVAL;

    private int score = 0;
    private int lives = START_LIVES;
    private boolean gameOver = false;

    public MyWorld()
    {
        super(600, 400, 1);
        addObject(new Spaceship(), 50, 200);
        updateHud();
    }

    @Override
    public void act()
    {
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

        actCount++;
        if (actCount >= spawnInterval)
        {
            spawnAlien();
            actCount = 0;
        }
    }

    private void spawnAlien()
    {
        int randomX = 450 + Greenfoot.getRandomNumber(600 - 450 + 1);
        int randomY = Greenfoot.getRandomNumber(400);
        addObject(new Alien(), randomX, randomY);
    }

    /**
     * Erhöht den Punktestand (Aufruf durch Bullet beim Treffer).
     */
    public void addScore(int points)
    {
        score += points;
        updateHud();
    }

    /**
     * Zieht ein Leben ab (Aufruf durch Alien, das links entkommt).
     * Bei 0 Leben endet das Spiel.
     */
    public void loseLife()
    {
        lives--;
        updateHud();
        if (lives <= 0)
        {
            lives = 0;
            endGame();
        }
    }

    public boolean isGameOver()
    {
        return gameOver;
    }

    /**
     * Aktualisiert die HUD-Anzeige (Punkte oben links, Leben oben rechts).
     */
    private void updateHud()
    {
        showText("Punkte: " + score, 70, 20);
        showText("Leben: " + lives, 530, 20);
    }

    /**
     * Beendet das Spiel: entfernt alle Objekte und zeigt den Game-Over-Screen.
     */
    private void endGame()
    {
        gameOver = true;

        removeObjects(getObjects(Alien.class));
        removeObjects(getObjects(Bullet.class));
        removeObjects(getObjects(Spaceship.class));

        showText("GAME OVER", 300, 160);
        showText("Punkte: " + score, 300, 200);
        showText("Druecke R fuer Neustart", 300, 240);
    }
}
