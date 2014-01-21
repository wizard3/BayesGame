package bayesGame.ui;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.math3.util.Pair;

import bayesGame.bayesbayes.BayesNet;
import bayesGame.bayesbayes.BayesNode;
import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.AbstractGraph;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

/**
 * @author Kaj Sotala
 * 
 * The default interface for the game. 
 *
 */
public class DefaultInterfaceView {

	private JFrame frame;
	private JPanel graphPanel;
	private JPanel infoPanel;
	private JTextPane textPane;
	private JScrollPane scroll;
	
	private Map<Map<Object, Boolean>,JLabel> visualizations;
	
	private AbstractGraph graph;
	
	public static final int graphTypeBayesGraph = 0;
	
	public DefaultInterfaceView() throws IOException {
		
		frame = new JFrame("Academy Game");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		addComponentsToPane(frame.getContentPane());
		
		frame.pack();
		frame.setVisible(true);
				
	}
	
	private void addComponentsToPane(Container pane) throws IOException {
		
		GridBagConstraints c;
		
		pane.setLayout(new GridBagLayout());
	    
		c = new GridBagConstraints();
		
	    graphPanel = new JPanel();
	    graphPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1;
	    c.weighty = 1;
	    c.ipady = 0;
	    c.ipadx = 0;
	    c.fill = GridBagConstraints.BOTH;
	    pane.add(graphPanel, c);
	    
	    c = new GridBagConstraints();
	    
	    infoPanel = new JPanel();
	    infoPanel.setBorder(BorderFactory.createLineBorder(Color.black));
	    
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1;
	    c.weighty = 1;
	    c.ipady = 100;
	    c.ipadx = 100;
	    c.fill = GridBagConstraints.BOTH;
	    pane.add(infoPanel, c);
	    
	    c = new GridBagConstraints();
	    
	    textPane = new JTextPane();
	    textPane.setEditable(false);
	    textPane.setPreferredSize(new Dimension(400,200));
	    
	    scroll = new JScrollPane (textPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	    frame.add(scroll);
	    
	    c.gridx = 0;
	    c.gridy = 1;
	    c.gridwidth = 2;
	    c.ipady = 0;
	    c.weightx = 1;
	    c.weighty = 1;
	    c.fill = GridBagConstraints.BOTH;
	    pane.add(textPane, c);
		
	}
	
	public void setVisible(boolean visible){
		
		frame.setVisible(visible);
		
	}
	
	public void addVisualization(Map<Object,Boolean> item){
		if (visualizations == null){
			visualizations = new HashMap<Map<Object,Boolean>,JLabel>();
			setupVisualizationPane();
		}
		addVisualizationToPane(item, true);
	}
	
	public boolean setVisualizationTruth(Map<Object,Boolean> item, boolean truth){
		if (!visualizations.containsKey(item)){
			return false;
		}
		addVisualizationToPane(item, truth);
		return true;
	}
	
	private void addVisualizationToPane(Map<Object,Boolean> item, boolean itemTruth){
		JLabel visualization;
		boolean editingOldVisualization = visualizations.containsKey(item);
		
		if (editingOldVisualization){
			visualization = visualizations.get(item);
		} else {
			visualization = new JLabel();
		}
		
		Set<Entry<Object,Boolean>> entrySet = item.entrySet();
		String html = "<html>";
		if (!itemTruth){
			html = html + "<strike><font color=black>";
		}
		for (Entry<Object,Boolean> e : entrySet){
			Boolean truth = e.getValue();
			String objectString = e.getKey().toString();
			char objectChar = objectString.charAt(0);
			if (truth && itemTruth){
				html = html + "<font color=green>" + objectChar + " </font>";
			} else if (!truth && itemTruth) {
				html = html + "<font color=red>" + objectChar + " </font>";
			} else if (!itemTruth){
				html = html + objectChar + " ";
			}
		}
		if (!itemTruth){
			html = html + "</strike>";
		}
		html = html + "</html>";
		visualization.setText(html);
		visualization.setAlignmentX(Component.CENTER_ALIGNMENT);
		visualization.setFont(new Font("Serif", Font.BOLD, 32));
		
		if (!editingOldVisualization){
			
			infoPanel.add(visualization);
		}
		
		infoPanel.add(Box.createVerticalGlue());
		visualizations.put(item, visualization);
	}
	
	public void addText(String text){
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setFontSize(style, 16);
		addText(text, style); 
	}
	
	public void addTutorialText(String text){
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setFontSize(style, 18);
		StyleConstants.setBold(style, true);
		addText(text, style);
	}
	
	private void addText(String text, SimpleAttributeSet style){
		text = text + System.getProperty("line.separator");
		
		StyledDocument doc = textPane.getStyledDocument();
		
		try { doc.insertString(doc.getLength(), text, style); }
        catch (BadLocationException e){}
		
		frame.pack();
		
		textPane.setCaretPosition(textPane.getDocument().getLength());
		scroll.revalidate();
		
		
	}
	
	public void clearInfoPanel(){
		infoPanel.removeAll();
		visualizations = new HashMap<Map<Object,Boolean>,JLabel>();
	}
	
	private void setupVisualizationPane(){
		infoPanel.removeAll();
		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));
		infoPanel.add(Box.createVerticalGlue());
		
	}
	
	public void setGraph(BayesNet net){
		this.graph = net.getGraph();
	}
	
	public void displayGraph(int graphType){
		if (graph != null){
			switch(graphType){
			case(graphTypeBayesGraph):
				displayBayesGraph();			
			}	
		}
	}
	
	private void displayBayesGraph(){
		Layout<BayesNode, Pair<Integer,Integer>> layout = new DAGLayout<BayesNode, Pair<Integer, Integer>>(graph);
        layout.setSize(new Dimension(400,400));
        
        VisualizationViewer<BayesNode, Pair<Integer,Integer>> vv = new VisualizationViewer<BayesNode, Pair<Integer,Integer>>(layout);
        
        Transformer<BayesNode,Paint> vertexPaint = new Transformer<BayesNode,Paint>() {
        	public Paint transform(BayesNode i) {
        		if (i.isObserved()){
        			return Color.BLUE;
        		} else {
        			return Color.WHITE;
        		}
        	}
        	}; 
        	
        vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller()); 

        vv.setPreferredSize(new Dimension(500,500)); //Sets the viewing area size
        
        vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).rotate(-Math.PI, 200, 200);
        
        graphPanel.add(vv);
        frame.pack();
        graphPanel.setVisible(true);
	}
	

}
