package pt.up.fe.mieic.andreferreira.dipblue.bot.adviser;

import es.csic.iiia.fabregues.dip.orders.Order;
import pt.up.fe.mieic.andreferreira.dipblue.bot.common.AgreedOrder;

/**
 * User: Andre
 * Date: 27-10-2013
 * Time: 17:33
 */
public class AdviserAgreementExecutor extends Adviser {

    public AdviserAgreementExecutor(Double weight) {
        super(weight);
    }

    @Override
    protected void init() {
        operator = Adviser.ADD;
    }

    @Override
    public void beforePhase() {

    }

    @Override
    protected double evaluate(Order advise) {
        AgreedOrder[] orders = player.agreedOrders.toArray(new AgreedOrder[]{});
        for (AgreedOrder agreedOrder : orders) {
            if (agreedOrder.getOrder().equals(advise)) {
                return Double.POSITIVE_INFINITY;
            }
        }

        return 0.0;
    }
}

