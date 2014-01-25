package bayesGame.levelcontrollers;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.RotatingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;
import bayesGame.BayesGame;
import bayesGame.bayesbayes.BayesNet;
import bayesGame.bayesbayes.BayesNode;
import bayesGame.ui.AnyKeyListener;
import bayesGame.ui.AssumingMousePlugin;
import bayesGame.ui.DefaultInterfaceView;
import bayesGame.ui.InteractingMousePlugin;
import bayesGame.ui.TutorialMousePlugin;

public class TutorialController extends Controller {
	
	DefaultInterfaceView UI;
	int level = 0;
	int part = 0;
	int failedtries = 0;
	
	int correctGnorantSolutions = 0;
	
	BayesNet net;
	ArrayList<String> order = new ArrayList<String>(3);
	
	boolean awaitingkeypresses;
	boolean awaitingmousemessage;

	public TutorialController(){
		UI = new DefaultInterfaceView();
		UI.addKeyListener(new AnyKeyListener(this));
		advanceTutorial();
		awaitingkeypresses = true;
		awaitingmousemessage = false;
	}
	
	private void advanceTutorial(){
		switch(level){
		case 0:
			levelOne();
			break;
		case 5:
			talkToOpin();
			break;
		case 6:
			talkToMom();
			break;
		case 7:
			talkToDad();
			break;
		case 8:
			beginWaitingPlayerActions();
			break;
		}

		
	}
	
	private void levelOne(){
		switch(part){
		case 0:
			UI.addText("Celia: When I was little, Opin, my big brother, told me that there was a treasure hidden near our village.");
			UI.addText("");
			UI.addText("(press space for more)");
			break;
		case 1:
			UI.addTextMoreClear("Celia: Of course I wanted to know more, but he claimed that that was all he knew.");
			break;
		case 2:
			UI.addTextMoreClear("Celia: I asked Opin how he knew about the treasure, but he told me to figure it out myself.");
			break;
		case 3:
			UI.addTextMoreClear("Celia: Well, I�ll show him, I thought! I'll find out how he knows, and then I can learn more about the treasure!");
			break;
		case 4:
			UI.addTextMoreClear("Celia: Okay, so let me think. He's too impatient to keep a secret overnight, that means he must have heard it today. And he hasn't been out of the house today, so he must have heard it from either mom or dad. And on the other hand, if there is a treasure that mom or dad knows about, they would definitely have told him.");
			break;
		case 5:
			net = new BayesNet();
			net.addNode("Dad");
			net.addNode("Mom");
			net.addDeterministicOr("Opin", "Mom", "Dad");
						
			UI.setGraph(net);
			UI.displayGraph(DefaultInterfaceView.graphTypeBayesGraph);
			
			UI.clearText();
			UI.addTutorialText("Celia has figured out a rule: your brother knows about the treasure, if (and only if) at least one of your parents knows about it.");
			UI.addText("");
			UI.addTextMore("Celia: So if I want to know more, I think I should talk to at least mom or dad. Or maybe both, if they both know different things? Hmm.");
			break;
		case 6:
			UI.addTextMoreClear("Celia: But this is a serious investigation about a big treasure in our village! So I have to think about all possibilities. First, maybe I just misheard, and Opin doesn't actually know anything about a treasure. In that case, what does that mean about mom or dad knowing?");
			break;
		case 7:
			net.observe("Opin", false);
			UI.updateGraph();
			UI.clearText();
			UI.addTutorialText("In the above picture, your brother is painted as " + BayesGame.falseColorName + ", since you are considering a possibility where he does not actually know about the treasure after all. In that case, would your parents know? Left-click on your parents to change them to " + BayesGame.trueColorName + ", showing that they know, or " + BayesGame.falseColorName + ", showing that they don't know. When both of your parents have the colors you think are correct, right-click on someone to check your answer. Remember, the rule is that your brother knows, if and only if either of your parents knows.");
			awaitingkeypresses = false;
			addGameMouseListeners();
			level = 1;
			break;
		case 8:
			net.clearAssumptions();
			net.observe("Opin", true);
			UI.clearMouseListeners();
			UI.addTextMoreClear("Celia: Right, so if my brother doesn't actually know about a treasure, that means mom and dad don't know, either! So if I find out that Opin doesn't know, I don't need to talk to mom or dad anymore.");
			awaitingkeypresses = true;
			break;
		case 9:
			UI.clearVisualizationHighlights();
			UI.updateGraph();
			addGameMouseListeners();
			awaitingkeypresses = false;
			UI.addTextClear("Celia: So what if Opin does really know? What are the possibilities then?");
			UI.addText("");
			UI.addTutorialText("There are several possibilities that are consistent with your brother knowing. Enter them one at a time, again left-clicking on your parents to select your reply and then right-clicking to make it. Again, the rule is that your brother knows, if and only if either of your parents knows.");
			level = 2;
			break;
		case 10:
			UI.clearMouseListeners();
			UI.addTextMoreClear("Celia: Yes, I know every possibility now! So... who should I talk to?");
			awaitingkeypresses = true;
			break;
		case 11:
			net.clearAssumptions();
			net.resetNetworkBeliefsObservations();
			net.setTrueValue("Opin", true);
			net.setTrueValue("Mom", false);
			net.setTrueValue("Dad", false);
			
			UI.updateGraph();
			
			UI.clearText();
			UI.addTutorialText("Time to actually talk to someone! If you want, you can now play around with the map, left-clicking the various people involved to test what it'd look like if you assumed specific things. When you are done, you can right-click on anyone to talk to them and find out what they *actually* know. When you've eliminated all but one of the possibilities, you've found the true one. Try to find it in as few right-clicks as possible!");
			awaitingmousemessage = false;
			
			level = 4;
			part = -1;
			
			UI.clearMouseListeners();
			addGameMouseListeners();
		}
		part++;
	}
	
