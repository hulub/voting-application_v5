package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.MessageEvent;

/**
 * <p>State to provide the following to hardware wallet clients:</p>
 * <ul>
 * <li>State transitions based on low level message events</li>
 * </ul>
 * <p>The "detached" state represents when the underlying hardware is waiting for a wallet to be attached.</p>
 *
 * @since 0.0.1
 *  
 */
public class DetachedState extends AbstractHardwareWalletState {

  @Override
  protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

    switch (event.getEventType()) {
      case DEVICE_FAILED:
        context.resetToFailed();
        break;
      case DEVICE_DETACHED:
        // Do nothing
        break;
      case DEVICE_ATTACHED:
        context.resetToAttached();
        break;
      default:
        handleUnexpectedMessageEvent(context, event);
    }

  }
}
