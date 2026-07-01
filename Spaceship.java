import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)

/**
 * Das Raumschiff des Spielers. Bewegt sich in alle vier Richtungen (innerhalb
 * der Bildschirmgrenzen) und schießt Bullets mit einem kurzen Cooldown.
 */
public class Spaceship extends Actor
{
    private static final int SPEED = 3;                // Bewegung pro Act
    private static final int FIRE_COOLDOWN = 15;        // Acts zwischen zwei Schuessen
    private static final int RAPID_FIRE_COOLDOWN = 4;   // Cooldown bei Schnellfeuer

    private int cooldown = 0;
    private int rapidFireTimer = 0;

    public void act()
    {
        handleMovement();
        handleFiring();
    }

    private void handleMovement()
    {
        int x = getX();
        int y = getY();

        if (Greenfoot.isKeyDown("up")) {
            y -= SPEED;
        }
        if (Greenfoot.isKeyDown("down")) {
            y += SPEED;
        }
        if (Greenfoot.isKeyDown("left")) {
            x -= SPEED;
        }
        if (Greenfoot.isKeyDown("right")) {
            x += SPEED;
        }

        // Innerhalb der Weltgrenzen halten.
        x = clamp(x, 0, getWorld().getWidth() - 1);
        y = clamp(y, 0, getWorld().getHeight() - 1);

        setLocation(x, y);
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
