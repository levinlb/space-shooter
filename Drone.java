import greenfoot.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Eine Kampfdrohne (Draft-Waffe "Kampfdrohne"). Sie umkreist das Schiff auf
 * einer eigenen Umlaufbahn und feuert selbststaendig nach rechts. Mehrere
 * Drohnen verteilen sich gleichmaessig um das Schiff. Feuerrate und Schaden
 * steigen mit dem Drohnen-Level des Schiffs. Verschwindet mit dem Schiff.
 */
public class Drone extends Actor
{
    private final Spaceship owner;
    private final int index;          // Position dieser Drohne im Kreis
    private final int total;          // Gesamtzahl der Drohnen
    private double angle;
    private int cooldown;

    public Drone(Spaceship owner, int index, int total)
    {
        this.owner = owner;
        this.index = index;
        this.total = Math.max(1, total);
        this.angle = index * (2 * Math.PI / this.total);
        this.cooldown = Greenfoot.getRandomNumber(20);
        setImage(buildImage());
    }

    public void act()
    {
        // Schiff weg -> Drohne entfernen.
        if (owner == null || owner.getWorld() == null)
        {
            if (getWorld() != null) getWorld().removeObject(this);
            return;
        }

        angle += 0.05;
        int r = 34;
        int px = owner.getX() + (int) (Math.cos(angle) * r);
        int py = owner.getY() + (int) (Math.sin(angle) * r * 0.7);
        setLocation(px, py);

        MyWorld world = (getWorld() instanceof MyWorld) ? (MyWorld) getWorld() : null;
        if (world == null || !world.isPlaying()) return;

        int level = owner.getDroneLevel();
        if (cooldown > 0)
        {
            cooldown--;
        }
        else
        {
            int dmg = 1 + level / 2;
            getWorld().addObject(new Bullet(dmg, 7, 0, 1, Bullet.STYLE_LASER), getX() + 10, getY());
            cooldown = Math.max(14, 42 - level * 5);
        }
    }

    private GreenfootImage buildImage()
    {
        String[] rows = {
            ".CCC.",
            "CWWWC",
            "CWKWC",
            "CWWWC",
            ".CCC."
        };
        Map<Character, Color> p = new HashMap<Character, Color>();
        p.put('C', new Color(120, 200, 255));
        p.put('W', new Color(210, 240, 255));
        p.put('K', new Color(60, 120, 200));
        return PixelArt.fromRows(rows, p, 3);
    }
}
