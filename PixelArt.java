import greenfoot.*;
import java.util.Map;

/**
 * Hilfsklasse zum Erzeugen von Pixel-Art-Bildern aus einem Zeichen-Raster.
 * Jede Zeile ist ein String; jedes Zeichen wird ueber die Palette einer Farbe
 * zugeordnet. Zeichen, die nicht in der Palette stehen (z.B. '.'), bleiben
 * transparent. Jeder Rasterpunkt wird als scale x scale grosses Quadrat
 * gezeichnet -> klarer Pixel-Look, ganz ohne Bilddateien.
 */
public class PixelArt
{
    public static GreenfootImage fromRows(String[] rows, Map<Character, Color> palette, int scale)
    {
        int h = rows.length;
        int w = 0;
        for (String r : rows)
        {
            if (r.length() > w) w = r.length();
        }

        GreenfootImage img = new GreenfootImage(w * scale, h * scale);
        for (int y = 0; y < h; y++)
        {
            String row = rows[y];
            for (int x = 0; x < row.length(); x++)
            {
                Color c = palette.get(row.charAt(x));
                if (c != null)
                {
                    img.setColor(c);
                    img.fillRect(x * scale, y * scale, scale, scale);
                }
            }
        }
        return img;
    }
}
