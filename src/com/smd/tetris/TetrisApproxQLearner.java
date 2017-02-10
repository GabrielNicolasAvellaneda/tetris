package com.smd.tetris;

import java.util.Random;

import static com.smd.tetris.TetrisModel.*;

public class TetrisApproxQLearner implements TetrisListener, Planner {
	
	private final TetrisModel tetris;
	
	protected State stateBeforeAction;
	protected int proposedAction = TetrisModel.ILLEGAL;

	private final double learningRate;
	private final double decay;
	private final double temperature;
	
	private boolean debug = false;
	private boolean trainMode = true;
	
	public static final int EXPLORE_GLIE1 = 1;
	public static final int EXPLORE_GLIE2 = 2;

	private static final int TIME_TO_DROP = 100;
	private final int exploration_policy;
	private final double epsilon = 0.1;
	private static final double DEFAULT_TEMP = 100;
	
	int numTimeSteps = 0;
	
	private final int[] allowedActions;
	
	private final double[] featureWeights;
	
	protected int rowsCompressed = 0;
	protected int blocksAccommodated = 0;
	
	public TetrisApproxQLearner(TetrisModel model, int[] allowedActions) {
		this(model, 0.05, 0.0001, DEFAULT_TEMP, EXPLORE_GLIE1, allowedActions);
	}
	
	public TetrisApproxQLearner(TetrisModel model, double decay, double learningRate, double temperature, int exploration_policy, int[] allowedActions) {
		this.tetris = model;
		this.learningRate = learningRate;
		this.decay = decay;
		this.temperature = temperature;
		this.exploration_policy = exploration_policy;
		this.allowedActions = allowedActions;
		this.featureWeights = new double[tetris.numFeatures];
		model.addListener(this);
	}
	
	public void blockAtBottom(Block block) {
		// do nothing
	}

	public void boardFull() {
		// do nothing
	}

	public void clearBlock(Block block, int row, int col) {
		// do nothing
	}

	public void setBlock(Block block, int row, int col) {
		// do nothing
	}

	public void startRound() {
		numTimeSteps = 0;
		this.stateBeforeAction = null;
		this.proposedAction = TetrisModel.ILLEGAL;
		blocksAccommodated++;
	}

	public void afterAction(int action, boolean success) {
		if (action == TetrisModel.MOVE_DOWN) { // some action taken by the policy
			State state = tetris.getState();
			if (stateBeforeAction != null) { // && stateBeforeAction.compareTo(state) != 0) {
				if (debug) {
					System.out.println("");
					if (stateBeforeAction != null)
						System.out.println(stateBeforeAction.toString()); // TODO Debug - to be commented later
					System.out.println(TetrisModel.actionStr(proposedAction) + ", reward=");
					System.out.println(state.toString()); // TODO Debug - to be commented later
				}
			}
			if (trainMode) {
				updateWeights(stateBeforeAction, proposedAction, state, featureWeights);
			}
			numTimeSteps++;
			if (state.completedRows != null && state.completedRows[0] > 0) {
				tetris.compressRowsCompleted();
			}
			stateBeforeAction = null;
			proposedAction = TetrisModel.ILLEGAL;
		}
	}

	private void updateWeights(State stateBeforeAction, int action, State stateAfterAction,
			double[] featureWeights) {
		double[] sFeatures = relevantFeatures(stateBeforeAction.features);
//		if (stateAfterAction.completedRows != null && stateAfterAction.completedRows[0] > 0) {
//			System.out.println("Found completed rows: " + stateAfterAction.completedRows[0] + ", reward: " + stateAfterAction.stateReward);
//		}
		double qState = stateBeforeAction.terminalState ? 
				stateBeforeAction.stateReward : Q(sFeatures, action, featureWeights);
		double[] maxQ = getMaxQ(featureWeights);
		int bestAction = (int)maxQ[0];
		double change = learningRate*(stateBeforeAction.stateReward + decay*stateAfterAction.stateReward - qState);
		if (bestAction != ILLEGAL && !stateAfterAction.terminalState)
			change = learningRate*(stateBeforeAction.stateReward + decay*maxQ[1] - qState);
		for (int i = 0; i < featureWeights.length; i++) {
			featureWeights[i] += change*sFeatures[i];
		}
		if (debug)
			System.out.println(State.vec2Str(maxQ, 4, 4) + ", Change by:" + change + "\n" + State.vec2Str(featureWeights, 4, 6));
	}

