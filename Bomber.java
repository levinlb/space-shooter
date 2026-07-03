import greenfoot.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Ein schwerer, gepanzerter Bomber. Bewegt sich langsam nach links, haelt viel
 * aus und feuert in Intervallen einen dreifachen Streufaecher aus Projektilen
 * in Richtung des Schiffs. Gibt entsprechend viele Punkte und Credits.
 */
public class Bomber extends Enemy
{
    private static final int SPEED = 1;
    private static final int FIRE_INTERVAL = 110;
    private static final int RAM_DAMAGE = 60;

    private int fireCooldown;
    private double y;
    private boolean placed = false;
    private double bob;

    public Bomber()
    {
        hp = 7;
        scoreValue = 45;
        creditValue = 25;
        explosionColor = new Color(255, 150, 80);
        explosionSize = 42;
        explosionSparks = 16;
        fireCooldown = 50 + Greenfoot.getRandomNumber(FIRE_INTERVAL);
        bob = Math.random() * Math.PI * 2;
        setImage(buildImage());
    }

    public void act()
    {
        if (!placed)
        {
            y = getY();
            placed = true;
        }

        MyWorld world = getMyWorld();
        if (world == null) return;
        Spaceship ship = world.getShip();

        if (ship != null && intersects(ship))
        {
            ship.takeDamage(RAM_DAMAGE);
            despawn();
            return;
        }

        bob += 0.05;
        y += Math.sin(bob) * 0.6;
        setLocation(getX() - SPEED, clampY((int) Math.round(y)));

        if (getX() < 10)
        {
            despawn();
            return;
        }

        if (fireCooldown > 0)
        {
            fireCooldown--;
        }
        else
        {
            fireSpread(ship);
            fireCooldown = FIRE_INTERVAL;
        }
    }

    private void fireSpread(Spaceship ship)
    {
        World world = getWorld();
        if (world == null) return;

        double base = Math.PI;   // nach links
        if (ship != null)
        {
            base = Math.atan2(ship.getY() - getY(), ship.getX() - getX());
        }
        double[] offsets = { -0.32, 0.0, 0.32 };
        for (double off : offsets)
        {
            world.addObject(new EnemyBullet((double) getX() - 14, (double) getY(),
                            base + off, 3.2, 26, EnemyBullet.STYLE_DEFAULT),
                            getX() - 14, getY());
        }
    }

    private GreenfootImage buildImage()
    {
        String[] rows = {
            "..KKKKKK..",
            ".KDDDDDDK.",
            "KDGGGGGGDK",
            "KDGYYYYGDK",
            "GDGGGGGGDG",
            "KDGYYYYGDK",
            "KDGGGGGGDK",
            ".KDDDDDDK.",
            "..KKKKKK.."
        };
        Map<Character, Color> p = new HashMap<Character, Color>();
        p.put('K', new Color(90, 90, 100));
        p.put('D', new Color(130, 130, 145));
        p.put('G', new Color(70, 100, 130));
        p.put('Y', new Color(255, 200, 70));
        return PixelArt.fromRows(rows, p, 4);
    }
}
