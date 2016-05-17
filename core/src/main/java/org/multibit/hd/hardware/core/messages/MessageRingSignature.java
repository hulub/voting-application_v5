package org.multibit.hd.hardware.core.messages;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.google.protobuf.ByteString;

public class MessageRingSignature implements HardwareWalletMessage {

	private final byte[] c;
	private final byte[][] s;
	private final int n;
	private final byte[] ytx, yty;

	public MessageRingSignature(byte[] c, List<ByteString> s, int n, byte[] ytx, byte[] yty) {

		this.c = c;
		this.n = n;
		this.s = new byte[n][32];
		int i = 0;
		for (ByteString bs : s) {
			this.s[i] = bs.toByteArray();
			i++;
		}
		this.ytx = ytx;
		this.yty = yty;
	}

	/**
	 * @return The signature
	 */
	public byte[] getC() {
		return Arrays.copyOf(c, c.length);
	}
	
	public byte[][] getS() {
		byte[][] copy = new byte[s.length][32];
		for (int i=0;i<n;i++) {
			copy[i] = Arrays.copyOf(s[i], s[i].length);
		}
		return copy;
	}
	
	public int getN() {
		return n;
	}
	
	public byte[] getYtx() {
		return Arrays.copyOf(ytx, ytx.length);
	}
	
	public byte[] getYty() {
		return Arrays.copyOf(yty, yty.length);
	}

	@Override
	public String toString() {
		ToStringBuilder builder = new ToStringBuilder(this);
		builder.append("c", c);
		builder.append("s - "+s.length+" elements:");
		for (byte[] ba : s) {
			builder.append("bytes", ba);
		}
		builder.append("ytx", ytx);
		builder.append("yty", yty);
		return builder.toString();
	}
}
