package bayesGame.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import bayesGame.ui.swinglisteners.AnyKeyListener;
import bayesGame.ui.swinglisteners.KeyController;
import bayesGame.viewcontrollers.ViewController;

public class GameInterface implements InterfaceView, KeyController {

	private JFrame frame;
	private JPanel bigPanel;
	private JPanel smallPanel;
	private JTextPane textPane;
	private JScrollPane scroll;
	
	private ViewController owner;
	private boolean waitingForInput;
	
	private List<String> events;
	
	public GameInterface() {
		frame = new JFrame("Academy Game");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	
		
		bigPanel = new JPanel();
		smallPanel = new JPanel();
		textPane = new JTextPane();
		
		events = new ArrayList<String>();
		
		waitingForInput = false;
		textPane.addKeyListener(new AnyKeyListener(this));
		
		addComponentsToPane(frame.getContentPane());
	}
	
	public void setBigPanel(JPanel bigPanel) {
		frame.getContentPane().remove(this.bigPanel);
		
	    bigPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		bigPanel.setMinimumSize(new Dimension(500,500));
		frame.getContentPane().add(bigPanel, getBigPanelConstraints());
		
		this.bigPanel = bigPanel;
	}

	public void setSmallPanel(JPanel smallPanel) {
		frame.getContentPane().remove(this.smallPanel);
		
	    smallPanel.setBorder(BorderFactory.createLineBorder(Color.black));
	    smallPanel.setMinimumSize(new Dimension(250,500));
		frame.getContentPane().add(smallPanel, getSmallPanelConstraints());
		
		this.smallPanel = smallPanel;
	}
	
	public void display(){
		frame.pack();
		frame.setVisible(true);
	}
		
	private void addComponentsToPane(Container pane){
		pane.setLayout(new GridBagLayout());
	    
		GridBagConstraints c = getBigPanelConstraints();
		
	    bigPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		bigPanel.setMinimumSize(new Dimension(500,500));
	    
	    pane.add(bigPanel, c);
	    
	    c = getSmallPanelConstraints();
	    
	    smallPanel.setBorder(BorderFactory.createLineBorder(Color.black));
	    smallPanel.setMinimumSize(new Dimension(250,500));
	    
	    pane.add(smallPanel, c);
	    
	    c = new GridBagConstraints();
	    
	    textPane.setEditable(false);
	    textPane.setPreferredSize(new Dimension(400,150));
	    textPane.putClientProperty("IgnoreCharsetDirective", Boolean.TRUE);
	    
	    scroll = new JScrollPane (textPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	    
	    c.gridx = 0;
	    c.gridy = 1;
	    c.gridwidth = 2;
	    c.ipady = 0;
	    c.weightx = 1;
	    c.weighty = 1;
	    c.fill = GridBagConstraints.BOTH;
	    pane.add(scroll, c);
	}
	
	private GridBagConstraints getBigPanelConstraints(){
		GridBagConstraints c = new GridBagConstraints();
		
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1;
	    c.weighty = 1;
	    c.ipady = 0;
	    c.ipadx = 0;
	    c.fill = GridBagConstraints.BOTH;
	    
	    return c;
	}
	
	private GridBagConstraints getSmallPanelConstraints(){
		GridBagConstraints c = new GridBagConstraints();
		
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1;
	    c.weighty = 1;
	    c.ipady = 0;
	    c.ipadx = 0;
	    c.fill = GridBagConstraints.BOTH;
	    
	    return c;
	}

	@Override
	public void addText(String text) {
		if (events.size() > 0){
			addMoreIndicatorToPreviousItem();
		}
		events.add(text);
	}

	private void addMoreIndicatorToPreviousItem(){
		int lastitem = events.size()-1;
		String text = events.get(lastitem);
		if (!text.startsWith("$$")){
			text = text + ">";
			events.remove(lastitem);
			events.add(text);
		}
	}
	
	@Override
	public void addRefreshDisplay() {
		addText("$$REFRESHDISPLAY");
	}

	@Override
	public void processEventQueue() {
		if (events.size() > 0){
			processFirstEvent();
			waitForInput();
		} else {
			owner.processingDone();
		}
	}
	
	private void processFirstEvent(){
		String text = events.remove(0);
		
		if (text.equals("$$REFRESHDISPLAY")){
			bigPanel.repaint();
		} else {
			deletePreviousNextIndicatorFromPane();
			writeToTextPane(text);
		}
	}
	
	private void deletePreviousNextIndicatorFromPane(){
		try {
			String text = textPane.getText(textPane.getDocument().getLength()-3, 1);
			if (text.equals(">")){
				textPane.getDocument().remove(textPane.getDocument().getLength()-3, 1);
			}
		} catch (BadLocationException e) { }
		
		
		// textPane.getDocument().remove(textPane., arg1);
	}
	
	private void writeToTextPane(String text){
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setFontSize(style, 16);
		
		text = text + System.getProperty("line.separator");
		
		StyledDocument doc = textPane.getStyledDocument();
		
		try { doc.insertString(doc.getLength(), text, style); }
        catch (BadLocationException e){}
		
		
		
		textPane.setCaretPosition(textPane.getDocument().getLength());
		scroll.revalidate();
		// frame.pack();
	}
	
	private void waitForInput(){
		textPane.requestFocusInWindow();
		
		waitingForInput = true;
		
	}
	
	@Override
	public void keyMessage(KeyEvent e) {
		if (waitingForInput){
			waitingForInput = false;
			processEventQueue();
		}
		
	}

	public JFrame getFrame() {
		// TODO Auto-generated method stub
		return frame;
	}
	
	public void dispose(){
		frame.dispose();
	}

	public void setOwner(ViewController viewController) {
		this.owner = viewController;
	}
	
	

}
