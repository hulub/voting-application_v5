package Model;

import java.io.Serializable;
import java.util.List;

public class ElectionParameters implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public List<byte[]> L;
	public String question;
	public List<String> M;
	public int pi;
	
	public ElectionParameters(List<byte[]> L, String question, List<String> M, int pi) {
		this.L = L;
		this.question = question;
		this.M = M;
		this.pi = pi;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(question + "\n");
		sb.append("Candidates : " + "\n");
		for (String candidate : M) {
			sb.append(candidate + "\n");
		}
		
		return sb.toString();
	}
}
