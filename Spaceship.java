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
    }

    // ---- Schild / Schaden ----

    public void takeDamage(int amount)
    {
        if (invulnTimer > 0) return;

        shield -= amount;
        shieldRegenDelay = SHIELD_REGEN_WAIT;

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
