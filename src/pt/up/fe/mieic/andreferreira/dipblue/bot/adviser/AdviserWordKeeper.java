package pt.up.fe.mieic.andreferreira.dipblue.bot.adviser;

import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.Order;
import pt.up.fe.mieic.andreferreira.dipblue.bot.common.Opponent;

/**
 * User: Andre
 * Date: 27-10-2013
 * Time: 17:33
 */
public class AdviserWordKeeper extends Adviser {

    public AdviserWordKeeper(Double weight) {
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
        Region region = getFinalDestination(advise);

        if (region != null) {
            Power victim = player.getGame().getController(region);
            if (victim != null) {
                Opponent opponent = player.opponents.get(victim.getName());
                if (opponent != null) {
                    return opponent.getEffectiveRatio();
                }
            }
        }

        return 1.0;
    }
}

