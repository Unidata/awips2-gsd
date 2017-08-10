package gov.noaa.gsd.viz.ensemble.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.procedures.Bundle;

/**
 * Generic Utilities class to contain a hodge-podge of utility capabilties.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 08, 2014  5056       polster     Initial creation
 * Nov 19, 2016  19443      polster     Add a dump bundle method
 * 
 * </pre>
 * 
 * @author polster
 * @version 1.0
 */
public class Utilities {

    private static int lastRandom = 0;

    private Utilities() {
        super();
    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }

    public static String removeExtraSpaces(String source) {
        String result = source.replaceAll("\\s+", " ");
        return result;
    }

    public static String trimQuotes(String pqs) {
        if (pqs.startsWith("\"")) {
            pqs = pqs.substring(1);
        }
        if (pqs.endsWith("\"")) {
            pqs = pqs.substring(0, pqs.length() - 1);
        }
        return pqs;
    }

    /**
     * This method returns either a randmomly generated color or a color
     * randomly chosen from a set of global colors.
     * 
     * @return RGB of the produced color
     */
    public static RGB getRandomNiceContrastColor() {

        RGB niceColor = null;

        /*
         * Either randomly generate an RGB value or randomly pick an RGB value
         * from the large list of global colors enumeration.
         */
        Random random = new Random();
        int index = random.nextInt(2);
        if (index == 0) {

            /*
             * To make sure we don't get anything close to a gray color, use
             * different ranges for min and max for red, green, and blue, and
             * randomly assign the ranges.
             */

            final int a_max = 255;
            final int a_min = 220;

            final int b_max = 220;
            final int b_min = 190;

            final int c_max = 190;
            final int c_min = 145;

            int red = 0;
            int blue = 0;
            int green = 0;

            /*
             * Don't allow the same random number (i.e. from 0 - 5) to be used
             * in consecutive calls which gives a greater chance of not getting
             * similar colors on back-to-back calls.
             */
            int r = random.nextInt(6);
            while (lastRandom == r) {
                r = random.nextInt(6);
            }
            lastRandom = r;
            switch (r) {
            case 0:
                red = (int) (Math.random() * (a_max - a_min) + a_min);
                blue = (int) (Math.random() * (b_max - b_min) + b_min);
                green = (int) (Math.random() * (c_max - c_min) + c_min);
                break;
            case 1:
                red = (int) (Math.random() * (b_max - b_min) + b_min);
                blue = (int) (Math.random() * (c_max - c_min) + c_min);
                green = (int) (Math.random() * (a_max - a_min) + a_min);
                break;
            case 2:
                red = (int) (Math.random() * (c_max - c_min) + c_min);
                blue = (int) (Math.random() * (a_max - a_min) + a_min);
                green = (int) (Math.random() * (b_max - b_min) + b_min);
                break;
            case 3:
                red = (int) (Math.random() * (a_max - a_min) + a_min);
                blue = (int) (Math.random() * (c_max - c_min) + c_min);
                green = (int) (Math.random() * (b_max - b_min) + b_min);
                break;
            case 4:
                red = (int) (Math.random() * (b_max - b_min) + b_min);
                blue = (int) (Math.random() * (a_max - a_min) + a_min);
                green = (int) (Math.random() * (c_max - c_min) + c_min);
                break;
            case 5:
                red = (int) (Math.random() * (c_max - c_min) + c_min);
                blue = (int) (Math.random() * (b_max - b_min) + b_min);
                green = (int) (Math.random() * (a_max - a_min) + a_min);
                break;
            }

            niceColor = new RGB(red, green, blue);

        } else {
            /*
             * Return a random RGB between 0 and GlobalColor.size()
             */
            GlobalColor gcolor = null;
            do {
                index = random.nextInt(GlobalColor.size());
                gcolor = GlobalColor.getAtOrdinal(index);
            } while (gcolor.isHighContrast() == false);
            niceColor = gcolor.getRGB();
        }
        return niceColor;
    }

    /**
     * Create a gradient between a color and White.
     * 
     * @param color1
     *            The color to gradiate.
     * 
     * @param ratio
     *            Blend ratio. 0.5 will give even blend, 1.0 will return color1,
     *            0.0 will return color2 and so on.
     * @return Blended color.
     */
    public static Color gradient(Color color1, double ratio) {

        float r = (float) ratio;
        float ir = (float) 1.0 - r;

        RGB rgb1 = color1.getRGB();
        RGB rgb2 = GlobalColor.get(GlobalColor.WHITE).getRGB();

        int r1 = rgb1.red;
        int b1 = rgb1.blue;
        int g1 = rgb1.green;
        int red = 255;
        int blue = 255;
        int green = 255;

        if ((b1 > r1) && (b1 > g1)) {
            red = (int) (rgb1.red * ir + rgb2.red * r);
            green = (int) (rgb1.green * ir + rgb2.green * r);
            blue = b1;
        } else if ((r1 > b1) && (r1 > g1)) {
            red = r1;
            blue = (int) (rgb1.blue * ir + rgb2.blue * r);
            green = (int) (rgb1.green * ir + rgb2.green * r);
        } else if ((g1 > b1) && (g1 > r1)) {
            red = (int) (rgb1.red * ir + rgb2.red * r);
            blue = (int) (rgb1.blue * ir + rgb2.blue * r);
            green = g1;
        } else {
            red = (int) (rgb1.red * ir + rgb2.red * r);
            blue = (int) (rgb1.blue * ir + rgb2.blue * r);
            green = (int) (rgb1.green * ir + rgb2.green * r);
        }

        RGB blend = new RGB(red, blue, green);

        Color color = SWTResourceManager.getColor(blend);
        return color;

    }

