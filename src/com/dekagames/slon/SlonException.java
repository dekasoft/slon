package com.dekagames.slon;

/**
 *
 * @author Deka
 */
public class SlonException extends Exception{
    private String message;
    
    public SlonException(){
	    message = "";
    }
    
    public SlonException(String msg){
	    message = msg;
    }
    
    public String toString(){
	    return "SlonException - ("+message+")";
    }
    
    
}
