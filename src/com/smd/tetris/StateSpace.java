package com.smd.tetris;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StateSpace {
	public List<State> states = new ArrayList<State>();
	/**
	 * Looks up a state in the state space only on the basis of features and no
	 * other attributes of the state.
	 * 
	 * @param state
	 * @param addIfNotPresent
	 * @return State if found, else <code>null</code>
	 */
	public State lookupState(State state, boolean addIfNotPresent) {
		State lookedUp = null;
		int pos = Collections.binarySearch(this.states, state);
		if (pos < 0) {
			if (addIfNotPresent) {
				this.states.add(-pos-1, state);
				lookedUp = state;
			}
		} else
			lookedUp = this.states.get(pos);
		return lookedUp;
	}
	/**
	 * @see StateSpace#lookupState(State, boolean)
	 */
	public State lookupState(State state) {
		return lookupState(state, true);
	}
	/**
	 * Returns if the state represented by the feature vector is present
	 * in the state space
	 * 
	 * @see StateSpace#lookupState(State, boolean)
	 */
	public State lookupState(double[] features) {
		return lookupState(new State(features, false, 0, features.length, 0, null), false);
	}
}
