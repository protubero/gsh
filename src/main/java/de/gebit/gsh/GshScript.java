package de.gebit.gsh;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.JCommander.Builder;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import groovy.lang.Script;

/**
 * Parent class of all GSH scripts. 
 * 
 * @author MSchaefer
 *
 */
public abstract class GshScript extends Script implements Output {

    public static class Args {
	@Parameter(names = { "-h", "--help", "-?" }, description = "Show usage on console", help = true, hidden=true)
	public boolean help;

	@Parameter(names = { "--verbose" }, description = "Verbose output", hidden = true)
	public boolean verbose;

	@DynamicParameter(names = "-D", description = "Dynamic parameters go here", hidden = true)
	public Map<String, String> dynargs = new HashMap<>();
    }

    protected String scriptArgs[];
    protected String target;
    protected String scriptName;
    protected File scriptFile;
    protected Map<String, String> dynargs = Gsh.dynargs;

    protected Output input;

    private GshScript next;

    protected JCommander jCommander;
    private MetaDescription sendDesc;
    private MetaDescription metaDesc;

    public GshScript() {
    }

    public String[] getScriptArgs() {
	return scriptArgs;
    }

    public void setScriptArgs(String[] scriptArgs) {
	this.scriptArgs = scriptArgs;
    }

    public String getScriptName() {
	return scriptName;
    }

    public void setScriptName(String scriptName) {
	this.scriptName = scriptName;
    }
    
    @Override
    public Object run() {
	try {
	    Object result = doRun();
	    endPipe();
	    return result;
	} catch (ScriptExitException see) {
	    throw see;
	} catch (TerminalExitException tee) {
	    throw tee;
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new ScriptExitException(e.getMessage());
	}
    }

    
    void prepare() {
	Builder jCommanderSpec = JCommander.newBuilder().addObject(this);

	Args args = new Args();
	jCommanderSpec.addObject(args);
	jCommander = jCommanderSpec.build();
	jCommander.setProgramName(Gsh.appName + " " + scriptName);
	jCommander.setAllowParameterOverwriting(true);

	boolean exitWithUsage = false;
	try {
	    jCommander.parse(scriptArgs);
	} catch (ParameterException ex) {
	    System.err.println(ex.getMessage());
	    exitWithUsage = true;
	}

	if (args.help || exitWithUsage) {
	    GshScriptMeta meta = Gsh.extractScriptInfo(scriptFile);
	    
	    System.out.println();
	    if (meta.getShortDescription() != null) {
		System.out.println(meta.getShortDescription());
    	    	System.out.println();
	    }	
	    
	    StringBuilder sb = new StringBuilder();
	    jCommander.usage(sb, "  ");
	    System.out.println(sb.toString());
	    
	    if (meta.getLongDescription() != null) {
		System.out.println(meta.getLongDescription());
	    }	
	    Gsh.exitWithoutError();
	}
    }

    public abstract Object doRun();

    public File getScriptFile() {
        return scriptFile;
    }

    public void setScriptFile(File scriptFile) {
        this.scriptFile = scriptFile;
    }

    public GshScript getNext() {
        return next;
    }

    public void setNext(GshScript next) {
        this.next = next;
    }

    /**
     * PIPE communication
     */
    @Override
    public final void sendMeta(MetaDescription metaDesc) {
	if (next != null) {
	    next.receiveMetaDescription(metaDesc);
	} else {
	    this.sendDesc = metaDesc;
	    metaDesc.getFields().forEach(f -> this.writer.column().header(f.getTitle()));
	}
    }

    private final void receiveMetaDescription(MetaDescription metaDesc) {
	this.metaDesc = metaDesc;
	onMeta();
    }

    private TableWriter writer = new TableWriter();
    
    @Override
    public void sendObject(Object obj) {
	if (next != null) {
	    next.receive(obj);
	} else {
	    List<Object> objects = new ArrayList<>();
	    if (metaDesc != null) {
		metaDesc.getFields().forEach(f -> objects.add(f.value(obj)));
		writer.row(objects);
	    } else {
		System.out.println("no receiver for object " + String.valueOf(obj));
	    }
	}
    }

    @Override
    public final void commitObjects() {
	commit();
	if (next != null) {
	    next.commit();
	} else {
	    writer.flush();
	    this.writer = new TableWriter();
	}
    }

    public final void endPipe() {
	onEndPipe();
	if (next != null) {
	    next.endPipe();
	}
    }

    
    /**
     * override in the script
     */
    protected void onMeta() {
	
    }
    
    protected void receive(Object obj) {
	Gsh.exitWithError("receive not overridden");
    }

    protected void commit() {
	
    }
    
    /**
     * override in the script
     */
    protected void onEndPipe() {
	
    }


}
