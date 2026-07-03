import greenfoot.*;
import java.util.List;

/**
 * Der Laserstrahl (Draft-Waffe "Laserstrahl"). Ein breiter, kurzlebiger Strahl,
 * der vom Schiff bis zum rechten Rand reicht und alle Gegner auf seiner Bahn
 * durchdringt. Er richtet ueber seine Lebensdauer mehrfach Schaden an und wird
 * animiert (pulsierender Kern). Rein im Code gezeichnet.
 */
public class LaserBeam extends Actor
{
    private final int damage;
    private final int length;
    private int life;
    private final int maxLife;
    private int tick = 0;

    public LaserBeam(int damage, int length, int life)
    {
        this.damage = damage;
        this.length = Math.max(20, length);
        this.life = life;
        this.maxLife = Math.max(1, life);
        setImage(render());
    }

    public void act()
    {
        tick++;

        // Schaden im Takt (nicht jeden Act, sonst zu stark).
        if (tick % 4 == 0)
        {
            List<Enemy> hits = getIntersectingObjects(Enemy.class);
            for (Enemy e : hits)
            {
                e.hit(damage);
            }
        }

        life--;
        if (life <= 0)
        {
            getWorld().removeObject(this);
            return;
        }
        setImage(render());
    }

    private GreenfootImage render()
    {
        int h = 14;
        GreenfootImage img = new GreenfootImage(length, h);
        double f = (double) life / maxLife;          // 1 -> 0
        int coreH = Math.max(2, (int) (10 * f));

        int glowA = (int) (150 * f);
        img.setColor(new Color(120, 220, 255, glowA));
        img.fillRect(0, 0, length, h);

        int mid = h / 2;
        img.setColor(new Color(90, 200, 255, (int) (200 * f)));
        img.fillRect(0, mid - coreH / 2, length, coreH);

        // Heller, flackernder Innenkern.
        int flick = (tick % 2 == 0) ? 255 : 210;
        img.setColor(new Color(flick, flick, 255, (int) (255 * f)));
        img.fillRect(0, mid - 1, length, 2);

        return img;
    }
}
