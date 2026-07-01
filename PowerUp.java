import greenfoot.*;

/**
 * Ein einsammelbares Power-up. Es driftet nach links und wird aktiviert,
 * sobald das Raumschiff es beruehrt. Das Icon wird im Code generiert,
 * es werden keine Bilddateien benoetigt.
 *
 * Typen: Extra-Leben, Schnellfeuer (temporaer), Bildschirm-Bombe, Punkte-Bonus.
 */
public class PowerUp extends Actor
{
    public static final int LIFE = 0;
    public static final int RAPID_FIRE = 1;
    public static final int BOMB = 2;
    public static final int POINTS = 3;
    public static final int TYPE_COUNT = 4;

    private static final int SPEED = 1;
    private static final int RAPID_FIRE_DURATION = 250; // Acts (~5s bei speed=50)
    private static final int POINTS_BONUS = 50;

    private final int type;

    public PowerUp(int type)
    {
        this.type = type;
        setImage(createIcon(type));
    }

    public void act()
    {
        // Einsammeln durch Beruehrung mit dem Raumschiff.
        if (isTouching(Spaceship.class))
        {
            Spaceship ship = (Spaceship) getOneIntersectingObject(Spaceship.class);
            applyEffect(ship);
            getWorld().removeObject(this);
            return;
        }

        // Nicht eingesammelt -> links aus dem Bild (ohne Strafe).
        if (getX() < 0)
        {
            getWorld().removeObject(this);
            return;
        }

        move(-SPEED);
    }

    private void applyEffect(Spaceship ship)
    {
        MyWorld world = (MyWorld) getWorld();
        switch (type)
        {
            case LIFE:
                world.addLife();
                break;
            case RAPID_FIRE:
                if (ship != null)
                {
                    ship.activateRapidFire(RAPID_FIRE_DURATION);
                }
                break;
            case BOMB:
                world.bombAliens();
                break;
            case POINTS:
                world.addScore(POINTS_BONUS);
                break;
        }
    }

    /**
     * Erzeugt ein farbiges Icon mit Buchstaben je nach Power-up-Typ.
     */
    private GreenfootImage createIcon(int type)
    {
        int size = 26;
        GreenfootImage img = new GreenfootImage(size, size);

        Color fill;
        String label;
        switch (type)
        {
            case LIFE:       fill = new Color(220, 40, 40);  label = "+"; break; // rot
            case RAPID_FIRE: fill = new Color(240, 200, 30); label = "F"; break; // gelb
            case BOMB:       fill = new Color(240, 120, 20); label = "B"; break; // orange
            case POINTS:     fill = new Color(40, 180, 220); label = "$"; break; // cyan
            default:         fill = new Color(150, 150, 150); label = "?"; break;
        }

        img.setColor(fill);
        img.fillOval(1, 1, size - 3, size - 3);
        img.setColor(new Color(255, 255, 255));
        img.drawOval(1, 1, size - 3, size - 3);
        img.setFont(new Font(true, false, 16));
        img.drawString(label, 8, 19);

        return img;
    }
}
