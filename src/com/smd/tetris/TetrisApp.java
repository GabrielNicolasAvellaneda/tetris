package com.smd.tetris;

import java.util.List;

import javax.xml.bind.Marshaller.Listener;

import static com.smd.tetris.TetrisModel.*;

/**
 * This program is based on the paper "Applying Reinforcement Learning To Tetris"
 * by Donald carr
 * 
 * We allow seven types of blocks as defined by BLOCK_TEMPLATES variable
 * 
 * @author Moy
 *
 */

public class TetrisApp implements TetrisViewListener {
	
//	private static final int BOARD_HEIGHT = 8;
//	private static final int BOARD_WIDTH  = 4;
//	private static final int START_COL  = -1;
	private static final int BOARD_HEIGHT = 20;
	private static final int BOARD_WIDTH  = 10;
	private static final int START_COL  = 3;
	private static final int V_BOUNDARY = 5; //10;
	private static final int DELAY = 10;
	private static final int TRAIN_CYCLES = 3000;
	private static final boolean stopLearningAfterTraining = true;
	
	private static final int[] ALLOWED_ACTIONS = {
		DO_NOTHING, MOVE_LEFT, MOVE_RIGHT, ROT_CLOCK, ROT_COUNTER_CLOCK, DROP
	};
	
	private Object LOCK = new int[0];
	private boolean reset = false;
	private boolean stopped = false;
	
	private TetrisModel tetris;
	private Planner learner;
	private TetrisView tetrisView;
	private Thread thread;
	
	public static void main(String[] args) {
		
		TetrisApp app = new TetrisApp();
		//app.testBlockRotations();
		//app.testRowCompress();
		app.startTetris();
		//app.sampleScreenShot();
		//app.testFutureStates();
		//app.testState();
		//app.tetrisIntro();
		//app.dispRewards();
		//app.testStateHeightCalc();
	}

	@SuppressWarnings("unused")
	private void testFutureStates() {
		
		tetris = new TetrisModel(BOARD_HEIGHT + V_BOUNDARY, BOARD_WIDTH, BOARD_HEIGHT, ALLOWED_ACTIONS);
		
		learner = new TetrisQLearner(tetris, ALLOWED_ACTIONS);
		tetrisView = new TetrisView(tetris);
		tetrisView.setListener(this);
		
		tetris.newBlock(new Block(6), 10, START_COL);
		
		System.out.println("Current State:\n" + tetris.getState());
		
		for (int action : ALLOWED_ACTIONS) {
			State state = tetris.getFutureStateIfAllowed(action);
			if (state != null) {
				System.out.println("Action: " + actionStr(action));
				System.out.println(state);
				if (state.rewardsForFull != 0) {
					System.out.print("Full: " + state.rewardsForFull);
				}
				if (state.completedRows != null) {
					System.out.print(" Completed#: " + state.completedRows[0] + ", rewards: " + state.completedRows[1]);
				}
				System.out.println();
			} else {
				System.out.println("Action: " + actionStr(action) + " not allowed");
			}
		}
		
	}
	
	@SuppressWarnings("unused")
	private void sampleScreenShot() {
		
		tetris = new TetrisModel(BOARD_HEIGHT + V_BOUNDARY, BOARD_WIDTH, BOARD_HEIGHT, ALLOWED_ACTIONS);
		
		learner = new TetrisQLearner(tetris, ALLOWED_ACTIONS);
		tetrisView = new TetrisView(tetris);
		tetrisView.setListener(this);
		
		tetris.newBlock(new Block(4), 22, 0);
		tetris.newBlock(new Block(6), 21, 1);
		
		tetris.newBlock(new Block(0), 8, -1);
		
		double[] completedRows = tetris.getRowsCompleted();
		
		Block tblock = new Block(3);
		tetris.newBlock(tblock, 19, 2);
		
	}
	
