import greenfoot.*;

/**
 * Basisklasse fuer alle Gegner (Asteroiden, Angreifer). Kapselt Trefferpunkte
 * (hp) sowie Punkte- und Credit-Wert und die gemeinsame Logik fuers Sterben
 * und Verschwinden.
 *
 * Alle Weltzugriffe sind gegen null abgesichert: Wird ein Gegner mitten in
 * seinem act() durch endGame()/Bombe entfernt, darf kein weiterer Zugriff auf
 * getWorld() erfolgen (fixt den NullPointerException beim Sterben).
 */
public abstract class Enemy extends Actor
{
    protected int hp = 1;
    protected int scoreValue = 10;
    protected int creditValue = 5;

    // Aussehen der Todesexplosion (von Unterklassen anpassbar).
    protected Color explosionColor = new Color(255, 170, 60);
    protected int explosionSize = 26;
    protected int explosionSparks = 8;

    /**
     * Wird von einem Spieler-Schuss aufgerufen. Zieht Schaden ab; bei 0 hp
     * stirbt der Gegner (mit Belohnung).
     */
    public void hit(int damage)
    {
        hp -= damage;
        if (hp <= 0)
        {
            die();
        }
    }

    /** Vom Spieler zerstoert: Punkte + Credits gutschreiben und entfernen. */
    public void die()
    {
        MyWorld world = getMyWorld();
        if (world != null)
        {
            onDeath(world);
            world.addObject(new Explosion(explosionSize, explosionColor, explosionSparks),
                            getX(), getY());
            world.addScore(scoreValue);
            world.addCredits(creditValue);
            world.removeObject(this);
        }
    }

    /**
     * Haken fuer Unterklassen, die beim Tod etwas Besonderes tun (z.B. der
     * Teiler, der sich in kleinere Gegner aufspaltet). Standard: nichts.
     */
    protected void onDeath(MyWorld world) { }

    /** Ohne Belohnung entfernen (entkommen oder in das Schiff gerammt). */
    protected void despawn()
    {
        MyWorld world = getMyWorld();
        if (world != null)
        {
            world.removeObject(this);
        }
    }

    protected MyWorld getMyWorld()
    {
        World world = getWorld();
        return (world instanceof MyWorld) ? (MyWorld) world : null;
    }

    /** Haelt einen y-Wert innerhalb der Weltgrenzen. */
    protected int clampY(int y)
    {
        World w = getWorld();
        if (w == null) return y;
        if (y < 0) return 0;
        if (y > w.getHeight() - 1) return w.getHeight() - 1;
        return y;
    }
}
