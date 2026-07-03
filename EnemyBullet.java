import greenfoot.*;

/**
 * Ein feindliches Projektil. Fliegt mit konstanter Geschwindigkeit in eine
 * feste Richtung und fuegt dem Schiff bei Treffer Schaden zu. Es gibt zwei
 * Erzeugungsarten:
 *  - auf ein Ziel gerichtet (targetX/targetY) fuer Jaeger,
 *  - mit direktem Winkel/Tempo fuer Boss-Muster (Faecher, Kreise).
 *
 * Der Stil bestimmt die Farbe (Standard: oranger Bolzen, Boss: violettes
 * Plasma). Pixel-Art im Code, keine Bilddateien.
 */
public class EnemyBullet extends Actor
{
    public static final int STYLE_DEFAULT = 0;
    public static final int STYLE_BOSS    = 1;

    private static final double DEFAULT_SPEED = 4.0;

    private double x, y, vx, vy;
    private final int damage;
    private final int style;
    private int anim = 0;

    /** Gerichtet auf eine Zielposition (Standard-Jaeger, 34 Schaden). */
    public EnemyBullet(int fromX, int fromY, int targetX, int targetY)
    {
        x = fromX;
        y = fromY;

        double dx = targetX - fromX;
        double dy = targetY - fromY;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001) len = 1;
        vx = dx / len * DEFAULT_SPEED;
        vy = dy / len * DEFAULT_SPEED;
        damage = 34;
        style = STYLE_DEFAULT;
        setImage(buildImage());
    }

    /** Freier Winkel und Tempo (Boss-Muster / Bomber-Faecher). */
    public EnemyBullet(double fromX, double fromY, double angleRad, double speed, int damage, int style)
    {
        x = fromX;
        y = fromY;
        vx = Math.cos(angleRad) * speed;
        vy = Math.sin(angleRad) * speed;
        this.damage = damage;
        this.style = style;
        setImage(buildImage());
    }

    public void act()
    {
        x += vx;
        y += vy;

        World world = getWorld();
        if (world == null) return;

        if (x < -12 || x > world.getWidth() + 12 || y < -12 || y > world.getHeight() + 12)
        {
            world.removeObject(this);
            return;
        }

        setLocation((int) x, (int) y);

        if (style == STYLE_BOSS && (++anim % 3 == 0))
        {
            setImage(buildImage());
        }

        Spaceship ship = (Spaceship) getOneIntersectingObject(Spaceship.class);
        if (ship != null)
        {
            ship.takeDamage(damage);
            world.removeObject(this);
        }
    }

    private GreenfootImage buildImage()
    {
        if (style == STYLE_BOSS)
        {
            int d = 12;
            GreenfootImage img = new GreenfootImage(d, d);
            int pulse = (anim / 3) % 2;
            img.setColor(new Color(255, 80, 200, 150));
            img.fillOval(0, 0, d, d);
            img.setColor(pulse == 0 ? new Color(255, 150, 230) : new Color(230, 120, 255));
            img.fillOval(2, 2, d - 4, d - 4);
            img.setColor(new Color(255, 255, 255));
            img.fillOval(4, 4, d - 8, d - 8);
            return img;
        }

        int w = 12, h = 6;
        GreenfootImage img = new GreenfootImage(w, h);
        img.setColor(new Color(255, 80, 40, 160));   // Glow
        img.fillRect(0, 1, w, h - 2);
        img.setColor(new Color(255, 180, 60));        // Kern
        img.fillRect(2, 2, w - 4, h - 4);
        img.setColor(new Color(255, 255, 200));       // heller Kopf
        img.fillOval(0, 1, 4, 4);
        return img;
    }
}