	@SuppressWarnings("unused")
	private void testRowCompress() {
		
		tetris = new TetrisModel(BOARD_HEIGHT + V_BOUNDARY, BOARD_WIDTH, BOARD_HEIGHT, ALLOWED_ACTIONS);
		
		learner = new TetrisQLearner(tetris, ALLOWED_ACTIONS);
		tetrisView = new TetrisView(tetris);
		tetrisView.setListener(this);
		
		tetris.newBlock(new Block(4), 10, 0);
		tetris.newBlock(new Block(6), 9, 1);
		
		tetris.newBlock(new Block(0), 8, -1);
		
		double[] completedRows = tetris.getRowsCompleted();
		System.out.println("Total rows completed: " + completedRows[0] + ", rewards=" + completedRows[1]);
		
		try {
			Thread.currentThread().sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		tetris.compressRowsCompleted();
		
		//Block tblock = new Block(3);
		//tetris.newBlock(tblock, 19, 2);
		
	}
	
	@SuppressWarnings("unused")
	private void testState() {
		
		tetris = new TetrisModel(BOARD_HEIGHT + V_BOUNDARY, BOARD_WIDTH, BOARD_HEIGHT, ALLOWED_ACTIONS);
		
		learner = new TetrisQLearner(tetris, ALLOWED_ACTIONS);
		tetrisView = new TetrisView(tetris);
		tetrisView.setListener(this);
		
		Block tblock = new Block(6);
		tblock.rotateCounterClockwise();
		tetris.newBlock(tblock, 21, 6);
		learner.dispState();
		
	}
	
	@SuppressWarnings("unused")
	private void testStateHeightCalc() {
		
		tetris = new TetrisModel(BOARD_HEIGHT + V_BOUNDARY, BOARD_WIDTH, BOARD_HEIGHT, ALLOWED_ACTIONS);
		
		learner = new TetrisQLearner(tetris, ALLOWED_ACTIONS);
		tetrisView = new TetrisView(tetris);
		tetrisView.setListener(this);
		
		tetris.newBlock(new Block(6), 9, -1);
		
		//Block tblock = new Block(3);
		//tetris.newBlock(tblock, 19, 2);
		learner.dispState();
		
	}
	
	@SuppressWarnings("unused")
	private void tetrisIntro() {
		
		tetris = new TetrisModel(BOARD_HEIGHT + V_BOUNDARY, BOARD_WIDTH, BOARD_HEIGHT, ALLOWED_ACTIONS);
		
		learner = new TetrisQLearner(tetris, ALLOWED_ACTIONS);
		tetrisView = new TetrisView(tetris);
		tetrisView.setListener(this);
		
		tetris.newBlock(new Block(0), 8, 1);
		tetris.newBlock(new Block(1), 8, 5);
		tetris.newBlock(new Block(2), 12, 1);
		tetris.newBlock(new Block(3), 12, 5);
		tetris.newBlock(new Block(4), 16, 1);
		tetris.newBlock(new Block(5), 16, 5);
		tetris.newBlock(new Block(6), 20, 3);
		
	}
	
	@SuppressWarnings("unused")
	private void dispRewards() {
		
		tetris = new TetrisModel(BOARD_HEIGHT + V_BOUNDARY, BOARD_WIDTH, BOARD_HEIGHT, ALLOWED_ACTIONS);
		
		learner = new TetrisQLearner(tetris, ALLOWED_ACTIONS);
		tetrisView = new TetrisView(tetris, true);
		tetrisView.setListener(this);
		
	}
	
	@SuppressWarnings("unused")
	private void testBlockRotations() {
		Block block = new Block(0);
		System.out.println(block.prettyString());
		System.out.println(block + "\n");
		block.rotateClockwise();
		System.out.println(block.prettyString());
		System.out.println(block + "\n");
		block.rotateClockwise();
		System.out.println(block.prettyString());
		System.out.println(block + "\n");
		block.rotateClockwise();
		System.out.println(block.prettyString());
		System.out.println(block);
	}
	
	@SuppressWarnings("unused")
	private void startTetris() {
		
		int[] allowedActions = ALLOWED_ACTIONS;
		//int[] allowedActions = {DO_NOTHING, ROT_CLOCK, ROT_COUNTER_CLOCK};
		tetris = new TetrisModel(BOARD_HEIGHT + V_BOUNDARY, BOARD_WIDTH, BOARD_HEIGHT, allowedActions, true);

		//learner = new TetrisRandomLearner(tetris);
		learner = new TetrisApproxQLearner(tetris, allowedActions);
		//learner = getDummyLearner(tetris, allowedActions);
		
		learner.debugOff();
		learner.startTrainingMode();
		tetrisView = new TetrisView(tetris);
		tetrisView.setListener(this);
		
		tetrisView.disableView();
		
//		learner.debugOn();
//		tetris.debugOn();
//		tetrisView.enableView();
		
		thread = new Thread() {

			public void run() {
				int epoch = 0;
				double averageBlocks = 0;
				double averageRowsCompressed = 0;
				while (!stopped) {
					if (epoch % 100 == 0) {
						System.out.print("Epoch: " + epoch);
						if (epoch > 0) {
							averageBlocks = averageBlocks / 100;
							averageRowsCompressed = averageRowsCompressed / 100;
							System.out.println(", Average blocks: " + averageBlocks + ", Average rows compressed: " + averageRowsCompressed);
							averageBlocks = 0;
							averageRowsCompressed = 0;
						} else
							System.out.println(); // Blank Line
					}
					if (epoch > TRAIN_CYCLES) {
						System.out.println("Total states: " + tetris.stateSpace.states.size());
						tetrisView.enableView();
						//learner.debugOn();
						//tetris.debugOn();
						if (stopLearningAfterTraining && learner.isInTrainingMode() && TRAIN_CYCLES > 0)
							learner.stopTrainingMode();
						if (epoch == TRAIN_CYCLES + 1) {
							dispFirstStates(300); //System.exit(0);
						}
					}
					runTetrisEpoch(tetris, epoch);
					averageBlocks += learner.getBlocksAccommodated();
					averageRowsCompressed += learner.getRowsCompressed();
					epoch++;
				}
			}

			private void dispFirstStates(int num) {
				System.out.println("------ First " + num + " ------");
				List<com.smd.tetris.State> states = tetris.stateSpace.states;
				for (int i = 0; i < num && i < tetris.stateSpace.states.size(); i++) {
					com.smd.tetris.State state = states.get(i);
					System.out.println(state.toString());
				}
			}
		};
		
		thread.start();
		
	}

	@SuppressWarnings("unused")
	private Planner getDummyLearner(final TetrisModel tetris, int[] allowedActions) {
		Planner learner = new TetrisApproxQLearner(tetris, allowedActions) {
			int[][] actions = {
					{DO_NOTHING, ROT_COUNTER_CLOCK, ROT_COUNTER_CLOCK},
					{DO_NOTHING, MOVE_RIGHT, DO_NOTHING},
			};
			int a = 0, p = 1;
			public int proposeAction() {
//				return super.proposeAction();
				State state = tetris.getState();
				stateBeforeAction = state;
				proposedAction = DO_NOTHING;
				if (a < actions[p].length) proposedAction = actions[p][a++];
				//proposedAction = actions[tetris.random(0, 2)];
				return proposedAction;
			}
			public void startRound() {
				a = 0; p = 1-p;
				super.startRound();
			}
			
		};
		return learner;
	}

	private void runTetrisEpoch(TetrisModel tetris, int epoch) {
		//System.out.println("New Epoch...");
		tetris.reset();
		tetrisView.resetGrid();
		while (!stopped && !reset && !tetris.isFull()) {
			Block tblock = new Block(tetris.randomBlock());
			tetris.newBlock(tblock, 0, START_COL);
			for (int i = 0; !reset && !tetris.reachedLowestPossible(); i++) {
				try {
					if (tetrisView.isViewEnabled()) {
						Thread.sleep(DELAY);
					}
					tetris.takeAction(learner.proposeAction());
					if (tetrisView.isViewEnabled()) {
						Thread.sleep(DELAY);
					}
					tetris.takeAction(TetrisModel.MOVE_DOWN);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (tetrisView.isViewEnabled()) {
				try {
					Thread.sleep(DELAY);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			//break;
//			if (!reset)
//				stopped = true;
		}
		if (reset)
			System.out.println("Board Reset");
		synchronized(LOCK) {
			reset = false;
		}
	}
	
	public void reset() {
		synchronized(LOCK) {
			this.reset = true;
		}
		System.out.println("Restarting...");
	}

}
