package de.gebit.gsh;

/**
 * To be used by the receiving end of a pipe connection to extract data from the transferred objects. 
 * 
 * @author MSchaefer
 *
 */
public interface Field {

    String getTitle();
    
    Class<?> getType();
    
    Object value(Object obj);
    
}
