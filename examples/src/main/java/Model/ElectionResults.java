package Model;

import java.io.Serializable;
import java.util.List;

public class ElectionResults implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public List<VoteItem> votes;
	
	public ElectionResults (List<VoteItem> votes) {
		this.votes=votes;
	}
}
