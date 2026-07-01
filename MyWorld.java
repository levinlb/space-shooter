import greenfoot.*;

/**
 * Die Spielwelt des Space Shooters.
 *
 * Ablauf: Gegner kommen in Wellen (Asteroiden + schiessende Angreifer). Ist eine
 * Welle geschafft, oeffnet sich der Upgrade-Shop (Spiel pausiert); mit Credits,
 * die Gegner fallen lassen, kauft man Verbesserungen. ENTER startet die naechste,
 * staerkere Welle. Bei 0 Herzen: Game Over (R = Neustart).
 *
 * Gezeichnet wird alles im Code: bewegtes Sternenfeld, Pixel-Art-HUD und Shop.
 */
public class MyWorld extends World
{
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;

    private static final int START_LIVES = 3;
    private static final int POWERUP_INTERVAL = 500;
    private static final int STAR_COUNT = 80;

    // Spielzustaende.
    private static final int PLAYING = 0;
    private static final int SHOP = 1;
    private static final int GAMEOVER = 2;

    // Upgrade-Obergrenzen.
    private static final int WEAPON_MAX = 5;
    private static final int ARMOR_MAX = 5;
    private static final int ENGINE_MAX = 5;
    private static final int TURRET_MAX = 3;

    private int gameState = PLAYING;
    private int wave = 0;
    private int enemiesToSpawn = 0;
    private int spawnTimer = 0;
    private int waveSpawnInterval = 70;
    private int powerUpCount = 0;

    private int score = 0;
    private int credits = 0;
    private int lives = START_LIVES;

    // Sternenfeld.
    private int[] starX = new int[STAR_COUNT];
    private int[] starY = new int[STAR_COUNT];
    private int[] starSpeed = new int[STAR_COUNT];

    public MyWorld()
    {
        super(WIDTH, HEIGHT, 1);
        initStars();
        addObject(new Spaceship(), 60, HEIGHT / 2);
        startWave(1);
        drawScene();
    }

    @Override
    public void act()
    {
        drawScene();

        switch (gameState)
        {
            case PLAYING:  updatePlaying(); break;
            case SHOP:     updateShop();    break;
            case GAMEOVER:
                if (Greenfoot.isKeyDown("r")) Greenfoot.setWorld(new MyWorld());
                break;
        }
    }

    // ---- Wellen ----

    private void startWave(int n)
    {
        wave = n;
        enemiesToSpawn = 4 + n * 2;
        waveSpawnInterval = Math.max(25, 70 - n * 3);
        spawnTimer = 0;
        gameState = PLAYING;
    }

    private void updatePlaying()
    {
        // Gegner der Welle nach und nach spawnen.
        if (enemiesToSpawn > 0)
        {
            spawnTimer++;
            if (spawnTimer >= waveSpawnInterval)
            {
                spawnTimer = 0;
                spawnWaveEnemy();
                enemiesToSpawn--;
            }
        }

        // Gelegentlich ein Power-up.
        powerUpCount++;
        if (powerUpCount >= POWERUP_INTERVAL)
        {
            powerUpCount = 0;
            spawnPowerUp();
        }

        // Welle geschafft? (nichts mehr zu spawnen, keine Gegner/Projektile mehr)
        if (enemiesToSpawn == 0
            && getObjects(Enemy.class).isEmpty()
            && getObjects(EnemyBullet.class).isEmpty())
        {
            enterShop();
        }
    }

    private void spawnWaveEnemy()
    {
        int attackerChance = Math.min(65, 15 + wave * 6); // Prozent
        int y = 30 + Greenfoot.getRandomNumber(HEIGHT - 60);
        if (Greenfoot.getRandomNumber(100) < attackerChance)
        {
            addObject(new Attacker(), WIDTH - 20, y);
        }
        else
        {
            addObject(new Alien(), WIDTH - 20, y);
        }
    }

    private void spawnPowerUp()
    {
        int type = Greenfoot.getRandomNumber(PowerUp.TYPE_COUNT);
        int y = 30 + Greenfoot.getRandomNumber(HEIGHT - 60);
        addObject(new PowerUp(type), WIDTH - 20, y);
    }

