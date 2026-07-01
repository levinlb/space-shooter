import greenfoot.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Ein Asteroid. Treibt nach links, zerbricht bei einem Treffer und fuegt dem
 * Schiff bei Kollision Schaden zu. Wird als Pixel-Art im Code gezeichnet.
 */
public class Alien extends Enemy
{
    private static final int SPEED = 1;
    private static final int RAM_DAMAGE = 40;

    public Alien()
    {
        hp = 1;
        scoreValue = 10;
        creditValue = 5;
        setImage(buildImage());
    }

    public void act()
    {
        // Kollision mit dem Schiff -> Schaden, Asteroid zerbricht.
        Spaceship ship = (Spaceship) getOneIntersectingObject(Spaceship.class);
        if (ship != null)
        {
            ship.takeDamage(RAM_DAMAGE);
            despawn();
            return;
        }

        if (getX() < 10)
        {
            despawn();
            return;
        }

        move(-SPEED);
    }

    private GreenfootImage buildImage()
    {
        String[] rows = {
            "..DDDD...",
            ".DRRRRD..",
            "DRRLRRRD.",
            "DRRRRRRRD",
            "DRLRRRRRD",
            "DRRRRRLRD",
            ".DRRRRRD.",
            ".DDRRDD..",
            "...DD...."
        };
        Map<Character, Color> p = new HashMap<Character, Color>();
        p.put('R', new Color(120, 120, 130));
        p.put('D', new Color(70, 70, 82));
        p.put('L', new Color(165, 165, 175));
        return PixelArt.fromRows(rows, p, 4);
    }
}
