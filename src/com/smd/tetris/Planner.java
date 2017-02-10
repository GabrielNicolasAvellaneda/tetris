package com.smd.tetris;

public interface Planner {
	int proposeAction();
	void dispState();
	void debugOn();
	void debugOff();
	void startTrainingMode();
	void stopTrainingMode();
	boolean isInTrainingMode();
	
	int getBlocksAccommodated();
	int getRowsCompressed();
	void resetCounts();
}
