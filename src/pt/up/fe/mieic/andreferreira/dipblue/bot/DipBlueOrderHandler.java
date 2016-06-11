package pt.up.fe.mieic.andreferreira.dipblue.bot;

import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.*;
import pt.up.fe.mieic.andreferreira.dipblue.bot.adviser.Adviser;
import pt.up.fe.mieic.andreferreira.dipblue.bot.common.AgreedOrder;

import java.util.*;

public class DipBlueOrderHandler {
    private final DipBlue player;
    public int holds = 0;
    public int moves = 0;
    public int movesCut = 0;
    public int supports = 0;
    public int supportsAlly = 0;
    public int walk = 0;
    public int attackSelf = 0;
    public int attackAlly = 0;
    public int attackOtherDude = 0;
    public int attackEnemy = 0;

    public DipBlueOrderHandler(DipBlue player) {
        this.player = player;
    }

    /**
     * Run all advisers to evaluate the movement action to take in SPR/FAL.
     * Possible moves: HLD Hold MTO Move to SUP Support Holding SUPMTO Support
     * Move to
     */
    protected List<Order> evaluateMove(List<Region> units) {
        List<Order> allOrders = new ArrayList<Order>();
        for (Region unit : units) {
            allOrders.add(new HLDOrder(player.getMe(), unit));
            for (Region attack : unit.getAdjacentRegions()) {
                MTOOrder move = new MTOOrder(player.getMe(), unit, attack);
                Power owner = player.getGame().getController(attack);
                if (owner != null && !player.getMe().equals(owner)) {
                    move.setNeedOfSupport(true);
                }

                List<Region> supporters = new ArrayList<Region>();
                for (Region adjacentToAttack : attack.getAdjacentRegions()) {
                    if (player.getMe().getControlledRegions().contains(adjacentToAttack) && !adjacentToAttack.equals(unit)) {
                        supporters.add(adjacentToAttack);
                    }
                }
                move.setSupporters(supporters);
                allOrders.add(move);
            }
        }


        for (Iterator<AgreedOrder> iterator = player.agreedOrders.iterator(); iterator.hasNext(); ) {
            AgreedOrder agreedOrder = iterator.next();
            allOrders.add(agreedOrder.getOrder());
        }

        evaluateOrders(allOrders);
        Collections.sort(allOrders);
        List<Order> ordersToExecute = getOrdersToExecute(allOrders);
        profileOrders(ordersToExecute);
        player.agreedOrders.clear();

        return ordersToExecute;
    }

    /**
     * Run all advisers to evaluate the retreat action to take in SUM/AUT.
     * Possible moves: RTO Retreat to DSB Disband
     */
    protected List<Order> evaluateRetreat(List<Region> dislodgedUnits) {
        List<Order> orders = new ArrayList<Order>();
        List<Region> doneRegions = new ArrayList<Region>();
        List<Order> ordersToExecute = new ArrayList<Order>();

        for (Region dislodgedUnit : dislodgedUnits) {
            for (Region retreat : player.getGame().getDislodgedRegions().get(dislodgedUnit).getRetreateTo()) {
                orders.add(new RTOOrder(dislodgedUnit, player.getMe(), retreat));
            }
            orders.add(new DSBOrder(dislodgedUnit, player.getMe()));
        }
        evaluateOrders(orders);

        for (Order order : orders) {
            if (!doneRegions.contains(order.getLocation())) {
                doneRegions.add(order.getLocation());
                ordersToExecute.add(order);
            }
        }

        return ordersToExecute;
    }

    /**
     * Run all advisers to evaluate the build action to take in WIN. Possible
     * moves: BLD Build unit WVE Waive build
     */
    protected List<Order> evaluateBuild(int nBuilds) {
        List<Order> orders = new ArrayList<Order>();
        List<Order> ordersToExecute = new ArrayList<Order>();
        List<Province> destinations = new ArrayList<Province>();

        for (Region emptyBuildRegion : player.getGame().getBuildHomeList(player.getMe())) {
            orders.add(new BLDOrder(player.getMe(), emptyBuildRegion));
        }
        for (int i = 0; i < nBuilds; i++) {
            orders.add(new WVEOrder(player.getMe()));
        }

        evaluateOrders(orders);

        for (int i = 0; i < orders.size() && ordersToExecute.size() < nBuilds; i++) {
            Order order = orders.get(i);
            if (order instanceof BLDOrder) {
                if (!destinations.contains(order.getLocation().getProvince())) {
                    destinations.add(order.getLocation().getProvince());
                    ordersToExecute.add(order);
                }
            } else if (order instanceof WVEOrder) {
                ordersToExecute.add(order);
            }
        }

        return ordersToExecute;
    }

