package bayesGame.bayesbayes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.fraction.Fraction;

public class OptionNode extends BayesNode {

	private List<OptionNodeOption> options;
	private List<String> skills;
	
	public OptionNode(Object type) {
		super(type);
		nodeInit();
	}

	public OptionNode(Object type, Object[] scope) {
		super(type, scope);
		nodeInit();
	}

	public OptionNode(Object type, Object[] scope,
			HashMap<Object, Integer> strides, Fraction[] cpt) {
		super(type, scope, strides, cpt);
		nodeInit();
	}
	
	private void nodeInit(){
		options = new ArrayList<OptionNodeOption>();
		skills = new ArrayList<String>();
	}

	public void addOption(OptionNodeOption body) {
		options.add(body);
	}

	public List<OptionNodeOption> getOptions() {
		return options;
	}
	
	public void addSkill(String... skill){
		for (String string : skill){
			skills.add(string);
		}
	}
	
	public List<String> getSkills(){
		return skills;
	}
	
	
	

}