    // ---- Shop ----

    private void enterShop()
    {
        gameState = SHOP;
        // Reste aufraeumen, damit die naechste Welle sauber startet.
        removeObjects(getObjects(EnemyBullet.class));
        removeObjects(getObjects(Bullet.class));
        removeObjects(getObjects(PowerUp.class));
    }

    private void updateShop()
    {
        String key = Greenfoot.getKey();
        if (key == null) return;

        if (key.equals("1")) buyWeapon();
        else if (key.equals("2")) buyArmor();
        else if (key.equals("3")) buyEngine();
        else if (key.equals("4")) buyTurret();
        else if (key.equals("enter") || key.equals("space")) startWave(wave + 1);
    }

    private int weaponCost(int lvl) { return 30 + lvl * 25; }
    private int armorCost(int lvl)  { return 30 + lvl * 25; }
    private int engineCost(int lvl) { return 25 + lvl * 20; }
    private int turretCost(int lvl) { return 50 + lvl * 40; }

    private void buyWeapon()
    {
        Spaceship ship = getShip();
        if (ship == null || ship.getWeaponLevel() >= WEAPON_MAX) return;
        int cost = weaponCost(ship.getWeaponLevel());
        if (credits < cost) return;
        credits -= cost;
        ship.upgradeWeapon();
    }

    private void buyArmor()
    {
        Spaceship ship = getShip();
        if (ship == null || ship.getArmorLevel() >= ARMOR_MAX) return;
        int cost = armorCost(ship.getArmorLevel());
        if (credits < cost) return;
        credits -= cost;
        ship.upgradeArmor();
        lives++; // Panzerung gibt zusaetzlich ein Herz
    }

    private void buyEngine()
    {
        Spaceship ship = getShip();
        if (ship == null || ship.getEngineLevel() >= ENGINE_MAX) return;
        int cost = engineCost(ship.getEngineLevel());
        if (credits < cost) return;
        credits -= cost;
        ship.upgradeEngine();
    }

    private void buyTurret()
    {
        Spaceship ship = getShip();
        if (ship == null || ship.getTurretLevel() >= TURRET_MAX) return;
        int cost = turretCost(ship.getTurretLevel());
        if (credits < cost) return;
        credits -= cost;
        ship.upgradeTurret();
        if (ship.getTurretLevel() == 1)
        {
            addObject(new Turret(ship), ship.getX() - 6, ship.getY() - 24);
        }
    }

    // ---- Spielereignisse ----

    public void addScore(int points) { score += points; }
    public void addCredits(int amount) { credits += amount; }
    public void addLife() { lives++; }

    public void loseLife()
    {
        lives--;
        if (lives <= 0)
        {
            lives = 0;
            endGame();
        }
    }

    /** Bildschirm-Bombe: alle Gegner zerstoeren (mit Belohnung). */
    public void bombEnemies()
    {
        for (Object o : getObjects(Enemy.class))
        {
            ((Enemy) o).die();
        }
    }

    public boolean isPlaying() { return gameState == PLAYING; }
    public boolean isGameOver() { return gameState == GAMEOVER; }

    public Spaceship getShip()
    {
        java.util.List<Spaceship> ships = getObjects(Spaceship.class);
        return ships.isEmpty() ? null : ships.get(0);
    }

    private void endGame()
    {
        gameState = GAMEOVER;
        removeObjects(getObjects(Enemy.class));
        removeObjects(getObjects(EnemyBullet.class));
        removeObjects(getObjects(Bullet.class));
        removeObjects(getObjects(PowerUp.class));
        removeObjects(getObjects(Turret.class));
        removeObjects(getObjects(Spaceship.class));
    }

    // ---- Zeichnen ----

    private void initStars()
    {
        for (int i = 0; i < STAR_COUNT; i++)
        {
            starX[i] = Greenfoot.getRandomNumber(WIDTH);
            starY[i] = Greenfoot.getRandomNumber(HEIGHT);
            starSpeed[i] = 1 + Greenfoot.getRandomNumber(5);
        }
    }

