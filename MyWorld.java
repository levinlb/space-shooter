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
    private static final int DRAFT = 3;     // Waffenwahl nach einem Boss

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

    private boolean bossWave = false;

    // Waffenwahl (Draft).
    private final int[] draftChoices = new int[3];

    // Bildschirm-Blitz (Effekt).
    private Color flashColor = null;
    private int flashTimer = 0;
    private int flashMax = 1;

    // Sternenfeld.
    private int[] starX = new int[STAR_COUNT];
    private int[] starY = new int[STAR_COUNT];
    private int[] starSpeed = new int[STAR_COUNT];

    public MyWorld()
    {
        super(WIDTH, HEIGHT, 1);
        setPaintOrder(PowerUp.class, Spaceship.class, Turret.class, Drone.class,
                      Boss.class, Enemy.class, Missile.class, Bullet.class,
                      EnemyBullet.class, LaserBeam.class, Explosion.class, Particle.class);
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
            case DRAFT:    updateDraft();   break;
            case GAMEOVER:
                if (Greenfoot.isKeyDown("r")) Greenfoot.setWorld(new MyWorld());
                break;
        }
    }

    // ---- Wellen ----

    private void startWave(int n)
    {
        wave = n;
        spawnTimer = 0;
        gameState = PLAYING;

        if (n % 5 == 0)
        {
            // Boss-Welle: nur der Boss (er kann selbst Begleiter rufen).
            bossWave = true;
            enemiesToSpawn = 0;
            addObject(new Boss(n / 5), getWidth() - 60, HEIGHT / 2);
        }
        else
        {
            bossWave = false;
            enemiesToSpawn = 4 + n * 2;
            waveSpawnInterval = Math.max(25, 70 - n * 3);
        }
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

        // Gelegentlich ein Power-up (nicht waehrend eines Boss-Kampfes).
        if (!bossWave)
        {
            powerUpCount++;
            if (powerUpCount >= POWERUP_INTERVAL)
            {
                powerUpCount = 0;
                spawnPowerUp();
            }
        }

        // Welle geschafft? (nichts mehr zu spawnen, keine Gegner/Projektile mehr)
        if (enemiesToSpawn == 0
            && getObjects(Enemy.class).isEmpty()
            && getObjects(EnemyBullet.class).isEmpty())
        {
            if (bossWave) enterDraft();
            else enterShop();
        }
    }

    private void spawnWaveEnemy()
    {
        int y = 30 + Greenfoot.getRandomNumber(HEIGHT - 60);
        addObject(createWaveEnemy(), WIDTH - 20, y);
    }

    /**
     * Waehlt einen Gegnertyp gewichtet nach der aktuellen Welle. Fruehe Wellen
     * bestehen aus Asteroiden und Jaegern; spaeter kommen Drohnen, Rammer,
     * Teiler und schwere Bomber dazu.
     */
    private Enemy createWaveEnemy()
    {
        int w = wave;
        int[] weights = {
            Math.max(12, 45 - w * 3),          // Alien
            Math.min(40, 18 + w * 2),          // Attacker
            (w >= 2) ? 16 : 0,                 // Drohne
            (w >= 3) ? 12 : 0,                 // Rammer
            (w >= 3) ? 12 : 0,                 // Teiler
            (w >= 4) ? 10 + (w - 4) : 0        // Bomber
        };

        int total = 0;
        for (int weight : weights) total += weight;
        int roll = Greenfoot.getRandomNumber(Math.max(1, total));

        int acc = 0;
        for (int i = 0; i < weights.length; i++)
        {
            acc += weights[i];
            if (roll < acc)
            {
                switch (i)
                {
                    case 0: return new Alien();
                    case 1: return new Attacker();
                    case 2: return new Drohne();
                    case 3: return new Rammer();
                    case 4: return new Teiler();
                    case 5: return new Bomber();
                }
            }
        }
        return new Alien();
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
        clearProjectiles();
    }

    /** Entfernt alle fliegenden Projektile/Effekte (Drohnen bleiben erhalten). */
    private void clearProjectiles()
    {
        removeObjects(getObjects(EnemyBullet.class));
        removeObjects(getObjects(Bullet.class));
        removeObjects(getObjects(Missile.class));
        removeObjects(getObjects(LaserBeam.class));
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

    // ---- Waffenwahl (Draft, Brotato-Stil) ----

    private void enterDraft()
    {
        gameState = DRAFT;
        clearProjectiles();

        // Drei verschiedene Waffen zufaellig auslosen.
        boolean[] used = new boolean[Spaceship.WEAPON_KINDS];
        for (int i = 0; i < 3; i++)
        {
            int t;
            do { t = Greenfoot.getRandomNumber(Spaceship.WEAPON_KINDS); }
            while (used[t]);
            used[t] = true;
            draftChoices[i] = t;
        }
    }

    private void updateDraft()
    {
        String key = Greenfoot.getKey();
        if (key == null) return;

        int pick = -1;
        if (key.equals("1")) pick = 0;
        else if (key.equals("2")) pick = 1;
        else if (key.equals("3")) pick = 2;
        if (pick < 0) return;

        applyDraftPick(draftChoices[pick]);
        enterShop();     // nach der Waffenwahl in den normalen Upgrade-Shop
    }

    private void applyDraftPick(int type)
    {
        Spaceship ship = getShip();
        if (ship == null) return;
        ship.addOrLevelWeapon(type);
        if (type == Spaceship.W_DRONE) syncDrones(ship);
        screenFlash(new Color(120, 240, 180), 60);
    }

    /** Passt die Anzahl umkreisender Drohnen an das Drohnen-Level an (max 4). */
    private void syncDrones(Spaceship ship)
    {
        removeObjects(getObjects(Drone.class));
        int count = Math.min(4, ship.getDroneLevel());
        for (int i = 0; i < count; i++)
        {
            addObject(new Drone(ship, i, count), ship.getX(), ship.getY() - 30);
        }
    }

    // ---- Effekte ----

    /** Loest die Nova-Bombe aus: Flaechenschaden, raeumt Projektile, Schockwelle. */
    public void novaBlast(int cx, int cy, int damage, int radius)
    {
        for (Object o : getObjects(Enemy.class))
        {
            Enemy e = (Enemy) o;
            int dx = e.getX() - cx, dy = e.getY() - cy;
            if (dx * dx + dy * dy <= radius * radius)
            {
                e.hit(damage);
            }
        }
        // Feindliche Projektile im Umkreis wegraeumen.
        for (Object o : getObjects(EnemyBullet.class))
        {
            EnemyBullet b = (EnemyBullet) o;
            int dx = b.getX() - cx, dy = b.getY() - cy;
            if (dx * dx + dy * dy <= (radius + 40) * (radius + 40))
            {
                removeObject(b);
            }
        }
        addObject(new Explosion(radius * 2, new Color(120, 220, 255), 30), cx, cy);
        screenFlash(new Color(150, 230, 255), 120);
    }

    /** Legt einen kurz aufleuchtenden, farbigen Vollbild-Blitz an. */
    public void screenFlash(Color color, int strength)
    {
        this.flashColor = color;
        this.flashTimer = strength;
        this.flashMax = Math.max(1, strength);
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

    /** Bildschirm-Bombe: normale Gegner zerstoeren, einen Boss stark treffen. */
    public void bombEnemies()
    {
        for (Object o : getObjects(Enemy.class))
        {
            Enemy e = (Enemy) o;
            if (e instanceof Boss) e.hit(60);   // Boss nicht sofort ausschalten
            else e.die();
        }
        screenFlash(new Color(255, 200, 120), 90);
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
        removeObjects(getObjects(Missile.class));
        removeObjects(getObjects(LaserBeam.class));
        removeObjects(getObjects(PowerUp.class));
        removeObjects(getObjects(Turret.class));
        removeObjects(getObjects(Drone.class));
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

        drawFlash(bg);
        drawHud(bg);

        if (gameState == SHOP) drawShop(bg);
        else if (gameState == DRAFT) drawDraft(bg);
        else if (gameState == GAMEOVER) drawGameOver(bg);
    }

    private void drawFlash(GreenfootImage bg)
    {
        if (flashTimer <= 0 || flashColor == null) return;
        int alpha = (int) (110.0 * flashTimer / flashMax);
        if (alpha < 0) alpha = 0;
        bg.setColor(new Color(flashColor.r, flashColor.g, flashColor.b, alpha));
        bg.fill();
        flashTimer--;
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

        drawBossBar(bg);
        drawWeaponInventory(bg, ship);
    }

    private void drawBossBar(GreenfootImage bg)
    {
        Boss boss = getBoss();
        if (boss == null) return;

        int bw = 380;
        int bx = WIDTH / 2 - bw / 2;
        int by = 40;
        drawPanel(bg, bx - 6, by - 4, bw + 12, 36);

        bg.setColor(new Color(255, 120, 120));
        bg.setFont(new Font("Monospaced", true, false, 13));
        String label = "BOSS  " + boss.getName() + (boss.isPhase2() ? "  [PHASE 2]" : "");
        bg.drawString(label, bx, by + 8);

        double f = boss.getHpFraction();
        Color c = boss.isPhase2() ? new Color(255, 90, 60) : new Color(255, 60, 90);
        drawBar(bg, bx, by + 14, bw, 12, f, c);
    }

    private void drawWeaponInventory(GreenfootImage bg, Spaceship ship)
    {
        if (ship == null) return;

        // Nur besessene Waffen auflisten.
        int count = 0;
        for (int t = 0; t < Spaceship.WEAPON_KINDS; t++)
            if (ship.getDraftLevel(t) > 0) count++;
        if (count == 0) return;

        int rowH = 15;
        int ph = 6 + count * rowH;
        int px = 8;
        int py = HEIGHT - 8 - ph;
        drawPanel(bg, px, py, 168, ph);

        bg.setFont(new Font("Monospaced", true, false, 11));
        int y = py + 14;
        for (int t = 0; t < Spaceship.WEAPON_KINDS; t++)
        {
            int lvl = ship.getDraftLevel(t);
            if (lvl <= 0) continue;

            String name = Spaceship.WEAPON_NAMES[t];
            if (name.length() > 12) name = name.substring(0, 12);

            bg.setColor(new Color(200, 225, 255));
            bg.drawString(name, px + 8, y);

            if (t == Spaceship.W_NOVA)
            {
                bg.setColor(new Color(150, 230, 255));
                int charges = (int) Math.floor(ship.getNovaCharge());
                bg.drawString("[X] " + charges + "/" + ship.getNovaMax(), px + 108, y);
            }
            else
            {
                bg.setColor(new Color(120, 240, 140));
                bg.drawString("Lv " + lvl, px + 130, y);
            }
            y += rowH;
        }
    }

    public Boss getBoss()
    {
        java.util.List<Boss> bosses = getObjects(Boss.class);
        return bosses.isEmpty() ? null : bosses.get(0);
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

    private void drawDraft(GreenfootImage bg)
    {
        int pw = 500, ph = 260;
        int px = WIDTH / 2 - pw / 2;
        int py = HEIGHT / 2 - ph / 2;
        drawPanel(bg, px, py, pw, ph);

        bg.setColor(new Color(255, 230, 120));
        bg.setFont(new Font("Monospaced", true, false, 22));
        bg.drawString("WAFFE WAEHLEN", px + 140, py + 32);

        bg.setColor(new Color(180, 220, 255));
        bg.setFont(new Font("Monospaced", true, false, 12));
        bg.drawString("Boss besiegt! Waehle 1 von 3 - Dopplung = Upgrade", px + 90, py + 52);

        Spaceship ship = getShip();
        int cardW = 150, cardH = 150, gap = 12;
        int totalW = cardW * 3 + gap * 2;
        int startX = WIDTH / 2 - totalW / 2;
        int cardY = py + 70;

        for (int i = 0; i < 3; i++)
        {
            int type = draftChoices[i];
            int cx = startX + i * (cardW + gap);
            int lvl = (ship != null) ? ship.getDraftLevel(type) : 0;
            boolean owned = lvl > 0;
            boolean maxed = lvl >= Spaceship.WEAPON_MAX_LEVEL;

            // Kartenrahmen.
            bg.setColor(new Color(25, 32, 60, 240));
            bg.fillRect(cx, cardY, cardW, cardH);
            bg.setColor(owned ? new Color(120, 240, 160) : new Color(255, 220, 120));
            bg.drawRect(cx, cardY, cardW - 1, cardH - 1);
            bg.drawRect(cx + 1, cardY + 1, cardW - 3, cardH - 3);

            // Tastennummer.
            bg.setColor(new Color(255, 230, 120));
            bg.setFont(new Font("Monospaced", true, false, 22));
            bg.drawString("[" + (i + 1) + "]", cx + 12, cardY + 30);

            // Symbol.
            drawWeaponIcon(bg, type, cx + cardW / 2 - 14, cardY + 42);

            // Name.
            bg.setColor(new Color(235, 240, 255));
            bg.setFont(new Font("Monospaced", true, false, 14));
            bg.drawString(Spaceship.WEAPON_NAMES[type], cx + 12, cardY + 96);

            // Beschreibung.
            bg.setColor(new Color(170, 195, 235));
            bg.setFont(new Font("Monospaced", false, false, 10));
            bg.drawString(Spaceship.WEAPON_DESC[type], cx + 12, cardY + 114);

            // Status.
            bg.setFont(new Font("Monospaced", true, false, 12));
            if (maxed)
            {
                bg.setColor(new Color(120, 200, 255));
                bg.drawString("MAX (Lv " + lvl + ")", cx + 12, cardY + 136);
            }
            else if (owned)
            {
                bg.setColor(new Color(120, 240, 160));
                bg.drawString("UPGRADE Lv " + lvl + "->" + (lvl + 1), cx + 12, cardY + 136);
            }
            else
            {
                bg.setColor(new Color(255, 220, 120));
                bg.drawString("NEU", cx + 12, cardY + 136);
            }
        }
    }

    /** Kleines Symbol je Waffentyp fuer die Draft-Karten. */
    private void drawWeaponIcon(GreenfootImage bg, int type, int x, int y)
    {
        int s = 28;
        switch (type)
        {
            case Spaceship.W_SPREAD:
                bg.setColor(new Color(90, 255, 200));
                for (int k = -1; k <= 1; k++) bg.drawLine(x, y + s / 2, x + s, y + s / 2 + k * 8);
                break;
            case Spaceship.W_HOMING:
                bg.setColor(new Color(255, 150, 70));
                bg.fillRect(x + 4, y + s / 2 - 3, s - 8, 6);
                bg.fillRect(x + s - 6, y + s / 2 - 5, 6, 10);
                break;
            case Spaceship.W_LASER:
                bg.setColor(new Color(120, 220, 255));
                bg.fillRect(x, y + s / 2 - 3, s, 6);
                bg.setColor(new Color(255, 255, 255));
                bg.fillRect(x, y + s / 2 - 1, s, 2);
                break;
            case Spaceship.W_PLASMA:
                bg.setColor(new Color(150, 60, 240, 150));
                bg.fillOval(x + 2, y, s - 4, s - 4);
                bg.setColor(new Color(210, 160, 255));
                bg.fillOval(x + 7, y + 5, s - 14, s - 14);
                break;
            case Spaceship.W_DRONE:
                bg.setColor(new Color(120, 200, 255));
                bg.fillOval(x + 6, y + 4, s - 12, s - 12);
                bg.setColor(new Color(210, 240, 255));
                bg.fillOval(x + 10, y + 8, s - 20, s - 20);
                break;
            case Spaceship.W_NOVA:
                bg.setColor(new Color(150, 230, 255));
                bg.drawOval(x + 2, y + 2, s - 4, s - 4);
                bg.drawOval(x + 7, y + 7, s - 14, s - 14);
                bg.setColor(new Color(255, 255, 255));
                bg.fillOval(x + s / 2 - 3, y + s / 2 - 3, 6, 6);
                break;
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
