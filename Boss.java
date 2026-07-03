import greenfoot.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Ein Boss-Gegner. Erscheint auf jeder 5. Welle. Er faehrt von rechts ein,
 * patrouilliert dann vertikal und feuert wechselnde Angriffsmuster (gezieltes
 * Sperrfeuer, Kugelkreise, Spiralen, Kugelwaende). Ist seine Lebensleiste unter
 * der Haelfte, geht er in Phase 2: schneller und aggressiver.
 *
 * Es gibt drei Varianten, die sich abwechseln (Kreuzer, Zerstoerer,
 * Mutterschiff) - jede mit eigenem Aussehen und Musterschwerpunkt. Beim Tod
 * gibt es eine grosse Explosion und viele Punkte/Credits; danach folgt die
 * Waffenwahl.
 */
public class Boss extends Enemy
{
    // Angriffsmuster.
    private static final int AIMED  = 0;
    private static final int RADIAL = 1;
    private static final int SPIRAL = 2;
    private static final int WALL   = 3;
    private static final int SUMMON = 4;

    private final int bossNumber;
    private final int variant;
    private final int maxHp;
    private final String name;
    private final Color bodyColor;
    private final int[] patterns;      // Musterfolge dieser Variante

    private double x, y;
    private boolean placed = false;
    private boolean entering = true;
    private int targetX;
    private double bobPhase = 0;
    private double amp;
    private int centerY;

    private int patternIndex = 0;
    private int patternTimer = 0;
    private int fireTimer = 0;
    private double spiralAngle = 0;

    private boolean phase2 = false;
    private int contactCooldown = 0;
    private int hitFlash = 0;

    private GreenfootImage body;
    private GreenfootImage bodyBright;

    public Boss(int bossNumber)
    {
        this.bossNumber = Math.max(1, bossNumber);
        this.variant = (this.bossNumber - 1) % 3;

        this.maxHp = 110 + this.bossNumber * 55;
        this.hp = maxHp;
        this.scoreValue = 300 * this.bossNumber;
        this.creditValue = 100 + 45 * this.bossNumber;
        this.explosionColor = new Color(255, 180, 90);
        this.explosionSize = 70;
        this.explosionSparks = 26;

        switch (variant)
        {
            case 0:
                name = "KREUZER";
                bodyColor = new Color(200, 70, 90);
                patterns = new int[] { AIMED, RADIAL, AIMED, WALL };
                break;
            case 1:
                name = "ZERSTOERER";
                bodyColor = new Color(150, 90, 210);
                patterns = new int[] { SPIRAL, WALL, SPIRAL, RADIAL };
                break;
            default:
                name = "MUTTERSCHIFF";
                bodyColor = new Color(80, 140, 190);
                patterns = new int[] { RADIAL, SUMMON, AIMED, RADIAL };
                break;
        }

        body = buildImage(bodyColor, false);
        bodyBright = buildImage(bodyColor, true);
        setImage(body);
    }

    public void act()
    {
        World w = getWorld();
        if (w == null) return;

        if (!placed)
        {
            x = getX();
            y = getY();
            targetX = w.getWidth() - 95;
            centerY = w.getHeight() / 2;
            amp = w.getHeight() / 2.0 - 55;
            placed = true;
        }

        if (hitFlash > 0)
        {
            hitFlash--;
            if (hitFlash == 0) setImage(body);
        }

        if (contactCooldown > 0) contactCooldown--;

        // Phase 2 aktivieren.
        if (!phase2 && hp <= maxHp / 2)
        {
            phase2 = true;
            triggerPhaseChange(w);
        }

        if (entering)
        {
            x -= 2;
            if (x <= targetX)
            {
                x = targetX;
                entering = false;
            }
            setLocation((int) Math.round(x), (int) Math.round(y));
            return;   // waehrend der Einfahrt kein Feuer
        }

        // Vertikale Patrouille.
        double bobSpeed = phase2 ? 0.045 : 0.03;
        bobPhase += bobSpeed;
        y = centerY + Math.sin(bobPhase) * amp;
        setLocation((int) Math.round(x), (int) Math.round(y));

        // Kontaktschaden.
        MyWorld mw = getMyWorld();
        Spaceship ship = (mw != null) ? mw.getShip() : null;
        if (ship != null && contactCooldown == 0 && intersects(ship))
        {
            ship.takeDamage(50);
            contactCooldown = 40;
        }

        updateAttacks(w, ship);
    }

    private void updateAttacks(World w, Spaceship ship)
    {
        // Muster in Abstaenden wechseln.
        patternTimer++;
        int patternLen = phase2 ? 150 : 200;
        if (patternTimer >= patternLen)
        {
            patternTimer = 0;
            patternIndex = (patternIndex + 1) % patterns.length;
        }

        if (fireTimer > 0)
        {
            fireTimer--;
            return;
        }

        switch (patterns[patternIndex])
        {
            case AIMED:  doAimed(w, ship);  break;
            case RADIAL: doRadial(w);       break;
            case SPIRAL: doSpiral(w);       break;
            case WALL:   doWall(w);         break;
            case SUMMON: doSummon(w, ship); break;
        }
    }

    private void doAimed(World w, Spaceship ship)
    {
        double base = Math.PI;
        if (ship != null) base = Math.atan2(ship.getY() - y, ship.getX() - x);
        for (double off : new double[] { -0.18, 0.0, 0.18 })
        {
            w.addObject(new EnemyBullet(x - 30, y, base + off, 3.4, 22, EnemyBullet.STYLE_BOSS),
                        (int) x - 30, (int) y);
        }
        fireTimer = phase2 ? 24 : 38;
    }

