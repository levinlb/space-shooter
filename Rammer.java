import greenfoot.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Ein Kamikaze-Gegner. Zuerst naehert er sich langsam und zielt (kurzes
 * Aufblinken als Vorwarnung), dann rastet er auf die aktuelle Position des
 * Schiffs ein und schiesst mit hohem Tempo geradlinig darauf zu. Trifft er,
 * richtet er grossen Schaden an; danach zerschellt er.
 */
public class Rammer extends Enemy
{
    private static final int AIM_TIME = 45;
    private static final int RAM_DAMAGE = 55;

    private int aimTimer = AIM_TIME;
    private boolean charging = false;
    private double vx, vy;
    private double x, y;
    private boolean placed = false;
    private final GreenfootImage normal, bright;

    public Rammer()
    {
        hp = 2;
        scoreValue = 30;
        creditValue = 15;
        explosionColor = new Color(255, 120, 60);
        explosionSize = 30;
        explosionSparks = 12;
        normal = buildImage(false);
        bright = buildImage(true);
        setImage(normal);
    }

    public void act()
    {
        if (!placed)
        {
            x = getX();
            y = getY();
            placed = true;
        }

        MyWorld world = getMyWorld();
        if (world == null) return;
        Spaceship ship = world.getShip();

        // Kollision -> Schaden und zerschellen.
        if (ship != null && intersects(ship))
        {
            ship.takeDamage(RAM_DAMAGE);
            despawn();
            return;
        }

        if (!charging)
        {
            // Zielphase: langsam heran, Hoehe des Schiffs angleichen, blinken.
            x -= 1.2;
            if (ship != null)
            {
                if (ship.getY() < y) y -= 1.2;
                else if (ship.getY() > y) y += 1.2;
            }
            aimTimer--;
            setImage((aimTimer / 5) % 2 == 0 ? bright : normal);

            if (aimTimer <= 0 && ship != null)
            {
                // Auf die aktuelle Schiffsposition einrasten und lossturmen.
                double dx = ship.getX() - x;
                double dy = ship.getY() - y;
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len < 0.001) len = 1;
                double speed = 8.0;
                vx = dx / len * speed;
                vy = dy / len * speed;
                charging = true;
                setImage(bright);
            }
        }
        else
        {
            x += vx;
            y += vy;
            // Kurze Feuerspur.
            if (Greenfoot.getRandomNumber(2) == 0)
            {
                world.addObject(new Particle(-vx * 0.2, -vy * 0.2, 8, 3,
                                new Color(255, 140, 60), true), (int) x, (int) y);
            }
        }

        setLocation((int) Math.round(x), (int) Math.round(y));

        if (x < -20 || y < -20 || y > world.getHeight() + 20)
        {
            despawn();
        }
    }

    private GreenfootImage buildImage(boolean bright)
    {
        String[] rows = {
            "....RR...",
            "RR..RR..R",
            ".RRRRRRR.",
            "RRRYYYRRR",
            ".RRRRRRR.",
            "RR..RR..R",
            "....RR..."
        };
        Map<Character, Color> p = new HashMap<Character, Color>();
        p.put('R', bright ? new Color(255, 140, 120) : new Color(220, 60, 50));
        p.put('Y', new Color(255, 230, 120));
        return PixelArt.fromRows(rows, p, 3);
    }
}
