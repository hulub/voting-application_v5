import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;

import Model.ElectionParameters;
import Model.ElectionResults;
import Model.Failure;
import Model.MyPublicKey;
import Model.MyVote;
import Model.VoteItem;

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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.InputMismatchException;
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
	private MessageRingSignature vote;
	private ElectionParameters electionParameters;

	private org.multibit.hd.hardware.core.messages.PublicKey65 myPublicKey;

	private static String hostname;
	private static int port = 7777;
	private static Socket socket;

	private static ObjectOutputStream oos;
	private static ObjectInputStream ois;

	private Thread userThread;
	private Scanner keyboard = new Scanner(System.in);

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

		// before start communication with the trezor ...
		// start communication with server
		try {
			System.out.println("host : ");
			hostname = keyboard.nextLine();

			socket = new Socket(hostname, port);

			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());

		} catch (IOException e) {
			System.out.println("can't connect to server");
			System.exit(1);
		}

		// start communication asks trezor for public key
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


		userThread = new UserCommunicationThread();
		hardwareWalletService.start();

//		 try {
//		 Thread.sleep(20_000);
//		 } catch (InterruptedException e) {
//		 // TODO Auto-generated catch block
//		 e.printStackTrace();
//		 }

		// the thread is started after you receive the public key from trezor
