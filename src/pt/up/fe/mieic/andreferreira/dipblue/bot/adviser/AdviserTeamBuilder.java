package pt.up.fe.mieic.andreferreira.dipblue.bot.adviser;

import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;

/**
 * User: Andre
 * Date: 27-10-2013
 * Time: 17:33
 */
public class AdviserTeamBuilder extends Adviser {

    public AdviserTeamBuilder(Double weight) {
        super(weight);
    }

    @Override
    protected void init() {
        operator = Adviser.MULTIPLY;
    }

    @Override
    public void beforePhase() {

    }

    @Override
    protected double evaluate(Order advise) {
        if (advise instanceof MTOOrder) {
            if (((MTOOrder) advise).needsSupport() && ((MTOOrder) advise).getSupporters().size() > 0) {
                return 3.0;
            }
        }

        return 1.0;
    }
}

