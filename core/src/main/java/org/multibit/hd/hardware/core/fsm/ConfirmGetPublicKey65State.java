package org.multibit.hd.hardware.core.fsm;

import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.events.HardwareWalletEventType;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.MessageEvent;

public class ConfirmGetPublicKey65State extends AbstractHardwareWalletState {

	@Override
	protected void internalTransition(HardwareWalletClient client, HardwareWalletContext context, MessageEvent event) {

		switch (event.getEventType()) {
		case PIN_MATRIX_REQUEST:
			// Device is asking for a PIN matrix to be displayed (user must read
			// the screen carefully)
			HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_PIN_ENTRY,
					event.getMessage().get(), client.name());
			// Further state transitions will occur after the user has provided
			// the PIN via the service
			break;
		case PASSPHRASE_REQUEST:
			// Device is asking for a passphrase screen to be displayed
			HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_PASSPHRASE_ENTRY, client.name());
			// Further state transitions will occur after the user has provided
			// the passphrase via the service
			break;
		case BUTTON_REQUEST:
			// Device is requesting a button press
			HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_BUTTON_PRESS,
					event.getMessage().get(), client.name());
			client.buttonAck();
			break;
		/* Ring Sign Message */
		case MESSAGE_RING_SIGNATURE:
			// Device has completed the operation and provided a ring signed message
			HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.MESSAGE_RING_SIGNATURE,
					event.getMessage().get(), client.name());
			break;
		case PUBLIC_KEY_65:
			// Device has completed the operation and provided a ring signed message
			HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.PUBLIC_KEY_65,
					event.getMessage().get(), client.name());
			break;
		case FAILURE:
			// User has cancelled or operation failed
			HardwareWalletEvents.fireHardwareWalletEvent(HardwareWalletEventType.SHOW_OPERATION_FAILED,
					event.getMessage().get(), client.name());
			context.resetToInitialised();
			break;
		default:
			handleUnexpectedMessageEvent(context, event);
		}

	}
}