	@Override
	public void keyMessage(Object o){
		if (awaitingkeypresses){
			advanceTutorial();
		}
	}
	
	@Override
	public void mouseMessage(Object o){
		if (awaitingmousemessage){
			advanceTutorial();
		}
	}

	@Override
	public void genericMessage() {
		if (level >= 4){
			net.updateBeliefs();
			ArrayList<Map<Object,Boolean>> newPossibilities = net.getNonZeroProbabilities("Opin");
			UI.updateVisualizations(newPossibilities);
		}
	}
	
	@Override
	public void genericMessage(Object o){
		if (level < 4){
			checkFirstStageMessage();
		} else {
			checkSecondStageMessage(o);
		}
	}
	
	private void checkFirstStageMessage(){
		if (net.isFullyAssumed()){
			if(level == 1){
				opinIgnorantAttempt();
			} else {
				opinGnorantAttempt();
			}
		}
	}
	
	private void opinIgnorantAttempt(){
		Map<Object,Boolean> offeredSolution = net.getCurrentAssignments();
		Map<Object,Boolean> correctSolution = new HashMap<Object,Boolean>(3);
		correctSolution.put("Mom", false);
		correctSolution.put("Dad", false);
		correctSolution.put("Opin", false);
		UI.clearVisualizationHighlights();
		if (offeredSolution.equals(correctSolution)){
			UI.addVisualization(correctSolution);
			UI.highlightVisualization(correctSolution, true);
			level = 0;
			part = 8;
			advanceTutorial();
		} else {
			if (UI.containsVisualization(offeredSolution)){
				UI.addTextClear("You tried that solution already, remember? It's listed there on the right.");
				UI.highlightVisualization(offeredSolution, true);
			} else {
				switch(failedtries){
				case(0):
					UI.addTextClear("That's not quite it. Remember, if either mom or dad knew, they would tell your brother. Still, you've found out that one possibility doesn't work, which is progress as well!");
				    break;
				case(1):
					UI.addTextClear("Nope, not that either. But at least you've eliminated another incorrect solution! Remember, if either mom or dad knew, they would tell your brother.");
				    break;
				case(2):
					UI.addTextClear("That wasn't it, either. But now you've eliminated all but one possibility, so what's the final answer?");
				    break;
				}
			    UI.addVisualization(offeredSolution, false);
			    UI.highlightVisualization(offeredSolution, true);
			    failedtries++;
			}
		}
	}
	
	private void opinGnorantAttempt(){
		Map<Object,Boolean> offeredSolution = net.getCurrentAssignments();
		
		Map<Object,Boolean> incorrectSolution = new HashMap<Object,Boolean>(3);
		incorrectSolution.put("Opin", true);
		incorrectSolution.put("Mom", false);
		incorrectSolution.put("Dad", false);
		
		Map<Object,Boolean> bothKnowSolution = new HashMap<Object,Boolean>(3);
		bothKnowSolution.put("Opin", true);
		bothKnowSolution.put("Mom", true);
		bothKnowSolution.put("Dad", true);
		
		UI.clearVisualizationHighlights();
		
		if (UI.containsVisualization(offeredSolution)){
			if (offeredSolution.equals(incorrectSolution)){
				UI.addTextClear("You already tried that, remember? It's listed there on the right.");
			} else {
				UI.addTextClear("Well, that's certainly a correct solution, but you already gave it, remember? It's listed there on the right.");
			}
			UI.highlightVisualization(offeredSolution, true);
		} else {
			
			if (offeredSolution.equals(incorrectSolution)){
				UI.addTextClear("I'm afraid that's not quite it - remember, if neither of your parents knows, then there's no way for your brother to know (according to this rule, at least).");
				UI.addVisualization(offeredSolution, false);
				UI.highlightVisualization(offeredSolution, true);
			} else {
				UI.addVisualization(offeredSolution);
				UI.highlightVisualization(offeredSolution, true);
				if (offeredSolution.equals(bothKnowSolution)){
					UI.addTextClear("Celia: Ha, I got it! If Opin knows, then it's possible that both mom and dad know!");
				} else {
					UI.addTextClear("Celia: So, hmm, if Opin knows, then at least mom or dad has to know...");
				}
				correctGnorantSolutions++;
				if (correctGnorantSolutions == 3){
					level = 0;
					part = 10;
					advanceTutorial();
				}
			}
		}
	}
	
