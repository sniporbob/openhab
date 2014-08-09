package org.openhab.binding.xbee.internal.pin;

/**
 * @author Antoine Bertin
 * @since 1.3.0
 */
public class XBeePin {
	public XBeePinType pinType;
	public int pinNumber;

	public XBeePin(String pin) {
		if (pin.startsWith("A")) {
			pinType = XBeePinType.ANALOG;
		} else if (pin.startsWith("D")) {
			pinType = XBeePinType.DIGITAL;
		} else {
			throw new IllegalArgumentException("Pin should start with A (for analog) or D (for digital)");
		}
		pinNumber = Integer.parseInt(pin.substring(1));
	}

	@Override
	public String toString() {
		String repr;
		if (pinType == XBeePinType.ANALOG) {
			repr = "A";
		} else {
			repr = "B";
		}
		repr += Integer.toString(pinNumber);
		return repr;
	}
}
