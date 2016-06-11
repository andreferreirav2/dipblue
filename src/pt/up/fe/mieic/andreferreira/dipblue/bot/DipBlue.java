package pt.up.fe.mieic.andreferreira.dipblue.bot;

import com.google.common.collect.Lists;
import es.csic.iiia.fabregues.dip.Player;
import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Phase;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.comm.CommException;
import es.csic.iiia.fabregues.dip.comm.IComm;
import es.csic.iiia.fabregues.dip.comm.daide.DaideComm;
import es.csic.iiia.fabregues.dip.orders.HLDOrder;
import es.csic.iiia.fabregues.dip.orders.MTOOrder;
import es.csic.iiia.fabregues.dip.orders.Order;
import pt.up.fe.mieic.andreferreira.dipblue.bot.adviser.Adviser;
import pt.up.fe.mieic.andreferreira.dipblue.bot.adviser.AdviserMapTactician;
import pt.up.fe.mieic.andreferreira.dipblue.bot.common.AgreedOrder;
import pt.up.fe.mieic.andreferreira.dipblue.bot.common.Opponent;
import pt.up.fe.mieic.andreferreira.dipblue.bot.negotiator.DipBlueNegotiator;
import pt.up.fe.mieic.andreferreira.dipblue.game.GameLauncher;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class DipBlue extends Player {

    public final DipBlueOrderHandler dipBlueOrderHandler = new DipBlueOrderHandler(this);
    private final InetAddress negotiationServer;
    private final int negotiationPort;
    public List<Adviser> advisers;
    public Map<String, Opponent> opponents;
    public List<AgreedOrder> agreedOrders;
    private DipBlueNegotiator negotiator;
    private Map<String, Order> previousRoundOrders;

    public boolean tracking;
    public boolean COMM = true;
    public boolean ACTIVE_COMM = true;
    public boolean STRATEGY_YES = true;
    public boolean STRATEGY_BALANCED = false;
    public boolean TRUST = true;

    private static final List<String> powersNames = Lists.newArrayList("AUS", "RUS", "TUR", "FRA", "ENG", "ITA", "GER");
    private List<String> positions = Lists.newArrayList("AUS", "RUS", "TUR", "FRA", "ENG", "ITA", "GER");

    public DipBlue(InetAddress negotiationIp, int negotiationPort, String logPath) {
        super(logPath);
        this.negotiationServer = negotiationIp;
        this.negotiationPort = negotiationPort;

        advisers = new ArrayList<Adviser>();
        advisers.add(new AdviserMapTactician(1.0));
        opponents = new HashMap<String, Opponent>();
        agreedOrders = new ArrayList<AgreedOrder>();
        previousRoundOrders = new HashMap<String, Order>();

        if (!new File(logPath).exists()) {
            new File(logPath).mkdirs();
        }
    }

    /**
     * Launch a DipBlue agent
     */

    public static void main(String[] args) {
        try {
            IComm comm = new DaideComm(InetAddress.getByName("localhost"), 16713, "DipBlue");
            DipBlue dipblue = new DipBlue(InetAddress.getByName("localhost"), 16713, "logs");
            dipblue.start(comm);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host name.");
        } catch (CommException e) {
            System.err.println("Cannot connect to the server.");
        }
    }

    @Override
    public void submissionError(String[] message) {
        super.submissionError(message);

        try {
            sendOrders(Lists.<Order>newArrayList(new HLDOrder(me, game.getRegion(message[5] + message[4]))));
        } catch (CommException e) {
            e.printStackTrace();
        }
    }


    public void beforeNewPhase() throws CommException {
        //Once per year
        if (TRUST && game.getPhase().equals(Phase.SPR)) {
            for (Opponent opponent : opponents.values()) {
                if (opponent.isInPeace()) {
                    opponent.multRatio(0.95);
                } else {
                    opponent.multRatio(0.99);
                }
            }
        }
        super.beforeNewPhase();
    }

    /**
     * Movement Phase Orders: SPR/FAL HLD Hold MTO Move to SUP Support Holding
     * SUPMTO Support Move to
     * <p/>
     * Retreat Phase: SUM/AUT RTO Retreat to DSB Disband
     * <p/>
     * Build Phase: WIN BLD Build unit REM Remove unit WVE Waive build
     */
    @Override
    public List<Order> play() {
        System.out.println(game.getYear() + " - " + game.getPhase());
        if (game.getPhase() == Phase.SUM || game.getPhase() == Phase.AUT) {
            for (Order order : previousRoundOrders.values()) {
                if (order instanceof MTOOrder) {
                    MTOOrder move = (MTOOrder) order;
                    if (!getMe().getControlledRegions().contains(move.getDestination())) {
                        dipBlueOrderHandler.movesCut++;
                    }
                }
            }
        }

        List<Order> orders = new Vector<Order>();
        switch (game.getPhase()) {
            case SPR:
            case FAL:
                // Move or Hold units
                if (COMM) {
                    negotiator.negotiate();
                }
                orders = dipBlueOrderHandler.evaluateMove(me.getControlledRegions());
                if (COMM) {
                    negotiator.requestSupports(orders);
                }
                break;
            case SUM:
            case AUT:
                // Retreat or Disband Dislodged units
                orders = dipBlueOrderHandler.evaluateRetreat(game.getDislodgedRegions(me));
                break;
            case WIN:
                // That's WIN
                int nBuilds = me.getOwnedSCs().size() - me.getControlledRegions().size();
                if (nBuilds > 0) {
                    // Build nBuilds times
                    orders = dipBlueOrderHandler.evaluateBuild(nBuilds);
                } else if (nBuilds < 0) {
                    // Removing nBuilds units
                    orders = dipBlueOrderHandler.evaluateRemove(-nBuilds);
                }
                break;
        }


        previousRoundOrders.clear();
        for (Order order : orders) {
            if (order.getLocation() != null) {
                previousRoundOrders.put(order.getLocation().toString(), order);
            }
        }
        return orders;
    }

    /**
     * Called when bot receives confirmation from server
     */
    @Override
    public void init() {

        for (Power power : game.getPowers()) {
            if (!power.equals(me)) {
                opponents.put(power.getName(), new Opponent(power.getName()));
            }
        }
    }

    /**
     * Called before the first turn in year 1901 and turn SPR.
     */
    @Override
    public void start() {
        if (COMM) {
            negotiator = new DipBlueNegotiator(negotiationServer, negotiationPort, this);
            negotiator.init();
        }
        for (Adviser adviser : advisers) {
            adviser.init(this);
        }
    }

    @Override
    public void receivedOrder(Order arg0) {
        if (TRUST) {
            if (arg0 instanceof MTOOrder) {
                MTOOrder move = (MTOOrder) arg0;
                Power attacker = move.getPower();
                Power attacked = game.getController(move.getDestination());

                if (!me.equals(attacker) && me.equals(attacked)) {
                    Opponent opponent = opponents.get(attacker.getName());

                    if (opponent.getEffectiveRatio() < 1) {
                        opponent.addRatio(0.04 * (1 / opponent.getEffectiveRatio()));
                    } else {
                        opponent.addRatio(0.01);
                    }
                }
            }
        }
    }

    @Override
    public void handleServerOFF() {
        Collections.sort(positions, new Comparator<String>() {
            public int compare(String s1, String s2) {
                int val1;
                if (dateOfDeath.containsKey(s1)) {
                    val1 = dateOfDeath.get(s1);
                } else {
                    val1 = game.getPower(s1).getControlledRegions().size() + 10000;
                }

                int val2;
                if (dateOfDeath.containsKey(s2)) {
                    val2 = dateOfDeath.get(s2);
                } else {
                    val2 = game.getPower(s2).getControlledRegions().size() + 10000;
                }

                return -Integer.compare(val1, val2);
            }
        });

        int place = positions.indexOf(this.getMe().getName()) + 1;

        double allyAvgDistance = 0.0;
        double numAllies = 0.0;
        String alliances = "[ ";
        for (Opponent opponent : opponents.values()) {
            if (opponent.isInPeace()) {
                allyAvgDistance += Opponent.distance(me.getName(), opponent.getName());
                numAllies++;
                alliances += opponent.getName() + "-" + Opponent.distance(me.getName(), opponent.getName()) + " ";
            }
        }
        allyAvgDistance /= numAllies;
        alliances += "]";


        double enemyAvgDistance = 0.0;
        double numEnemies = 0.0;
        String enemies = "[ ";
        for (Opponent opponent : opponents.values()) {
            if (opponent.wasInWar()) {
                numEnemies++;
                enemyAvgDistance += Opponent.distance(me.getName(), opponent.getName());
                enemies += opponent.getName() + "-" + Opponent.distance(me.getName(), opponent.getName()) + " ";
            }
        }
        enemyAvgDistance /= numEnemies;
        enemies += "]";

        int numberNegotiatiors = negotiator != null ? negotiator.negotiators.size() : 0;
        int numberAccepts = negotiator != null ? negotiator.accepts : 0;
        int numberRejects = negotiator != null ? negotiator.rejects : 0;

        GameLauncher.outputCVSLine(
                GameLauncher.ROUND,
                getName(),
                me.getName(),
                game.getYear(),
                place,
                winner,
                dateOfDeath.get(powersNames.get(0)),
                dateOfDeath.get(powersNames.get(1)),
                dateOfDeath.get(powersNames.get(2)),
                dateOfDeath.get(powersNames.get(3)),
                dateOfDeath.get(powersNames.get(4)),
                dateOfDeath.get(powersNames.get(5)),
                dateOfDeath.get(powersNames.get(6)),
                positions.indexOf(powersNames.get(0)) + 1,
                positions.indexOf(powersNames.get(1)) + 1,
                positions.indexOf(powersNames.get(2)) + 1,
                positions.indexOf(powersNames.get(3)) + 1,
                positions.indexOf(powersNames.get(4)) + 1,
                positions.indexOf(powersNames.get(5)) + 1,
                positions.indexOf(powersNames.get(6)) + 1,
                alliances,
                allyAvgDistance,
                enemies,
                enemyAvgDistance,
                numberNegotiatiors,
                numberAccepts,
                numberRejects,
                dipBlueOrderHandler.holds,
                dipBlueOrderHandler.moves,
                dipBlueOrderHandler.movesCut,
                dipBlueOrderHandler.attackSelf,
                dipBlueOrderHandler.attackAlly,
                dipBlueOrderHandler.attackOtherDude,
                dipBlueOrderHandler.attackEnemy,
                dipBlueOrderHandler.walk,
                dipBlueOrderHandler.supports,
                dipBlueOrderHandler.supportsAlly);

        System.out.println("\n# " + GameLauncher.ROUND + "\n" + getName() + " -> " + place + (tracking ? " *** " : "") +
                "\nyears \t\t\t\t" + game.getYear() +
                "\npower \t\t\t\t" + me.getName() +
                "\nalliances \t\t\t" + alliances +
                "\nenemies \t\t\t" + enemies +
                "\nnegotiators \t\t" + numberNegotiatiors +
                "\naccepted \t\t\t" + numberAccepts +
                "\nrejected \t\t\t" + numberRejects +
                "\nhld: \t\t\t\t" + dipBlueOrderHandler.holds +
                "\nmov:  \t\t\t\t" + dipBlueOrderHandler.moves +
                " cut %: " + ((double) dipBlueOrderHandler.movesCut * 100) / ((double) dipBlueOrderHandler.moves) +
                "\n\tattackSelf \t\t" + dipBlueOrderHandler.attackSelf +
                "\n\tattackAlly \t\t" + dipBlueOrderHandler.attackAlly +
                "\n\tattackOther \t" + dipBlueOrderHandler.attackOtherDude +
                "\n\tattackEnemy \t" + dipBlueOrderHandler.attackEnemy +
                "\n\twalk \t\t\t" + dipBlueOrderHandler.walk +
                "\nsup:  \t\t\t\t" + dipBlueOrderHandler.supports +
                "\n\tsupAlly \t\t" + dipBlueOrderHandler.supportsAlly);
        super.handleServerOFF();
    }

    @Override
    public void exit() {
        super.exit();
        if (negotiator != null) {
            negotiator.disconnect();
        }
    }

    public boolean isInPeace(String powerName) {
        if (powerName != null && opponents.containsKey(powerName)) {
            return opponents.get(powerName).isInPeace();
        }
        return false;
    }

    public boolean isInWar(String powerName) {
        if (powerName != null && !this.getMe().getName().equals(powerName) && opponents.containsKey(powerName)) {
            return opponents.get(powerName).isInWar();
        }
        return false;
    }

    public Game getGame() {
        return game;
    }

    public List<Adviser> getAdvisers() {
        return advisers;
    }
}
