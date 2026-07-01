import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)

/**
 * Write a description of class Aliens here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Alien extends Actor
{
    /**
     * Act - do whatever the Aliens wants to do. This method is called whenever
     * the 'Act' or 'Run' button gets pressed in the environment.
     */
    public void act()
    {
        if(getX() < 10) {
            // Alien ist entkommen -> ein Leben verloren.
            ((MyWorld) getWorld()).loseLife();
            getWorld().removeObject(this);
            return;
        }

        move(-1);
    }
}
