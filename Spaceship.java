import greenfoot.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Das Raumschiff des Spielers. Bewegt sich in alle vier Richtungen, schiesst,
 * hat einen Boost (Shift) und eine regenerierende Schild-/HP-Leiste. Wird der
 * Schild leer, geht ein Herz verloren (kurze Unverwundbarkeit, Blinken).
 *
 * Vier Upgrade-Stufen (im Shop kaufbar) veraendern die Werte:
 *  - Waffe:     Feuerrate, Schaden, Mehrfachschuss
 *  - Panzerung: max. Schild, +Leben, Regeneration
 *  - Antrieb:   Grundtempo, Boost-Kapazitaet und -Nachladung
 *  - Turret:    automatischer Begleiter (siehe Turret / MyWorld)
 */
public class Spaceship extends Actor
{
    private static final int RAPID_FIRE_COOLDOWN = 4;
    private static final int INVULN_TIME = 60;
    private static final int SHIELD_REGEN_WAIT = 120; // Acts nach letztem Treffer
    private static final int SHIELD_REGEN_EVERY = 8;  // je N Acts +1 Schild

    // ---- Draft-Waffen (im Waffen-Auswahl-Bildschirm alle 5 Wellen erhaltbar) ----
    public static final int W_SPREAD = 0;   // Streuschuss
    public static final int W_HOMING = 1;   // Lenkraketen
    public static final int W_LASER  = 2;   // Laserstrahl
    public static final int W_PLASMA = 3;   // Plasma
    public static final int W_DRONE  = 4;   // Kampfdrohne (Begleiter)
    public static final int W_NOVA   = 5;   // Nova-Bombe (aktive Faehigkeit, Taste X)
    public static final int WEAPON_KINDS = 6;
    public static final int WEAPON_MAX_LEVEL = 5;

    public static final String[] WEAPON_NAMES =
        { "Streuschuss", "Lenkraketen", "Laserstrahl", "Plasma", "Kampfdrohne", "Nova-Bombe" };
    public static final String[] WEAPON_DESC =
        { "Faecher aus Schuessen", "Suchende Raketen", "Durchschlag-Strahl",
          "Grosse Plasmakugel", "Umkreisende Drohne", "Taste X: Rundumschlag" };

    private final int[] draftLevel = new int[WEAPON_KINDS];
    private final int[] draftCd = new int[WEAPON_KINDS];
    private double novaCharge = 0;
    private boolean novaKeyPrev = false;
    private int engineTrailTick = 0;

    // Upgrade-Level.
    private int weaponLevel = 0;
    private int armorLevel = 0;
    private int engineLevel = 0;
    private int turretLevel = 0;

    // Abgeleitete Werte (durch applyUpgrades gesetzt).
    private int baseSpeed;
    private int boostSpeed;
    private int fireCooldownMax;
    private int bulletDamage;
    private int shots;
    private int boostMax;
    private int boostRecharge;
    private int maxShield;

    private int cooldown = 0;
    private int rapidFireTimer = 0;

    private int boostCharge;
    private boolean boosting = false;

    private int shield;
    private int shieldRegenDelay = 0;
    private int shieldRegenAccum = 0;
    private int invulnTimer = 0;

    private GreenfootImage shipImage;
    private GreenfootImage shipImageDim;

    public Spaceship()
    {
        shipImage = buildImage();
        shipImageDim = new GreenfootImage(shipImage);
        shipImageDim.setTransparency(80);
        setImage(shipImage);

        applyUpgrades();
        boostCharge = boostMax;
        shield = maxShield;
    }

    public void act()
    {
        updateShieldRegen();
        updateInvuln();

        MyWorld world = (getWorld() instanceof MyWorld) ? (MyWorld) getWorld() : null;
        if (world != null && !world.isPlaying())
        {
            return; // im Shop / Game Over ruht das Schiff
        }

        handleMovement();
        handleFiring();
        updateDraftWeapons();
        updateNova();
        updateEngineTrail();
    }

