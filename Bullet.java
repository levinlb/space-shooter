import greenfoot.*;

/**
 * Ein Laser-Schuss des Raumschiffs. Fliegt schnell nach rechts, zerstoert
 * Aliens bei Beruehrung und wird animiert (pulsierender/flackernder Laser,
 * im Code gezeichnet).
 */
public class Bullet extends Actor
{
    private static final int SPEED = 7; // schneller als zuvor (war 1)

    private int animCount = 0;

    public Bullet()
    {
        updateImage();
    }

    public void act()
    {
        animCount++;
        updateImage();

        if (isTouching(Alien.class))
        {
            Alien alienTouching = (Alien) getOneIntersectingObject(Alien.class);
            MyWorld world = (MyWorld) getWorld();
            world.removeObject(alienTouching);
            world.addScore(10);
            world.removeObject(this);
            return;
        }

        if (getX() >= 590)
        {
            getWorld().removeObject(this);
            return;
        }

        move(SPEED);
    }

    /**
     * Zeichnet den Laser als animierten Bolzen: cyan Glow, flackernder Kern
     * und heller Kopf. Der Frame wechselt fuer einen pulsierenden Effekt.
     */
    private void updateImage()
    {
        int w = 22, h = 8;
        GreenfootImage img = new GreenfootImage(w, h);
        int frame = (animCount / 2) % 3;

        // Aeusserer Glow (cyan), pulsierende Helligkeit.
        int glow = 150 + frame * 30;
        if (glow > 255) glow = 255;
        img.setColor(new Color(0, glow, 255, 170));
        img.fillRect(0, 2, w - 2, h - 4);

        // Innerer Kern, flackert zwischen Weiss/Blau/Gelb.
        Color core = (frame == 0) ? new Color(255, 255, 255)
                   : (frame == 1) ? new Color(180, 240, 255)
                                  : new Color(255, 255, 190);
        img.setColor(core);
        img.fillRect(2, 3, w - 6, h - 6);

        // Heller Kopf vorne.
        img.setColor(new Color(255, 255, 255));
        img.fillOval(w - 7, 1, 6, 6);

        setImage(img);
    }
}
