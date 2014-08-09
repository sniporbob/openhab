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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.binding.xbee.XBeeBindingProvider;
import org.openhab.binding.xbee.internal.pin.XBeePin;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.openhab.model.item.binding.BindingConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rapplogic.xbee.api.XBeeAddress;
import com.rapplogic.xbee.api.XBeeAddress16;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeRequest;
import com.rapplogic.xbee.api.XBeeResponse;
import com.rapplogic.xbee.api.zigbee.ZNetRxIoSampleResponse;
import com.rapplogic.xbee.api.zigbee.ZNetRxResponse;

/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author Antoine Bertin
 * @since 1.3.0
 */
public class XBeeGenericBindingProvider extends AbstractGenericBindingProvider implements XBeeBindingProvider,
		BindingConfigReader {

	/**
	 * Artificial command for the in-binding configuration (which has no command
	 * part by definition). Because we use this artificial command we can reuse
	 * the {@link XBeeBindingConfig} for both in- and out-configuration.
	 */
	protected static final Command IN_BINDING_KEY = StringType.valueOf("IN_BINDING");

	/** {@link Pattern} which matches for an IoSampleResponse In-Binding */
	private static final Pattern IOSAMPLERESPONSE_IN_PATTERN = Pattern
			.compile("<(?<responseType>\\w+)@(?<address>([0-9a-zA-Z])+)#(?<pin>[AD][0-9]{1,2})(:(?<transformation>.*))?");

	/** {@link Pattern} which matches for an RxResponse In-Binding */
	private static final Pattern RXRESPONSE_IN_PATTERN = Pattern
			.compile("<(?<responseType>\\w+)@(?<address>([0-9a-zA-Z])+)#(?<dataOffset>\\d+)(\\[(?<dataType>\\w+)\\])?(:(?<firstByte>\\d{1,3}))?");

	/** {@link Pattern} which matches an Out-Binding */
	private static final Pattern OUT_PATTERN = Pattern.compile(">");

	private static final Logger logger = LoggerFactory.getLogger(XBeeGenericBindingProvider.class);

	@Override
	public String getBindingType() {
		return "xbee";
	}

	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		// Accept all sort of items
	}

	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig)
			throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);

		// Create the config
		XBeeBindingConfig config = new XBeeBindingConfig();
		config.itemType = item.getClass();

		// Match patterns
		if (IOSAMPLERESPONSE_IN_PATTERN.matcher(bindingConfig).matches()) {
			Matcher matcher = IOSAMPLERESPONSE_IN_PATTERN.matcher(bindingConfig);
			while (matcher.find()) {
				XBeeInBindingConfigElement configElement;
				configElement = new XBeeInBindingConfigElement();

				// Parse the responseType
				if (matcher.group("responseType").equals("znetrxiosampleresponse")) {
					configElement.responseType = ZNetRxIoSampleResponse.class;
				} else {
					throw new BindingConfigParseException("Invalid binding configuration: responseType '"
							+ matcher.group("responseType") + "' is not a valid responseType");
				}

				// Parse the address, pin and transformation
				configElement.address = parseAddress(matcher.group("address"));
				configElement.pin = new XBeePin(matcher.group("pin"));
				configElement.transformation = matcher.group("transformation");

				// Add to the config
				logger.debug("Adding in-binding configElement: {}", configElement.toString());
				config.put(IN_BINDING_KEY, configElement);
			}
		}
		if (RXRESPONSE_IN_PATTERN.matcher(bindingConfig).matches()) {
			Matcher matcher = RXRESPONSE_IN_PATTERN.matcher(bindingConfig);
			while (matcher.find()) {
				XBeeInBindingConfigElement configElement;
				configElement = new XBeeInBindingConfigElement();

				// Parse the responseType
				if (matcher.group("responseType").equals("znetrxresponse")) {
					configElement.responseType = ZNetRxResponse.class;
				} else {
					throw new BindingConfigParseException("Invalid binding configuration: responseType '"
							+ matcher.group("responseType") + "' is not a valid responseType");
				}

				// Parse the address, dataOffset, dataType and firstByte
				configElement.address = parseAddress(matcher.group("address"));
				configElement.dataOffset = Integer.parseInt(matcher.group("dataOffset"));
				configElement.dataType = parseDataType(matcher.group("dataType"));
				configElement.firstByte = matcher.group("firstByte") != null ? Byte.parseByte(matcher
						.group("firstByte")) : null;

				// Add to the config
				logger.debug("Adding in-binding configElement: {}", configElement.toString());
				config.put(IN_BINDING_KEY, configElement);
			}
		}
		if (OUT_PATTERN.matcher(bindingConfig).matches()) {
			// TODO: Out-bindings
		}
		if (!IOSAMPLERESPONSE_IN_PATTERN.matcher(bindingConfig).matches()
				&& !RXRESPONSE_IN_PATTERN.matcher(bindingConfig).matches()
				&& !OUT_PATTERN.matcher(bindingConfig).matches()) {
			throw new BindingConfigParseException("Invalid binding configuration '" + bindingConfig + "'");
		}
		logger.debug("Adding binding config for item {}", item.getName());
		addBindingConfig(item, config);
	}

	@Override
	public Class<? extends Item> getItemType(String itemName) {
		XBeeBindingConfig config = (XBeeBindingConfig) bindingConfigs.get(itemName);
		return config != null ? config.itemType : null;
	}

	@Override
	public Class<? extends XBeeResponse> getResponseType(String itemName) {
		XBeeBindingConfig config = (XBeeBindingConfig) bindingConfigs.get(itemName);
		return config != null && config.get(IN_BINDING_KEY) != null ? ((XBeeInBindingConfigElement) config
				.get(IN_BINDING_KEY)).responseType : null;
	}

	@Override
	public XBeeAddress getAddress(String itemName) {
		XBeeBindingConfig config = (XBeeBindingConfig) bindingConfigs.get(itemName);
		return config != null && config.get(IN_BINDING_KEY) != null ? ((XBeeInBindingConfigElement) config
				.get(IN_BINDING_KEY)).address : null;
	}

	@Override
	public Integer getDataOffset(String itemName) {
		XBeeBindingConfig config = (XBeeBindingConfig) bindingConfigs.get(itemName);
		return config != null && config.get(IN_BINDING_KEY) != null ? ((XBeeInBindingConfigElement) config
				.get(IN_BINDING_KEY)).dataOffset : null;
	}

	@Override
	public Class<? extends Number> getDataType(String itemName) {
		XBeeBindingConfig config = (XBeeBindingConfig) bindingConfigs.get(itemName);
		return config != null && config.get(IN_BINDING_KEY) != null ? ((XBeeInBindingConfigElement) config
				.get(IN_BINDING_KEY)).dataType : null;
	}

	@Override
	public XBeePin getPin(String itemName) {
		XBeeBindingConfig config = (XBeeBindingConfig) bindingConfigs.get(itemName);
		return config != null && config.get(IN_BINDING_KEY) != null ? ((XBeeInBindingConfigElement) config
				.get(IN_BINDING_KEY)).pin : null;
	}

	@Override
	public String getTransformation(String itemName) {
		XBeeBindingConfig config = (XBeeBindingConfig) bindingConfigs.get(itemName);
		return config != null && config.get(IN_BINDING_KEY) != null ? ((XBeeInBindingConfigElement) config
				.get(IN_BINDING_KEY)).transformation : null;
	}

	@Override
	public Byte getFirstByte(String itemName) {
		XBeeBindingConfig config = (XBeeBindingConfig) bindingConfigs.get(itemName);
		return config != null && config.get(IN_BINDING_KEY) != null ? ((XBeeInBindingConfigElement) config
				.get(IN_BINDING_KEY)).firstByte : null;
	}

	@Override
	public XBeeRequest getRequest(String itemName, Command command) {
		XBeeBindingConfig config = (XBeeBindingConfig) bindingConfigs.get(itemName);
		return config != null && config.get(command) != null ? ((XBeeOutBindingConfigElement) config.get(command)).request
				: null;
	}

	@Override
	public List<String> getInBindingItemNames() {
		List<String> inBindings = new ArrayList<String>();
		for (String itemName : bindingConfigs.keySet()) {
			XBeeBindingConfig config = (XBeeBindingConfig) bindingConfigs.get(itemName);
			if (config.containsKey(IN_BINDING_KEY)) {
				inBindings.add(itemName);
			}
		}
		return inBindings;
	}

	private XBeeAddress parseAddress(String address) throws BindingConfigParseException {
		XBeeAddress xbeeAddress;
		String[] addressString = address.split("(?<=\\G[0-9a-fA-F]{2})");
		int[] addressHex = new int[addressString.length];
		for (int i = 0; i < addressString.length; i++) {
			addressHex[i] = Integer.parseInt(addressString[i], 16);
		}
		switch (addressHex.length) {
		case 8:
			xbeeAddress = new XBeeAddress64(addressHex);
			break;
		case 2:
			xbeeAddress = new XBeeAddress16(addressHex);
			break;
		default:
			throw new BindingConfigParseException("Invalid binding configuration: address '" + address
					+ "' is not a valid address");
		}
		return xbeeAddress;
	}

	private Class<? extends Number> parseDataType(String dataType) {
		if (dataType == null) {
			return null;
		} else if (dataType.equals("float")) {
			return float.class;
		} else if (dataType.equals("int")) {
			return int.class;
		} else if (dataType.equals("byte")) {
			return byte.class;
		}
		return null;

	}

	static class XBeeBindingConfig extends HashMap<Command, BindingConfig> implements BindingConfig {
		/**
		 * Generated serial version uid
		 */
		private static final long serialVersionUID = 2541964231552108432L;
		Class<? extends Item> itemType;
	}

	static class XBeeInBindingConfigElement implements BindingConfig {
		Class<? extends XBeeResponse> responseType;
		XBeeAddress address;
		Integer dataOffset;
		Class<? extends Number> dataType;
		XBeePin pin;
		String transformation;
		Byte firstByte;

		@Override
		public String toString() {
			String repr = responseType.getName() + "(";
			repr += address != null ? "address=" + address : "address=null";
			repr += dataOffset != null ? ", dataOffset=" + dataOffset : "";
			repr += dataType != null ? ", dataType=" + dataType : "";
			repr += pin != null ? ", pin=" + pin : "";
			repr += transformation != null ? ", transformation=" + transformation : "";
			repr += firstByte != null ? ", firstByte=" + firstByte : "";
			repr += ")";
			return repr;
		}
	}

	static class XBeeOutBindingConfigElement implements BindingConfig {
		XBeeRequest request;
	}
}
