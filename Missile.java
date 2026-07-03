import greenfoot.*;
import java.util.List;

/**
 * Eine Lenkrakete (Draft-Waffe "Lenkraketen"). Sucht sich den naechsten Gegner
 * und lenkt mit begrenzter Wendigkeit auf ihn zu. Hinterlaesst eine Rauchspur
 * und explodiert beim Treffer. Findet sie kein Ziel, fliegt sie geradeaus.
 */
public class Missile extends Actor
{
    private static final double SPEED = 5.0;
    private static final double TURN = 0.18;   // Wendigkeit (Bogenmass pro Act)
    private static final int LIFE = 130;

    private final int damage;
    private double x, y;
    private double dir;                          // Flugrichtung in Bogenmass
    private boolean placed = false;
    private int life = LIFE;
    private int trailCount = 0;

    public Missile(int damage)
    {
        this.damage = damage;
        this.dir = 0;                            // startet nach rechts
        setImage(render());
    }

    public void act()
    {
        if (!placed)
        {
            x = getX();
            y = getY();
            placed = true;
        }

        World world = getWorld();
        if (world == null) return;

        // Ziel suchen: naechster Gegner.
        Enemy target = nearestEnemy(world);
        if (target != null)
        {
            double desired = Math.atan2(target.getY() - y, target.getX() - x);
            dir = steer(dir, desired, TURN);
        }

        x += Math.cos(dir) * SPEED;
        y += Math.sin(dir) * SPEED;
        setImage(render());
        setLocation((int) Math.round(x), (int) Math.round(y));

        // Rauchspur.
        if (++trailCount % 2 == 0)
        {
            world.addObject(new Particle(-Math.cos(dir) * 0.6, -Math.sin(dir) * 0.6,
                            10, 3, new Color(255, 170, 70), true), (int) x, (int) y);
        }

        // Treffer?
        Enemy hit = (Enemy) getOneIntersectingObject(Enemy.class);
        if (hit != null)
        {
            hit.hit(damage);
            world.addObject(new Explosion(30, new Color(255, 150, 60), 8), (int) x, (int) y);
            world.removeObject(this);
            return;
        }

        if (--life <= 0 || x < -20 || x > world.getWidth() + 20 || y < -20 || y > world.getHeight() + 20)
        {
            world.removeObject(this);
        }
    }

    private Enemy nearestEnemy(World world)
    {
        List<Enemy> enemies = world.getObjects(Enemy.class);
        Enemy best = null;
        double bestD = Double.MAX_VALUE;
        for (Enemy e : enemies)
        {
            double dx = e.getX() - x, dy = e.getY() - y;
            double d = dx * dx + dy * dy;
            if (d < bestD)
            {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /** Dreht "from" um hoechstens "max" in Richtung "to" (kuerzester Weg). */
    private double steer(double from, double to, double max)
    {
        double diff = to - from;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        if (diff > max) diff = max;
        if (diff < -max) diff = -max;
        return from + diff;
    }

    private GreenfootImage render()
    {
        GreenfootImage img = new GreenfootImage(16, 8);
        img.setColor(new Color(230, 230, 240));
        img.fillRect(3, 2, 9, 4);
        img.setColor(new Color(255, 90, 60));           // Spitze
        img.fillRect(11, 2, 4, 4);
        img.setColor(new Color(120, 120, 130));         // Heck
        img.fillRect(0, 1, 3, 6);
        img.rotate((int) Math.toDegrees(dir));
        return img;
    }
}