	private double[] getMaxQ(double[] featureWeights) {
		double[] ret = new double[2];
		ret[0] = ILLEGAL;
		ret[1] = Double.MIN_VALUE;
		double[] actionQ = new double[allowedActions.length];
		for (int a : allowedActions) {
			State sPrime = tetris.getFutureStateIfAllowed(a);
			if (sPrime != null) {
				double[] sPrimeFeatures = relevantFeatures(sPrime.features);
				actionQ[a] = sPrime.terminalState ? 
						sPrime.stateReward : Q(sPrimeFeatures, a, featureWeights);
				if (ret[0] == ILLEGAL || actionQ[a] > ret[1]) {
					ret[0] = a;
					ret[1] = actionQ[a];
				}
			}
		}
		// send out a random policy in case more than one best is found
		int[] possible = new int[actionQ.length];
		int maxA = 0;
		for (int i = 0; i < actionQ.length; i++) {
			if (actionQ[i] == ret[1]) {
				possible[maxA++] = i;
			}
		}
		if (maxA > 1)
			ret[0] = possible[tetris.random(0, maxA-1)];
		if (debug) {
			System.out.println("Q:" + State.vec2Str(actionQ, 3, 6));
		}
		return ret;
	}

	private double Q(double[] sFeatures, int action, double[] featureWeights) {
		double q = 0;
		for (int i = 0; i < featureWeights.length; i++) {
			q += sFeatures[i] * featureWeights[i];
		}
		return q;
	}

	private double[] relevantFeatures(double[] features) {
		double[] relFeatures = features.clone();
		double sum = 0;
		for (double v : features) {
			sum += (v*v);
		}
		if (sum == 0.0) return relFeatures;
		sum = Math.sqrt(sum);
		for (int i = 0; i < relFeatures.length; i++) {
			relFeatures[i] = relFeatures[i] / sum;
		}
		return relFeatures;
	}

	public void beforeAction(int action) {
		// do nothing
	}

	public int proposeAction() {
		State state = tetris.getState();
		stateBeforeAction = state;
		proposedAction = TetrisModel.ILLEGAL;
		if (!this.trainMode) {
			//System.out.println("Best Policy");
			if (numTimeSteps > TIME_TO_DROP)
				proposedAction = DROP;
			else {
				double[] qvals = getMaxQ(featureWeights);
				int action = (int)qvals[0];
				if (action == ILLEGAL) action = DO_NOTHING;
				proposedAction = action;
			}
		} else {
//			if (state.features[state.features.length-1] == 1)
//				proposedAction = DROP;
//			else 
			if (numTimeSteps > TIME_TO_DROP)
				proposedAction = DROP;
			else 
				if (exploration_policy == EXPLORE_GLIE2) {
				proposedAction = getBoltzmanExplorationAction(state);
			} else {
				// GLIE1
				double rnd = random.nextDouble();
				if (rnd < epsilon) {
					int act = tetris.random(0, allowedActions.length-1);
					proposedAction = allowedActions[act];
				} else {
					double[] qvals = getMaxQ(featureWeights);
					int action = (int)qvals[0];
					if (action == ILLEGAL) action = DO_NOTHING;
					proposedAction = action;
				}
			}
		}
		return proposedAction;
	}
	
	private int getBoltzmanExplorationAction(State state) {
		double sum = 0;
		double[] actionRewards = state.getActionRewards();
		double[] vals = new double[allowedActions.length];
		for (int i = 0; i < vals.length; i++) {
			vals[i] = Math.exp(actionRewards[allowedActions[i]] / temperature);
			sum += vals[i];
		}
		double prob = random.nextDouble();
		int action = TetrisModel.DO_NOTHING;
		for (int i = 0; i < allowedActions.length; i++) {
			vals[i] = vals[i] / sum + (i == 0 ? 0.0 : vals[i-1]); // cumulative
			if (prob < vals[i]) {
				action = allowedActions[i];
				break;
			}
		}
		//System.out.println(State.vec2Str(vals, 3, 3) + ": " + actionStr(action));
		return action;
	}

	public void dispState() {
		State state = tetris.getState();
		System.out.println(state.toString());
	}

	public void reset() {
		blocksAccommodated = 0;
		rowsCompressed = 0;
	}
	
	public void startTrainingMode() {
		this.trainMode = true;
	}
	
	public void stopTrainingMode() {
		System.out.println("Turning off training...");
		this.trainMode = false;
	}
	
	public void debugOn() {
		this.debug = true;
	}
	
	public void debugOff() {
		this.debug = false;
	}
	
	private final Random random = new Random();

	public boolean isInTrainingMode() {
		return this.trainMode;
	}

	public void compressed(int rows) {
		rowsCompressed += rows;
	}

	public int getBlocksAccommodated() {
		return this.blocksAccommodated;
	}

	public int getRowsCompressed() {
		return this.rowsCompressed;
	}

	public void resetCounts() {
		blocksAccommodated = 0;
		rowsCompressed = 0;
	}
	
}
