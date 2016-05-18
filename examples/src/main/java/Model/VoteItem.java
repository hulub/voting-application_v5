package Model;

import java.io.Serializable;

public class VoteItem implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public String message;
	public byte[] Yt;
	
	public VoteItem (String message, byte[] Yt) {
		this.message=message;
		this.Yt = Yt;
	}

	public boolean isLinked(byte[] ytilda) {
		return Yt.equals(ytilda);
	}
}