    public static RGB desaturate(RGB c) {

        float[] hsb = c.getHSB();
        hsb[1] = 0.15f;
        RGB nc = new RGB(hsb[0], hsb[1], hsb[2]);
        return nc;
    }

    public static RGB brighten(RGB c) {

        float[] hsb = c.getHSB();
        hsb[2] = 1.0f;
        RGB nc = new RGB(hsb[0], hsb[1], hsb[2]);
        return nc;
    }

    /**
     * This method prints an Object reference as a series of eight (8) upper
     * case hexidecimal values.
     * 
     * @param o
     *            - An object reference
     * @return - A string containing the hex representation of an Object
     *         reference
     */
    public static String getReference(Object o) {

        String es = null;
        if (o == null) {
            es = "@00000000";
        } else {
            es = o.toString();
        }
        int atSignIndex = es.indexOf("@");
        if (atSignIndex > 0) {
            es = es.substring(atSignIndex);
        }
        es = es.toUpperCase();
        return es;
    }

    public static void dumpMap(PrintStream out, Map<String, String> map,
            String keyDescr, String valueDescr) {

        String variable = null;
        String value = null;

        out.println("");
        out.println(
                "----------------------------------------------------------");
        Set<String> keySet = map.keySet();
        Iterator<String> variablesIter = keySet.iterator();
        while (variablesIter.hasNext()) {
            variable = variablesIter.next();
            value = map.get(variable);
            out.println(">>>>>>>>> " + keyDescr + " " + variable + " "
                    + valueDescr + ": " + value);
        }
        out.println(
                "----------------------------------------------------------");
        out.println("");

    }

    public static void echo(PrintStream out, String str) {

        out.println("");
        out.println(
                "----------------------------------------------------------");
        out.println(">>>>>>>>>>>>>>>>>  " + str);
        out.println(
                "----------------------------------------------------------");
        out.println("");

    }

    public static String bytesIntoHumanReadable(long bytes) {
        final float kilobyte = 1024.0f;
        final float megabyte = kilobyte * 1024.0f;
        final float gigabyte = megabyte * 1024.0f;
        final float terabyte = gigabyte * 1024.0f;
        String result = null;
        if ((bytes >= 0) && (bytes < kilobyte)) {
            result = String.format("%.1f %s", Float.valueOf(bytes), "B");

        } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
            result = String.format("%.1f %s",
                    Float.valueOf(((float) bytes / kilobyte)), "KB");

        } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
            result = String.format("%.1f %s",
                    Float.valueOf(((float) bytes / megabyte)), "MB");

        } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
            result = String.format("%.1f %s",
                    Float.valueOf(((float) bytes / gigabyte)), "GB");

        } else if (bytes >= terabyte) {
            result = String.format("%.1f %s",
                    Float.valueOf(((float) bytes / terabyte)), "TB");
        } else {
            return bytes + " Bytes";
        }
        return result;
    }

    public static void dumpStackTrace(int traceCount) {

        Exception e = new Exception();
        StackTraceElement[] traces = e.getStackTrace();
        System.out.println("________________");
        int count = 0;
        for (StackTraceElement ste : traces) {
            if (ste.toString().indexOf("dumpStackTrace") >= 0) {
                continue;
            }
            System.out.println(">>>>> " + ste.toString());
            count++;
            if (count == traceCount)
                break;
        }
        System.out.println(
                "__________________________________________________________________");

    }

    public static void dumpBundleToFile(Bundle b, String fileLocation,
            String prefix) {
        String bundleAsXML = null;
        try {
            bundleAsXML = b.toXML();
        } catch (VizException e) {
            return;
        }

        if (!fileLocation.endsWith(File.separator)) {
            fileLocation.concat(File.separator);
        }

        if (bundleAsXML != null && bundleAsXML.length() > 0) {
            Path outFile = Paths.get(fileLocation + prefix + "-bundle.xml");
            Charset charset = Charset.forName("UTF-8");
            try (BufferedWriter writer = Files.newBufferedWriter(outFile,
                    charset)) {
                writer.write(bundleAsXML, 0, bundleAsXML.length());
            } catch (IOException x) {
                /* ignore */
            }
        }
    }
}