package Model;

import java.io.Serializable;

public class MyPublicKey implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public byte[] bytes;
	
	public MyPublicKey(byte[] bytes) {
		this.bytes=bytes;
	}
}
