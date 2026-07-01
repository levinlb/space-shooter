import greenfoot.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Ein feindliches Jaegerschiff. Fliegt nach links, folgt dabei leicht der Hoehe
 * des Spielers und feuert in Intervallen Projektile (EnemyBullet) auf ihn ab.
 * Robuster als ein Asteroid (mehr hp), gibt mehr Punkte und Credits.
 */
public class Attacker extends Enemy
{
    private static final int SPEED = 1;
    private static final int FIRE_INTERVAL = 90;
    private static final int RAM_DAMAGE = 60;

    private int fireCooldown;

    public Attacker()
    {
        hp = 3;
        scoreValue = 25;
        creditValue = 15;
        fireCooldown = 30 + Greenfoot.getRandomNumber(FIRE_INTERVAL);
        setImage(buildImage());
    }

    public void act()
    {
        MyWorld world = getMyWorld();
        if (world == null) return;

        Spaceship ship = world.getShip();

        // Kollision -> Schaden und selbst zerstoeren.
        if (ship != null && intersects(ship))
        {
            ship.takeDamage(RAM_DAMAGE);
            despawn();
            return;
        }

        // Nach links, mit leichter vertikaler Verfolgung.
        int dy = 0;
        if (ship != null)
        {
            if (ship.getY() < getY() - 4) dy = -1;
            else if (ship.getY() > getY() + 4) dy = 1;
        }
        setLocation(getX() - SPEED, clampY(getY() + dy));

        if (getX() < 10)
        {
            despawn();
            return;
        }

        // Feuern.
        if (fireCooldown > 0)
        {
            fireCooldown--;
        }
        else
        {
            fire(ship);
            fireCooldown = FIRE_INTERVAL;
        }
    }

    private void fire(Spaceship ship)
    {
        World world = getWorld();
        if (world == null) return;

        int targetX = (ship != null) ? ship.getX() : getX() - 100;
        int targetY = (ship != null) ? ship.getY() : getY();
        world.addObject(new EnemyBullet(getX() - 12, getY(), targetX, targetY), getX() - 12, getY());
    }

    private GreenfootImage buildImage()
    {
        String[] rows = {
            "...........",
            "......RR...",
            "....RRRRR..",
            "..RRRRRRRDD",
            "GRRRYYRRRDD",
            "..RRRRRRRDD",
            "....RRRRR..",
            "......RR...",
            "..........."
        };
        Map<Character, Color> p = new HashMap<Character, Color>();
        p.put('R', new Color(210, 55, 55));
        p.put('D', new Color(120, 25, 25));
        p.put('Y', new Color(255, 220, 60));
        p.put('G', new Color(185, 185, 195));
        return PixelArt.fromRows(rows, p, 3);
    }
}
