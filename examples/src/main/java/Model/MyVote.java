package Model;

import java.io.Serializable;

public class MyVote implements Serializable{

	private static final long serialVersionUID = 1L;
	
	public byte[] c;
	public byte[][] s;
	public int n;
	public byte[] ytx, yty;
	
	public MyVote(byte[] c, byte[][] s, int n, byte[] ytx, byte[] yty) {

		this.c = c;
		this.n = n;
		this.s = s;
		this.ytx = ytx;
		this.yty = yty;
	}
	
	public byte[] getYtilda () {
		byte[] ytbytes = new byte[65];
		ytbytes[0] = 0x04;
		System.arraycopy(ytx, 0, ytbytes, 1, 32);
		System.arraycopy(yty, 0, ytbytes, 33, 32);
		return ytbytes;
	}
}
