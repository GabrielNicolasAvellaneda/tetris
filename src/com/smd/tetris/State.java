package com.smd.tetris;

import java.util.Formatter;

public class State implements Comparable<State> {
	
	public final double[] features;
	public final int relevantFeatures;
	
	public boolean terminalState;
	public double stateReward;
	private final double[] actionRewards;
	
	private int bestAction = 0;
	
	public double rewardsForFull = 0;
	public double[] completedRows = null;
	
	public State(double[] features) {
		this(features, false, 0, features.length, TetrisModel.DO_NOTHING, new double[TetrisModel.NUM_ACTIONS_ALLOWED]);
	}
	public State(double[] features, boolean terminalState, double stateReward, int relevantFeatures, int policy, double[] actionRewards) {
		this.features = features;
		this.terminalState = terminalState;
		this.stateReward = stateReward;
		this.relevantFeatures = relevantFeatures;
		this.actionRewards = actionRewards;
		this.bestAction = policy;
	}
	
	public int compareTo(State s) {
		int ret = 0;
		for (int i = 0; i < relevantFeatures; i++) {
			if (this.features[i] < s.features[i])
				return -1;
			else if (this.features[i] > s.features[i])
				return 1;
		}
		return ret;
	}
	
	public String toString() {
		Formatter formatter = new Formatter(new StringBuilder());
		formatter.format("%2.1f", stateReward);
		String stateStr = "R:" + formatter.toString() + ",T:" + terminalState + ",Pi:" + bestAction + ",AR:" + rewardStr() + ",F:" + featureStr();
		return stateStr;
	}
	
	public String rewardStr() {
		return vec2Str(actionRewards, 3, 6);
	}
	
	public String featureStr() {
		return vec2Str(features, 3, 0);
	}
	
	public static String vec2Str(double[] vector, int bD, int aD) {
		if (vector == null) return "[null]";
		String numberFormat = "%" + bD + "." + aD + "f,";
		Formatter formatter = new Formatter(new StringBuilder());
		formatter.format("[");
		for (double val : vector) {
			formatter.format(numberFormat, val);
		}
		formatter.format("]");
		return formatter.toString();
	}
	
	public double getActionReward(int action) {
		return actionRewards[action];
	}
	
	public double getBestReward() {
		if (this.stateReward > actionRewards[bestAction]) return this.stateReward;
		return actionRewards[bestAction];
	}
	
	public int getBestAction() {
		return bestAction;
	}
	
	public void setActionReward(int action, double actionReward) {
		actionRewards[action] = actionReward;
		if (actionRewards[bestAction] < actionReward) {
			bestAction = action;
		}
	}
	
	public double[] getActionRewards() {
		return this.actionRewards;
	}
	
}
