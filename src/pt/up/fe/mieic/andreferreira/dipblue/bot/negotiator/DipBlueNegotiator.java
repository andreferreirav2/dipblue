package pt.up.fe.mieic.andreferreira.dipblue.bot.negotiator;

import es.csic.iiia.fabregues.dip.Player;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import es.csic.iiia.fabregues.dip.orders.SUPMTOOrder;
import es.csic.iiia.fabregues.utilities.Log;
import org.dipgame.dipNego.language.illocs.Accept;
import org.dipgame.dipNego.language.illocs.Illocution;
import org.dipgame.dipNego.language.illocs.Propose;
import org.dipgame.dipNego.language.illocs.Reject;
import org.dipgame.dipNego.language.infos.Agree;
import org.dipgame.dipNego.language.infos.Commit;
import org.dipgame.dipNego.language.infos.CommitSequence;
import org.dipgame.dipNego.language.infos.Deal;
import org.dipgame.dipNego.language.offers.*;
import org.dipgame.negoClient.DipNegoClient;
import org.dipgame.negoClient.Negotiator;
import org.dipgame.negoClient.simple.DipNegoClientHandler;
import org.dipgame.negoClient.simple.DipNegoClientImpl;
import org.json.JSONException;
import pt.up.fe.mieic.andreferreira.dipblue.bot.DipBlue;
import pt.up.fe.mieic.andreferreira.dipblue.bot.common.AgreedOrder;
import pt.up.fe.mieic.andreferreira.dipblue.bot.common.Opponent;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

public class DipBlueNegotiator implements Negotiator {

    private DipBlue player;
    private Log log;
    //Negotiation variables
    private int negotiationPort;
    private InetAddress negotiationServer;
    private DipNegoClient chat;
    private boolean occupied;
    private Set<String> nonNegotiators;
    public Set<String> negotiators;
    private int messagesToReceive = 0;
    public int accepts = 0;
    public int rejects = 0;

    public DipBlueNegotiator(InetAddress negotiationIp, int negotiationPort, Player player) {
        this.negotiationServer = negotiationIp;
        this.negotiationPort = negotiationPort;
        this.player = (DipBlue) player;
        this.log = player.log.getLog();
        occupied = false;
        nonNegotiators = new HashSet<String>();
        negotiators = new HashSet<String>();
    }

    @Override
    public void disconnect() {
        chat.disconnect();
    }

    @Override
    public boolean isOccupied() {
        return occupied;
    }

    @Override
    public void negotiate() {
        occupied = true;
        if (player.ACTIVE_COMM && knowsAllOpponents()) {

            if (player.STRATEGY_BALANCED) {
                //break peace
                for (Opponent opponent : player.opponents.values()) {
                    if (opponent.isInPeace() && opponent.getEffectiveRatio() > 1.0) {
                        opponent.setInPeace(false);
                    }
                }
            }

            String highestPower = null;
            int maxSC = -1;
            for (Power power : player.getGame().getNonDeadPowers()) {
                if (!player.getMe().equals(power) &&
                        !player.isInPeace(power.getName()) &&
                        power.getOwnedSCs().size() >= maxSC &&
                        !(power.getOwnedSCs().size() == maxSC && player.isInWar(highestPower))) {
                    highestPower = power.getName();
                    maxSC = power.getOwnedSCs().size();
                }
            }

            if (highestPower != null && !player.isInWar(highestPower)) {
                List<Power> alliance = new Vector<Power>();
                alliance.add(player.getMe());
                for (Opponent opponent : player.opponents.values()) {
                    Power ally = player.getGame().getPower(opponent.getName());
                    if (ally.getOwnedSCs().size() > 0 && opponent.isInPeace()) {
                        alliance.add(ally);
                    }
                }
                List<Power> against = new Vector<Power>();
                against.add(player.getGame().getPower(highestPower));
                Deal deal = new Agree(alliance, new Alliance(alliance, against));
                Illocution illocution = new Propose(player.getMe(), alliance, deal);

                if (alliance.size() > 1 && against.size() > 0) {
                    messagesToReceive++;
                    System.out.println("Declare war to " + highestPower + " " + player.getGame().getYear());
                    send(illocution);
                }
            }
        }

        occupied = false;

        waitForAnswers();
    }

