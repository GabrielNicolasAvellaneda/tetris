package com.smd.tetris;

public class Block {
	
	protected final int type;
	protected int angle = 0;
	protected final int[][] blockMat;
	
	protected int topLeftR, topLeftC;
	protected int botRghtR, botRghtC;
	
	public Block(int type) {
		this.type = type;
		int[][] template = BLOCK_TEMPLATES[type];
		int dim = template.length;
		if (template[0].length > dim)
			dim = template[0].length;
		this.blockMat = new int[dim][dim];
		for (int i = 0; i < template.length; i++) {
			System.arraycopy(template[i], 0, blockMat[i], 0, template[i].length);
		}
		setBorders();
	}
	
	private void setBorders() {
		topLeftR = Integer.MAX_VALUE; topLeftC = Integer.MAX_VALUE;
		botRghtR = -1; botRghtC = -1;
		for (int i = 0; i < blockMat.length; i++) {
			for (int j = 0; j < blockMat[i].length; j++) {
				if (blockMat[i][j] > 0) {
					if (topLeftR > i) {
						topLeftR = i;
					}
					if(botRghtR < i) {
						botRghtR = i;
					}
					if (topLeftC > j) {
						topLeftC = j;
					}
					if(botRghtC < j) {
						botRghtC = j;
					}
				}
			}
		}
	}
	
	public void rotateClockwise() {
		angle = (360 + angle - 90) % 360;
		adjustBlock();
	}
	
	public void rotateCounterClockwise() {
		angle = (angle + 90) % 360;
		adjustBlock();
	}
	
	private void adjustBlock() {
		int[][] template = BLOCK_TEMPLATES[type];
		//System.out.println("Template: \n" + prettyString(template) + "\n");
		switch (angle) {
		case 0:
			for (int i = 0; i < template.length; i++) {
				System.arraycopy(template[i], 0, blockMat[i], 0, template[i].length);
			}
			break;
		case 90:
			for (int r = 0; r < blockMat.length; r++) {
				for (int c = 0; c < blockMat[r].length; c++) {
					blockMat[r][c] = template[c][blockMat[r].length - r - 1];
				}
			}
			break;
		case 180:
			for (int r = 0; r < blockMat.length; r++) {
				for (int c = 0; c < blockMat[r].length; c++) {
					blockMat[r][c] = template[blockMat[r].length - r - 1][blockMat[r].length - c - 1];
				}
			}
			break;
		case 270:
			for (int r = 0; r < blockMat.length; r++) {
				for (int c = 0; c < blockMat[r].length; c++) {
					blockMat[r][c] = template[blockMat[r].length - c - 1][r];
				}
			}
			break;
		}
		setBorders();
	}
	
	/**
	 * Get the rotation angle
	 * 
	 * @return rotation angle ( 0 - 0, 1 - 90, 2 - 180, 3 - 270 )
	 */
	public int getOrientation() {
		return angle / 90;
	}
	
	public String prettyString() {
		return prettyString(blockMat);
	}
	
	private String prettyString(int[][] mat) {
		StringBuilder sb = new StringBuilder();
		for (int r = 0; r < mat.length; r++) {
			for (int c = 0; c < mat[r].length; c++) {
				sb.append("" + mat[r][c]);
			}
			sb.append("\n");
		}
		return sb.substring(0, sb.length() - 1);
	}
	
	@Override
	public String toString() {
		return "Block [angle=" + angle + ", type=" + type + ",(" + topLeftR + "," + topLeftC + ")-(" + botRghtR + "," + botRghtC + ")]";
	}

	protected static int[][][] BLOCK_TEMPLATES = {
		{ // 0
			{0,0,0,0,0},
			{0,0,0,0,0},
			{0,1,2,0,0},
			{0,1,1,0,0},
			{0,0,0,0,0},
		},
		{ // 1
			{0,0,0,0,0},
			{0,0,0,0,0},
			{0,0,2,1,0},
			{0,1,1,0,0},
			{0,0,0,0,0},
		},
		{ // 2
			{0,0,0,0,0},
			{0,0,0,0,0},
			{0,1,2,1,0},
			{0,1,0,0,0},
			{0,0,0,0,0},
		},
		{ // 3
			{0,0,0,0,0},
			{0,0,0,0,0},
			{0,1,2,1,0},
			{0,0,1,0,0},
			{0,0,0,0,0},
		},
		{ // 4
			{0,0,0,0,0},
			{0,0,0,0,0},
			{1,1,2,1,0},
			{0,0,0,0,0},
			{0,0,0,0,0},
		},
		{ // 5
			{0,0,0,0,0},
			{0,0,0,0,0},
			{0,1,2,0,0},
			{0,0,1,1,0},
			{0,0,0,0,0},
		},
		{ // 6
			{0,0,0,0,0},
			{0,0,0,0,0},
			{0,1,2,1,0},
			{0,0,0,1,0},
			{0,0,0,0,0},
		},
	};
	
	private static final int PIVOTS[][];
	static {
		PIVOTS = new int[BLOCK_TEMPLATES.length][2];
		for (int i = 0; i < PIVOTS.length; i++) {
			for (int j = 0; j < BLOCK_TEMPLATES[i].length; j++) {
				for (int k = 0; k < BLOCK_TEMPLATES[i][j].length; k++) {
					if (BLOCK_TEMPLATES[i][j][k] == 2) {
						PIVOTS[i][0] = j;
						PIVOTS[i][1] = k;
					}
				}
			}
		}
	}
	
}
