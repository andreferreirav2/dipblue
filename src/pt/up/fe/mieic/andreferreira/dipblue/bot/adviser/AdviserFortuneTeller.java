package pt.up.fe.mieic.andreferreira.dipblue.bot.adviser;

import com.google.common.collect.Lists;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;

import java.util.List;

/**
 * User: Andre
 * Date: 27-10-2013
 * Time: 17:33
 */
public class AdviserFortuneTeller extends Adviser {

    public AdviserFortuneTeller(Double weight) {
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
        double prob = 1.0;

        if (advise instanceof MTOOrder) {
            double valid = 0;
            double all = 1;

            List<Region> allRegions = Lists.newArrayList(advise.getLocation().getAdjacentRegions());
            allRegions.addAll(((MTOOrder) advise).getDestination().getAdjacentRegions());

            for (Region adjacent : allRegions) {
                Power controller = player.getGame().getController(adjacent);
                if (controller != null && !controller.equals(player.getMe())) {
                    all += adjacent.getAdjacentRegions().size() + 1;
                    valid += adjacent.getAdjacentRegions().size();
                }
            }

            prob = valid / all;
        }
        return prob;
    }
}

