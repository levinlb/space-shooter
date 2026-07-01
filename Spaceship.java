import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)
import java.util.ArrayList;

/**
 * Write a description of class Spaceship here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Spaceship extends Actor
{
    private ArrayList<Bullet> bullets = new ArrayList<Bullet>();
    
    /**
     * Act - do whatever the Spaceship wants to do. This method is called whenever
     * the 'Act' or 'Run' button gets pressed in the environment.
     */
    public void act()
    {
        if (Greenfoot.isKeyDown("up")) {
             setLocation(getX(), getY() - 1);
        }
        
        if (Greenfoot.isKeyDown("down")) {
             setLocation(getX(), getY() + 1);
        }
        
        if (Greenfoot.isKeyDown("space")) {
            fireBullet();
        }
        
        
        // Add your action code here.
    }
    
    private void fireBullet() {
        Bullet newBullet = new Bullet();
        
        getWorld().addObject(newBullet, getX() + 10, getY());
        bullets.add(newBullet);
    }
}
