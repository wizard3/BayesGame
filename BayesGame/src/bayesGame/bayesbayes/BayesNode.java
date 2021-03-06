/**
 * This code is terrible and probably contains multiple exponential blowups and I apologize to anyone reading this.
 * 
 * Note that some of the seeming unnecessary complexity in this class is in preparation for the possibility of 
 * implementing junction trees. Since this comment was originally written, I've forgotten which parts, however.
 */

package bayesGame.bayesbayes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.Pair;

public class BayesNode {
	
	public final Object type;
	protected Object[] scope;
	
	protected HashMap<Object,Integer> strides = new HashMap<Object,Integer>();
	
	private Fraction[] cpt;
	private Fraction[] potential;
	
	public String cptDescription;
	public String cptName;
	
	// getProbability returns the contents of this variable, but it isn't actually used for anything
	// inside the node: it's just a cache.
	private Fraction probability;

	private HashSet<Message> upstreamMessages = new HashSet<Message>();
	private HashSet<Message> downstreamMessages = new HashSet<Message>();
	
	private boolean observed = false;
	private Boolean assumedValue = null;
	private Boolean trueValue = null;
	private Set<String> properties = new HashSet<String>();
	
	protected BayesNode(Object type){
		this.type = type;
		this.scope = new Object[]{type};
		
		this.cpt = this.createRawFractionArray(this.scope);
		this.strides = this.createStridesFromScope(this.scope);
		
		this.cptDescription = this.createCPTDescription(this.scope);
		this.cptName = this.createCPTType(this.scope);
		
		this.potential = this.cpt.clone();
	}
	
	protected BayesNode(Object type, Object[] scope){
		this(type, scope, null, null);
	}
	
	protected BayesNode(Object type, Object[] scope, HashMap<Object,Integer> strides, Fraction[] cpt){
		this.type = type;

		// the scope of a node must contain its own type
		if (!Arrays.asList(scope).contains(type)){
			// if one is missing, try to add the type of the node to the scope
			if ((strides == null) && (cpt == null)){				
				scope = this.copyArrayAddingItem(scope, type);
			} else {
				throw new IllegalArgumentException("The scope of a node must contain its own type");
			}
		}
		
		this.scope = scope;
		
		if (strides != null){
			this.strides = strides;
		} else {
			this.strides = this.createStridesFromScope(this.scope);
		}
		
		if (cpt != null){
			this.cpt = copyFraction(cpt);
		} else {
			this.cpt = createRawFractionArray(scope);
		}
		
		this.potential = copyFraction(this.cpt);
	}
	
	private Object[] copyArrayAddingItem(Object [] array, Object item){
		Object[] newscope = new Object[array.length + 1];
		newscope[0] = item;
		int location = 1;
		for (Object o : array){
			newscope[location] = o;
			location++;
		}
		
		return newscope;
	}
	
	protected boolean addItemToScope(Object item){
		if (Arrays.asList(scope).contains(item)){
			return false;
		}
		
		scope = copyArrayAddingItem(scope, item);
		
		cpt = this.createRawFractionArray(scope);
		strides = this.createStridesFromScope(scope);
		
		resetPotential();
		
		return true;	
	}
	
	protected void updateProbability(){
		Fraction[] probabilities = this.getNormalizedMarginalPotential(type);
		probability = probabilities[0];
	}
	
	public Fraction getProbability(){
		if (probability == null){
			updateProbability();
		}
		return copyFraction(probability);
	}
	
	protected void setProbability(Fraction probability){
		this.probability = probability;
	}
	
	public ArrayList<Map<Object,Boolean>> getNonZeroProbabilities(){
		indexChooser chooser = new indexChooser();
		ArrayList<Map<Object,Boolean>> truthValues = new ArrayList<Map<Object,Boolean>>();
		
		// map<object,boolean> items = the truth values of single items in a p > 0 row
		// list of maps = the whole thing
		
		for (int i = 0; i < potential.length; i++){
			Fraction f = potential[i];
			if (f.doubleValue() > 0.00d){
				ArrayList<Boolean> valuesAtIndex = chooser.getTruthValues(i);
				Map<Object,Boolean> row = new HashMap<Object,Boolean>(valuesAtIndex.size());
				for (int j = 0; j < valuesAtIndex.size(); j++){
					Object o = scope[j];
					row.put(o, valuesAtIndex.get(j));
				}
				truthValues.add(row);
			}
		}
		
		return truthValues;
	}
	
