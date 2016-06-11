package pt.up.fe.mieic.andreferreira.dipblue.bot.common;

import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.orders.Order;

/**
 * User: FerreiraA
 * Date: 04/11/13
 * Time: 21:43
 */
public class AgreedOrder {
    private final Order order;
    private final Power power;

    public AgreedOrder(Order order, Power power) {
        this.order = order;
        this.power = power;
    }

    public Power getPower() {
        return power;
    }

    public Order getOrder() {
        return order;
    }
}
