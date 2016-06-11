package pt.up.fe.mieic.andreferreira.dipblue.bot;

import pt.up.fe.mieic.andreferreira.dipblue.bot.adviser.Adviser;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class DipBlueBuilder {
    private DipBlue dipblue;
    private String logPath;
    private List<Adviser> advisers;
    private InetAddress address;
    private int port;
    private boolean log = false;
    private boolean COMM = false;
    private boolean ACTIVE_COMM = false;
    private boolean STRATEGY_YES = false;
    private boolean STRATEGY_BALANCED = false;
    private boolean TRUST = false;

    public DipBlueBuilder() {
        advisers = new ArrayList<Adviser>();
    }

    public DipBlueBuilder withInetAddress(InetAddress address) {
        this.address = address;
        return this;
    }

    public DipBlueBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    public DipBlueBuilder withLogPath(String logPath) {
        this.logPath = logPath;
        return this;
    }

    public DipBlueBuilder withAdviser(Adviser adviser) {
        advisers.add(adviser);
        return this;
    }

    public DipBlueBuilder withLog(boolean log) {
        this.log = log;
        return this;
    }

    public DipBlueBuilder withCOMM(boolean COMM) {
        this.COMM = COMM;
        return this;
    }

    public DipBlueBuilder withACTIVE_COMM(boolean ACTIVE_COMM) {
        this.ACTIVE_COMM = ACTIVE_COMM;
        return this;
    }

    public DipBlueBuilder withSTRATEGY_YES(boolean STRATEGY_YES) {
        this.STRATEGY_YES = STRATEGY_YES;
        return this;
    }

    public DipBlueBuilder withSTRATEGY_BALANCED(boolean STRATEGY_BALANCED) {
        this.STRATEGY_BALANCED = STRATEGY_BALANCED;
        return this;
    }

    public DipBlueBuilder withTRUST(boolean TRUST) {
        this.TRUST = TRUST;
        return this;
    }

    public DipBlue build() {
        dipblue = new DipBlue(address, port, logPath);
        dipblue.advisers = advisers;

        dipblue.tracking = log;
        dipblue.COMM = COMM;
        dipblue.ACTIVE_COMM = ACTIVE_COMM;
        dipblue.STRATEGY_BALANCED = STRATEGY_BALANCED;
        dipblue.STRATEGY_YES = STRATEGY_YES;
        dipblue.TRUST = TRUST;

        return dipblue;
    }
}