    private void drawScene()
    {
        GreenfootImage bg = getBackground();
        bg.setColor(new Color(0, 0, 0));
        bg.fill();

        for (int i = 0; i < STAR_COUNT; i++)
        {
            int v = 80 + starSpeed[i] * 35;
            if (v > 255) v = 255;
            bg.setColor(new Color(v, v, v));
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

        if (gameState == SHOP) drawShop(bg);
        else if (gameState == GAMEOVER) drawGameOver(bg);
    }

    // Pixel-Art-Herz (7x6 Raster).
    private static final int[][] HEART = {
        {0, 1, 1, 0, 1, 1, 0},
        {1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1},
        {0, 1, 1, 1, 1, 1, 0},
        {0, 0, 1, 1, 1, 0, 0},
        {0, 0, 0, 1, 0, 0, 0}
    };

    private void drawHud(GreenfootImage bg)
    {
        Spaceship ship = getShip();

        // Punkte (oben links).
        drawPanel(bg, 8, 8, 170, 30);
        bg.setColor(new Color(250, 210, 60));
        bg.fillOval(16, 15, 16, 16);
        bg.setColor(new Color(180, 130, 20));
        bg.drawOval(16, 15, 16, 16);
        bg.setColor(new Color(255, 245, 190));
        bg.setFont(new Font("Monospaced", true, false, 16));
        bg.drawString(String.format("%06d", score), 40, 29);

        // Credits.
        drawPanel(bg, 8, 42, 170, 22);
        bg.setColor(new Color(120, 255, 170));
        bg.setFont(new Font("Monospaced", true, false, 13));
        bg.drawString("CREDITS: " + credits, 14, 58);

        // Boost-Leiste.
        drawPanel(bg, 8, 68, 170, 22);
        bg.setColor(new Color(120, 220, 255));
        bg.setFont(new Font("Monospaced", true, false, 11));
        bg.drawString("BOOST", 14, 83);
        double bfrac = (ship != null) ? ship.getBoostFraction() : 0;
        Color bcol = (ship != null && ship.isBoosting()) ? new Color(255, 120, 60)
                    : (bfrac >= 1.0) ? new Color(90, 240, 120) : new Color(80, 180, 240);
        drawBar(bg, 70, 72, 100, 14, bfrac, bcol);

        // Schild-Leiste.
        drawPanel(bg, 8, 94, 170, 22);
        bg.setColor(new Color(180, 210, 255));
        bg.setFont(new Font("Monospaced", true, false, 11));
        bg.drawString("SCHILD", 14, 109);
        double sfrac = (ship != null) ? ship.getShieldFraction() : 0;
        Color scol = (sfrac > 0.5) ? new Color(90, 200, 255)
                   : (sfrac > 0.2) ? new Color(240, 210, 70) : new Color(240, 80, 80);
        drawBar(bg, 70, 98, 100, 14, sfrac, scol);

        // Welle (oben mittig).
        drawPanel(bg, WIDTH / 2 - 55, 8, 110, 26);
        bg.setColor(new Color(255, 255, 255));
        bg.setFont(new Font("Monospaced", true, false, 15));
        bg.drawString("WELLE " + wave, WIDTH / 2 - 40, 26);

        // Leben als Herzen (oben rechts).
        int hearts = Math.min(lives, 6);
        int hx = WIDTH - 12 - hearts * 24;
        for (int i = 0; i < hearts; i++)
        {
            drawPixelHeart(bg, hx + i * 24, 12, 3);
        }
        if (lives > 6)
        {
            bg.setColor(new Color(255, 255, 255));
            bg.setFont(new Font("Monospaced", true, false, 13));
            bg.drawString("x" + lives, WIDTH - 40, 42);
        }

        // Schnellfeuer-Anzeige.
        if (ship != null && ship.isRapidFireActive())
        {
            drawPanel(bg, WIDTH / 2 - 72, 40, 144, 22);
            bg.setColor(new Color(250, 220, 40));
            bg.setFont(new Font("Monospaced", true, false, 12));
            bg.drawString("SCHNELLFEUER!", WIDTH / 2 - 58, 56);
        }
    }

    private void drawShop(GreenfootImage bg)
    {
        int pw = 440, ph = 250;
        int px = WIDTH / 2 - pw / 2;
        int py = HEIGHT / 2 - ph / 2 + 10;
        drawPanel(bg, px, py, pw, ph);

        bg.setColor(new Color(255, 230, 120));
        bg.setFont(new Font("Monospaced", true, false, 24));
        bg.drawString("UPGRADE-SHOP", px + 110, py + 30);

        bg.setColor(new Color(120, 255, 170));
        bg.setFont(new Font("Monospaced", true, false, 15));
        bg.drawString("Credits: " + credits, px + 20, py + 55);

        Spaceship ship = getShip();
        int wl = ship != null ? ship.getWeaponLevel() : 0;
        int al = ship != null ? ship.getArmorLevel() : 0;
        int el = ship != null ? ship.getEngineLevel() : 0;
        int tl = ship != null ? ship.getTurretLevel() : 0;

        drawShopRow(bg, px + 20, py + 85, "[1] Waffe (Feuer+Schaden)", wl, WEAPON_MAX, weaponCost(wl));
        drawShopRow(bg, px + 20, py + 115, "[2] Panzerung (Schild+Herz)", al, ARMOR_MAX, armorCost(al));
        drawShopRow(bg, px + 20, py + 145, "[3] Antrieb (Tempo+Boost)", el, ENGINE_MAX, engineCost(el));
        drawShopRow(bg, px + 20, py + 175, "[4] Turret (Begleiter)", tl, TURRET_MAX, turretCost(tl));

        bg.setColor(new Color(180, 220, 255));
        bg.setFont(new Font("Monospaced", true, false, 13));
        bg.drawString("1-4: Kaufen    ENTER: naechste Welle", px + 20, py + 225);
    }

    private void drawShopRow(GreenfootImage bg, int x, int y, String name, int level, int max, int cost)
    {
        boolean maxed = level >= max;
        boolean affordable = !maxed && credits >= cost;

        bg.setColor(new Color(235, 235, 245));
        bg.setFont(new Font("Monospaced", true, false, 14));
        bg.drawString(name, x, y);

        bg.setColor(new Color(160, 200, 255));
        bg.drawString("Lv " + level + "/" + max, x + 250, y);

        if (maxed)
        {
            bg.setColor(new Color(120, 200, 255));
            bg.drawString("MAX", x + 330, y);
        }
        else
        {
            bg.setColor(affordable ? new Color(120, 240, 140) : new Color(220, 110, 110));
            bg.drawString(cost + " C", x + 330, y);
        }
    }

    private void drawGameOver(GreenfootImage bg)
    {
        int pw = 360, ph = 160;
        int px = WIDTH / 2 - pw / 2;
        int py = HEIGHT / 2 - ph / 2;
        drawPanel(bg, px, py, pw, ph);

        bg.setColor(new Color(255, 80, 80));
        bg.setFont(new Font("Monospaced", true, false, 40));
        bg.drawString("GAME OVER", WIDTH / 2 - 118, HEIGHT / 2 - 25);

        bg.setColor(new Color(255, 255, 255));
        bg.setFont(new Font("Monospaced", true, false, 16));
        bg.drawString("Welle " + wave + "  -  Punkte: " + String.format("%06d", score),
                      WIDTH / 2 - 130, HEIGHT / 2 + 12);

        bg.setColor(new Color(180, 220, 255));
        bg.setFont(new Font("Monospaced", true, false, 14));
        bg.drawString("Druecke R fuer Neustart", WIDTH / 2 - 92, HEIGHT / 2 + 42);
    }

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
        bg.setColor(new Color(255, 150, 170));
        bg.fillRect(x + scale, y + scale, scale, scale);
    }
}
