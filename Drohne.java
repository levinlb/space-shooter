import greenfoot.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Eine schnelle Kampfdrohne des Gegners. Fliegt zuegig nach links und schwingt
 * dabei in einer Sinuswelle auf und ab, was sie schwer zu treffen macht. Wenig
 * Trefferpunkte, aber flott. Rammt das Schiff bei Kontakt.
 */
public class Drohne extends Enemy
{
    private static final int SPEED = 3;
    private static final int RAM_DAMAGE = 30;

    private double phase;
    private final double amp;

    public Drohne()
    {
        hp = 2;
        scoreValue = 20;
        creditValue = 10;
        explosionColor = new Color(120, 230, 160);
        explosionSize = 24;
        explosionSparks = 9;
        phase = Math.random() * Math.PI * 2;
        amp = 2.5 + Math.random() * 1.5;
        setImage(buildImage());
    }

    public void act()
    {
        Spaceship ship = (Spaceship) getOneIntersectingObject(Spaceship.class);
        if (ship != null)
        {
            ship.takeDamage(RAM_DAMAGE);
            despawn();
            return;
        }

        phase += 0.2;
        int dy = (int) Math.round(Math.sin(phase) * amp);
        setLocation(getX() - SPEED, clampY(getY() + dy));

        if (getX() < 10)
        {
            despawn();
        }
    }

    private GreenfootImage buildImage()
    {
        String[] rows = {
            "..G..G..",
            "..GGGG..",
            "GGKWWKGG",
            "GKWRRWKG",
            "GGKWWKGG",
            "..GGGG..",
            "..G..G.."
        };
        Map<Character, Color> p = new HashMap<Character, Color>();
        p.put('G', new Color(60, 170, 110));
        p.put('K', new Color(30, 110, 70));
        p.put('W', new Color(150, 240, 190));
        p.put('R', new Color(255, 90, 90));
        return PixelArt.fromRows(rows, p, 3);
    }
}
