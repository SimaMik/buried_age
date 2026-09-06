import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Builds the shipped pegasus texture from the artist's original plus the vanilla saddle sheet:
 * the top strip of {@code textures/entity/equipment/horse_saddle/saddle.png} (body saddle, head
 * strap, mouth wrap, bit rings) is pasted into the unused corner at (32,104) of the 128x128 sheet,
 * so the saddle bones of the geo model can point at unchanged vanilla pixels.
 *
 * Usage: java tools/PasteSaddle.java <artist original> <vanilla saddle.png> <output pegasus.png>
 */
public final class PasteSaddle {
    static final int DST_X = 32;
    static final int DST_Y = 104;
    static final int STRIP_W = 64;
    static final int STRIP_H = 22;

    public static void main(String[] args) throws Exception {
        BufferedImage base = ImageIO.read(new File(args[0]));
        BufferedImage saddle = ImageIO.read(new File(args[1]));
        if (base.getWidth() != 128 || base.getHeight() != 128) {
            throw new IllegalStateException("artist texture is " + base.getWidth() + "x" + base.getHeight() + ", expected 128x128");
        }
        if (saddle.getWidth() != 64 || saddle.getHeight() != 64) {
            throw new IllegalStateException("saddle sheet is " + saddle.getWidth() + "x" + saddle.getHeight() + ", expected 64x64");
        }
        BufferedImage out = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        int overwritten = 0;
        int painted = 0;
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                out.setRGB(x, y, base.getRGB(x, y));
            }
        }
        for (int y = 0; y < STRIP_H; y++) {
            for (int x = 0; x < STRIP_W; x++) {
                int argb = saddle.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                if ((base.getRGB(DST_X + x, DST_Y + y) >>> 24) != 0) {
                    overwritten++;
                }
                out.setRGB(DST_X + x, DST_Y + y, argb);
                painted++;
            }
        }
        ImageIO.write(out, "png", new File(args[2]));
        System.out.println("pasted " + painted + " saddle pixels at (" + DST_X + "," + DST_Y + "), overwrote " + overwritten + " artist pixels");
    }
}
