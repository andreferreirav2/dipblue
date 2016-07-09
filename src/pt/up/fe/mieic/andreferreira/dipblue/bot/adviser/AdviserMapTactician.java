package pt.up.fe.mieic.andreferreira.dipblue.bot.adviser;

import es.csic.iiia.fabregues.dip.board.Game;
import es.csic.iiia.fabregues.dip.board.Power;
import es.csic.iiia.fabregues.dip.board.Province;
import es.csic.iiia.fabregues.dip.board.Region;
import es.csic.iiia.fabregues.dip.orders.*;

import java.util.*;

/**
 * Map Tactician is an Adviser that evaluates based on the map alone
 */
public class AdviserMapTactician extends Adviser {

    protected static final double ATTACK_RATIO = 1.0;
    protected static final double DEFENSE_RATIO = 0.9;

    private AdviserDumbBot dumbbot;
    HashMap<Region, Integer> destinations;

    public AdviserMapTactician(Double weight) {
        super(weight);
    }

    @Override
    protected void init() {
        operator = Adviser.ADD;
        dumbbot = new AdviserDumbBot();
    }

    @Override
    public void beforePhase() {
        dumbbot.calcValues(player.getGame(), player.getMe());
        destinations = dumbbot.getDestinationValue();
    }

    @Override
    protected double evaluate(Order advise) {
        Region evaluate = getFinalDestination(advise);
        double value = 0.0;

        if (advise instanceof HLDOrder) {
            value = evaluateRegion(evaluate) * DEFENSE_RATIO;
        } else if (advise instanceof MTOOrder) {
            value = evaluateRegion(evaluate) * ATTACK_RATIO;
        } else if (advise instanceof DSBOrder) {
            value = -evaluateRegion(evaluate) * DEFENSE_RATIO;
        } else if (advise instanceof RTOOrder) {
            value = evaluateRegion(evaluate) * ATTACK_RATIO;
        } else if (advise instanceof BLDOrder) {
            value = evaluateRegion(evaluate) * ATTACK_RATIO;
        } else if (advise instanceof WVEOrder) {
            value = 0.0;
        } else if (advise instanceof REMOrder) {
            value = -evaluateRegion(evaluate) * DEFENSE_RATIO;
        }

        return value;
    }

    protected double evaluateRegion(Region region) {
        return destinations.get(region).doubleValue();
    }
}

/**
 * The following class was extracted from the original DumbBot included in the DipGame GameManager available at http://www.dipgame.org/browse/gameManager
 * 
 * The objective of the DipBlue is to test the effectiveness of communication.
 * Dumbbot was used as a base to test how much communication would improve an already tested bot. 
 */
class AdviserDumbBot {
    private final int[] m_spr_prox_weight = {100, 1000, 30, 10, 6, 5, 4, 3, 2, 1};
    private final int[] m_fall_prox_weight = {1000, 100, 30, 10, 6, 5, 4, 3, 2, 1};
    private final int[] m_build_prox_weight = {1000, 100, 30, 10, 6, 5, 4, 3, 2, 1};
    private final int[] m_rem_prox_weight = {1000, 100, 30, 10, 6, 5, 4, 3, 2, 1};

    private HashMap<Province, Float> defenseValue;
    private HashMap<Province, Integer> strengthValue;
    private HashMap<Province, Integer> competitionValue;
    private HashMap<Region, Float[]> proximity;
    private HashMap<Region, Integer> destinationValue;

    Game game;
    Power me;

    public HashMap<Region, Integer> getDestinationValue() {
        return destinationValue;
    }

    public void calcValues(Game game, Power me) {
        this.game = game;
        this.me = me;
        calculateFactors();
        calculateDestinationValue();
    }

    private float getSize(Power power) {
        if (power == null) {
            return 0.0F;
        }
        float ownedSCs = power.getOwnedSCs().size();
        return ownedSCs * ownedSCs * 1.0F +
                ownedSCs * 4.0F + 16.0F;
    }

    protected void calculateDestinationValue() {
        switch (this.game.getPhase()) {
            case SPR:
            case SUM:
                calculateDestinationValue(this.m_fall_prox_weight, 1000, 1000);
                break;
            case AUT:
            case FAL:
                calculateDestinationValue(this.m_spr_prox_weight, 1000, 1000);
                break;
            case WIN:
                if (this.me.getOwnedSCs().size() > this.me.getControlledRegions().size()) {
                    calculateWINDestinationValue(this.m_rem_prox_weight, 1000);
                } else {
                    calculateWINDestinationValue(this.m_build_prox_weight, 1000);
                }
                break;
        }
    }

    private void calculateDestinationValue(int[] prox_weight, int strength_weight, int competition_weight) {
        this.destinationValue = new HashMap(this.game.getRegions().size());
        for (Region region : this.game.getRegions()) {
            int destWeight = 0;
            for (int i = 0; i < 10; i++) {
                destWeight = (int) (destWeight + ((Float[]) this.proximity.get(region))[i].floatValue() * prox_weight[i]);
            }
            destWeight = destWeight + strength_weight * ((Integer) this.strengthValue.get(region.getProvince())).intValue();
            destWeight = destWeight - competition_weight * ((Integer) this.competitionValue.get(region.getProvince())).intValue();
            this.destinationValue.put(region, Integer.valueOf(destWeight));
        }
        List<Region> regions = this.game.getRegions();
        Collections.sort(regions, new DestValueComparator(this.destinationValue));
    }

    private void calculateWINDestinationValue(int[] prox_weight, int defense_weight) {
        this.destinationValue = new HashMap(this.game.getRegions().size());
        for (Region region : this.game.getRegions()) {
            int destWeight = 0;
            for (int proxCount = 0; proxCount < 10; proxCount++) {
                destWeight = (int) (destWeight + (this.proximity.get(region))[proxCount] * prox_weight[proxCount]);
            }
            destWeight = (int) (destWeight + defense_weight * this.defenseValue.get(region.getProvince()));
            this.destinationValue.put(region, Integer.valueOf(destWeight));
        }
    }

