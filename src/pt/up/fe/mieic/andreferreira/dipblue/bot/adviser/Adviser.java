package pt.up.fe.mieic.andreferreira.dipblue.bot.adviser;

import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.*;
import pt.up.fe.mieic.andreferreira.dipblue.bot.DipBlue;

public abstract class Adviser {
    public static final String ADD = "ADD";
    public static final String MULTIPLY = "MULTIPLY";

    protected DipBlue player;
    protected double weight;
    protected int phase;
    public String operator;

    public Adviser(Double weight) {
        this.weight = weight;
    }

    public void init(DipBlue player) {
        this.player = player;
        init();
    }

    public double evaluate(Order advise, int phase) {
        this.phase = phase;
        return weight * evaluate(advise);
    }

    public static Region getFinalDestination(Order order) {
        Region region = null;
        if (order instanceof HLDOrder) {
            region = order.getLocation();
        } else if (order instanceof MTOOrder) {
            region = ((MTOOrder) order).getDestination();
        } else if (order instanceof SUPMTOOrder) {
            region = order.getLocation();
        } else if (order instanceof DSBOrder) {
            region = order.getLocation();
        } else if (order instanceof RTOOrder) {
            region = ((RTOOrder) order).getDestination();
        } else if (order instanceof BLDOrder) {
            region = ((BLDOrder) order).getDestination();
        } else if (order instanceof REMOrder) {
            region = order.getLocation();
        }
        return region;
    }

    protected abstract void init();

    public abstract void beforePhase();

    protected abstract double evaluate(Order advise);
}
