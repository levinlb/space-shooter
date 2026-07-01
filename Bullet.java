import greenfoot.*;

/**
 * Ein Laser-Schuss des Raumschiffs (oder eines Turrets). Fliegt schnell nach
 * rechts, trifft Gegner (Alien/Attacker) und wird animiert (pulsierender Laser,
 * im Code gezeichnet). Der Schaden haengt vom Waffen-Upgrade ab.
 */
public class Bullet extends Actor
{
    private static final int SPEED = 7;

    private final int damage;
    private int animCount = 0;

    public Bullet()
    {
        this(1);
    }

    public Bullet(int damage)
    {
        this.damage = damage;
        updateImage();
    }

    public void act()
    {
        animCount++;
        updateImage();

        Enemy enemy = (Enemy) getOneIntersectingObject(Enemy.class);
        if (enemy != null)
        {
            World world = getWorld();
            enemy.hit(damage);
            if (world != null) world.removeObject(this);
            return;
        }

        if (getX() >= 590)
        {
            getWorld().removeObject(this);
            return;
        }

        move(SPEED);
    }

    private void updateImage()
    {
        int w = 22, h = 8;
        GreenfootImage img = new GreenfootImage(w, h);
        int frame = (animCount / 2) % 3;

        int glow = 150 + frame * 30;
        if (glow > 255) glow = 255;
        img.setColor(new Color(0, glow, 255, 170));
        img.fillRect(0, 2, w - 2, h - 4);

        Color core = (frame == 0) ? new Color(255, 255, 255)
                   : (frame == 1) ? new Color(180, 240, 255)
                                  : new Color(255, 255, 190);
        img.setColor(core);
        img.fillRect(2, 3, w - 6, h - 6);

        img.setColor(new Color(255, 255, 255));
        img.fillOval(w - 7, 1, 6, 6);

        setImage(img);
    }
}