    // ---- Bewegung & Boost ----

    private void handleMovement()
    {
        updateBoost();
        int speed = boosting ? boostSpeed : baseSpeed;

        int x = getX();
        int y = getY();
        if (Greenfoot.isKeyDown("up"))    y -= speed;
        if (Greenfoot.isKeyDown("down"))  y += speed;
        if (Greenfoot.isKeyDown("left"))  x -= speed;
        if (Greenfoot.isKeyDown("right")) x += speed;

        x = clamp(x, 0, getWorld().getWidth() - 1);
        y = clamp(y, 0, getWorld().getHeight() - 1);
        setLocation(x, y);
    }

    private void updateBoost()
    {
        boolean wantBoost = Greenfoot.isKeyDown("shift");
        boolean moving = Greenfoot.isKeyDown("up") || Greenfoot.isKeyDown("down")
                      || Greenfoot.isKeyDown("left") || Greenfoot.isKeyDown("right");
        boolean enoughCharge = boosting ? (boostCharge > 0) : (boostCharge >= 30);

        if (wantBoost && moving && enoughCharge)
        {
            boosting = true;
            boostCharge -= 3;
            if (boostCharge < 0) boostCharge = 0;
        }
        else
        {
            boosting = false;
            boostCharge += boostRecharge;
            if (boostCharge > boostMax) boostCharge = boostMax;
        }
    }

    // ---- Feuern ----

    private void handleFiring()
    {
        if (cooldown > 0) cooldown--;
        if (rapidFireTimer > 0) rapidFireTimer--;

        if (cooldown == 0 && Greenfoot.isKeyDown("space"))
        {
            fireBullet();
            cooldown = (rapidFireTimer > 0) ? RAPID_FIRE_COOLDOWN : fireCooldownMax;
        }
    }

    private void fireBullet()
    {
        World world = getWorld();
        if (world == null) return;

        // Mehrfachschuss: Geschosse vertikal gestaffelt.
        int spread = 10;
        int startOffset = -(shots - 1) * spread / 2;
        for (int i = 0; i < shots; i++)
        {
            int oy = startOffset + i * spread;
            world.addObject(new Bullet(bulletDamage), getX() + 16, getY() + oy);
        }
        spawnMuzzleFlash(world);
    }

    private void spawnMuzzleFlash(World world)
    {
        for (int i = 0; i < 3; i++)
        {
            double vy = (Greenfoot.getRandomNumber(7) - 3) * 0.3;
            world.addObject(new Particle(1.5 + Math.random(), vy, 5, 3,
                            new Color(180, 240, 255), true), getX() + 20, getY());
        }
    }

    // ---- Schild / Schaden ----

    public void takeDamage(int amount)
    {
        if (invulnTimer > 0) return;

        shield -= amount;
        shieldRegenDelay = SHIELD_REGEN_WAIT;

        MyWorld mw = (getWorld() instanceof MyWorld) ? (MyWorld) getWorld() : null;
        if (mw != null) mw.screenFlash(new Color(255, 60, 60), 55);

        if (shield <= 0)
        {
            shield = 0;
            invulnTimer = INVULN_TIME;
            MyWorld world = (getWorld() instanceof MyWorld) ? (MyWorld) getWorld() : null;
            if (world != null)
            {
                world.loseLife();     // kann das Spiel beenden und dieses Schiff entfernen
            }
            shield = maxShield;        // Schild wieder aufladen (falls noch am Leben)
        }
    }

    private void updateShieldRegen()
    {
        if (shieldRegenDelay > 0)
        {
            shieldRegenDelay--;
            return;
        }
        if (shield < maxShield)
        {
            shieldRegenAccum++;
            if (shieldRegenAccum >= SHIELD_REGEN_EVERY)
            {
                shieldRegenAccum = 0;
                shield++;
            }
        }
    }

