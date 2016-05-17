package org.multibit.hd.hardware.core.messages;

import java.util.Arrays;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class PublicKey65 implements HardwareWalletMessage {

	private final byte[] publicKey;

	public PublicKey65(byte[] bytes) {
	    this.publicKey = Arrays.copyOf(bytes, bytes.length);
	  }
	/**
	 * @return The signature
	 */
	public byte[] getPublicKey() {
		return Arrays.copyOf(publicKey, publicKey.length);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("public key", publicKey).toString();
	}
}
