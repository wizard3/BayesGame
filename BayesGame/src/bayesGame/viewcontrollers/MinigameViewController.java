package bayesGame.viewcontrollers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import bayesGame.bayesbayes.BayesNet;
import bayesGame.bayesbayes.BayesNode;
import bayesGame.bayesbayes.OptionNodeOption;
import bayesGame.levelcontrollers.ChoiceMenu;
import bayesGame.levelcontrollers.Controller;
import bayesGame.levelcontrollers.LevelController;
import bayesGame.minigame.DiscussionNet;
import bayesGame.minigame.MinigameController;
import bayesGame.ui.DialogMenu;
import bayesGame.ui.GameInterface;
import bayesGame.ui.GraphPanel;
import bayesGame.ui.verbs.InteractingVerb;
import bayesGame.ui.verbs.Verb;
import bayesGame.world.TutorialMessages;

public class MinigameViewController implements Controller, ViewController {
	
	private final MinigameController owner;
	private final DiscussionNet gameNet;
	private GameInterface gameInterface;
	private GraphPanel graphPanel;
	private JPanel infoPanel;
	private JLabel infoPanelText;
	private JPanel buttonPanel;
	
	private boolean lectureMode;
	private String helpReference;

	
	public MinigameViewController(MinigameController minigameController,
			DiscussionNet gameNet) {
		this.owner = minigameController;
		this.gameNet = gameNet;
		this.gameInterface = new GameInterface();
		this.lectureMode = false;
	}
	
	public void setLectureMode(boolean bool){
		lectureMode = bool;
	}
	
	public void display(){
		initializeView();
		gameInterface.display();
		gameInterface.refreshScrollbar();
	}
	
	private void initializeView(){
		graphPanel = new GraphPanel(gameNet);
		Verb interactingVerb = new InteractingVerb(this, Verb.returnCall.MouseMessage, MouseEvent.BUTTON3);
		Verb textVerb = new InteractingVerb(this, Verb.returnCall.KeyMessage, MouseEvent.BUTTON1);
		graphPanel.addVerb(interactingVerb);
		graphPanel.addVerb(textVerb);

		gameInterface.setBigPanel(graphPanel);
		
	    infoPanel = new JPanel();
	    gameInterface.setSmallPanel(infoPanel);
	    
	    buttonPanel = new JPanel();
	    
	    if (helpReference != null){
		    JButton helpButton = new JButton("Show help");
		    helpButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                String helpText = TutorialMessages.get(helpReference);
	            	gameInterface.addText(helpText);
	            	gameInterface.processEventQueue();
	            }
	        });
		    
		    buttonPanel.add(helpButton);
	    }
	    
	    if (lectureMode){
	    	final JButton doneButton = new JButton("Done");
	    	
	        doneButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                owner.genericMessageReceived();
	                doneButton.setEnabled(false);
	            }
	        });      

	    	buttonPanel.add(doneButton);
	    }
	    
	    gameInterface.setButtonPanel(buttonPanel);
	}
	
	public void addText(String text){
		gameInterface.addText(text);
	}
	
	public void showText(String text){
		this.addText(text);
		this.processEventQueue();
	}
	
	public void addRefreshDisplay(){
		gameInterface.addRefreshDisplay();
	}
	
	public void updateGraph(){
		graphPanel.updateGraph();
	}
	
	public void processEventQueue(){
		gameInterface.processEventQueue();
	}
	
	public void dispose() {
		gameInterface.dispose();
	}

	@Override
	public void keyMessage(Object o) {
	    BayesNode node = (BayesNode)o;
	    String text = gameNet.getCPTDescription(node.type);
	    
	    if (node.hasProperty("hidden")){
	    	text = text + "<p><p>It is a <b>hidden variable</b>, meaning that you cannot observe it directly.<br>You need to discover its value through the other variables it is<br>connected to.";
	    } else if (node.isObserved()) {
	    	text = text + "<p><p>It is an <b>observed variable</b>, meaning that you know its value for certain.";
	    } else {
	    	text = text + "<p><p>It is an <b>observable variable</b>, meaning that you can try<br>to discover its value by right-clicking on it.";
	    }
	    
	    int nodeProbability = (int)(100 * node.getProbability().doubleValue());
	    text = text + "<p><p>It has a " + nodeProbability + "% chance of being true.</html>";
	    
	    System.out.println(text);
	    
	    infoPanel.removeAll();
	    infoPanel.revalidate();
	    infoPanel.repaint();
	    	    
	    infoPanelText = new JLabel();
		infoPanelText.setText(text);
		infoPanelText.setAlignmentY(Component.LEFT_ALIGNMENT);
		infoPanelText.setFont(new Font("Serif", Font.PLAIN, 24));
		
		infoPanel.add(infoPanelText);
		
		gameInterface.getFrame().revalidate();
		
		// gameInterface.getFrame().pack();
	}

	@Override
	public void mouseMessage(Object o) {
		if (o != null){
			BayesNode node = (BayesNode)o;
			Object type = node.type;
			owner.chooseNode(type);
		}
	}

	@Override
	public void genericMessage() {
		owner.genericMessageReceived();
		
	}

	@Override
	public void genericMessage(Object o) {
		Object[] message = (Object[])o;
		Object type = message[0];
		
		OptionNodeOption option = (OptionNodeOption)message[1];
		owner.observeNode(type, option);
	}

	public void displayOptions(Object node) {
		if (gameNet.getOptions(node) != null){
			DialogMenu menu = new DialogMenu(this, gameInterface.getFrame(), node, gameNet.getOptions(node));
			menu.setVisible(true);
		} else {
			owner.observeNode(node, null);
		}
	}

	public void displayPopup(String description, String response) {
		String message = description + "\n\n" + response;
		JOptionPane.showMessageDialog(gameInterface.getFrame(), message, "", JOptionPane.PLAIN_MESSAGE);
	}

	@Override
	public void giveControlTo(ViewController viewController) {
		viewController.receiveControl(gameInterface);
	}
	
	@Override
	public void receiveControl(Object control){
		gameInterface = (GameInterface)control;
		gameInterface.setOwner(this);
	}

	@Override
	public void setOwner(LevelController owner) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processingDone() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showMenu(ChoiceMenu choice, LevelController levelController) {
		// TODO Auto-generated method stub
		
	}

	public void setHelpReference(String helpReference) {
		this.helpReference = helpReference;
		
	}


	
	

}
