package com.smd.tetris;

import java.util.Random;

import static com.smd.tetris.TetrisModel.*;

public class TetrisQLearner implements TetrisListener, Planner {
	
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

	private static final int TIME_TO_DROP = 5;
	private final int exploration_policy;
	private final double epsilon = 0.05;
	
	int numTimeSteps = 0;
	
	private final int[] allowedActions;
	
	protected int rowsCompressed = 0;
	protected int blocksAccommodated = 0;
	
	public TetrisQLearner(TetrisModel model, int[] allowedActions) {
		this(model, 0.8, 0.2, 10.0, EXPLORE_GLIE2, allowedActions);
	}
	
	public TetrisQLearner(TetrisModel model, double decay, double learningRate, double temperature, int exploration_policy, int[] allowedActions) {
		this.tetris = model;
		this.learningRate = learningRate;
		this.decay = decay;
		this.temperature = temperature;
		this.exploration_policy = exploration_policy;
		this.allowedActions = allowedActions;
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
		State state = tetris.getState();
		double actionReward = 0;
		if (action == TetrisModel.MOVE_DOWN) { // some action taken by the policy
			numTimeSteps++;
			double[] completedRows = null;
			if (tetris.reachedLowestPossible()) {
					completedRows = tetris.getRowsCompleted();
					if (completedRows[0] > 0) {
						System.out.println("Found completed rows: " + completedRows[0] + ", reward: " + completedRows[1]);
						state.stateReward = completedRows[1];
					} else {
						if (tetris.isFull())
							state.stateReward = tetris.getRewardForFull();
					}
			}
			if (stateBeforeAction != null) { // && stateBeforeAction.compareTo(state) != 0) {
				actionReward = 
					stateBeforeAction.getActionReward(proposedAction) + learningRate * (
						stateBeforeAction.stateReward + decay * state.getBestReward()
							- stateBeforeAction.getActionReward(proposedAction)
					);
				stateBeforeAction.setActionReward(proposedAction, actionReward);
				if (completedRows != null && completedRows[0] > 0) {
					tetris.compressRowsCompleted();
				}
				if (debug) {
					if (stateBeforeAction != null)
						System.out.println(stateBeforeAction.toString()); // TODO Debug - to be commented later
					System.out.println(TetrisModel.actionStr(proposedAction) + ", reward=" + actionReward);
					System.out.println(state.toString()); // TODO Debug - to be commented later
					System.out.println("");
				}
				stateBeforeAction = null;
				proposedAction = TetrisModel.ILLEGAL;
			}
		}
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
			else 
				proposedAction = state.getBestAction();
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
				} else
					proposedAction = state.getBestAction();
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
		int action = tetris.getRandomActionCode();
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