    /**
     * Run all advisers to evaluate the remove action to take in WIN. Possible
     * moves: REM Remove unit
     */
    protected List<Order> evaluateRemove(int nRemovals) {
        List<Order> orders = new ArrayList<Order>();
        List<Region> doneRegions = new ArrayList<Region>();
        List<Order> ordersToExecute = new ArrayList<Order>();

        for (Region occupiedRegion : player.getMe().getControlledRegions()) {
            orders.add(new REMOrder(player.getMe(), occupiedRegion));
        }

        evaluateOrders(orders);

        for (int i = 0; i < orders.size() && ordersToExecute.size() < nRemovals; i++) {
            Order order = orders.get(i);
            if (!doneRegions.contains(order.getLocation())) {
                doneRegions.add(order.getLocation());
                ordersToExecute.add(order);
            }
        }

        return ordersToExecute;
    }

    private void evaluateOrders(List<Order> orders) {
        for (Adviser adviser : player.getAdvisers()) {
            adviser.beforePhase();
        }

        for (Order order : orders) {
            double totalEval = 0f;
            for (Adviser adviser : player.getAdvisers()) {
                if (adviser.operator == Adviser.ADD) {
                    totalEval += adviser.evaluate(order, player.getGame().getPhase().ordinal());
                } else if (adviser.operator == Adviser.MULTIPLY) {
                    totalEval *= adviser.evaluate(order, player.getGame().getPhase().ordinal());
                }
            }
            order.setOrderValue(totalEval);
        }
    }

    private List<Order> getOrdersToExecute(List<Order> orders) {
        Map<String, Order> orderByRegion = new HashMap<String, Order>();
        List<Province> destinations = new ArrayList<Province>();
        List<Region> supported = new ArrayList<Region>();

        for (Order order : orders) {
            if (player.getMe().getControlledRegions().contains(order.getLocation()) &&
                    !orderByRegion.containsKey(order.getLocation().toString())) {
                Region finalDestination = Adviser.getFinalDestination(order);
                Power owner = player.getGame().getController(finalDestination);

                if (order instanceof MTOOrder
                        && !destinations.contains(finalDestination.getProvince())
                        && !player.getMe().equals(owner)) {
                    orderByRegion.put(order.getLocation().toString(), order);
                    destinations.add(((MTOOrder) order).getDestination().getProvince());
                } else if (order instanceof HLDOrder) {
                    destinations.add(order.getLocation().getProvince());
                    orderByRegion.put(order.getLocation().toString(), order);
                } else if (order instanceof SUPMTOOrder) {
                    Power supportedPower = player.getGame().getController(((SUPMTOOrder) order).getSupportedOrder().getLocation());
                    Power attackedPower = player.getGame().getController(((SUPMTOOrder) order).getSupportedOrder().getDestination());

                    if (supportedPower != null && attackedPower != null) {
                        destinations.add(order.getLocation().getProvince());
                        orderByRegion.put(order.getLocation().toString(), order);
                    }
                }
            }
//            if (orderByRegion.size() == player.getMe().getControlledRegions().size()) {
//                break;
//            }
        }


        for (String unit : orderByRegion.keySet()) {
            Order order = orderByRegion.get(unit);
            if (order instanceof MTOOrder) {
                MTOOrder mtoOrder = (MTOOrder) order;
                if (mtoOrder.needsSupport() && mtoOrder.getSupporters().size() > 0) {
                    Order supporter = null;
                    for (Region possibleSupporter : mtoOrder.getSupporters()) {
                        Order supporterOrder = orderByRegion.get(possibleSupporter.toString());
                        if (!supported.contains(supporterOrder.getLocation())) {
                            supporter = supporterOrder;
                        }
                    }
                    if (supporter != null) {
                        orderByRegion.put(supporter.getLocation().toString(), new SUPMTOOrder(player.getMe(), supporter.getLocation(), mtoOrder));
                        supported.add(mtoOrder.getLocation());
                        mtoOrder.setNeedOfSupport(false);
                    }
                }
            }
        }

        return new ArrayList<Order>(orderByRegion.values());
    }

    private void profileOrders(List<Order> ordersToExecute) {
        for (Order order : ordersToExecute) {
            if (order instanceof MTOOrder) {
                moves++;
                Power power = player.getGame().getController(Adviser.getFinalDestination(order));
                if (power != null) {
                    if (player.isInPeace(power.getName())) {
                        attackAlly++;
                    } else if (player.isInWar(power.getName())) {
                        attackEnemy++;
                    } else if (power.equals(player.getMe())) {
                        attackSelf++;
                    } else {
                        attackOtherDude++;
                    }
                } else {
                    walk++;
                }
            } else if (order instanceof HLDOrder) {
                holds++;
            } else if (order instanceof SUPMTOOrder) {
                SUPMTOOrder supmtoOrder = (SUPMTOOrder) order;
                supports++;
                Power power = player.getGame().getController(supmtoOrder.getSupportedOrder().getLocation());
                if (!player.getMe().equals(power)) {
                    supportsAlly++;
                }
            }
        }
    }
}