/**
 * openHAB, the open Home Automation Bus.
 * Copyright (C) 2010-2013, openHAB.org <admin@openhab.org>
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with Eclipse (or a modified version of that library),
 * containing parts covered by the terms of the Eclipse Public License
 * (EPL), the licensors of this Program grant you additional permission
 * to convey the resulting work.
 */
package org.openhab.binding.xbee.internal;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Dictionary;
import java.util.TooManyListenersException;

import org.openhab.binding.xbee.XBeeBindingProvider;
import org.openhab.binding.xbee.internal.connector.XBeeSerialConnector;
import org.openhab.binding.xbee.internal.pin.XBeePinType;
import org.openhab.core.binding.AbstractBinding;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rapplogic.xbee.api.ErrorResponse;
import com.rapplogic.xbee.api.PacketListener;
import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.zigbee.ZNetRxIoSampleResponse;
import com.rapplogic.xbee.api.zigbee.ZNetRxResponse;

import de.congrace.exp4j.ExpressionBuilder;

/**
 * XBee Binding. This class establishes the connection to the XBee through the
 * {@link XBeeSerialConnector} and parses incoming responses to update items.
 * 
 * @author Antoine Bertin
 * @since 1.3.0
 */
public class XBeeBinding extends AbstractBinding<XBeeBindingProvider> implements ManagedService, PacketListener {

	private static final Logger logger = LoggerFactory.getLogger(XBeeBinding.class);

	private XBee xbee;
	private XBeeSerialConnector xbeeConnector;

	@Override
	public void activate() {
		logger.debug("Activate");
	}

	@Override
	public void deactivate() {
		logger.debug("Deactivate");
		disconnect();
	}

	@Override
	public void updated(Dictionary<String, ?> config) throws ConfigurationException {
		logger.debug("Configuration updated");

		if (config != null) {
			// Get the configuration
			String serialPort = (String) config.get("serialPort");
			String baudRate = (String) config.get("baudRate");

			// Disconnect from the previous XBee
			disconnect();

			// Connect to the XBee
			try {
				connect(serialPort, baudRate != null ? Integer.parseInt(baudRate) : 9600);
			} catch (Exception e) {
				logger.error("XBee connection failed: {}", e);
				throw new ConfigurationException("serialPort", e.getMessage());
			}
		}
	}

	/**
	 * Connect to the XBee
	 * 
	 * @param serialPort
	 *            port to connect to
	 * @param baudRate
	 *            baud rate to use
	 * @throws XBeeException
	 * @throws PortInUseException
	 * @throws UnsupportedCommOperationException
	 * @throws IOException
	 * @throws NoSuchPortException
	 * @throws TooManyListenersException
	 */
	public void connect(String serialPort, int baudRate) throws XBeeException, PortInUseException,
			UnsupportedCommOperationException, IOException, NoSuchPortException, TooManyListenersException {
		logger.info("Connecting to XBee [serialPort='{}', baudRate={}].", new Object[] { serialPort, baudRate });
		if (xbee != null) {
			throw new IllegalStateException("XBee already connected or improperly disconnected");
		}
		xbee = new XBee();
		xbeeConnector = new XBeeSerialConnector(serialPort, baudRate);
		xbee.initProviderConnection(xbeeConnector);
		xbee.addPacketListener(this);
		logger.info("Connection successfull");
	}

