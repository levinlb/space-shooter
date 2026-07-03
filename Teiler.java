import greenfoot.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Ein Teiler-Gegner (eine Art lebende Mine). Wird er zerstoert, zerfaellt er in
 * zwei kleinere Splitter, die auseinanderdriften. Die kleinen Splitter teilen
 * sich nicht weiter. Treibt langsam nach links und rammt bei Kontakt.
 */
public class Teiler extends Enemy
{
    private static final int RAM_DAMAGE = 35;

    private final int generation;   // 1 = gross (teilt sich), 0 = kleiner Splitter
    private double x, y, vy;
    private boolean placed = false;

    public Teiler()
    {
        this(1, 0);
    }

    public Teiler(int generation, double vy)
    {
        this.generation = generation;
        this.vy = vy;

        if (generation >= 1)
        {
            hp = 4;
            scoreValue = 25;
            creditValue = 12;
            explosionSize = 30;
            explosionSparks = 10;
        }
        else
        {
            hp = 1;
            scoreValue = 10;
            creditValue = 5;
            explosionSize = 18;
            explosionSparks = 6;
        }
        explosionColor = new Color(200, 130, 255);
        setImage(buildImage(generation));
    }

    public void act()
    {
        if (!placed)
        {
            x = getX();
            y = getY();
            placed = true;
        }

        Spaceship ship = (Spaceship) getOneIntersectingObject(Spaceship.class);
        if (ship != null)
        {
            ship.takeDamage(RAM_DAMAGE);
            despawn();
            return;
        }

        x -= 1.0;
        y += vy;
        vy *= 0.98;
        setLocation((int) Math.round(x), (int) Math.round(y));

        if (getX() < 10 || getY() < 0 || getY() > getWorld().getHeight() - 1)
        {
            despawn();
        }
    }

    @Override
    protected void onDeath(MyWorld world)
    {
        if (generation >= 1)
        {
            world.addObject(new Teiler(0, -1.6), getX(), getY() - 6);
            world.addObject(new Teiler(0,  1.6), getX(), getY() + 6);
        }
    }

    private GreenfootImage buildImage(int generation)
    {
        String[] rows = {
            ".PPPP.",
            "PPWWPP",
            "PWMMWP",
            "PWMMWP",
            "PPWWPP",
            ".PPPP."
        };
        Map<Character, Color> p = new HashMap<Character, Color>();
        p.put('P', new Color(150, 70, 200));
        p.put('W', new Color(210, 160, 255));
        p.put('M', new Color(255, 240, 255));
        return PixelArt.fromRows(rows, p, generation >= 1 ? 5 : 3);
    }
}
