package pt.up.fe.mieic.andreferreira.dipblue.bot.common;

import java.util.HashMap;
import java.util.Map;

/**
 * User: FerreiraA
 * Date: 03/11/13
 * Time: 23:42
 */
public class Opponent {

    private static Map<String, Integer> distances;

    static {
        distances = new HashMap<String, Integer>();
        distances.put("ENGFRA", 0);
        distances.put("ENGGER", 1);
        distances.put("ENGRUS", 4);
        distances.put("ENGAUS", 3);
        distances.put("ENGITA", 3);
        distances.put("ENGTUR", 8);

        distances.put("FRAGER", 0);
        distances.put("FRARUS", 2);
        distances.put("FRAAUS", 1);
        distances.put("FRAITA", 0);
        distances.put("FRATUR", 4);

        distances.put("GERRUS", 0);
        distances.put("GERAUS", 0);
        distances.put("GERITA", 1);
        distances.put("GERTUR", 3);

        distances.put("RUSAUS", 0);
        distances.put("RUSITA", 3);
        distances.put("RUSTUR", 0);

        distances.put("AUSITA", 0);
        distances.put("AUSTUR", 2);

        distances.put("ITATUR", 2);


    }

    /**
     * The name of the Power
     */
    private final String name;

    public double PEACE_RATIO = 0.5;
    public double WAR_RATIO = 2.0;

    /**
     * Ratio goes from 0 to infinity.
     * Standard is 1 which means neutral.
     * 0 means no threat.
     * Above 1 means increased threat
     */
    private double ratio;
    private boolean inPeace;
    private boolean inWar;
    private boolean wasinWar;

    public Opponent(String name) {
        this.name = name;
        ratio = 1.0;
        inPeace = false;
        inWar = false;
        wasinWar = false;
    }

    public String getName() {
        return name;
    }

    public double getRatio() {
        return ratio;
    }

    public void multRatio(double ratio) {
        this.ratio *= ratio;
    }

    public void addRatio(double ratio) {
        this.ratio += ratio;
    }

    public double getEffectiveRatio() {
        double mult = 1.0;
        if (isInPeace()) {
            mult = PEACE_RATIO;
        } else if (isInWar()) {
            mult = WAR_RATIO;
        }
        return ratio * mult;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    public boolean isInPeace() {
        return inPeace;
    }

    public void setInPeace(boolean inPeace) {
        this.inPeace = inPeace;
    }

    public boolean isInWar() {
        return inWar;
    }

    public void setInWar(boolean inWar) {
        this.inWar = inWar;
        if (inWar) {
            this.wasinWar = true;
        }
    }

    public static int distance(String name, String name1) {
        if (distances.containsKey(name + name1)) {
            return distances.get(name + name1);
        }
        return distances.get(name1 + name);
    }

    public boolean wasInWar() {
        return wasinWar;
    }
}
