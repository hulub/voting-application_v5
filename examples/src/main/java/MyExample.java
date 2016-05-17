import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.wallet.KeyChain.KeyPurpose;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.HDNodeType;
import org.multibit.hd.hardware.core.messages.MessageRingSignature;
import org.multibit.hd.hardware.core.messages.MessageSignature;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.messages.PublicKey;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Base64;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Sign a message
 * </p>
 * <p>
 * Requires Trezor V1 production device plugged into a USB HID interface.
 * </p>
 * <p>
 * This example demonstrates the sequence to Bitcoin sign a message.
 * </p>
 *
 * @since 0.0.1  
 */
public class MyExample {

	private static final Logger log = LoggerFactory.getLogger(MyExample.class);

	private HardwareWalletService hardwareWalletService;
	private String message;
	private List<byte[]> L;
	private int n, pi;
	private MessageRingSignature vote;

	/**
	 * <p>
	 * Main entry point to the example
	 * </p>
	 *
	 * @param args
	 *            None required
	 *
	 * @throws Exception
	 *             If something goes wrong
	 */
	public static void main(String[] args) throws Exception {

		// All the work is done in the class
		MyExample example = new MyExample();

		example.executeExample();

		// Simulate the main thread continuing with other unrelated work
		// We don't terminate main since we're using safe executors
		Uninterruptibles.sleepUninterruptibly(30, TimeUnit.MINUTES);

	}

	/**
	 * Execute the example
	 */
	public void executeExample() {

		// initialize parameters
		L = new ArrayList<byte[]>();
		byte[] ElTrezorPublicKey65Bytes = new byte[] { 4, -61, 127, -43, -89, 84, -54, -6, 99, -15, 98, 23, 36, 123,
				-12, -66, 84, -31, -54, -59, 3, -67, 55, 100, -65, -121, -91, 1, 37, -120, -125, -99, -32, 55, 51, 96,
				-95, -43, -51, 2, -21, 104, -8, 38, -25, -32, 20, -100, 110, -50, 126, -15, -19, -84, -100, -108, -16,
				51, 109, 67, 53, 22, 117, -117, 33 };
		byte[] ElBandidoPublicKey65Bytes = new byte[] { 4, 57, 116, 59, -118, -35, 98, -90, -31, 93, 81, 3, -112, 58,
				-66, 9, -104, -112, -102, 6, -30, 99, 116, -115, -111, 86, -68, -100, 23, -77, -105, 29, -32, 122, -63,
				-70, 106, 34, 65, 111, -80, -33, 33, 11, -55, 123, 74, 41, 73, -88, 105, -98, -57, 42, -87, 69, 7, -63,
				-73, 15, -83, 117, 46, -53, 79 };
		byte[] FakePublicKey65Bytes = new byte[] { 4, -45, -70, -18, -2, 38, 26, 69, -123, -19, 94, -122, 0, 84, -68,
				56, 115, -3, 103, -91, -22, 58, 117, -84, -1, 79, 54, 18, -64, -42, -98, -41, 93, 122, -97, -50, 96,
				-61, 29, 120, -70, -59, -37, 37, -36, 62, 2, -8, -71, -122, 56, 75, 123, 91, -33, -43, 26, 99, -56, -39,
				56, 33, -17, 100, 46 };

		L.add(ElTrezorPublicKey65Bytes);
		L.add(FakePublicKey65Bytes);
		L.add(ElBandidoPublicKey65Bytes);
		message = "Hello World!";
		pi = 1;
		n = L.size();
		
		startCommunication();
	}

	public void startCommunication() {
		// Use factory to statically bind the specific hardware wallet
		TrezorV1HidHardwareWallet wallet = HardwareWallets.newUsbInstance(TrezorV1HidHardwareWallet.class,
				Optional.<Integer> absent(), Optional.<Integer> absent(), Optional.<String> absent());

		// Wrap the hardware wallet in a suitable client to simplify message API
		HardwareWalletClient client = new TrezorHardwareWalletClient(wallet);

		// Wrap the client in a service for high level API suitable for
		// downstream applications
		hardwareWalletService = new HardwareWalletService(client);

		// Register for the high level hardware wallet events
		HardwareWalletEvents.subscribe(this);
		
		hardwareWalletService.start();
	}
	