    public void requestSupports(List<Order> orders) {
        occupied = true;

        if (player.ACTIVE_COMM && knowsAllOpponents()) {
            for (Order order : orders) {
                if (order instanceof MTOOrder) {
                    MTOOrder mtoOrder = (MTOOrder) order;
                    if (mtoOrder.needsSupport()) {
                        for (Region adjacentToAttack : mtoOrder.getDestination().getAdjacentRegions()) {
                            for (Power power : player.getGame().getNonDeadPowers()) {
                                if (player.isInPeace(power.getName()) && power.getControlledRegions().contains(adjacentToAttack)) {
                                    CommitSequence commits = new CommitSequence();
                                    Offer offer = new Do(new SUPMTOOrder(power, adjacentToAttack, mtoOrder));
                                    Commit commit = new Commit(player.getMe(), Arrays.asList(power), offer);
                                    commits.addCommit(commit);

                                    Illocution illocution = new Propose(player.getMe(), Arrays.asList(power), commits);
                                    messagesToReceive++;
                                    send(illocution);
                                }
                            }
                        }
                    }
                }
            }
        }

        occupied = false;

        waitForAnswers();

    }

    public void init() {
        DipNegoClientHandler negoClientHandler = new DipNegoClientHandler() {

            @Override
            public void handleClientAccepted() {
            }

            @Override
            public void handleNewGamePhase() {

            }

            @Override
            public void handleFirstGamePhase() {
                occupied = true;
                if (player.ACTIVE_COMM) {
                    for (Power power : player.getGame().getPowers()) {
                        if (!power.equals(player.getMe())) {
                            List<Power> peace = new Vector<Power>(2);
                            peace.add(player.getMe());
                            peace.add(power);
                            Deal deal = new Agree(peace, new Peace(peace));

                            Illocution illoc = new Propose(player.getMe(), power, deal);
                            messagesToReceive++;
                            send(illoc);
                        }
                    }
                }
                occupied = false;
            }

            @Override
            public void handleNegotiationMessage(Power from, List<Power> to, Illocution illocution) {
                occupied = true;
                if (illocution instanceof Propose) {
                    handleAnswerPropose(illocution);
                } else if (illocution instanceof Accept) {
                    handleAnswerAccept(illocution);
                    accepts++;
                    messagesToReceive--;
                } else if (illocution instanceof Reject) {
                    handleAnswerReject(illocution);
                    rejects++;
                    messagesToReceive--;
                }
                if (!knowsAllOpponents()) {
                    negotiators.add(from.getName());
                }
                occupied = false;
            }

            @Override
            public void handleErrorMessage(String errorMsg) {
                occupied = true;
                if (errorMsg.contains("not found")) {
                    String powerName = errorMsg.substring(7, 10);
                    nonNegotiators.add(powerName);
                    messagesToReceive--;
                }
                occupied = false;
            }

            @Override
            public void handleServerOff() {

            }
        };
        chat = new DipNegoClientImpl(negotiationServer, negotiationPort, player.getMe().getName(), negoClientHandler, log);
        try {
            chat.init();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        log.print("Negotiation module initiated.");
    }

    /**
     * There is no difference between Peace and Alliance
     */
    private void handleAnswerPropose(Illocution illocution) {
        Deal deal = ((Propose) illocution).getDeal();
        if (deal instanceof Agree) {
            Offer offer = ((Agree) deal).getOffer();
            handleAnswerProposeOffer(illocution, deal, offer);
        } else if (deal instanceof CommitSequence) {
            List<Commit> commits = ((CommitSequence) deal).getCommits();
            for (Commit commit : commits) {
                Offer offer = commit.getOffer();
                handleAnswerProposeOffer(illocution, deal, offer);
            }
        }
    }

    private void handleAnswerProposeOffer(Illocution illocution, Deal deal, Offer offer) {
        Illocution response = null;
        Vector<Power> too;
        switch (offer.getType()) {
            case PEACE:
                Peace peace = (Peace) offer;
                too = new Vector<Power>(1);
                too.add(illocution.getSender());

                /*
                * Accept Peace if no power is in war and have ratio below 2
                * */
                boolean acceptPeace = true;
                for (Power power : peace.getPowers()) {
                    if (!player.getMe().equals(power)) {
                        acceptPeace &= (!player.isInWar(power.getName()) && player.opponents.get(power.getName()).getEffectiveRatio() < 2);
                    }
                }

                if (player.STRATEGY_YES || (player.STRATEGY_BALANCED && acceptPeace)) {
                    response = new Accept(player.getMe(), too, deal);
                    for (Power power : peace.getPowers()) {
                        startPeaceWith(power.getName());
                    }
                }
                break;
            case ALLIANCE:
                Alliance alliance = (Alliance) offer;
                too = new Vector<Power>(1);
                too.add(illocution.getSender());

                /*
                * Accept alliance if all allies are not in war and have ratio below 2 and
                * all enemies are not in peace and have ratio above 0.5
                * */
                boolean acceptAlliance = true;
                for (Power power : alliance.getAlliedPowers()) {
                    if (!player.getMe().equals(power)) {
                        acceptAlliance &= (!player.isInWar(power.getName()) && player.opponents.get(power.getName()).getEffectiveRatio() < 2);
                    }
                }
                for (Power power : alliance.getEnemyPowers()) {
                    if (!player.getMe().equals(power)) {
                        acceptAlliance &= (!player.isInPeace(power.getName()) && player.opponents.get(power.getName()).getEffectiveRatio() > 0.5);
                    }
                }

                if (player.STRATEGY_YES || (player.STRATEGY_BALANCED && acceptAlliance)) {
                    response = new Accept(player.getMe(), too, deal);
                    for (Opponent opponent : player.opponents.values()) {
                        reset(opponent.getName());
                    }
                    for (Power power : alliance.getAlliedPowers()) {
                        startPeaceWith(power.getName());
                    }
                    for (Power power : alliance.getEnemyPowers()) {
                        startWarWith(power.getName());
                    }
                }
                break;
            case AND:
                And and = (And) offer;
                handleAnswerProposeOffer(illocution, deal, and.getLeftOffer());
                handleAnswerProposeOffer(illocution, deal, and.getRightOffer());
                break;
            case DO:
                Do doOffer = (Do) offer;
                Order order = doOffer.getOrder();
                Power sender = illocution.getSender();
                too = new Vector<Power>(1);
                too.add(sender);

                boolean promised = false;
                for (Iterator<AgreedOrder> iterator = player.agreedOrders.iterator(); iterator.hasNext(); ) {
                    AgreedOrder agreedOrder = iterator.next();
                    if (agreedOrder.getOrder().getLocation().toString().equals(order.getLocation().toString())) {
                        promised = true;
                    }
                }

                if (sender != null && !promised &&
                        player.isInPeace(sender.getName()) &&
                        player.getMe().getControlledRegions().contains(order.getLocation())) {

                    if (player.STRATEGY_YES) {
                        response = new Accept(player.getMe(), too, deal);
                        player.agreedOrders.add(new AgreedOrder(order, sender));
                    } else if (player.STRATEGY_BALANCED) {
                        if (player.opponents.get(sender.getName()).getEffectiveRatio() <= 0.5 ||
                                sender.getControlledRegions().size() > player.getMe().getControlledRegions().size()) {
                            response = new Accept(player.getMe(), too, deal);
                            player.agreedOrders.add(new AgreedOrder(order, sender));
                        }
                    }
                }

                break;
            default:
                break;
        }

        if (response == null) {
            too = new Vector<Power>(1);
            too.add(illocution.getSender());
            response = new Reject(player.getMe(), too, deal);
        }

        send(response);
    }

    private void handleAnswerAccept(Illocution illocution) {
        Deal deal = ((Accept) illocution).getDeal();
        if (deal instanceof Agree) {
            Offer offer = ((Agree) deal).getOffer();
            switch (offer.getType()) {
                case PEACE:
                    Peace peace = (Peace) offer;
                    for (Power power : peace.getPowers()) {
                        startPeaceWith(power.getName());
                    }
                    break;
                case ALLIANCE:
                    Alliance alliance = (Alliance) offer;
                    for (Opponent opponent : player.opponents.values()) {
                        opponent.setInWar(false);
                    }
                    for (Power power : alliance.getAlliedPowers()) {
                        startPeaceWith(power.getName());
                    }
                    for (Power power : alliance.getEnemyPowers()) {
                        startWarWith(power.getName());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void handleAnswerReject(Illocution illocution) {
        Deal deal = ((Reject) illocution).getDeal();
        if (deal instanceof Agree) {
            Offer offer = ((Agree) deal).getOffer();
            switch (offer.getType()) {
                case PEACE:
                    Opponent opponent = player.opponents.get(illocution.getSender().getName());
                    opponent.addRatio(0.1);
                    player.opponents.get(illocution.getSender().getName()).setInPeace(false);
                    break;
                case ALLIANCE:
                    break;
                default:
                    break;
            }
        }
    }

    private boolean knowsAllOpponents() {
        return nonNegotiators.size() + negotiators.size() == 6;
    }

    private void send(Illocution illocution) {
        try {
            Vector<String> receivers = new Vector<String>();
            for (Power rec : illocution.getReceivers()) {
                if (!rec.getName().equals(player.getMe().getName())) {
                    receivers.add(rec.getName());
                }
            }
            chat.send(receivers, illocution);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reset(String enemy) {
        if (!enemy.equals(player.getMe().getName())) {
            player.opponents.get(enemy).setInWar(false);
            player.opponents.get(enemy).setInPeace(false);
        }
    }

    private void startWarWith(String enemy) {
        if (!enemy.equals(player.getMe().getName())) {
            player.opponents.get(enemy).setInWar(true);
            player.opponents.get(enemy).setInPeace(false);
        }
    }

    private void startPeaceWith(String allied) {
        if (!allied.equals(player.getMe().getName())) {
            player.opponents.get(allied).setInPeace(true);
            player.opponents.get(allied).setInWar(false);
        }
    }


    private void waitForAnswers() {
        do {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (messagesToReceive != 0);
    }
}