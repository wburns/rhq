package org.rhq.plugins.apache.augeas;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.augeas.Augeas;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeBuilder;
import org.rhq.augeas.util.Glob;
import org.rhq.rhqtransform.AugeasRhqException;


public class AugeasTreeBuilderApache implements AugeasTreeBuilder {

    private Map<String, List<File>> includes;
    private Map<AugeasNode, List<String>> incl;
    private String[] nestedIncludesDirective = {"<VirtualHost","<Directory"};
    private Augeas ag;

    public AugeasTreeBuilderApache() {
        includes = new HashMap<String, List<File>>();
        incl = new HashMap<AugeasNode, List<String>>();
    }

    public AugeasTree buildTree(AugeasProxy component, AugeasConfiguration config, String name, boolean lazy)
        throws AugeasRhqException {

        this.ag = component.getAugeas();

        AugeasModuleConfig module = config.getModuleByName(name);
        ApacheAugeasTree tree = new ApacheAugeasTree(component.getAugeas(), module);

        List<String> incld = module.getConfigFiles();

        if (incld.isEmpty())
            throw new AugeasRhqException("No configuration provided.");

        String rootPath = incld.get(0);

        AugeasNode rootNode = new ApacheAugeasNode(ApacheAugeasTree.AUGEAS_DATA_PATH + rootPath, tree);
        AugeasConfigurationApache apacheConfig = (AugeasConfigurationApache) config;
        tree.setRootNode(rootNode);
        File rootFile = new File(apacheConfig.getServerRootPath());
        // we need to know which files are related to each glob
        
        for (String inclName : module.getIncludedGlobs()) {
            
        	List<File> files = new ArrayList<File> ();
            
        	if (inclName.indexOf(File.separatorChar)==0){
         	   files.add(new File(inclName));
            }else
         	   files.addAll(Glob.match(rootFile, inclName));
            
            if (module.getExcludedGlobs() != null)
                Glob.excludeAll(files, module.getExcludedGlobs());
            
            if (!includes.containsKey(inclName))
                includes.put(inclName, files);
        }

        updateIncludes((ApacheAugeasNode) rootNode, tree, rootPath,false);
        
        //List<String> rootconf = new ArrayList<String>();
       // rootconf.add(ApacheAugeasTree.AUGEAS_DATA_PATH + rootPath);
      //  this.incl.put(rootNode, rootconf);
        
        tree.setIncludes(this.incl);
        return tree;
    }
    
    public void updateIncludes(ApacheAugeasNode parentNode, AugeasTree tree, String fileName,boolean update) throws AugeasRhqException {

	  	  List<String> nestedNodes = ag.match(ApacheAugeasTree.AUGEAS_DATA_PATH+fileName+File.separator+"*");
	  	  
	  	  List<AugeasNode> createdNodes = new ArrayList<AugeasNode>();
	  	  
	  	  for (String nodeName : nestedNodes){
	  		      ApacheAugeasNode newNode = (ApacheAugeasNode)tree.createNode(nodeName);
	  		      newNode.setParentNode(parentNode);
	  			  //ApacheAugeasNode newNode = new ApacheAugeasNode(parentNode,tree,nodeName);
	  			  createdNodes.add(newNode);
	  		  }
	  	  
	  	if (update)	  
	  		parentNode.addIncludeNodes(createdNodes);
	  	   	
      for (AugeasNode node : createdNodes){
    	    if (isNested(node.getLabel())){
    	    	String labelName = node.getLabel()+((node.getSeq()!=0)? "["+String.valueOf(node.getSeq())+"]":"");
    	    	updateIncludes((ApacheAugeasNode)node,tree,fileName+File.separator+labelName,false);
    	    }
            if (node.getLabel().equals("Include")){
            	String val = ag.get(node.getFullPath()+File.separator+"param");
	        	if (includes.containsKey(val))
	        	{
	        		//include directive contains globNames
	        	  	List<File> files = includes.get(val);
	        	  	List<String> names = new ArrayList<String>();
	        	  	for (File file : files)
	        	  	 {
	        	      names.add(ApacheAugeasTree.AUGEAS_DATA_PATH+file.getAbsolutePath());
	        	  	  updateIncludes((ApacheAugeasNode)node.getParentNode(),tree,file.getAbsolutePath(),true);
	        	  	 }
	        	  	if (incl.containsKey(node.getParentNode()))
	        	  	{
	        	  		List<String> list = incl.get(node.getParentNode());
	        	  		list.addAll(names);	        	  	
	        	    }  else
	        	    	incl.put(node.getParentNode(), names);
                 }
	        	}   
         }
    }
    
    private boolean isNested(String name){
    	for (String directive : nestedIncludesDirective)
    	{
    		if (directive.equals(name))
    			return true;
    	}
    	return false;
    }
}