	private Fraction[] createRawFractionArray(Object[] scope){
		Fraction[] array = new Fraction[(int) Math.pow(2, scope.length)];
		Fraction fraction = new Fraction(1, array.length);
		Arrays.fill(array, fraction);
		return array;
	}
	
	private HashMap<Object,Integer> createStridesFromScope(Object[] scope){
		HashMap<Object,Integer> stride = new HashMap<Object,Integer>();
		
		int i = 1;
		
		for (Object o : scope){
			stride.put(o, i);
			i = i * 2;
		}		
		return stride;
	}
	
	private String createCPTDescription(Object[] scope) {
		if (scope.length == 1){
			return "<html>'" + this.type + "' is a <b>prior variable</b>.<br>The truth values of any of its child variables are derived from it, <br>as well as from any other parent variables.";
		} else {
			return "<html>'" + this.type + "' is a <b>conditional probability variable</b><br>of type <b>custom distribution</b>.";			
		}
	}
	
	private String createCPTType(Object[] scope){
		if (scope.length == 1){
			return "Prior";
		} else {
			return "Custom";			
		}
	}
	
	public void setTrueValue(boolean value){
		trueValue = value;
	}
	
	
	/**
	 * Resets the node's potential to the initial CPT, clearing any changes from messages,
	 * setting the node as unobserved, and clearing any assumed values. To reset the node's
	 * potential while maintaining its status as observed, use resetPotential instead.
	 */
	public void resetNode(){
		
		observed = false;
		potential = copyFraction(cpt);
		probability = null;
		assumedValue = null;
	}
	
	/**
	 * Resets the node's potential to the initial CPT, clearing any changes from messages.
	 * If the node has been observed or assumed, it remains so, with corresponding effects
	 * to the CPT.
	 * To reset the node's observation status as well, use resetNode instead.
	 */
	public void resetPotential(){
	
		potential = copyFraction(cpt);
		if (observed){
			observe();
		} else if (assumedValue != null){
			assumeValue(assumedValue);
		}
		probability = null;
	}
	
	
	
	public boolean isObserved(){

		return observed;
	}
	
	public boolean isAssumed(){
		
		if (assumedValue == null){
			return false;
		} else {
			return true;
		}
	}
	
	public boolean setProbabilityOfUntrueVariables(Fraction probability, Object... variables){
		probability = copyFraction(probability);
		
		if (!checkCPTInputForValidity(variables)){
			return false;
		}
		
		if (probability.doubleValue() > 1.0d || probability.doubleValue() < 0.0d){
			return false;
		}
		
		indexChooser selfChecker = new indexChooser();
		selfChecker.requestUntrue(variables);
		int index = selfChecker.getIndex();
		cpt[index] = probability;
		potential[index] = copyFraction(probability);
		
		this.probability = null;
		
		return true;
	}
	
	private boolean checkCPTInputForValidity(Object[] variables){
		if (variables.length > scope.length){
			return false;
		}
		
		List<Object> scopeList = Arrays.asList(scope);
		for (Object o : variables){
			if (!scopeList.contains(o)){
				return false;
			}
		}
		
	    return true;
	}
	
	/**
	 * Observes the node, setting its probabilities according to its true value.
	 * If the true value has not been set, randomly generates it based on the
	 * current probabilities. Note that network is responsible for updating the
	 * probabilities of any adjacent nodes afterwards.
	 */
	public void observe(){
		boolean assumedCleared = false;
		if (assumedValue != null){
			assumedCleared = true;
			resetNode();
		}
		
		observed = true;
				
		if (trueValue == null){
			double diceRoll = Math.random();
			if (diceRoll <= this.getProbability().doubleValue()){
				trueValue = Boolean.TRUE;
			} else {
				trueValue = Boolean.FALSE;
			}
			if (assumedCleared){
				System.out.println("WARNING: the node had an assumed value which was cleared when observing it, and its value was then randomly generated - the probability used for generating the value may not have been the intended one.");
			}
		}
				
		changePotentialOfValues(!trueValue, Fraction.ZERO);
		
		// normalizeNodePotential();
		
		probability = null;
	}
	
	private void changePotentialOfValues(boolean value, Fraction newpotential){
		indexChooser selfChooser = new indexChooser();
		ArrayList<Integer> locationsToChange;
		
		if (value){
			
			locationsToChange = selfChooser.getAllIndexes(type, true);
			
		} else {
			
			locationsToChange = selfChooser.getAllIndexes(type, false);
		}
		
		for (Integer i : locationsToChange){
			potential[i] = newpotential;
		}
		
		
	}
	
