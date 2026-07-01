import greenfoot.*;

/**
 * Ein feindliches Projektil. Fliegt mit konstanter Geschwindigkeit in Richtung
 * der Position, die das Schiff beim Abschuss hatte, und fuegt dem Schiff bei
 * Treffer Schaden zu. Pixel-Art (roter Bolzen), im Code gezeichnet.
 */
public class EnemyBullet extends Actor
{
    private static final double SPEED = 4.0;
    private static final int DAMAGE = 34;

    private double x, y, vx, vy;

    public EnemyBullet(int fromX, int fromY, int targetX, int targetY)
    {
        x = fromX;
        y = fromY;

        double dx = targetX - fromX;
        double dy = targetY - fromY;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001) len = 1;
        vx = dx / len * SPEED;
        vy = dy / len * SPEED;

        setImage(buildImage());
    }

    public void act()
    {
        x += vx;
        y += vy;

        World world = getWorld();
        if (world == null) return;

        if (x < -10 || x > world.getWidth() + 10 || y < -10 || y > world.getHeight() + 10)
        {
            world.removeObject(this);
            return;
        }

        setLocation((int) x, (int) y);

        Spaceship ship = (Spaceship) getOneIntersectingObject(Spaceship.class);
        if (ship != null)
        {
            ship.takeDamage(DAMAGE);
            world.removeObject(this);
        }
    }

    private GreenfootImage buildImage()
    {
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