	/**
	 * Disconnect from the XBee
	 */
	public void disconnect() {
		if (xbee != null) {
			logger.debug("Disconnecting from the XBee");
			xbee.close();
			xbee = null;
		}
	}

	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		logger.debug("internalReceiveCommand");
		// TODO: Out-binding handling
	}

	@Override
	public void processResponse(XBeeResponse response) {
		// Handle error responses
		if (response.isError()) {
			logger.error("Error response received: {}", ((ErrorResponse) response).getErrorMsg());
			return;
		}

		// Handle incoming responses
		logger.debug("Response received: {}", response);
		for (XBeeBindingProvider provider : providers) {
			for (String itemName : provider.getInBindingItemNames()) {
				State newState = null;

				// Check the responseType
				if (response.getClass() != provider.getResponseType(itemName)) {
					continue;
				}

				// Depending on the response type
				// TODO: Support more
				if (response.getClass() == ZNetRxIoSampleResponse.class) { // ZNetRxIoSampleResponse
					ZNetRxIoSampleResponse znetRxIoSampleResponse = (ZNetRxIoSampleResponse) response;

					// Check the address
					if (!znetRxIoSampleResponse.getRemoteAddress64().equals(provider.getAddress(itemName))) {
						continue;
					}

					// Get the data depending on the pin
					if (provider.getPin(itemName).pinType == XBeePinType.DIGITAL) {
						// Check that the digital pin is enabled
						if (!znetRxIoSampleResponse.isDigitalEnabled(provider.getPin(itemName).pinNumber)) {
							logger.error("Digital pin {} is not enabled", provider.getPin(itemName).pinNumber);
							continue;
						}

						// Get the value
						Boolean isPinOn = znetRxIoSampleResponse.isDigitalOn(provider.getPin(itemName).pinNumber);

						// Cast according to the itemType
						// TODO: Support more
						if (provider.getItemType(itemName).isAssignableFrom(SwitchItem.class)) {
							if (isPinOn) {
								newState = OnOffType.ON;
							} else {
								newState = OnOffType.OFF;
							}
						} else if (provider.getItemType(itemName).isAssignableFrom(ContactItem.class)) {
							if (isPinOn) {
								newState = OpenClosedType.OPEN;
							} else {
								newState = OpenClosedType.CLOSED;
							}
						} else {
							logger.error("Cannot create state of type {} for value {}", provider.getItemType(itemName),
									isPinOn);
							continue;
						}
					} else {
						// Check that the analog pin is enabled
						if (!znetRxIoSampleResponse.isAnalogEnabled(provider.getPin(itemName).pinNumber)) {
							logger.error("Analog pin {} is not enabled", provider.getPin(itemName).pinNumber);
							continue;
						}

						// Get the value
						Double pinValue = (double) znetRxIoSampleResponse
								.getAnalog(provider.getPin(itemName).pinNumber);

						// Apply the transformation if any
						if (provider.getTransformation(itemName) != null) {
							try {
								pinValue = new ExpressionBuilder(provider.getTransformation(itemName))
										.withVariable("x", pinValue).build().calculate();
							} catch (Exception e) {
								logger.error("Transformation error: {}", e);
								continue;
							}
						}

						// Cast according to the itemType
						// TODO: Support more
						if (provider.getItemType(itemName).isAssignableFrom(NumberItem.class)) {
							newState = new DecimalType(pinValue);
						} else if (provider.getItemType(itemName).isAssignableFrom(DimmerItem.class)) {
							newState = new PercentType(pinValue.intValue());
						} else {
							logger.error("Cannot create state of type {} for value {}", provider.getItemType(itemName),
									pinValue);
							continue;
						}
					}
				} else if (response.getClass() == ZNetRxResponse.class) { // ZNetRxResponse
					ZNetRxResponse znetRxResponse = (ZNetRxResponse) response;

					// Check the address
					if (!znetRxResponse.getRemoteAddress64().equals(provider.getAddress(itemName))) {
						continue;
					}

					// Feed the raw data to the buffer
					ByteBuffer buffer = ByteBuffer.allocate(100);
					buffer.order(ByteOrder.BIG_ENDIAN);
					for (int i : znetRxResponse.getData()) {
						buffer.put((byte) i);
					}
					
					// Check the first byte
					if (provider.getFirstByte(itemName) != null && provider.getFirstByte(itemName) != buffer.get(0)) {
						continue;
					}

					// Cast according to the itemType
					// TODO: Support more
					if (provider.getItemType(itemName).isAssignableFrom(SwitchItem.class)) {
						if (buffer.get(provider.getDataOffset(itemName)) == 1) {
							newState = OnOffType.ON;
						} else {
							newState = OnOffType.OFF;
						}
					} else if (provider.getItemType(itemName).isAssignableFrom(ContactItem.class)) {
						if (buffer.get(provider.getDataOffset(itemName)) == 1) {
							newState = OpenClosedType.OPEN;
						} else {
							newState = OpenClosedType.CLOSED;
						}
					} else if (provider.getItemType(itemName).isAssignableFrom(NumberItem.class)) {
						if (provider.getDataType(itemName) == int.class) {
							newState = new DecimalType(buffer.getInt(provider.getDataOffset(itemName)));
						} else if (provider.getDataType(itemName) == byte.class) {
							newState = new DecimalType(buffer.get(provider.getDataOffset(itemName)));
						} else {
							newState = new DecimalType(buffer.getFloat(provider.getDataOffset(itemName)));
						}
					} else if (provider.getItemType(itemName).isAssignableFrom(DimmerItem.class)) {
						newState = new PercentType(buffer.get(provider.getDataOffset(itemName)));
					} else {
						logger.debug("Cannot create state of type {} for value {}", provider.getItemType(itemName),
								buffer);
						continue;
					}
				} else {
					logger.debug("Unhandled response type {}", response.getClass().toString().toLowerCase());
					continue;
				}

				// Publish the new state
				eventPublisher.postUpdate(itemName, newState);
			}
		}
	}
}