	public void observe(boolean observation){
		trueValue = observation;
		observe();
	}
	
	public boolean assumeValue(boolean value){
		if (observed){
			return false;
		}
		
		if (assumedValue != null){
			clearAssumedValue();
		}
		
		assumedValue = value;
		
		changePotentialOfValues(!value, Fraction.ZERO);
		
		// normalizeNodePotential();
		
		probability = null;
		
		return true;
	}
	
	public void clearAssumedValue(){
		if (assumedValue != null){
			if (!observed){
				resetNode();
			} else {
				assumedValue = null;
			}
		}
	}
	
	
	public Boolean assumedValue(){
		if (assumedValue == null){
			return null;
		}
		if (assumedValue){
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}
	
	public Fraction[] getPotential(){
		
		return copyFraction(potential);	
	}
	
	private Fraction copyFraction(Fraction f){
		Fraction newFraction = new Fraction(f.getNumerator(), f.getDenominator());
		return newFraction;
	}
	
	private Fraction[] copyFraction(Fraction[] f){
		Fraction[] newFraction = new Fraction[f.length];
		for (int i = 0; i < newFraction.length ; i++){
			newFraction[i] = copyFraction(f[i]);
		}
		return newFraction;
	}
	
	public Fraction[] getMarginalPotential(Object o){
		Object[] targetScope = {o};
		HashMap<Object,Integer> targetStride = this.createStridesFromScope(targetScope);
		return marginalizeOut(potential, targetScope, targetStride);
	}
	
	public Fraction[] getNormalizedMarginalPotential(Object o){
		Fraction[] marginalPotential = this.getMarginalPotential(o);
		return this.normalizePotentials(marginalPotential);
	}
	
	protected void normalizeNodePotential(){
		Fraction total = Fraction.ZERO;
		for (Fraction f : potential){
			total = total.add(f);
		}
		if (!total.equals(Fraction.ZERO)){
			for (int i = 0; i < potential.length; i++){
				Fraction f = potential[i];
				potential[i] = f.divide(total);
			}
		}
		
		for (Fraction f : potential){
		}
	}
	
	protected void receiveUpstreamMessage(Message message){
		upstreamMessages.add(message);
	}
	
	protected void receiveDownstreamMessage(Message message){
		downstreamMessages.add(message);
	}
	
	protected boolean receivedMessageFrom(BayesNode source, boolean upstream){
		HashSet<Message> receivedMessages;
		if (upstream){
			receivedMessages = upstreamMessages;
		} else {
			receivedMessages = downstreamMessages;
		}
		
		return receivedMessages.contains(new Message(source));
	}
	
	protected void clearMessages(){
		upstreamMessages = new HashSet<Message>();
		downstreamMessages = new HashSet<Message>();
	}
	
	public Fraction[] normalizePotentials(Fraction[] targetPotential){
		Fraction totalSum = Fraction.ZERO;
		for (Fraction f : targetPotential){
			totalSum = totalSum.add(f);
		}
		if (totalSum.equals(Fraction.ZERO)){
			Fraction f = Fraction.ZERO;
			Arrays.fill(targetPotential, f);
			System.out.println("Encountered division by zero, check your network");
		} else {
			for (int i = 0; i < targetPotential.length; i++){
				Fraction f = targetPotential[i];
				targetPotential[i] = f.divide(totalSum);
			}
		}

		return targetPotential;
	}
	
	/**
	 * Multiplies received messages from the specified direction with the initial
	 * CPT, sums out any variables not in the specified scope, and packs the result into a
	 * message. Note that this method does NOT check whether the node has received all the
	 * prerequisite messages and is thus ready to send - this is the responsibility of the
	 * calling class. (Individual nodes are not aware of their neighbors, thus cannot check
	 * their own readiness.)
	 * 
	 * @param upstream true if the message is sent to be upstream, false if downstream
	 * @param scope the scope of the message
	 * @return a message with the specified scope, computed message, and this node as the sender
	 */
	protected Message generateMessage(boolean upstream, Object... targetScope){
		HashSet<Message> receivedMessages;
		if (upstream){
			receivedMessages = upstreamMessages;
		} else {
			receivedMessages = downstreamMessages;
		}
		
		Fraction[] multipliedPotential;
		
		if (!receivedMessages.isEmpty()){
			multipliedPotential = multiplyPotentialWithMessages(this.getPotential(), receivedMessages);
		} else {
			multipliedPotential = potential;
		}
		
		HashMap<Object,Integer> targetStride = this.createStridesFromScope(targetScope);
		
		Fraction[] potentialMessage = marginalizeOut(multipliedPotential, targetScope, targetStride);
		Message message = new Message(targetScope, potentialMessage, targetStride, this);
		
		return message;
	}
	
	protected Message generateUpstreamMessage(Object... targetScope){
		return generateMessage(true, targetScope);
	}
	
	protected Message generateDownstreamMessage(Object... targetScope){
		return generateMessage(false, targetScope);
	}
	
	protected void multiplyPotentialWithMessages(){
				
		probability = null;
		
		if (!upstreamMessages.isEmpty()){
			potential = multiplyPotentialWithMessages(potential, upstreamMessages);
		}
		
		probability = null;
		
		if (!downstreamMessages.isEmpty()){
			potential = multiplyPotentialWithMessages(potential, downstreamMessages);
		}
		
		probability = null;
		
		clearMessages();
		probability = null;
		
	}	
	
	private Fraction[] multiplyPotentialWithMessages(Fraction [] currentPotential, HashSet<Message> receivedMessages){
		indexChooser selfChooser = new indexChooser();
		Fraction[] newPotential = copyFraction(currentPotential);
		for (Message m : receivedMessages){
			if (m.scope.length == 1){
				Object o = m.scope[0];
				selfChooser.requestUntrue(o);
				ArrayList<Integer> arrayReferencesToWantedObjectBeingUntrueInOwnPotential = selfChooser.getAllIndexes();
				Fraction trueMultiplier = m.message[0];
				Fraction untrueMultiplier = m.message[1];
				for (int i = 0; i < currentPotential.length; i++){
					if (arrayReferencesToWantedObjectBeingUntrueInOwnPotential.contains(i)){
						newPotential[i] = newPotential[i].multiply(untrueMultiplier);
					} else {
						newPotential[i] = newPotential[i].multiply(trueMultiplier);
					}
				}
			} else {
				throw new IllegalStateException("Message contained truth values for multiple variables, not yet implemented");
				//TODO: implement the case where the message contains the truth values for multiple variables!
			}
			selfChooser.resetUntrue();
		}
		return newPotential;
	}
	
	
	private Fraction[] marginalizeOut(Fraction[] currentPotential, Object[] targetScope, HashMap<Object,Integer> targetStride){

		Fraction[] newPotential = new Fraction[(int) Math.pow(2, targetScope.length)];
		
		// note that we are assuming that the scope and stride of the potential we're summing out from are the same as for the node in general
		indexChooser targetChooser = new indexChooser(targetScope, newPotential, targetStride);
		indexChooser selfChooser = new indexChooser(this.scope, currentPotential, this.strides);
		
		// find the variables to be summed out by comparing the current scope and the target one
		HashSet<Object> targetSet = new HashSet<Object>(Arrays.asList(targetScope));
		HashSet<Object> differenceSet = new HashSet<Object>(Arrays.asList(this.scope));
		differenceSet.removeAll(targetSet);
		
		ArrayList<Integer> currentPotentialArrayReferencesToItemsToBeSummedOut = new ArrayList<Integer>();
		for (Object o : differenceSet){
			for (int i = 0; i < this.scope.length; i++){
				if (this.scope[i].equals(o)){
					currentPotentialArrayReferencesToItemsToBeSummedOut.add(i);
				}
			}
		}

		// sum out the contents of the old array locations to the new ones
		for (int i = 0; i < currentPotential.length; i++){
			//   take the fraction in the current index
			Fraction f = currentPotential[i];
			//   find the array reference to the new array that corresponds to the logical contents of this index, but without
			//   the variables to be summed out
			ArrayList<Boolean> logicalContentsOfIndex = selfChooser.getTruthValues(i);
			ArrayList<Boolean> logicalContentsOfTargetIndex = new ArrayList<Boolean>();
			for (int j = 0; j < logicalContentsOfIndex.size(); j++){
				if (!currentPotentialArrayReferencesToItemsToBeSummedOut.contains(j)){
					logicalContentsOfTargetIndex.add(logicalContentsOfIndex.get(j));
				}
			}
			int targetPotentialArrayReference = targetChooser.getIndex(logicalContentsOfTargetIndex);
			
			//   add the fraction to that new location
			if (newPotential[targetPotentialArrayReference] == null){
				newPotential[targetPotentialArrayReference] = f;
			} else {
				newPotential[targetPotentialArrayReference] = newPotential[targetPotentialArrayReference].add(f);
			}
		}
		
		
		
		return newPotential;
	}
	
	

	public boolean equals(Object other){
		
		boolean result = false;
		
		if (other instanceof BayesNode){
			BayesNode theOther = (BayesNode)other;
			result = (this.type.equals(theOther.type));
		}
		
		return result;
	}
	
	public int hashCode(){
		
		return type.hashCode();
	}
	
	public String toString(){
		return type.toString();
	}
	
	public void addProperty(String property){
		properties.add(property);
	}
	
	public boolean hasProperty(String property){
		return properties.contains(property);
	}
	
	public void removeProperty(String property){
		properties.remove(property);
	}




	private class indexChooser {
		
		private HashSet<Object> requestedUntrueVariables = new HashSet<Object>();
		
		private final Object[] targetScope;
		private final Fraction[] targetFactor;
		private final HashMap<Object,Integer> targetStrides;
		
		public indexChooser(Object[] targetScope, Fraction[] targetFactor, HashMap<Object,Integer> targetStrides){
			this.targetScope = targetScope;
			this.targetFactor = targetFactor;
			this.targetStrides = targetStrides;
		}
		
		public indexChooser(Message message){
			this(message.scope, message.message, message.strides);
		}
		
		public indexChooser(){
			this(scope, potential, strides);
		}
				
		public void requestUntrue(Object o){
			requestedUntrueVariables.add(o);
		}
		
		public void requestUntrue(Object[] o){
			requestedUntrueVariables.addAll(Arrays.asList(o));
		}
				
		public void resetUntrue(){
			requestedUntrueVariables = new HashSet<Object>();
		}
		
		/**
		 * Returns the index in the CPT/potential array corresponding to the row where
		 * all of the variables specified via the requestUntrue methods are untrue, and
		 * all the rest are true.
		 * 
		 * @return the CPT/potential array index 
		 */
		public int getIndex(){
			int location = 0;
			for (Object o : requestedUntrueVariables){
				if (targetStrides.containsKey(o)){
					location = location + targetStrides.get(o);
				}
			}
			
			return location;
		}
		
		public int getIndex(ArrayList<Boolean> logicalValues){
			this.resetUntrue();
			for (int i = 0; i < logicalValues.size(); i++){
				if (!logicalValues.get(i)){
					this.requestUntrue(targetScope[i]);
				}
			}
			return this.getIndex();
		}
		

		/**
		 * Returns a list of integers containing every index of the potential array
		 * where all the variables specified via the requestUntrue methods are untrue.
		 * 
		 * This is probably a horrible terrible implementation and there's some obvious
		 * way of making it faster. Right now it loops through the whole potential array,
		 * checks the logical equivalent of each array index, and then adds it to the
		 * list of indexes if it matches the criteria. 
		 * 
		 * @return
		 */
		public ArrayList<Integer> getAllIndexes(){
			ArrayList<Integer> allIndexes = new ArrayList<Integer>();
			for (int i = 0; i < targetFactor.length; i++){
				boolean canBeAdded = true;
				ArrayList<Boolean> x = getTruthValues(i);
				for (int j = 0; j < x.size(); j++){
					if (x.get(j) && requestedUntrueVariables.contains(targetScope[j])){
						canBeAdded = false;
					}
				}
				if (canBeAdded){
					allIndexes.add(i);
				}
			}
			
			return allIndexes;
		}
		
		/**
		 * Returns a list of all indices in which the specified object is either true or false.
		 * 
		 * @param o The object under examination
		 * @param t Whether to return indices where it's true, or indices where it's false
		 * @return An ArrayList of indexes
		 */
		public ArrayList<Integer> getAllIndexes(Object o, boolean t){
			
			ArrayList<Integer> indexes = new ArrayList<Integer>();
			int stride = targetStrides.get(o);
			for (int location = 0; location < targetFactor.length; location++){
				if (variableTruthValue(location, stride) == t) {
					indexes.add(location);
				}
			}		
			return indexes;
		}
			
		public ArrayList<Boolean> getTruthValues(int location){
			
			ArrayList<Boolean> values = new ArrayList<Boolean>();
			
			for (Object o : targetScope){
				int stride = targetStrides.get(o);
				values.add(variableTruthValue(location, stride));
			}
			
			return values;
		}
		
		private boolean variableTruthValue(int location, int stride){
			// yes, intentionally doing a division with ints here, as I'd want to round down the result anyway
			int value = (location / stride) % 2;
			if (value == 0){
				return true;
			} else {
				return false;
			}	
		}	
	}
	
}
