package com.smd.tetris;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TetrisModel {
	
	public static final int ILLEGAL = -1;
	public static final int DO_NOTHING = 0;
	public static final int MOVE_LEFT = 1;
	public static final int MOVE_RIGHT = 2;
	public static final int ROT_CLOCK = 3;
	public static final int ROT_COUNTER_CLOCK = 4;
	public static final int DROP = 5;
	public static final int MOVE_DOWN = 6;
	
	public static final int NUM_ACTIONS_ALLOWED = 6;
	
	private boolean debug = false;
	
	protected final int rows;
	protected final int cols;
	protected final int allowableDepth;
	protected final int verticalLimit;
	
	private final Random random = new Random();
	
	// Current game state
	protected Block block;
	private boolean atBottom = false;
	private boolean full = false;
	private int row;
	private int col;
	private State currentState = null;
	
	private final int[][] board;
	private final double[][] rewards;
	public final int numFeatures;
	
	protected final StateSpace stateSpace = new StateSpace();
	private final int[] allowedActions;
	
	private final boolean stateless;
	
	private List<TetrisListener> listeners = new ArrayList<TetrisListener>();
	
	public TetrisModel(int rows, int cols, int allowableDepth, int[] allowedActions) {
		this(rows, cols, allowableDepth, allowedActions, false);
	}
	public TetrisModel(int rows, int cols, int allowableDepth, int[] allowedActions, boolean stateless) {
		this.rows = rows;
		this.cols = cols;
		this.allowableDepth = allowableDepth;
		this.verticalLimit = rows - allowableDepth;
		this.board = new int[rows][cols];
		this.rewards = new double[rows][cols];
		this.allowedActions = allowedActions;
		this.stateless = stateless;
		
		// #features = 2 (row,col) + width + #block types + #orientations
		this.numFeatures = getFeatureCount();
		
		for (int i = verticalLimit; i < rewards.length; i++) {
			for (int j = 0; j < rewards[i].length; j++) {
				//rewards[i][j] = 1; //0;
				//rewards[i][j] = Math.pow(1.3, (double)(i-verticalLimit)); //(i-verticalLimit)*0.5;
				rewards[i][j] = (i-verticalLimit)*0.5;
			}
		}
	}

	private int getFeatureCount() {
		return board[0].length*2 - 1 + 2 + Block.BLOCK_TEMPLATES.length + 4 + 1; // last 1 is bias
	}
	
	public double[][] getRewards() {
		return this.rewards;
	}
	
	public boolean isFull() {
		return full;
	}
	
	public void reset() {
		full = false;
		atBottom = false;
		block = null;
		row = 0;
		col = 0;
		for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[i].length; j++) {
				board[i][j] = 0;
			}
		}
		triggerReset();
	}
	
	public void newBlock(Block block, int row, int col) {
		if (full)
			throw new RuntimeException("Board is full");
		this.atBottom = false;
		this.block = block;
		this.row = row;
		this.col = col;
		setBlock(block, row, col);
		triggerStart();
	}
	
	public boolean moveDown() {
		if (!isAllowed(1, 0, 0)) {
			atBottom = true;
			checkFull();
			return false;
		}
		clearBlock(block, row, col);
		row++;
		setBlock(block, row, col);
		return true;
	}
	
	public boolean moveLeft() {
		if (!isAllowed(0, -1, 0)) return false;
		clearBlock(block, row, col);
		col--;
		setBlock(block, row, col);
		return true;
	}
	
	public boolean moveRight() {
		if (!isAllowed(0, 1, 0)) return false;
		clearBlock(block, row, col);
		col++;
		setBlock(block, row, col);
		return true;
	}
	
	public boolean rotateClockwise() {
		if (!isAllowed(0, 0, 1)) return false;
		clearBlock(block, row, col);
		block.rotateClockwise();
		setBlock(block, row, col);
		return true;
	}
	
	public boolean rotateCounterClockwise() {
		if (!isAllowed(0, 0, -1)) return false;
		clearBlock(block, row, col);
		block.rotateCounterClockwise();
		setBlock(block, row, col);
		return true;
	}
	
	public boolean drop() {
		int rowsDropped = 0;
		while (isAllowed(1, 0, 0)) {
			clearBlock(block, row, col);
			rowsDropped++;
			row++;
			setBlock(block, row, col);
		}
		atBottom = true;
		checkFull();
		return rowsDropped > 0;
	}
	
	private void setBlock(Block block, int row, int col) {
		placeModel(block, row, col);
		if (listeners != null)
			for (TetrisListener listener : listeners)
				listener.setBlock(block, row, col);
	}
	
	private void clearBlock(Block block, int row, int col) {
		clearModel(block, row, col);
		if (listeners != null)
			for (TetrisListener listener : listeners)
				listener.clearBlock(block, row, col);
	}
	
	/**
	 * Returns if the current block has moved to the lowest position
	 * possible from it's current location.
	 * 
	 * <p>Also checks and sets the flag for full if the block has
	 * reached the lowest possible row and yet is at the top of the board</p>
	 * 
	 * @return
	 */
	public boolean reachedLowestPossible() {
		if (atBottom) return true;
		int[][] blockMat = block.blockMat;
		for (int i = block.topLeftR; i <= block.botRghtR; i++) {
			if (row+i+1 >= board.length) return true;
			for (int j = block.topLeftC; j <= block.botRghtC; j++) {
				if (blockMat[i][j] > 0 && i == block.botRghtR && (board[row+i+1][col+j] > 0))
					atBottom = true;
				else if (i < block.botRghtR && blockMat[i][j] > 0 && blockMat[i+1][j] == 0 && (board[row+i+1][col+j] > 0)) {
					atBottom = true;
				}
			}
		}
		checkFull();
		return atBottom;
	}
	
	/**
	 * Checks whether the block has reached the lowest row of the well.
	 * @return
	 */
	public boolean atWellBottom() {
		return (row + block.botRghtR == board.length - 1);
	}
	
	private boolean checkFull() {
		if (atBottom) {
			if (row + block.topLeftR <= verticalLimit) {
				this.full = true;
			}
		}
		return this.full;
	}
	
	/**
	 * Returns one step look-ahead estimate
	 * 
	 * @return
	 */
	public State getFutureStateIfAllowed(int action) {
		State state = null;
		int rowMove = 0;
		int maxRowMove = action == MOVE_DOWN ? 1 : action == DROP ? board.length-1 : 0;
		int colMove = (action == MOVE_LEFT ? -1 : action == MOVE_RIGHT ? 1 : 0);
		int rot = (action == ROT_CLOCK ? 1 : action == ROT_COUNTER_CLOCK ? -1 : 0);
		
		// preserve current state
		int currRow = row;
		int currCol = col;
		boolean currAtBottom = atBottom;
		boolean currFull = full;
		
		// Note: we are looking ahead one timestep when the
		// block will be one row below
		//if (!isAllowed(1, 0, 0)) return null;
		
		clearModel(block, row, col);
		rotate(block, rot);
		
		//row++; // next time step
		
		boolean allowed = true;
		while (allowed && rowMove <= maxRowMove) {
			if (rowMove < maxRowMove) rowMove++;
			if (col+colMove+block.topLeftC < 0 || col+colMove+block.botRghtC >= board[0].length) {
				allowed = false;
			} else if (row+rowMove+block.botRghtR >= board.length) {
				rowMove--;
				allowed = false;
			}else {
				allowed = check(block, row+rowMove, col+colMove);
				if (!allowed && rowMove > 0) rowMove--;
			}
			if (rowMove == 0) break;
		}
		
		// get state info
		allowed = (rowMove > 0 || allowed);
		if (allowed) {
			row = row+rowMove; col = col+colMove;
			placeModel(block, row, col);
			state = getState();
			clearModel(block, row, col);
		}
		
		rotate(block, rot*-1); // revert to previous orientation
		// Restore the previous state
		row = currRow; col = currCol;
		atBottom = currAtBottom; full = currFull;
		this.atBottom = false;
		this.full = false;
		placeModel(block, row, col);
		return state;
	}

	/**
	 * 
	 * @param block
	 * @param rowMove
	 * @param colMove
	 * @param rot 0 - no rotation, 1 - rotate clockwise, -1 - rotate counter clockwise
	 * @return
	 */
	private boolean isAllowed(int rowMove, int colMove, int rot) {
		clearModel(block, row, col);
		rotate(block, rot);
		boolean allowed = true;
		if (col+colMove+block.topLeftC < 0 || col+colMove+block.botRghtC >= board[0].length) {
			allowed = false;
		} else if (row+rowMove+block.botRghtR >= board.length) {
			allowed = false;
		}else {
			//System.out.println("New Loc: " + (row+rowMove) + ", " + (col+colMove) + " " + block + " " + board.length);
			allowed = check(block, row+rowMove, col+colMove);
		}
		rotate(block, rot*-1); // revert to previous orientation
		placeModel(block, row, col);
		return allowed;
	}

	private void rotate(Block block, int rot) {
		switch (rot) {
		case 1:
			block.rotateClockwise();
			break;
		case -1:
			block.rotateCounterClockwise();
			break;
		default:
			break;
		}
	}

	private boolean check(Block block, int row, int col) {
		int[][] blockMat = block.blockMat;
		for (int i = block.topLeftR; i <= block.botRghtR; i++) {
			for (int j = block.topLeftC; j <= block.botRghtC; j++) {
				if (blockMat[i][j] > 0 && board[row+i][col+j] > 0) {
					return false;
				}
			}
		}
		return true;
	}

	private void clearModel(Block block, int row, int col) {
		int[][] blockMat = block.blockMat;
		for (int i = block.topLeftR; i <= block.botRghtR; i++) {
			for (int j = block.topLeftC; j <= block.botRghtC; j++) {
				if (blockMat[i][j] > 0) {
					board[row+i][col+j] = 0;
				}
			}
		}
	}
	
	private void placeModel(Block block, int row, int col) {
		int[][] blockMat = block.blockMat;
		for (int i = block.topLeftR; i <= block.botRghtR; i++) {
			for (int j = block.topLeftC; j <= block.botRghtC; j++) {
				if (blockMat[i][j] > 0) {
					board[row+i][col+j] = blockMat[i][j];
				}
			}
		}
	}
	
	public int randomCol() {
		return random(0, board[0].length - 6);
	}
	
	public int randomBlock() {
		return random(0, Block.BLOCK_TEMPLATES.length - 1);
		//return 6;
	}
	
	public int getRandomActionCode() {
		return allowedActions[random(0, allowedActions.length - 1)];
		//return random(3, 4);
		//return 0;
	}
	
	public int takeRandomAction() {
		if (full) return -1;
		int action = getRandomActionCode();
		takeAction(action);
		return action;
	}
	
	public boolean takeAction(int action) {
		//if (debug)
		//	System.out.println("ACTION: " + action);
		triggerBeforeAction(action);
		atBottom = false;
		full = false;
		boolean success = false;
		this.currentState = null; // invalidate the cached state
		switch (action) {
		case DO_NOTHING: // do nothing
			success = true;
			break;
		case MOVE_LEFT:
			success = moveLeft();
			break;
		case MOVE_RIGHT:
			success = moveRight();
			break;
		case ROT_CLOCK:
			success = rotateClockwise();
			break;
		case ROT_COUNTER_CLOCK:
			success = rotateCounterClockwise();
			break;
		case DROP:
			success = drop();
			break;
		case MOVE_DOWN:
			success = moveDown();
			break;
		default:
			success = false;
		}
		triggerAfterAction(action, success);
		return success;
	}
	
	/**
	 * The following features will be used to capture the state
	 * - Location of the topmost occupied cell in each column starting from current
	 *   block location
	 * - *--Difference between the heights of each successive column from
	 *   *--left to right. The difference is (right - left). Differences are max (+/-)5
	 *   *--(not being used)
	 * - *--Location of first hole in column starting from the current
	 *   *--position of the block - (not implemented yet)
	 * - The current block number (0/1 in corresponding position for each block)
	 * - The orientation of the current block (0/1 for each angle index)
	 * - *--Number of occupied/wall cells that are adjacent to block on right
	 * - *--Number of occupied/wall cells that are adjacent to block on left
	 * - *--Number of occupied/wall cells that are adjacent to block below
	 * - *--Top Left row of current block
	 * - *--Top Left col of current block
	 * @return vector with indexes corresponding to the features as mentioned above
	 */
	public double[] getFeatures() {
		
		double[] features = new double[getFeatureCount()+7+1]; // 8 numbers for debug info
		int fIdx = 0;
		// features for top-most occupied cells in columns
		int prevColHt = 0;
		for (int i = 0; i < board[0].length; i++) {
			int blockRow = block.topLeftR + row;
			int j = block.topLeftR + row;
			if (i >= block.topLeftC + col && i <= block.botRghtC + col) {
				j = block.botRghtR + row;
				int t = block.botRghtR;
				while(t > 0 && block.blockMat[t][i-col] == 0) {
					t--; j--;
				}
				j++;
				blockRow = j;
			}
			for (; j < board.length && board[j][i] == 0; j++);
			int colHt = j - blockRow;
			features[fIdx++] = colHt;
			if (i > 0) {
				int diff = colHt - prevColHt;
				features[fIdx + board[0].length - 2] = diff;
			}
			prevColHt = colHt;
		}
		fIdx += board[0].length - 1; // adjusting for column height differences
		
		// features for current block location
		// ---only even number of rows so that state space may be reduced
		features[fIdx++] = block.topLeftR + row;
		features[fIdx++] = block.topLeftC + col;
		
		// features for type of block
		features[fIdx + block.type] = 1; //block.type;
		fIdx += Block.BLOCK_TEMPLATES.length;
		
		// feature for current block orientation
		features[fIdx + block.getOrientation()] = 1; //block.getOrientation();
		fIdx += 4; // total 4 orientations
		
		// bias term
		features[fIdx++] = 1;
		
		// Number of adjacent occupied cells
		int cRight = 0, cLeft = 0, cBottom = 0;
		boolean isAtWellBottom = atWellBottom();
		for (int i = block.topLeftR; i <= block.botRghtR; i++) {
			for (int j = block.topLeftC; j <= block.botRghtC; j++) {
				if (block.blockMat[i][j] > 0) {
					if (j == block.topLeftC) {
						if (col + block.topLeftC == 0)
							cLeft++;
					} else if (block.blockMat[i][j-1] == 0 && board[row+i][col+j-1] > 0) {
						cLeft++;
					}
					if (j == block.botRghtC) {
						if (col + block.botRghtC == board[0].length-1)
							cRight++;
					} else if (block.blockMat[i][j+1] == 0 && board[row+i][col+j+1] > 0) {
						cRight++;
					}
					if (isAtWellBottom) { 
						if(i == block.botRghtR) {
							cBottom++;
						}
					} else if (i == block.blockMat.length - 1  || (block.blockMat[i+1][j] == 0 && board[row+i+1][col+j] > 0)) {
						cBottom++;
					}
				}
			}
		}
		features[fIdx++] = cLeft;
		features[fIdx++] = cRight;
		features[fIdx++] = cBottom;
		
		// Additional debug information that will not be part of the state
		features[fIdx++] = row;
		features[fIdx++] = col;
		features[fIdx++] = block.type;
		features[fIdx++] = block.getOrientation();
		
		// check if a perfect fit has been found
		double ht = features[block.topLeftC + col];
		boolean fit = true;
		for (int j = block.topLeftC+1; j <= block.botRghtC; j++) {
			if (features[j + col] != ht) {
				fit = false;
				break;
			}
		}
		features[fIdx++] = fit ? 1 : 0;
		
		return features;
		
	}
	
	public State getState() {
		
		// not caching the current state
		//if (this.currentState != null)
		//	return this.currentState;
		
		double[] features = getFeatures();
		
		State state = stateless ? null : stateSpace.lookupState(features);
		if (state != null) return state;
		
		double[] actionRewards = new double[NUM_ACTIONS_ALLOWED];
		state = new State(features, false, 0, this.numFeatures, getRandomActionCode(), actionRewards);
		if (reachedLowestPossible()) {
			state.completedRows = getRowsCompleted();
			if (state.completedRows[0] == 0) {
				// if there were completed rows that may be compressed,
				// then the board is not full
				if (full) {
					state.rewardsForFull = getRewardForFull();
					state.terminalState = true;
				}
			}
			state.stateReward = 20*state.completedRows[1] + state.rewardsForFull;
			
			// Rewards for misfit
			double misfitReward = 0;
			if (state.features[state.features.length-1] == 0) {
				for (int j = block.topLeftC; j <= block.botRghtC; j++) {
					int ht = (int)features[col + j];
					for (int i = block.botRghtR; i >= block.topLeftR; i--) {
						if (block.blockMat[i][j] > 0) { 
							for (int l = row+i+1; l < board.length; l++) {
								//System.out.println("ht: " + ht);
								if (board[l][col+j] > 0) break;
								misfitReward += rewards[l][col+j];
							}
							break; // we reached the topmost occupied cell in the block
						} else {
							continue;
						}
//						if (board[row+i][col+j] == 0) {
//							// this location below the block was unoccupied
//							//misfitReward += rewards[board.length-1-(row+i)][col+j];
//							misfitReward += rewards[(row+i)][col+j];
//						}
						//state.stateReward = state.stateReward + rewards[row+block.topLeftR+ht][col+j];
					}
				}
				state.stateReward -= misfitReward;
			}
		}
		// reward for lower rows reached
		state.stateReward += rewards[board.length - (block.topLeftR+(int)state.features[state.features.length-5])-1][0];
		
		// Rewards for fit
		if (state.features[state.features.length-1] == 1) {
			int ht = (int)features[block.topLeftC + col];
			for (int j = block.topLeftC; j <= block.botRghtC; j++) {
				//if (features[j + col] == 0)
					state.stateReward = state.stateReward + rewards[row+block.topLeftR+ht][col+j];
			}
		}
		if (!stateless) stateSpace.lookupState(state, true);
		
		return state;
		
	}
	
	/**
	 * Rewards collected when the block cannot move further down
	 * Reward matrix will be maintained in a decreasing order from bottom to top
	 * so that occupying the lowest locations is preferable.
	 * @return
	 */
	private double getPositionRewards() {
		double reward = 0;
		for (int i = block.topLeftR; i <= block.botRghtR; i++) {
			for (int j = block.topLeftC; j <= block.botRghtC; j++) {
				if (block.blockMat[i][j] > 0) {
					reward += rewards[row+i][col+j];
				}
			}
		}
		return reward;
	}

	/**
	 * Rewards collected when the block cannot move further down
	 * Reward matrix will be maintained in a decreasing order from bottom to top
	 * so that occupying the lowest locations is preferable.
	 * @return
	 */
	public double[] getRowsCompleted() {
		double[] rowRewards = new double[2];
		for (int i = board.length - 1; i >= verticalLimit; i--) {
			boolean filled = true;
			double reward = 0;
			for (int j = 0; j < board[0].length; j++) {
				if (board[i][j] == 0) {
					filled = false; 
					break;
				} else
					reward += rewards[i][j];
			}
			if (filled) {
				rowRewards[0] = rowRewards[0]+1;
				rowRewards[1] = rowRewards[1]+reward;
			}
		}
		return rowRewards;
	}

	public int compressRowsCompleted() {
		int rows = 0;
		for (int i = board.length - 1; i >= verticalLimit; i--) {
			boolean filled = true;
			for (int j = 0; j < board[0].length; j++) {
				if (board[i][j] == 0) {
					filled = false; 
					break;
				}
			}
			if (filled) {
				rows = rows+1;
				for (int j = 0; j < board[0].length; j++) {
					for (int l = i; l > 0; l--) {
						board[l][j] = board[l-1][j];
					}
				}
				triggerCompressed(i);
				i++; // check this row again
			}
		}
		if (rows > 0) {
			//atBottom = true; // we will leave atBottom flag as is 
			full = false;
		}
		return rows;
	}

	/**
	 * Rewards will be collected when the board is full. We return the
	 * negated sum of all blank holes on board.
	 * 
	 * @return
	 */
	public double getRewardForFull() {
		double reward = 0;
		for (int i = verticalLimit; i < rewards.length; i++) {
			for (int j = 0; j < rewards[i].length; j++) {
				if (board[i][j] == 0) {
					reward += rewards[i][j];
				}
			}
		}
		//System.out.println("Reward for full board: " + reward);
		return -reward;
	}
	
	public int random(int start, int end) {
		return start + random.nextInt(end - start + 1);
	}
	
	public void addListener(TetrisListener listener) {
		this.listeners.add(listener);
	}
	
	private void triggerBeforeAction(int action) {
		if (listeners != null)
			for (TetrisListener listener : listeners)
				listener.beforeAction(action);
	}

	private void triggerAfterAction(int action, boolean success) {
		if (listeners != null)
			for (TetrisListener listener : listeners)
				listener.afterAction(action, success);
	}

	private void triggerStart() {
		if (listeners != null)
			for (TetrisListener listener : listeners)
				listener.startRound();
	}
	
	private void triggerCompressed(int row) {
		if (listeners != null)
			for (TetrisListener listener : listeners)
				listener.compressed(row);
	}
	
	private void triggerReset() {
		if (listeners != null)
			for (TetrisListener listener : listeners)
				listener.reset();
	}
	
	public void debugOn() {
		this.debug = true;
	}

	public void debugOff() {
		this.debug = false;
	}

	public static String actionStr(int action) {
		String str = "ILLEGAL";
		switch (action) {
		case ILLEGAL: // illegal action
			str = "ILLEGAL";
			break;
		case DO_NOTHING: // do nothing
			str = "DO_NOTHING";
			break;
		case MOVE_LEFT:
			str = "MOVE_LEFT";
			break;
		case MOVE_RIGHT:
			str = "MOVE_RIGHT";
			break;
		case ROT_CLOCK:
			str = "ROT_CLOCK";
			break;
		case ROT_COUNTER_CLOCK:
			str = "ROT_COUNTER_CLOCK";
			break;
		case DROP:
			str = "DROP";
			break;
		case MOVE_DOWN:
			str = "MOVE_DOWN";
			break;
		}
		return str;
	}

}
