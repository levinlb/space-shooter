import greenfoot.*;

/**
 * Eine Explosion: ein sich schnell ausdehnender, verblassender Leuchtring plus
 * ein einmaliger Funkenausbruch (Partikel). Entfernt sich selbst, wenn der Ring
 * seine Maximalgroesse erreicht hat. Rein im Code gezeichnet.
 */
public class Explosion extends Actor
{
    private final int maxRadius;
    private int radius;
    private final int step;
    private final Color color;
    private final int sparkCount;
    private boolean seeded = false;

    /**
     * @param size       ungefaehrer Enddurchmesser in Pixeln
     * @param color      Kernfarbe der Explosion
     * @param sparkCount Anzahl der Funken, die herausgeschleudert werden
     */
    public Explosion(int size, Color color, int sparkCount)
    {
        this.maxRadius = Math.max(6, size / 2);
        this.radius = Math.max(3, maxRadius / 4);
        this.step = Math.max(2, maxRadius / 6);
        this.color = color;
        this.sparkCount = sparkCount;
        setImage(render());
    }

    public void act()
    {
        if (!seeded)
        {
            seeded = true;
            spawnSparks();
        }

        radius += step;
        if (radius >= maxRadius)
        {
            getWorld().removeObject(this);
            return;
        }
        setImage(render());
    }

    private void spawnSparks()
    {
        World world = getWorld();
        if (world == null) return;
        for (int i = 0; i < sparkCount; i++)
        {
            double ang = Math.random() * Math.PI * 2;
            double spd = 1.5 + Math.random() * 3.5;
            int life = 12 + Greenfoot.getRandomNumber(16);
            int sz = 2 + Greenfoot.getRandomNumber(3);
            Color c = (Greenfoot.getRandomNumber(2) == 0) ? color : new Color(255, 230, 160);
            world.addObject(new Particle(Math.cos(ang) * spd, Math.sin(ang) * spd, life, sz, c, true),
                            getX(), getY());
        }
    }

    private GreenfootImage render()
    {
        int d = maxRadius * 2 + 4;
        GreenfootImage img = new GreenfootImage(d, d);
        int cx = d / 2;

        double f = (double) radius / maxRadius;      // 0..1
        int alpha = (int) (220 * (1.0 - f));
        if (alpha < 0) alpha = 0;

        // Aeusserer Ring.
        img.setColor(new Color(color.r, color.g, color.b, alpha));
        img.fillOval(cx - radius, cx - radius, radius * 2, radius * 2);

        // Heller Kern.
        int inner = (int) (radius * (1.0 - f) * 0.9);
        if (inner > 0)
        {
            int ka = Math.min(255, alpha + 60);
            img.setColor(new Color(255, 245, 210, ka));
            img.fillOval(cx - inner, cx - inner, inner * 2, inner * 2);
        }
        return img;
    }
}
