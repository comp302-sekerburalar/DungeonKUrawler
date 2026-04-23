package com.kurawler.model;

public class Item {
	private String name;
    private String type; // "KEY", "STATIC"
    
    public Item(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { 
    	return name; 
    	}
    public String getType() { 
    	return type; 
    	}
}