    private void updateInvuln()
    {
        if (invulnTimer > 0)
        {
            invulnTimer--;
            // Blinken zur Rueckmeldung.
            setImage(((invulnTimer / 4) % 2 == 0) ? shipImage : shipImageDim);
        }
        else
        {
            setImage(shipImage);
        }
    }

    // ---- Upgrades ----

    private void applyUpgrades()
    {
        baseSpeed = 3 + engineLevel;
        boostSpeed = baseSpeed + 4;
        boostMax = 200 + engineLevel * 60;
        boostRecharge = 1 + engineLevel;

        fireCooldownMax = Math.max(4, 15 - weaponLevel * 2);
        bulletDamage = 1 + weaponLevel / 2;
        shots = 1 + weaponLevel / 3;

        maxShield = 100 + armorLevel * 40;
    }

    public void upgradeWeapon() { weaponLevel++; applyUpgrades(); }
    public void upgradeEngine() { engineLevel++; applyUpgrades(); }
    public void upgradeTurret() { turretLevel++; }

    public void upgradeArmor()
    {
        armorLevel++;
        applyUpgrades();
        shield = maxShield;   // Schild bei Kauf voll auffuellen
    }

    public int getWeaponLevel() { return weaponLevel; }
    public int getArmorLevel()  { return armorLevel; }
    public int getEngineLevel() { return engineLevel; }
    public int getTurretLevel() { return turretLevel; }

    // ---- Power-up-Effekte (bestehend) ----

    public void activateRapidFire(int duration) { rapidFireTimer = duration; }
    public boolean isRapidFireActive() { return rapidFireTimer > 0; }

    // ---- HUD-Abfragen ----

    public double getBoostFraction() { return (double) boostCharge / boostMax; }
    public boolean isBoosting() { return boosting; }
    public double getShieldFraction() { return (double) shield / maxShield; }

    // ---- Draft-Waffen: automatisches Feuern ----

    private void updateDraftWeapons()
    {
        World world = getWorld();
        if (world == null) return;

        for (int t = 0; t < WEAPON_KINDS; t++)
        {
            if (t == W_DRONE || t == W_NOVA) continue;   // Drohne = Actor, Nova = aktiv
            if (draftLevel[t] <= 0) continue;
            if (draftCd[t] > 0) { draftCd[t]--; continue; }

            switch (t)
            {
                case W_SPREAD: fireSpread(world); break;
                case W_HOMING: fireHoming(world); break;
                case W_LASER:  fireLaser(world);  break;
                case W_PLASMA: firePlasma(world); break;
            }
        }
    }

    private void fireSpread(World world)
    {
        int lvl = draftLevel[W_SPREAD];
        int shotsN = lvl + 2;                       // 3..7
        int dmg = 1 + lvl / 2;
        double total = 0.5 + lvl * 0.08;            // Faecherbreite
        double step = (shotsN > 1) ? total / (shotsN - 1) : 0;
        double startAng = -total / 2;
        for (int i = 0; i < shotsN; i++)
        {
            double ang = startAng + i * step;
            double vx = Math.cos(ang) * 6.5;
            double vy = Math.sin(ang) * 6.5;
            world.addObject(new Bullet(dmg, vx, vy, 1, Bullet.STYLE_SPREAD), getX() + 16, getY());
        }
        draftCd[W_SPREAD] = Math.max(12, 32 - lvl * 3);
    }

    private void fireHoming(World world)
    {
        int lvl = draftLevel[W_HOMING];
        int count = 1 + lvl / 2;                     // 1..3
        int dmg = 2 + lvl;
        for (int i = 0; i < count; i++)
        {
            int oy = (count == 1) ? 0 : (i - (count - 1) / 2) * 14;
            world.addObject(new Missile(dmg), getX() + 12, getY() + oy);
        }
        draftCd[W_HOMING] = Math.max(28, 74 - lvl * 8);
    }

