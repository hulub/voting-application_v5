package Model;

import java.io.Serializable;

public class Failure implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String text;
	
	public Failure(String text) {
		this.text = text;
	}
}