	private void checkSecondStageMessage(Object o){
		BayesNode node = (BayesNode)o;
		String s = node.toString();
		if (!order.contains(s)){
			order.add(s);
			
			net.clearAssumptions();
			net.updateBeliefs();
			UI.updateGraph();
			UI.updateVisualizations(net.getNonZeroProbabilities("Opin"));
			
			switch(s){
			case "Opin":
				talkToOpin();
				break;
			case "Mom":
				talkToMom();
				break;
			case "Dad":
				talkToDad();
				break;
			}
		}
	}
	
	private void addGameMouseListeners(){
		PluggableGraphMouse pgm = new PluggableGraphMouse();
		pgm.add(new AssumingMousePlugin(this, MouseEvent.BUTTON1));
		pgm.add(new InteractingMousePlugin(this, MouseEvent.BUTTON3));
		UI.addGraphMouse(pgm);
	}
	
	private void beginWaitingPlayerActions(){
		level = 4;
		part = 0;
		awaitingkeypresses = false;
		
		net.observe(order.get(order.size()-1));
		net.updateBeliefs();
		
		ArrayList newPossibilities = net.getNonZeroProbabilities("Opin");
		UI.updateVisualizations(newPossibilities);
		UI.updateGraph();
		
		UI.clearText();
		
		if (newPossibilities.size() == 1){
			levelComplete();
		} else {
			addGameMouseListeners();
			UI.addTutorialText("Looks like you still need to narrow down the possibilities.");
		}
	}
	
	private void talkToOpin(){
		switch(part){
		case 0:
			level = 5;
			UI.clearMouseListeners();
			UI.addTextMoreClear("Celia: Opin, did you really say 'treasure'?");
			awaitingkeypresses = true;
			part++;
			break;
		case 1:
			UI.addTextMoreClear("Opin: That's right! A big huge treasure, just waiting for us to find it! But we need to hurry, before anyone else finds it.");
			part++;
			break;
		case 2:
			UI.addTextMoreClear("Celia: I thought you only knew that it was a treasure. Now you say it's huge too. And that we need to hurry.");
			part++;
			break;
		case 3:
			UI.addTextMoreClear("Opin: Well, aren't those things part of the definition of 'treasure'?");
			level = 8;
			break;
		}
	}
	
	private void talkToMom(){
		switch(part){
		case 0:
			level = 6;
			UI.clearMouseListeners();
			UI.addTextMoreClear("You go downstairs to find your mother. She's in her study, heating up stones and then throwing them into the large ball of water that is floating mid-air. The shapes of steam that are formed this way will give her some insight to what will happen in the future.");
			awaitingkeypresses = true;
			part++;
			break;
		case 1:
			UI.addTextMoreClear("Celia: Mom, did you hear about some treasure in the village?");
			part++;
			break;
		case 2:
			UI.addTextMoreClear("Mom: What did you say, dear? Treasure? No, I've been in my study all day, haven't heard of any treasures.");
			level = 8;
			break;
		}
	}
	
	private void talkToDad(){
		switch(part){
		case 0:
			level = 7;
			UI.clearMouseListeners();
			UI.addTextMoreClear("You go outside to find your father. He's just getting ready to go hunt one of the wild beasts in the forest in order to get all of you breakfast.");
			awaitingkeypresses = true;
			part++;
			break;
		case 1:
			UI.addTextMoreClear("Celia: Dad, did you hear about some treasure in the village?");
			part++;
			break;
		case 2:
			UI.addTextMoreClear("Your father looks up from the runestone that he has been calibrating. He has been preparing it so that it will glow warmer whenever he gets closer to a beast that's large enough for all of you to eat, but not dangerous enough that he'll be risking his life.");
			part++;
			break;
		case 3:
			UI.addTextMoreClear("Dad: Oh yes, I heard it from the mailman as he was doing his rounds. Didn't say much else, though.");
			level = 8;
			break;
		}
	}
	
	private void levelComplete(){
		UI.addTextClear("Level complete!");
	}

}
