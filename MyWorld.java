import greenfoot.*;

public class MyWorld extends World
{
    private int actCount = 0;
    private static final int SPAWN_INTERVAL = 100; //~3 seconds at 33 acts/sec

    public MyWorld()
    {    
        super(600, 400, 1);
        addObject(new Spaceship(), 50, 200);
    }
    
    @Override
    public void act()
    {
        actCount++;
        if (actCount >= SPAWN_INTERVAL)
        {
            spawnAlien();
            actCount = 0;
        }
    }
    
    private void spawnAlien()
    {
        int randomX = 450 + Greenfoot.getRandomNumber(600 - 450 + 1);   
        int randomY = Greenfoot.getRandomNumber(400);
        addObject(new Alien(), randomX, randomY);
    }
}