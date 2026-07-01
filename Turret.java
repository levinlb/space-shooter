import greenfoot.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Ein automatischer Begleiter (kaufbar im Shop). Folgt dem Schiff und feuert
 * selbststaendig nach rechts. Die Feuerrate steigt mit dem Turret-Level des
 * Schiffs. Verschwindet, wenn das Schiff nicht mehr existiert (Game Over).
 */
public class Turret extends Actor
{
    private final Spaceship owner;
    private int cooldown = 0;

    public Turret(Spaceship owner)
    {
        this.owner = owner;
        setImage(buildImage());
    }

    public void act()
    {
        // Schiff weg -> Turret entfernen.
        if (owner == null || owner.getWorld() == null)
        {
            if (getWorld() != null) getWorld().removeObject(this);
            return;
        }

        // Ueber dem Schiff mitfliegen.
        setLocation(owner.getX() - 6, owner.getY() - 24);

        // Nur im laufenden Spiel feuern.
        MyWorld world = (getWorld() instanceof MyWorld) ? (MyWorld) getWorld() : null;
        if (world == null || !world.isPlaying()) return;

        int level = owner.getTurretLevel();
        if (cooldown > 0)
        {
            cooldown--;
        }
        else
        {
            getWorld().addObject(new Bullet(1), getX() + 12, getY());
            cooldown = Math.max(10, 40 - level * 8);
        }
    }

    private GreenfootImage buildImage()
    {
        String[] rows = {
            ".GGGG..",
            "GWWWWGK",
            "GWWWWGK",
            "GWWWWG.",
            ".GGGG.."
        };
        Map<Character, Color> p = new HashMap<Character, Color>();
        p.put('G', new Color(30, 120, 45));
        p.put('W', new Color(120, 220, 120));
        p.put('K', new Color(185, 185, 195));
        return PixelArt.fromRows(rows, p, 3);
    }
}
