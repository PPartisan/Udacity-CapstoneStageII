package com.github.ppartisan.fishlesscycle.strategy;

public final class Strategy {

    private static StrategyImpl sInstance = null;

    public static synchronized AppStrategy get() {
        if(sInstance == null) {
            sInstance = new StrategyImpl();
        }
        return sInstance;
    }

}