//		userThread.start();
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
				hardwareWalletService.requestPublicKey65();
			} else {
				log.info("You need to have created a wallet before running this example");
			}

			break;
		case PUBLIC_KEY_65:
			myPublicKey = (org.multibit.hd.hardware.core.messages.PublicKey65) event.getMessage().get();
			userThread.start();
			try {
				log.info("Public Key:\n{}", Utils.HEX.encode(myPublicKey.getPublicKey()));
			} catch (Exception e) {
				log.error("Exception in receiving public key 65", e);
			}
			break;

		case SHOW_PIN_ENTRY:
			// Device requires the current PIN to proceed
			PinMatrixRequest request = (PinMatrixRequest) event.getMessage().get();
			String pin;
			switch (request.getPinMatrixRequestType()) {
			case CURRENT:
				System.err.println("Recall your PIN (e.g. '1').\n"
						+ "Look at the device screen and type in the numerical position of each of the digits\n"
						+ "with 1 being in the bottom left and 9 being in the top right (numeric keypad style) then press ENTER.");
				if (keyboard.hasNext()) {
					pin = keyboard.next();
					hardwareWalletService.providePIN(pin);
				}
				break;
			default:
				break;
			}
			break;
		case MESSAGE_SIGNATURE:
			// Successful message signature
			MessageSignature signature = (MessageSignature) event.getMessage().get();

			// Must have failed to be here
			// Treat as end of example
			System.exit(-1);
			break;

		case MESSAGE_RING_SIGNATURE:
			log.info("A Message Ring Signature received");
			// String message2 = "Milner";

			vote = (MessageRingSignature) event.getMessage().get();

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

	public class UserCommunicationThread extends Thread {
		public void run() {
			while (true) {
				System.out.println("Press 0 for refresh");
				if (electionParameters == null)
					System.out.println("Press 1 for getting election parameters");

				if (electionParameters != null)
					System.out.println("Press 2 for printing election parameters");

				if (electionParameters != null)
					System.out.println("Press 3 for selecting a candidate");

				if (message != null && electionParameters != null)
					System.out.println("Press 4 for signing vote");

				if (vote != null)
					System.out.println("Press 5 for casting vote");

				System.out.println("Press 6 for getting election results");
				System.out.println("Press 7 for getting election results raw");

				if (vote != null)
					System.out.println("Press 8 for printing Y tilda");

				System.out.println("Press 9 for closing application");

				try {
					if (keyboard.hasNextInt()) {
						int option = keyboard.nextInt();
						switch (option) {
						case 0:
							System.out.println();
							break;
						case 1: // ask server for election parameters
							if (electionParameters != null) {
								System.out.println("You already received election parameters. Press soemthing else!");
								break;
							}
							if (myPublicKey == null) {
								System.out.println("Did not receive public key from Trezor. Do that first!");
								break;
							}
							oos.writeObject(new MyPublicKey(myPublicKey.getPublicKey()));
							Object received;
							if ((received = ois.readObject()) != null) {
								if (received instanceof ElectionParameters) {
									electionParameters = (ElectionParameters) received;
									System.out.println("Election parameters received !");
								} else if (received instanceof Failure) {
									Failure fail = (Failure) received;
									System.out.println(fail.text);
								} else
									throw new ClassNotFoundException();
							}
							break;
						case 2: // print election parameters
							if (electionParameters == null) {
								System.out.println("You have to get election parameters first.");
								break;
							}

							System.out.println(electionParameters.toString());

							break;
						case 3: // Select candidate
							if (electionParameters == null) {
								System.out.println("You have to get election parameters first.");
								break;
							}
							System.out.println("Select from the following: ");
							int candidateNo = 0;
							for (String candidateName : electionParameters.M) {
								System.out.println(candidateNo + " " + candidateName);
								candidateNo++;
							}

							int candidateChoice = keyboard.nextInt();
							if (candidateChoice >= candidateNo)
								throw new InputMismatchException();

							message = electionParameters.M.get(candidateChoice);
							System.out.println("Selected candidate : " + message);

							break;
						case 4: // ring sign message
							if (electionParameters == null) {
								System.out.println("You have to get election parameters first.");
								break;
							}
							if (message == null) {
								System.out.println("You have to pick a candidate first.");
								break;
							}
							System.out.println("Refresh after you received the vote signature!");
							hardwareWalletService.ringSignMessage(electionParameters.L, electionParameters.L.size(),
									electionParameters.pi, message.getBytes());
							// hardwareWalletService.ringSignMessage(L,
							// L.size(),
							// pi, message.getBytes());
							break;
						case 5: // send vote to voting authority
							if (vote == null) {
								System.out.println("You have to sign your vote first.");
								break;
							}
							MyVote myVote = new MyVote(vote.getC(), vote.getS(), vote.getN(), vote.getYtx(),
									vote.getYty());
							oos.writeObject(myVote);
							if ((received = ois.readObject()) != null) {
								if (received instanceof Boolean) {
									boolean accepted = (Boolean) received;
									if (accepted)
										System.out.println("your vote was accepted");
									else
										System.out.println("your vote was NOT accepted");
								} else if (received instanceof Failure) {
									Failure fail = (Failure) received;
									System.out.println(fail.text);
								} else
									throw new ClassNotFoundException();
							}
							break;
						case 6: // ask for election results
							oos.writeObject("results");
							if ((received = ois.readObject()) != null) {
								if (received instanceof String) {
									String result = (String) received;
									System.out.println("Results are : ");
									System.out.println(result);
								} else if (received instanceof Failure) {
									Failure fail = (Failure) received;
									System.out.println(fail.text);
								} else
									throw new ClassNotFoundException();
							}
							break;
						case 7: // ask for election results raw
							oos.writeObject("results raw");
							if ((received = ois.readObject()) != null) {
								if (received instanceof ElectionResults) {
									ElectionResults results = (ElectionResults) received;
									System.out.println("Results are : ");
									for (VoteItem item : results.votes) {
										System.out.println(bytesToHex(item.Yt) + " : " + item.message);
									}
								} else if (received instanceof Failure) {
									Failure fail = (Failure) received;
									System.out.println(fail.text);
								} else
									throw new ClassNotFoundException();
							}
							break;
						case 8: // print Yt
							if (vote == null) {
								System.out.println("You have to sign your vote first.");
								break;
							}

							byte[] ytbytes = new byte[65];
							ytbytes[0] = 0x04;
							System.arraycopy(vote.getYtx(), 0, ytbytes, 1, 32);
							System.arraycopy(vote.getYty(), 0, ytbytes, 33, 32);
							System.out.println("Your Yt is : ");
							System.out.println(bytesToHex(ytbytes));

							break;
						case 9: // system.exit
							System.exit(1);
							break;
						default:
							throw new InputMismatchException();
						}
					}
				} catch (InputMismatchException e) {
					System.out.println("Please use the options provided");
				} catch (IOException e) {
					System.out.println("Server communication problem !!!");
				} catch (ClassNotFoundException e) {
					System.out.println("Server sent you something that you should not receive !!!");
				}
			}
		}
	}
}