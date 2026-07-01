import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)

/**
 * Write a description of class Bullet here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Bullet extends Actor
{
    /**
     * Act - do whatever the Bullet wants to do. This method is called whenever
     * the 'Act' or 'Run' button gets pressed in the environment.
     */
public void act()
{
    if (isTouching(Alien.class))
    {
        Alien alienTouching = (Alien) getOneIntersectingObject(Alien.class);
        World world = getWorld();
        world.removeObject(alienTouching);
        world.removeObject(this);
        return;
    }
    
    if(getX() >= 590) {
        getWorld().removeObject(this);
    }

    move(1);
}
}
