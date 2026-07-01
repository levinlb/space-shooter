import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)

/**
 * Das Raumschiff des Spielers. Bewegt sich in alle vier Richtungen (innerhalb
 * der Bildschirmgrenzen) und schießt Bullets mit einem kurzen Cooldown.
 */
public class Spaceship extends Actor
{
    private static final int BASE_SPEED = 3;           // normale Bewegung pro Act
    private static final int BOOST_SPEED = 7;          // Bewegung mit Boost
    private static final int FIRE_COOLDOWN = 15;        // Acts zwischen zwei Schuessen
    private static final int RAPID_FIRE_COOLDOWN = 4;   // Cooldown bei Schnellfeuer

    // Boost: schnell verbraucht, langsam nachgeladen. Aktivierung mit Shift.
    private static final int BOOST_MAX = 200;
    private static final int BOOST_DRAIN = 3;          // Verbrauch pro Act beim Boosten
    private static final int BOOST_RECHARGE = 1;       // langsames Nachladen pro Act
    private static final int BOOST_MIN_TO_START = 30;  // Mindestladung zum Aktivieren

    private int cooldown = 0;
    private int rapidFireTimer = 0;
    private int boostCharge = BOOST_MAX;
    private boolean boosting = false;

    public void act()
    {
        handleMovement();
        handleFiring();
    }

    private void handleMovement()
    {
        updateBoost();
        int speed = boosting ? BOOST_SPEED : BASE_SPEED;

        int x = getX();
        int y = getY();

        if (Greenfoot.isKeyDown("up")) {
            y -= speed;
        }
        if (Greenfoot.isKeyDown("down")) {
            y += speed;
        }
        if (Greenfoot.isKeyDown("left")) {
            x -= speed;
        }
        if (Greenfoot.isKeyDown("right")) {
            x += speed;
        }

        // Innerhalb der Weltgrenzen halten.
        x = clamp(x, 0, getWorld().getWidth() - 1);
        y = clamp(y, 0, getWorld().getHeight() - 1);

        setLocation(x, y);
    }

    /**
     * Verwaltet die Boost-Ladung. Boost (Shift) erhoeht die Geschwindigkeit,
     * verbraucht die Ladung schnell und laedt sonst langsam wieder auf.
     */
    private void updateBoost()
    {
        boolean wantBoost = Greenfoot.isKeyDown("shift");
        boolean moving = Greenfoot.isKeyDown("up") || Greenfoot.isKeyDown("down")
                      || Greenfoot.isKeyDown("left") || Greenfoot.isKeyDown("right");

        boolean enoughCharge = boosting ? (boostCharge > 0) : (boostCharge >= BOOST_MIN_TO_START);

        if (wantBoost && moving && enoughCharge)
        {
            boosting = true;
            boostCharge -= BOOST_DRAIN;
            if (boostCharge < 0) boostCharge = 0;
        }
        else
        {
            boosting = false;
            boostCharge += BOOST_RECHARGE;
            if (boostCharge > BOOST_MAX) boostCharge = BOOST_MAX;
        }
    }

    /** Ladezustand des Boosts als Anteil 0..1 (fuer die HUD-Anzeige). */
    public double getBoostFraction()
    {
        return (double) boostCharge / BOOST_MAX;
    }

    public boolean isBoosting()
    {
        return boosting;
    }

    private void handleFiring()
    {
        if (cooldown > 0) {
            cooldown--;
        }
        if (rapidFireTimer > 0) {
            rapidFireTimer--;
        }

        if (cooldown == 0 && Greenfoot.isKeyDown("space")) {
            fireBullet();
            cooldown = (rapidFireTimer > 0) ? RAPID_FIRE_COOLDOWN : FIRE_COOLDOWN;
        }
    }

    /**
     * Aktiviert Schnellfeuer fuer die angegebene Anzahl Acts.
     */
    public void activateRapidFire(int duration)
    {
        rapidFireTimer = duration;
    }

    public boolean isRapidFireActive()
    {
        return rapidFireTimer > 0;
    }

    private void fireBullet()
    {
        getWorld().addObject(new Bullet(), getX() + 10, getY());
    }

    private int clamp(int value, int min, int max)
    {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
