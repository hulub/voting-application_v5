package org.multibit.hd.hardware.examples.keepkey.usb.extras;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.keepkey.clients.KeepKeyHardwareWalletClient;
import org.multibit.hd.hardware.keepkey.wallets.v1.KeepKeyV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * <p>Wipe the device to factory defaults and load with known seed phrase</p>
 * <p>Requires KeepKey V1 production device plugged into a USB HID interface.</p>
 * <p>This example demonstrates the message sequence to wipe a KeepKey device back to its fresh out of the box
 * state and then set it up with a known seed phrase.</p>
 *
 * <h3>Only perform this example on a KeepKey that you are using for test and development!</h3>
 * <h3>Loading with a known seed phrase is not secure</h3>
 * <h3>The seed phrase for this example is taken from the test vectors at https://github.com/keepkey/python-mnemonic/blob/master/vectors.json</h3>
 *
 *
 * @since 0.0.1
 *  
 */
public class KeepKeyV1WipeAndLoadAbandonWalletExample {

  private static final Logger log = LoggerFactory.getLogger(KeepKeyV1WipeAndLoadAbandonWalletExample.class);

  private HardwareWalletService hardwareWalletService;

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // All the work is done in the class
    KeepKeyV1WipeAndLoadAbandonWalletExample example = new KeepKeyV1WipeAndLoadAbandonWalletExample();

    example.executeExample();

    // Simulate the main thread continuing with other unrelated work
    // We don't terminate main since we're using safe executors
    Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MINUTES);

  }

  /**
   * Execute the example
   */
  public void executeExample() {

    // Use factory to statically bind the specific hardware wallet
    KeepKeyV1HidHardwareWallet wallet = HardwareWallets.newUsbInstance(
            KeepKeyV1HidHardwareWallet.class,
      Optional.<Integer>absent(),
      Optional.<Integer>absent(),
      Optional.<String>absent()
    );

    // Wrap the hardware wallet in a suitable client to simplify message API
    HardwareWalletClient client = new KeepKeyHardwareWalletClient(wallet);

    // Wrap the client in a service for high level API suitable for downstream applications
    hardwareWalletService = new HardwareWalletService(client);

    // Register for the high level hardware wallet events
    HardwareWalletEvents.subscribe(this);

    hardwareWalletService.start();

  }

  /**
   * <p>Downstream consumer applications should respond to hardware wallet events</p>
   *
   * @param event The hardware wallet event indicating a state change
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

        // Set the seed phrase
        String seedPhrase ="abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";

        // Force loading of the wallet (wipe then load)
        // Specify PIN
        // This method reveals the seed phrase so is not secure
        hardwareWalletService.loadWallet(
          "english",
          "Abandon",
          seedPhrase,
          "1"
        );
        break;

      case SHOW_OPERATION_SUCCEEDED:
        // Treat as end of example
        System.exit(0);
        break;
      case SHOW_OPERATION_FAILED:
        // Treat as end of example
        System.exit(-1);
        break;
      default:
        // Ignore
    }


  }

}