    private void doRadial(World w)
    {
        int n = phase2 ? 18 : 12;
        double start = Math.random() * Math.PI * 2;
        for (int i = 0; i < n; i++)
        {
            double ang = start + i * (2 * Math.PI / n);
            w.addObject(new EnemyBullet(x, y, ang, 2.6, 20, EnemyBullet.STYLE_BOSS),
                        (int) x, (int) y);
        }
        fireTimer = phase2 ? 45 : 65;
    }

    private void doSpiral(World w)
    {
        int arms = phase2 ? 3 : 2;
        for (int a = 0; a < arms; a++)
        {
            double ang = spiralAngle + a * (2 * Math.PI / arms);
            w.addObject(new EnemyBullet(x, y, ang, 3.0, 18, EnemyBullet.STYLE_BOSS),
                        (int) x, (int) y);
        }
        spiralAngle += 0.4;
        fireTimer = phase2 ? 4 : 7;
    }

    private void doWall(World w)
    {
        // Senkrechte Kugelwand mit einer Luecke.
        int h = w.getHeight();
        int gap = 40 + Greenfoot.getRandomNumber(h - 160);
        for (int py = 20; py < h - 20; py += 34)
        {
            if (Math.abs(py - gap) < 44) continue;   // Luecke zum Ausweichen
            w.addObject(new EnemyBullet(x - 30, py, Math.PI, 3.0, 20, EnemyBullet.STYLE_BOSS),
                        (int) x - 30, py);
        }
        fireTimer = phase2 ? 55 : 85;
    }

    private void doSummon(World w, Spaceship ship)
    {
        // Zwei Begleitdrohnen ausspucken, dazu ein kleiner Kreis.
        w.addObject(new Drohne(), (int) x - 40, (int) y - 20);
        w.addObject(new Drohne(), (int) x - 40, (int) y + 20);
        doRadial(w);
        fireTimer = phase2 ? 60 : 90;
    }

    @Override
    public void hit(int damage)
    {
        hp -= damage;
        if (hp <= 0)
        {
            die();
            return;
        }
        // Kurzes Aufblitzen als Trefferrueckmeldung.
        hitFlash = 3;
        setImage(bodyBright);
    }

    @Override
    public void die()
    {
        MyWorld world = getMyWorld();
        if (world == null) return;

        // Grosse, mehrfache Explosion.
        for (int i = 0; i < 6; i++)
        {
            int ox = getX() + Greenfoot.getRandomNumber(70) - 35;
            int oy = getY() + Greenfoot.getRandomNumber(70) - 35;
            world.addObject(new Explosion(50 + Greenfoot.getRandomNumber(30),
                            explosionColor, 10), ox, oy);
        }
        world.screenFlash(new Color(255, 220, 150), 140);
        world.addScore(scoreValue);
        world.addCredits(creditValue);
        world.removeObject(this);
    }

    private void triggerPhaseChange(World w)
    {
        for (int i = 0; i < 3; i++)
        {
            w.addObject(new Explosion(34, new Color(255, 120, 90), 8),
                        getX() + Greenfoot.getRandomNumber(60) - 30,
                        getY() + Greenfoot.getRandomNumber(60) - 30);
        }
        MyWorld mw = getMyWorld();
        if (mw != null) mw.screenFlash(new Color(255, 120, 90), 90);
    }

    public String getName() { return name; }
    public double getHpFraction() { return Math.max(0, (double) hp / maxHp); }
    public boolean isPhase2() { return phase2; }

    private GreenfootImage buildImage(Color base, boolean bright)
    {
        Color c  = bright ? lighten(base, 90) : base;
        Color d  = darken(base, 60);
        Color hi = lighten(base, 60);

        String[] rows;
        if (variant == 0)   // Kreuzer - pfeilfoermig
        {
            rows = new String[] {
                "......KKKK....",
                "....KKCCCCK...",
                "..KKCCCCCCCK..",
                "GKCCCHHHHCCCK.",
                "GKCCHWWWWHCCKK",
                "GKCCCHHHHCCCK.",
                "..KKCCCCCCCK..",
                "....KKCCCCK...",
                "......KKKK...."
            };
        }
        else if (variant == 1)   // Zerstoerer - breit, kantig
        {
            rows = new String[] {
                "KKK......KKK..",
                "KCCKKKKKKCCK..",
                "KCCCCCCCCCCKK.",
                "KCCHHWWHHCCCKG",
                "KCCHW00WHCCCKG",
                "KCCHHWWHHCCCKG",
                "KCCCCCCCCCCKK.",
                "KCCKKKKKKCCK..",
                "KKK......KKK.."
            };
        }
        else                     // Mutterschiff - rund und massig
        {
            rows = new String[] {
                "...KKKKKKKK...",
                ".KKCCCCCCCCKK.",
                "KCCCHHHHHHCCCK",
                "KCCHWWWWWWHCCK",
                "GCCHW0000WHCCG",
                "KCCHWWWWWWHCCK",
                "KCCCHHHHHHCCCK",
                ".KKCCCCCCCCKK.",
                "...KKKKKKKK..."
            };
        }

        Map<Character, Color> p = new HashMap<Character, Color>();
        p.put('C', c);
        p.put('K', d);
        p.put('H', hi);
        p.put('W', new Color(255, 240, 220));
        p.put('0', new Color(255, 90, 90));
        p.put('G', new Color(190, 190, 200));
        return PixelArt.fromRows(rows, p, 6);
    }

    private Color lighten(Color c, int amt)
    {
        return new Color(Math.min(255, c.r + amt), Math.min(255, c.g + amt), Math.min(255, c.b + amt));
    }

    private Color darken(Color c, int amt)
    {
        return new Color(Math.max(0, c.r - amt), Math.max(0, c.g - amt), Math.max(0, c.b - amt));
    }
}