    private Power getOwner(Province province) {
        for (Power power : this.game.getPowers()) {
            if (power.getOwnedSCs().contains(province)) {
                return power;
            }
        }
        return null;
    }

    public float calcDefVal(Province province) {
        float maxPower = 0.0F;
        Vector<Region> adjacentRegions = new Vector();
        Vector<Power> neighborPowers = new Vector();
        for (Region region : province.getRegions()) {
            adjacentRegions.addAll(region.getAdjacentRegions());
        }
        for (Region region : adjacentRegions) {
            neighborPowers.add(this.game.getController(region));
        }
        for (Power power : neighborPowers) {
            if ((power != null) && (!power.equals(this.me)) && (getSize(power) > maxPower)) {
                maxPower = getSize(power);
            }
        }
        return maxPower;
    }

    public void calculateFactors() {
        int prox_att_weight = 0;
        int prox_def_weight = 0;
        switch (this.game.getPhase()) {
            case SPR:
            case SUM:
                prox_att_weight = 600;
                prox_def_weight = 400;
                break;
            case AUT:
            case FAL:
                prox_att_weight = 700;
                prox_def_weight = 300;
                break;
            case WIN:
                prox_att_weight = 700;
                prox_def_weight = 300;
        }

        this.defenseValue = new HashMap<Province, Float>();
        HashMap<Province, Float> attackValue = new HashMap<Province, Float>();
        for (Province province : this.game.getProvinces()) {
            if (province.isSC()) {
                if (this.me.getOwnedSCs().contains(province)) {
                    this.defenseValue.put(province, Float.valueOf(calcDefVal(province)));
                    attackValue.put(province, Float.valueOf(0.0F));
                } else {
                    attackValue.put(province, Float.valueOf(getSize(getOwner(province))));
                    this.defenseValue.put(province, Float.valueOf(0.0F));
                }
            } else {
                attackValue.put(province, 0.0F);
                this.defenseValue.put(province, 0.0F);
            }
        }

        this.proximity = new HashMap<Region, Float[]>();
        for (Province province : this.game.getProvinces()) {
            for (Region region : province.getRegions()) {
                Float[] proximities = new Float[10];
                proximities[0] = attackValue.get(province) * prox_att_weight + this.defenseValue.get(province) * prox_def_weight;
                this.proximity.put(region, proximities);
            }
        }

        for (int proxCount = 1; proxCount < 10; proxCount++) {
            for (Province province : this.game.getProvinces()) {
                for (Region region : province.getRegions()) {
                    Float[] proximities = this.proximity.get(region);
                    proximities[proxCount] = proximities[proxCount - 1];

                    Region multipleCoasts = null;
                    for (Region adjRegion : region.getAdjacentRegions()) {
                        if ((adjRegion.getName().substring(4).compareTo("CS") == 0) && (multipleCoasts != null)) {
                            if ((this.proximity.get(adjRegion))[(proxCount - 1)] > (this.proximity.get(multipleCoasts))[(proxCount - 1)]) {
                                (this.proximity.get(region))[proxCount] = (this.proximity.get(region))[proxCount] - (this.proximity.get(multipleCoasts))[(proxCount - 1)] + (this.proximity.get(adjRegion))[(proxCount - 1)];
                            }
                        } else {
                            (this.proximity.get(region))[proxCount] = (this.proximity.get(region))[proxCount] + (this.proximity.get(adjRegion))[(proxCount - 1)];
                            if (adjRegion.getName().substring(4).compareTo("CS") == 0) {
                                multipleCoasts = adjRegion;
                            }
                        }
                    }
                    this.proximity.get(region)[proxCount] = this.proximity.get(region)[proxCount] / 5.0F;
                }
            }
        }
        initStrCompValues();
    }

    public void initStrCompValues() {
        this.strengthValue = new HashMap<Province, Integer>();
        this.competitionValue = new HashMap<Province, Integer>();
        for (Province province : this.game.getProvinces()) {
            HashMap<Power, Integer> adjUnitCount = new HashMap<Power, Integer>();
            for (Power power : this.game.getPowers()) {
                int count = 0;
                for (Region unit : power.getControlledRegions()) {
                    for (Region region : province.getRegions()) {
                        if (region.getAdjacentRegions().contains(unit)) {
                            count++;
                            break;
                        }
                    }
                }
                adjUnitCount.put(power, count);
            }

            for (Power power : this.game.getPowers()) {
                if (power.equals(this.me)) {
                    this.strengthValue.put(province, adjUnitCount.get(this.me));
                } else if (!this.competitionValue.containsKey(province)) {
                    this.competitionValue.put(province, adjUnitCount.get(power));
                } else if (adjUnitCount.get(power) > this.competitionValue.get(province)) {
                    this.competitionValue.put(province, adjUnitCount.get(power));
                }
            }
        }

        for (Province province : this.game.getProvinces()) {
            if (!this.competitionValue.containsKey(province)) {
                this.competitionValue.put(province, 0);
            }
            if (!this.strengthValue.containsKey(province)) {
                this.strengthValue.put(province, 0);
            }
        }
    }

    class DestValueComparator implements Comparator<Region> {
        private HashMap<Region, Integer> destinationValue;

        public DestValueComparator(HashMap<Region, Integer> destValue) {
            this.destinationValue = destValue;
        }

        public int compare(Region region1, Region region2) {
            return -((Integer) this.destinationValue.get(region1)).compareTo((Integer) this.destinationValue.get(region2));
        }
    }
}
