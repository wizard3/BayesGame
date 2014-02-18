package bayesGame.levelcontrollers;

import bayesGame.bayesbayes.BayesNet;
import bayesGame.ui.DefaultInterfaceView;
import bayesGame.ui.swinglisteners.AnyKeyListener;

public class TutorialLevel2Controller extends Controller {
	
	private DefaultInterfaceView UI;
	private int level = 0;
	private int part = 0;
	
	private BayesNet net;
	
	private boolean awaitingkeypresses;
	private boolean awaitingmousemessage;

	public TutorialLevel2Controller() {
		UI = new DefaultInterfaceView();
		UI.addKeyListener(new AnyKeyListener(this));
		awaitingkeypresses = true;
		awaitingmousemessage = false;
		advanceTutorial();
	}
	
	private void advanceTutorial(){
		switch(level){
		case 0:
			introChat();
			break;
		}
	}
	
	private void introChat(){
		switch(part){
		case 0:
			net = new BayesNet();
			
			/*
			net.addNode("Mailman");
			net.addDeterministicOr("Dad", "Mailman");
			net.addNode("Mom");
			net.addDeterministicOr("Opin", "Mom", "Dad");
			*/
			
			net.addNode("Mom");
			net.addNode("Dad", new Object[]{"Mailman"});
			net.addNode("Opin", new Object[]{"Mom", "Dad"});
			net.addNode("Mailman");
			
			net.connectNodes("Mom", "Opin");
			net.connectNodes("Dad", "Opin");
			net.connectNodes("Mailman", "Dad");
			
			net.observe("Mom", false);
			net.observe("Dad", true);
			net.observe("Opin", true);
			net.observe("Mailman", true);
			
			UI.setGraph(net);
			UI.displayGraph(DefaultInterfaceView.graphTypeBayesGraph);
			
			UI.addText("Celia: Okay, so we know that dad heard about the treasure from the mailman. And didn't know anything else about the treasure, just that the mailman had spoken about it. So we should find out how the mailman knows about it!");
			break;
		case 1:
			UI.addTextClear("Opin: Right. So, the ");
		}
		part++;
	}
	
	

	@Override
	public void keyMessage(Object o) {
		if (awaitingkeypresses){
			advanceTutorial();
		}

	}

	@Override
	public void mouseMessage(Object o) {
		// TODO Auto-generated method stub

	}

	@Override
	public void genericMessage() {
		// TODO Auto-generated method stub

	}

	@Override
	public void genericMessage(Object o) {
		// TODO Auto-generated method stub

	}

}