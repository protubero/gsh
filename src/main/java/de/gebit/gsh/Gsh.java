package de.gebit.gsh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.fusesource.jansi.AnsiConsole;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.jansi.win.JansiWinSysTerminal;

import com.github.lalyos.jfiglet.FigletFont;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

/**
 * Main class of GSH
 * 
 * @author MSchaefer
 *
 */
public class Gsh {

    // application name
    public static String appName = "gsh";

    public static Map<String, String> dynargs = new HashMap<>();

    private static GroovyScriptEngine gse;
    private static CompilerConfiguration configuration = new CompilerConfiguration();

    // script folders, usually exactly two, the gsh commands folder and the app src folder
    private static List<File> roots = new ArrayList<>();

    public static void exitWithError(String message) {
	throw new ScriptExitException(message);
    }

    public static void exitWithoutError() {
	throw new ScriptExitException();
    }

    public static void main(String[] mainArgs) throws Exception {
	try {
	    new Gsh().execute(mainArgs);
	} catch (ScriptExitException see) {
	    if (see.getMessage() != null) {
		System.err.println(see.getMessage());
	    }
	    System.exit(1);
	}
    }

    private static void execute(String[] mainArgs) throws IOException, ResourceException, ScriptException {
	AnsiConsole.systemInstall();

	String tmpAppName = System.getenv("APP_NAME");
	if (tmpAppName != null) {
	    appName = tmpAppName;
	}

	// environment
	final String appHome = System.getenv("APP_HOME"); // APP_HOME is set in the storck start script
	String scriptPath = System.getenv("GSH_SCRIPTPATH");

	if (appHome == null) {
	    exitWithError("APP_HOME not set");
	}

	File appHomeDir = new File(appHome);
	File commandsDir = new File(appHomeDir, "commands");
	if (!commandsDir.exists() || !commandsDir.isDirectory()) {
	    exitWithError("APP_HOME/commands invalid: " + commandsDir.getAbsolutePath());
	}

	// Roots
	roots.add(commandsDir);
	if (scriptPath != null) {
	    for (String scpath : scriptPath.split(";")) {
		File scPathFile = new File(scpath);
		if (scPathFile.exists() && scPathFile.isDirectory()) {
		    roots.add(scPathFile);
		} else {
		    exitWithError("invalid path: " + scpath);
		}
	    }
	}

	// init groovy script engine
	reloadScripts();

	if (mainArgs.length == 1 && mainArgs[0].equalsIgnoreCase("terminal")) {
	    List<Candidate> scriptCandidates = new ArrayList<>();
	    for (File tempScriptsDir : roots) {
		File[] scriptFiles = tempScriptsDir.listFiles(new FilenameFilter() {

		    @Override
		    public boolean accept(File dir, String name) {
			return name.toLowerCase().endsWith(".groovy") && !Character.isUpperCase(name.charAt(0));
		    }
		});

		for (File sc : scriptFiles) {
		    String tempFilename = sc.getName().toLowerCase();
		    scriptCandidates.add(new Candidate(tempFilename.substring(0, tempFilename.length() - 7)));
		}
	    }

	    String asciiArt1 = FigletFont.convertOneLine(appName + " terminal");
	    AnsiConsole.out.println(asciiArt1);
	    AnsiConsole.out.println("Leave terminal with 'exit' or Ctrl-D, display help page with 'help'");
	    AnsiConsole.out.println("Execute script with <script> [options], use <script> -? to show script usage info");

	    Terminal terminal = new JansiWinSysTerminal(appName.toUpperCase() + " Terminal", true);

	    LineReaderBuilder lrBuilder = LineReaderBuilder.builder().appName(appName).terminal(terminal).completer(new Completer() {

		@Override
		public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		    // plump
		    scriptCandidates.stream().filter(c -> c.value().startsWith(line.line())).forEach(c -> candidates.add(c));
		}

	    });

	    // find user home dir
	    File historyFile = null;
	    String homeDir = System.getProperty("user.home");
	    if (homeDir != null) {
		File homeDirFile = new File(homeDir);
		if (homeDirFile.exists() && homeDirFile.isDirectory()) {
		    // does gsh dir already exist?
		    File gshDir = new File(homeDirFile, ".gsh" + (appName.equals("gsh") ? "" : "-" + appName));
		    if (!gshDir.exists()) {
			if (!gshDir.mkdir()) {
			    System.err.println("error creating dir " + gshDir);
			} else {
			    historyFile = new File(gshDir, "history");
			}
		    } else {
			historyFile = new File(gshDir, "history");
		    }
		}
	    }

	    if (historyFile != null) {
		lrBuilder.variable(LineReader.HISTORY_FILE, historyFile);
		lrBuilder.variable(LineReader.HISTORY_IGNORE, "\\:.*");
	    }

	    LineReader reader = lrBuilder.build();

	    String prompt = appName + ">";
	    dynargs = new HashMap<>();

	    while (true) {
		String line = null;
		try {
		    line = reader.readLine(prompt).trim();
		    if (line.startsWith("#")) {
			// experimental execution of system shell commands (windows only)
			String[] args = line.trim().substring(1).split("\\s+");
			executeShellCommand(args);
		    } else if (!line.isEmpty()) {
			List<String> allArgs = new ShellSplitter().shellSplit(line);
			List<String[]> pipeArgsList = new ArrayList<>();
			int lastIdx = 0;
			for (int i = 0; i < allArgs.size(); i++) {
			    if (allArgs.get(i).equals("\\|")) {
				pipeArgsList.add(allArgs.subList(lastIdx, i).toArray(new String[i - lastIdx]));
				lastIdx = i + 1;
			    }
			}
			if (lastIdx < allArgs.size()) {
			    pipeArgsList.add(allArgs.subList(lastIdx, allArgs.size()).toArray(new String[allArgs.size() - lastIdx]));
			}

			GshScript firstScript = null;
			GshScript lastScript = null;
			for (String[] args : pipeArgsList) {
			    GshScript script = createScript(args);
			    if (firstScript == null) {
				firstScript = script;
				lastScript = script;
			    } else {
				lastScript.setNext(script);
				lastScript = script;
			    }
			}

			firstScript.run();
		    }
		} catch (UserInterruptException e) {
		    // Ignore
		} catch (EndOfFileException e) {
		    return;
		} catch (TerminalExitException tee) {
		    return;
		} catch (ScriptExitException see) {
		    if (see.getMessage() != null) {
			System.err.println(see.getMessage());
		    }
		} catch (Throwable e) {
		    e.printStackTrace();
		    // Ignore
		}
	    }
	} else {
	    GshScript gsc = createScript(mainArgs);
	    gsc.run();
	}

    }

    public static void executeShellCommand(String[] args) {
	try {
	    List<String> cmd = new ArrayList<>();
	    cmd.add("powershell.exe");
	    cmd.add("/c");
	    cmd.addAll(Arrays.asList(args));
	    ProcessBuilder builder = new ProcessBuilder(cmd);
	    builder.redirectErrorStream(true);
	    Process p = builder.start();
	    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String line;
	    while (true) {
		line = r.readLine();
		if (line == null) {
		    break;
		}
		AnsiConsole.out.println(line);
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public static GshScript createScript(String[] args) throws ResourceException, ScriptException, IOException {
	// if no arguments at all, display help message and list all available commands
	if (args.length == 0) {
	    displayUsageMsg();
	    Gsh.exitWithoutError();
	}

	String scriptName = args[0];
	String scriptFileName = scriptName + ".groovy";
	File scriptFile = findFile(scriptFileName);
	if (scriptFile == null) {
	    Gsh.exitWithError("Script not found: " + scriptName);
	}

	// arguments to be processed by the script, i.e. all args but the first target and script param
	String[] scriptArgs = new String[args.length - 1];
	if (scriptArgs.length > 0) {
	    System.arraycopy(args, 1, scriptArgs, 0, scriptArgs.length - 0);
	}

	// prepare script execution
	Binding binding = new Binding();

	// create script
	GshScript script = (GshScript) gse.createScript(scriptFileName, binding);

	// configure script
	script.setBinding(binding);
	script.setScriptName(scriptName);
	script.setScriptArgs(scriptArgs);
	script.setScriptFile(scriptFile);

	script.prepare();

	// execute main script
	return script;
    }

    public static File findFile(String scriptNameCandidate) {
	File scriptFileCandidate;
	for (File tScriptDir : roots) {
	    scriptFileCandidate = new File(tScriptDir, scriptNameCandidate);
	    if (scriptFileCandidate.exists()) {
		return scriptFileCandidate;
	    }
	}

	return null;
    }

    /**
     * Display usage message of the tool on the console, including a list of available scripts
     * 
     * @throws IOException
     */
    public static void displayUsageMsg() throws IOException {
	System.out.println();
	System.out.println("Usage:	" + appName + " <script> [options]");
	System.out.println("  == open terminal with '" + appName + " terminal' ==");
	System.out.println();
	System.out.println("  Common options");
	System.out.println("    -h");
	System.out.println("      Show script-specific options");
	System.out.println("    --verbose");
	System.out.println("      Verbose output");
	System.out.println("    -D<param>");
	System.out.println("      Dynamic parameters");

	System.out.println();
	listScripts(ListType.Scripts);
    }

    /**
     * List either the commands or the scripts as a table with name and description column on the console
     * 
     * @param listType
     * @throws IOException
     */
    public static void listScripts(ListType listType) throws IOException {
	TableWriter tw = new TableWriter();
	tw.column().header(listType == ListType.Commands ? "Commands" : "Script");
	tw.column().header("Description");
	FilenameFilter filter = new FilenameFilter() {
	    @Override
	    public boolean accept(File dir, String name) {
		return name.toLowerCase().endsWith(".groovy") && !Character.isUpperCase(name.charAt(0));
	    }
	};
	switch (listType) {
	case Commands:
	    File[] commandFiles = roots.get(0).listFiles(filter);
	    listScripts(commandFiles, tw);
	    break;
	case Scripts:
	    for (File scriptsDir : roots.subList(1, roots.size())) {
		File[] scriptFiles = scriptsDir.listFiles(filter);
		listScripts(scriptFiles, tw);
	    }
	    break;
	default:
	    throw new RuntimeException("invalid list type");
	}
	tw.flush();
	System.out.println();
    }

    public static void listScripts(File[] scriptFiles, TableWriter tw) throws IOException {
	for (File scriptFile : scriptFiles) {
	    String filename = scriptFile.getName().toLowerCase();
	    GshScriptMeta meta = extractScriptInfo(scriptFile);

	    String desc = meta.getShortDescription();
	    if (meta.getLongDescription() != null) {
		desc = desc + System.lineSeparator() + System.lineSeparator() + meta.getLongDescription();
	    }

	    tw.row(filename.substring(0, filename.length() - 7), desc);
	}

    }

    /**
     * Extract short and long description texts by parsing a script file
     * 
     * @param scriptFile
     * @return
     */
    public static GshScriptMeta extractScriptInfo(File scriptFile) {
	List<String> lines;
	try {
	    lines = FileUtils.readLines(scriptFile, "UTF-8");
	    String shortDescription = null;
	    String longDescription = null;
	    if (lines.size() > 0) {
		for (String line : lines) {
		    if (line.startsWith("//!")) {
			if (shortDescription == null) {
			    shortDescription = line.substring(3).trim();
			} else {
			    if (longDescription == null) {
				longDescription = "";
			    }
			    longDescription = longDescription + line.substring(3).trim();
			}
		    }
		}
	    }

	    return new GshScriptMeta(shortDescription, longDescription);
	} catch (IOException e) {
	    throw new ScriptExitException("## error reading script file: " + e.toString());
	}
    }

    /**
     * Create and re-create GroovyScriptEngine from scratch with given configuration of script paths This is used by the reload command
     */
    public static void reloadScripts() {
	gse = new GroovyScriptEngine(roots.stream().map(file -> {
	    try {
		return file.toURI().toURL();
	    } catch (MalformedURLException e) {
		throw new RuntimeException(e);
	    }
	}).toArray(URL[]::new));

	configuration.setScriptBaseClass(GshScript.class.getName());
	gse.setConfig(configuration);
    }
}