    private void fireLaser(World world)
    {
        int lvl = draftLevel[W_LASER];
        int dmg = 1 + lvl / 2;
        int length = world.getWidth() - getX();
        if (length < 20) length = 20;
        int life = 14 + lvl * 2;
        world.addObject(new LaserBeam(dmg, length, life), getX() + length / 2, getY());
        draftCd[W_LASER] = Math.max(40, 92 - lvl * 8);
    }

    private void firePlasma(World world)
    {
        int lvl = draftLevel[W_PLASMA];
        int dmg = 3 + lvl;
        int pierce = 1 + lvl;
        world.addObject(new Bullet(dmg, 4.2, 0, pierce, Bullet.STYLE_PLASMA), getX() + 16, getY());
        draftCd[W_PLASMA] = Math.max(20, 46 - lvl * 4);
    }

    // ---- Nova-Bombe (aktive Faehigkeit) ----

    private void updateNova()
    {
        int lvl = draftLevel[W_NOVA];
        if (lvl <= 0) return;

        // Aufladen.
        if (novaCharge < lvl)
        {
            novaCharge += 1.0 / 540.0;               // ~11s pro Ladung
            if (novaCharge > lvl) novaCharge = lvl;
        }

        boolean down = Greenfoot.isKeyDown("x");
        if (down && !novaKeyPrev && novaCharge >= 1.0)
        {
            novaCharge -= 1.0;
            MyWorld world = (getWorld() instanceof MyWorld) ? (MyWorld) getWorld() : null;
            if (world != null)
            {
                int dmg = 6 + lvl * 3;
                world.novaBlast(getX(), getY(), dmg, 260);
            }
        }
        novaKeyPrev = down;
    }

    // ---- Triebwerksspur ----

    private void updateEngineTrail()
    {
        World world = getWorld();
        if (world == null) return;
        engineTrailTick++;
        int every = boosting ? 1 : 3;
        if (engineTrailTick % every != 0) return;

        double spd = boosting ? -3.5 : -2.0;
        int size = boosting ? 4 : 3;
        Color c = boosting ? new Color(255, 140, 60) : new Color(120, 180, 255);
        world.addObject(new Particle(spd, (Greenfoot.getRandomNumber(5) - 2) * 0.4,
                        boosting ? 14 : 9, size, c, true), getX() - 18, getY());
    }

    // ---- Draft-Waffen: Zugriff ----

    /** Fuegt eine Waffe hinzu oder steigert sie (Brotato-Stil). */
    public void addOrLevelWeapon(int type)
    {
        if (type < 0 || type >= WEAPON_KINDS) return;
        if (draftLevel[type] < WEAPON_MAX_LEVEL) draftLevel[type]++;
        if (type == W_NOVA && novaCharge < 1.0) novaCharge = 1.0;
    }

    public int getDraftLevel(int type) { return (type >= 0 && type < WEAPON_KINDS) ? draftLevel[type] : 0; }
    public boolean ownsWeapon(int type) { return getDraftLevel(type) > 0; }
    public int getDroneLevel() { return draftLevel[W_DRONE]; }
    public double getNovaCharge() { return novaCharge; }
    public int getNovaMax() { return draftLevel[W_NOVA]; }

    // ---- Hilfen ----

    private int clamp(int value, int min, int max)
    {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private GreenfootImage buildImage()
    {
        String[] rows = {
            "...........",
            "...BB......",
            "..BBBBB....",
            "EEBBBBBBB..",
            "EEBBBCCBBBW",
            "EEBBBBBBB..",
            "..BBBBB....",
            "...BB......",
            "..........."
        };
        Map<Character, Color> p = new HashMap<Character, Color>();
        p.put('B', new Color(60, 120, 220));
        p.put('C', new Color(120, 230, 255));
        p.put('E', new Color(255, 150, 40));
        p.put('W', new Color(230, 240, 255));
        return PixelArt.fromRows(rows, p, 3);
    }
}