	public void stopCommunication() {
		hardwareWalletService.stopAndWait();
		
		
	}

	/**
	 * <p>
	 * Downstream consumer applications should respond to hardware wallet events
	 * </p>
	 *
	 * @param event
	 *            The hardware wallet event indicating a state change
	 */
	@Subscribe
	public void onHardwareWalletEvent(HardwareWalletEvent event) {

		log.debug("Received hardware event: '{}'.{}", event.getEventType().name(), event.getMessage());

		switch (event.getEventType()) {
		case SHOW_DEVICE_FAILED:
			// Treat as end of example
			System.exit(0);
			break;
		case SHOW_DEVICE_DETACHED:
			// Can simply wait for another device to be connected again
			break;
		case SHOW_DEVICE_READY:
			if (hardwareWalletService.isWalletPresent()) {

				// Request a message signature from the device
				// The response will contain the address used
				// hardwareWalletService.signMessage(0,
				// KeyChain.KeyPurpose.RECEIVE_FUNDS, 0, message.getBytes());
				hardwareWalletService.ringSignMessage(L, n, pi, message.getBytes());
				// hardwareWalletService.requestPublicKey(0,
				// KeyPurpose.RECEIVE_FUNDS, 0);
				// hardwareWalletService.requestPublicKey65();

			} else {
				log.info("You need to have created a wallet before running this example");
			}

			break;
		case PUBLIC_KEY_65:
			org.multibit.hd.hardware.core.messages.PublicKey65 publicKey65 = (org.multibit.hd.hardware.core.messages.PublicKey65) event
					.getMessage().get();
			try {
				log.info("Public Key:\n{}", Utils.HEX.encode(publicKey65.getPublicKey()));
			} catch (Exception e) {
				log.error("Exception in receiving public key 65", e);
			}

			break;

		case SHOW_PIN_ENTRY:
			// Device requires the current PIN to proceed
			PinMatrixRequest request = (PinMatrixRequest) event.getMessage().get();
			Scanner keyboard = new Scanner(System.in);
			String pin;
			switch (request.getPinMatrixRequestType()) {
			case CURRENT:
				System.err.println("Recall your PIN (e.g. '1').\n"
						+ "Look at the device screen and type in the numerical position of each of the digits\n"
						+ "with 1 being in the bottom left and 9 being in the top right (numeric keypad style) then press ENTER.");
				pin = keyboard.next();
				hardwareWalletService.providePIN(pin);
				break;
			default:
				break;
			}
			break;
		case MESSAGE_SIGNATURE:
			// Successful message signature
			MessageSignature signature = (MessageSignature) event.getMessage().get();

			try {
				log.info("Signature:\n{}", Utils.HEX.encode(signature.getSignature()));

				// Verify the signature
				String base64Signature = Base64.toBase64String(signature.getSignature());

				ECKey key = ECKey.signedMessageToKey(message, base64Signature);
				Address gotAddress = key.toAddress(MainNetParams.get());

				if (gotAddress.toString().equals(signature.getAddress())) {
					log.info("Verified the signature");
					// Treat as end of example
					System.exit(0);
				}

			} catch (Exception e) {
				log.error("deviceTx FAILED.", e);
			}

			// Must have failed to be here
			// Treat as end of example
			System.exit(-1);
			break;

		case MESSAGE_RING_SIGNATURE:
			log.info("A Message Ring Signature received");
			// String message2 = "Milner";

			vote = (MessageRingSignature) event.getMessage().get();
			ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");

			// turn message in BigInteger m
			byte[] mhash = new byte[32];
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				mhash = digest.digest(message.getBytes());
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				System.exit(1);
			}
			BigInteger m = new BigInteger(mhash);

			// instantiate the array for concatenation
			int totalSize = 0;
			for (byte[] pk : L)
				totalSize += pk.length;
			byte[] ytotalBytes = new byte[totalSize];

			// add all public key bytes to concatentation array
			int fromindex = 0;
			for (byte[] pk : L) {
				System.arraycopy(pk, 0, ytotalBytes, fromindex, pk.length);
				fromindex += pk.length;
			}

			// compute hash of L
			byte[] hash = new byte[32];
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				hash = digest.digest(ytotalBytes);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// turn h into Point H
			BigInteger h = new BigInteger(1, hash).mod(ecSpec.getCurve().getOrder());
			System.out.println("h : " + bytesToHex(h.toByteArray()));
			ECPoint H = ecSpec.getG().multiply(h);
			printPoint(H, "H");

			// compute the ring
			byte[] c_bytes = vote.getC();
			BigInteger c = new BigInteger(1, c_bytes);
			System.out.println("c : " + bytesToHex(c.toByteArray()));
			byte[] ytbytes = new byte[65];
			ytbytes[0] = 0x04;
			System.arraycopy(vote.getYtx(), 0, ytbytes, 1, 32);
			System.arraycopy(vote.getYty(), 0, ytbytes, 33, 32);
			ECPoint Yt = ecSpec.getCurve().decodePoint(ytbytes);
			for (int i = 0; i < vote.getN(); i++) {
				byte[] s_i = vote.getS()[i];
				BigInteger si = new BigInteger(1, s_i);
				System.out.println("s_i : " + bytesToHex(si.toByteArray()));
				ECPoint Yi = ecSpec.getCurve().decodePoint(L.get(i));
				printPoint(Yi, "Yi");

				ECPoint MathG1 = ecSpec.getG().multiply(si);
				printPoint(MathG1, "MathG'1");
				ECPoint MathG2 = Yi.multiply(c);
				printPoint(MathG2, "MathG'2");
				ECPoint MathG = MathG1.add(MathG2);

				printPoint(MathG, "MathG'");

				ECPoint MathH1 = H.multiply(si);
				ECPoint MathH2 = Yt.multiply(c);
				ECPoint MathH = MathH1.add(MathH2);

				printPoint(MathH, "MathH'");

				ECPoint MathT = MathG.add(MathH);
				printPoint(MathT, "MathT'");
				ECPoint Result = MathT.multiply(m);
				printPoint(Result, "Result'");

				c = Result.getY().toBigInteger();
			}

			BigInteger originalC = new BigInteger(1, c_bytes);
			byte[] c_calculated_bytes = c.toByteArray();
			if (c.equals(originalC))
				log.info("message signature validated");
			else
				log.info("message signature NOT validated");

			System.out.println("calculated c bytes : " + bytesToHex(c.toByteArray()));
			System.out.println("received   c bytes : " + bytesToHex(originalC.toByteArray()));
			
			stopCommunication();

			break;

		case SHOW_OPERATION_FAILED:
			// Treat as end of example
			System.exit(-1);
			break;

		case PUBLIC_KEY:
			PublicKey received = (PublicKey) event.getMessage().get();
			HDNodeType node = received.getHdNodeType().get();
			if (node != null) {
				byte[] pubKey = node.getPublicKey().get();
				if (pubKey != null) {
					log.info("the public key is " + pubKey.length + " bytes long.");
					log.info("the public key bytes: " + bytesToHex(pubKey));
				} else {
					log.info("pub key is null");
				}
			} else {
				log.info("HDNode is null");
			}

			break;
		default:
			// Ignore
		}

	}

	final protected static char[] hexArray = "0123456789abcdef".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 3];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 3] = hexArray[v >>> 4];
			hexChars[j * 3 + 1] = hexArray[v & 0x0F];
			hexChars[j * 3 + 2] = ' ';
		}
		return new String(hexChars);
	}

	private static void printPoint(ECPoint point, String name) {
		System.out.println(name + ".x : " + bytesToHex(point.getX().toBigInteger().toByteArray()));
		System.out.println(name + ".y : " + bytesToHex(point.getY().toBigInteger().toByteArray()));
	}

}