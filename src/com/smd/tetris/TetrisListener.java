package com.smd.tetris;

public interface TetrisListener {
	
	void setBlock(Block block, int row, int col);
	void clearBlock(Block block, int row, int col);
	
	void startRound();
	void blockAtBottom(Block block);
	void boardFull();
	void beforeAction(int action);
	void afterAction(int action, boolean success);
	void compressed(int row);
	
	void reset();
	
}
