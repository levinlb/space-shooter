import greenfoot.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Ein Geschoss des Spielers (Schiffskanone, Turret oder Draft-Waffen). Fliegt
 * mit einer frei waehlbaren Geschwindigkeit (vx/vy), kann mehrere Gegner
 * durchschlagen (pierce) und wird je nach Stil unterschiedlich gezeichnet:
 * Laser (blau), Streuschuss (cyan), Plasma (violett, gross).
 *
 * Bei jedem Treffer spritzen Funken (Particle); der Schaden richtet sich nach
 * dem uebergebenen Wert. Alles im Code gezeichnet.
 */
public class Bullet extends Actor
{
    public static final int STYLE_LASER  = 0;
    public static final int STYLE_SPREAD = 1;
    public static final int STYLE_PLASMA = 2;

    private final int damage;
    private final double vx, vy;
    private int pierce;
    private final int style;

    private double x, y;
    private boolean placed = false;
    private int animCount = 0;

    private final Set<Enemy> alreadyHit = new HashSet<Enemy>();

    public Bullet()
    {
        this(1);
    }

    public Bullet(int damage)
    {
        this(damage, 7, 0, 1, STYLE_LASER);
    }

    public Bullet(int damage, double vx, double vy, int pierce, int style)
    {
        this.damage = damage;
        this.vx = vx;
        this.vy = vy;
        this.pierce = Math.max(1, pierce);
        this.style = style;
        updateImage();
    }

    public void act()
    {
        if (!placed)
        {
            x = getX();
            y = getY();
            placed = true;
        }

        animCount++;
        updateImage();

        // Treffer: alle noch nicht getroffenen Gegner beruecksichtigen (Pierce).
        java.util.List<Enemy> hits = getIntersectingObjects(Enemy.class);
        World world = getWorld();
        for (Enemy e : hits)
        {
            if (alreadyHit.contains(e)) continue;
            alreadyHit.add(e);
            spawnSpark();
            e.hit(damage);
            pierce--;
            if (pierce <= 0)
            {
                if (world != null) world.removeObject(this);
                return;
            }
        }

        x += vx;
        y += vy;

        if (world == null) return;
        if (x < -20 || x > world.getWidth() + 20 || y < -20 || y > world.getHeight() + 20)
        {
            world.removeObject(this);
            return;
        }
        setLocation((int) Math.round(x), (int) Math.round(y));
    }

    private void spawnSpark()
    {
        World world = getWorld();
        if (world == null) return;
        for (int i = 0; i < 4; i++)
        {
            double ang = Math.random() * Math.PI * 2;
            double spd = 1 + Math.random() * 2;
            world.addObject(new Particle(Math.cos(ang) * spd, Math.sin(ang) * spd,
                            6 + Greenfoot.getRandomNumber(6), 2,
                            new Color(180, 240, 255), true), getX(), getY());
        }
    }

    private void updateImage()
    {
        if (style == STYLE_PLASMA)
        {
            int d = 16;
            GreenfootImage img = new GreenfootImage(d, d);
            int pulse = (animCount / 2) % 3;
            img.setColor(new Color(150, 60, 240, 130));
            img.fillOval(0, 0, d, d);
            img.setColor(new Color(200, 120, 255));
            img.fillOval(2, 2, d - 4, d - 4);
            img.setColor((pulse == 0) ? new Color(255, 255, 255) : new Color(230, 200, 255));
            img.fillOval(5, 5, d - 10, d - 10);
            setImage(img);
            return;
        }

        int w = 22, h = 8;
        GreenfootImage img = new GreenfootImage(w, h);
        int frame = (animCount / 2) % 3;

        Color glowC = (style == STYLE_SPREAD) ? new Color(80, 255, 200, 170)
                                              : new Color(0, 150 + frame * 30 > 255 ? 255 : 150 + frame * 30, 255, 170);
        img.setColor(glowC);
        img.fillRect(0, 2, w - 2, h - 4);

        Color core = (frame == 0) ? new Color(255, 255, 255)
                   : (frame == 1) ? new Color(180, 240, 255)
                                  : new Color(255, 255, 190);
        img.setColor(core);
        img.fillRect(2, 3, w - 6, h - 6);

        img.setColor(new Color(255, 255, 255));
        img.fillOval(w - 7, 1, 6, 6);

        // Bei schraeg fliegenden Streuschuessen das Bild leicht drehen.
        if (vy != 0)
        {
            img.rotate((int) Math.toDegrees(Math.atan2(vy, vx)));
        }
        setImage(img);
    }
}
