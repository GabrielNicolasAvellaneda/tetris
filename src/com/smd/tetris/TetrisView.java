package com.smd.tetris;

import javax.swing.*;
import javax.swing.border.LineBorder;

import java.awt.*;
import java.awt.event.*;
import java.util.Formatter;

@SuppressWarnings("serial")
public class TetrisView extends JFrame implements TetrisListener {

	private JLabel[][] grid;
	private JFrame frame;
	
	private ImageIcon blankIcon;
	private ImageIcon[] colorIcons;
	
	private TetrisModel model;
	private TetrisViewListener listener;
	private boolean disableView = false;
	
	public TetrisView(TetrisModel model) {
		this(model, false);
	}
	public TetrisView(TetrisModel model, boolean dispRewardsOnly) {
		this.model = model;
		JPanel board = new JPanel(new GridLayout(model.rows, model.cols));
		grid = new JLabel[model.rows][model.cols];
		JFrame j = new JFrame("Tetris - " + model.rows + "," + model.cols);
		this.frame = j;
		
		j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container c = j.getContentPane();
		
		blankIcon = new ImageIcon("images/blank.gif");
		colorIcons = new ImageIcon[7];
		for (int i = 0; i < 7; i++) {
			colorIcons[i] = new ImageIcon("images/c" + i + ".gif");
		}
		
		double[][] rewards = model.getRewards();
		for (int i = 0; i < model.rows; i++) {
			for (int a = 0; a < model.cols; a++) {
				JLabel blankLabel = null;
				if (!dispRewardsOnly) {
					blankLabel = new JLabel("", blankIcon, JLabel.CENTER);
				} else {
					Formatter formatter = new Formatter(new StringBuilder());
					String reward = formatter.format(" %2.1f ", rewards[i][a]).toString();
					blankLabel = new JLabel(reward, null, JLabel.CENTER);
				}
				if (i == model.verticalLimit) {
					blankLabel.setBorder(LineBorder.createGrayLineBorder());
				} else {
					blankLabel.setBorder(null);
				}
				grid[i][a] = blankLabel;
				board.add(grid[i][a]);
			}
		}
		c.add(board, BorderLayout.SOUTH);
		
		// Add the retry button
		JButton retry = new JButton("Retry");
		retry.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Resetting...");
				resetNotify();
			}
		});
		c.add(retry, "Center");
		
		j.pack();
		j.setResizable(false);
		j.setLocation(400, 200);
		j.setVisible(true);
		
		j.addWindowListener(new java.awt.event.WindowAdapter() {
		    public void windowClosing(WindowEvent winEvt) {
		        System.exit(0); 
		    }
		});
		model.addListener(this);
	}
	
	private void resetNotify() {
		if (listener != null) {
			listener.reset();
		}
	}
	
	public void setListener(TetrisViewListener listener) {
		this.listener = listener;
	}
	
	public void resetGrid() {
		if (disableView) return;
		for (int i = 0; i < grid.length; i++) {
			for (int a = 0; a < grid[i].length; a++) {
				grid[i][a].setIcon(blankIcon);
			}
		}
	}
	
	public void compressed(int row) {
		if (disableView) return;
		for (int j = 0; j < grid[row].length; j++) {
			for (int l = row; l > 0; l--) {
				grid[l][j].setIcon(grid[l-1][j].getIcon());
			}
		}
	}
	
	public void setBlock(Block block, int row, int col) {
		setBlock(block, block.type, row, col);
	}
	public void clearBlock(Block block, int row, int col) {
		setBlock(block, -1, row, col);
	}
	
	private void setBlock(Block block, int color, int row, int col) {
		if (disableView) return;
		ImageIcon icon = (color == -1 ? blankIcon : colorIcons[color]);
		int[][] blockMat = block.blockMat;
		for (int i = block.topLeftR; i <= block.botRghtR; i++) {
			for (int j = block.topLeftC; j <= block.botRghtC; j++) {
				if (blockMat[i][j] > 0) {
					grid[row+i][col+j].setIcon(icon);
				}
			}
		}
	}

	public void blockAtBottom(Block block) {
		// do nothing
	}

	public void boardFull() {
		// do nothing
	}

	public void startRound() {
		// do nothing
	}

	public void afterAction(int action, boolean success) {
		// do nothing
	}

	public void beforeAction(int action) {
		// do nothing
	}

	public void dispRewards() {
		if (disableView) return;
		double[][] rewards = model.getRewards();
		for (int i = 0; i < rewards.length; i++) {
			for (int a = 0; a < rewards[i].length; a++) {
				Formatter formatter = new Formatter(new StringBuilder());
				String reward = formatter.format("%2.1f", rewards[i][a]).toString();
				grid[i][a].setText(reward);
				grid[i][a].setIcon(null);
				//grid[i][a].setSize(40,40);
				//System.out.println("Reward: (" + i + "," + a + "): " + rewardLabel.getText());
			}
		}
		frame.pack();
	}
	
	public void reset() {
		// Do Nothing
	}
	public boolean isViewEnabled() {
		return !this.disableView;
	}
	public void disableView() {
		this.disableView  = true;
	}
	public void enableView() {
		this.disableView  = false;
	}
	
}
