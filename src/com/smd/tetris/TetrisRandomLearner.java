package com.smd.tetris;

public class TetrisRandomLearner implements TetrisListener, Planner {
	
	private final TetrisModel tetris;
	
	protected int rowsCompressed = 0;
	protected int blocksAccommodated = 0;
	
	public TetrisRandomLearner(TetrisModel model) {
		this.tetris = model;
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
		blocksAccommodated++;
	}

	public void afterAction(int action, boolean success) {
		// do nothing
	}

	public void beforeAction(int action) {
		// do nothing
	}

	public int proposeAction() {
		return tetris.getRandomActionCode();
	}
	
	public void dispState() {
		State state = tetris.getState();
		System.out.println(state.toString());
	}

	public void reset() {
		blocksAccommodated = 0;
		rowsCompressed = 0;
	}

	public void debugOn() {
		// do nothing
	}

	public void debugOff() {
		// do nothing
	}

	public void startTrainingMode() {
		// TODO Auto-generated method stub
		
	}

	public void stopTrainingMode() {
		// TODO Auto-generated method stub
		
	}

	public boolean isInTrainingMode() {
		return false;
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
