package de.gebit.gsh;

/**
 * Specifies the 'pipe protocol', i.e. how to send data to the next script in the pipe.
 * 
 * @author MSchaefer
 */
public interface Output {

    /**
     * Say something about the objects, that will be send next via the sendObject method. 
     * 
     * @param table
     */
    void sendMeta(MetaDescription table);

    /**
     * Send any object to the next script in the pipe
     * 
     * @param obj
     */
    void sendObject(Object obj);
    
    /**
     * Signals to the receiver that sending is over wrt. the meta data given beforehand. 
     */
    void commitObjects();
        
}
