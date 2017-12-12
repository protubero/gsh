package de.gebit.gsh;

/**
 * Meta information of a script
 * 
 * @author MSchaefer
 */
public class GshScriptMeta {
    
    // one liner 
    private String shortDescription;
    
    // more information
    private String longDescription;
    
    public GshScriptMeta(String shortDescription, String longDescription) {
	this.shortDescription = shortDescription;
	this.longDescription = longDescription;
    }
    
    public String getShortDescription() {
        return shortDescription;
    }
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }
    public String getLongDescription() {
        return longDescription;
    }
    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

}
