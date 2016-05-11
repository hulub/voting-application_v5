package org.multibit.hd.hardware.core.messages;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * <p>Value object to provide the following to downstream API consumers:</p>
 * <ul>
 * <li>Description of why an operation failed</li>
 * </ul>
 *
 * <p>This object is typically built from a hardware wallet specific adapter</p>
 *
 * @since 0.0.1
 *  
 */
public class Failure implements HardwareWalletMessage {

  private final FailureType type;
  private final String message;

  public Failure(FailureType type, String message) {
    this.type = type;
    this.message = message;
  }

  /**
   * @return The failure type
   */
  public FailureType getType() {
    return type;
  }

  /**
   * @return The failure message providing details
   */
  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("type", type)
      .append("message", message)
      .toString();
  }
}
