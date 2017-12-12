package de.gebit.gsh;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple meta information about the objects that are transferred over the pipe: Which fields do they have?
 * Work in progress ...
 * 
 * @author MSchaefer
 *
 */
public class MetaDescription {

    private List<Field> fields = new ArrayList<>();
    
    public void addField(Field field) {
	fields.add(field);
    }

    public List<Field> getFields() {
        return fields;
    }
    
    
}
