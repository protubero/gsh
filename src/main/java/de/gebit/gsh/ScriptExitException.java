package de.gebit.gsh;

/**
 * Halt script execution by throwing this exception.
 * 
 * @author MSchaefer
 *
 */
public class ScriptExitException extends RuntimeException {


    /**
     * 
     */
    private static final long serialVersionUID = 1639983458384275977L;

    public ScriptExitException(String message) {
	super(message);
    }
    
    public ScriptExitException() {
    }
    
}
