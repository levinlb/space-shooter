import greenfoot.*;

/**
 * Ein einzelnes, kurzlebiges Partikel fuer Effekte: Funken, Truemmer,
 * Triebwerksrauch, Muendungsfeuer. Bewegt sich mit einer Geschwindigkeit,
 * wird langsamer (Reibung), verblasst und entfernt sich selbst.
 *
 * Alles wird im Code gezeichnet (kleiner leuchtender Punkt), keine Bilddateien.
 */
public class Particle extends Actor
{
    private double x, y, vx, vy;
    private double drag;
    private int life, maxLife;
    private final int size;
    private final Color color;
    private final boolean glow;
    private boolean placed = false;

    public Particle(double vx, double vy, int life, int size, Color color, boolean glow)
    {
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = Math.max(1, life);
        this.size = Math.max(1, size);
        this.color = color;
        this.glow = glow;
        this.drag = 0.92;
        setImage(render(255));
    }

    /** Setzt die Reibung (1.0 = keine Verlangsamung, kleiner = schneller langsam). */
    public Particle withDrag(double d) { this.drag = d; return this; }

    public void act()
    {
        if (!placed)
        {
            x = getX();
            y = getY();
            placed = true;
        }

        x += vx;
        y += vy;
        vx *= drag;
        vy *= drag;

        life--;
        if (life <= 0)
        {
            getWorld().removeObject(this);
            return;
        }

        // Verblassen und leicht schrumpfen zum Ende hin.
        int alpha = (int) (255.0 * life / maxLife);
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        setImage(render(alpha));

        setLocation((int) Math.round(x), (int) Math.round(y));

        World w = getWorld();
        if (w != null && (x < -20 || x > w.getWidth() + 20 || y < -20 || y > w.getHeight() + 20))
        {
            w.removeObject(this);
        }
    }

    private GreenfootImage render(int alpha)
    {
        double f = (double) life / maxLife;
        int s = Math.max(1, (int) Math.round(size * (0.4 + 0.6 * f)));
        int pad = glow ? s : 1;
        int dim = s + pad * 2;
        GreenfootImage img = new GreenfootImage(dim, dim);

        if (glow)
        {
            int ga = alpha / 3;
            img.setColor(new Color(color.r, color.g, color.b, ga));
            img.fillOval(0, 0, dim, dim);
        }
        img.setColor(new Color(color.r, color.g, color.b, alpha));
        img.fillOval(pad, pad, s, s);
        return img;
    }
}
