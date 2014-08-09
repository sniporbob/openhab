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
package org.openhab.binding.xbee;

import java.util.List;

import org.openhab.binding.xbee.internal.pin.XBeePin;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;

import com.rapplogic.xbee.api.XBeeAddress;
import com.rapplogic.xbee.api.XBeeRequest;
import com.rapplogic.xbee.api.XBeeResponse;

/**
 * @author Antoine Bertin
 * @since 1.3.0
 */
public interface XBeeBindingProvider extends BindingProvider {

	/**
	 * Returns the Type of the Item identified by {@code itemName}
	 * 
	 * @param itemName
	 *            the name of the item to find the type for
	 * @return the type of the Item identified by {@code itemName}
	 */
	Class<? extends Item> getItemType(String itemName);

	/**
	 * Returns the response type according to <code>itemName</code>. In-Binding
	 * only.
	 * 
	 * @param itemName
	 *            the item for which to find the response type
	 * @return the matching response type
	 */
	Class<? extends XBeeResponse> getResponseType(String itemName);

	/**
	 * Returns the address according to the <code>itemName</code>. In-Binding
	 * only.
	 * 
	 * @param itemName
	 *            the item for which to find the address
	 * @return the matching address
	 */
	XBeeAddress getAddress(String itemName);

	/**
	 * Returns the offset of the data to extract according to
	 * <code>itemName</code>. In-Binding and RxResponse responses only.
	 * 
	 * @param itemName
	 *            the item of which to find the offset
	 * @return the matching offset
	 */
	Integer getDataOffset(String itemName);

	/**
	 * Returns the type of the data to extract according to
	 * <code>itemName</code>. In-Binding and RxResponse responses only.
	 * 
	 * @param itemName
	 *            the item of which to find the type
	 * @return the matching type
	 */
	Class<? extends Number> getDataType(String itemName);

	/**
	 * Returns the pin of the XBee of which to read value according to
	 * <code>itemName</code>. In-Binding and IoSample responses only.
	 * 
	 * @param itemName
	 *            the item of which to find the xbee pin
	 * @return the matching xbee pin
	 */
	XBeePin getPin(String itemName);

	/**
	 * Returns the transformation rule to use according to <code>itemName</code>
	 * . In-Binding only.
	 * 
	 * @param itemName
	 *            the item for which to find a transformation rule
	 * 
	 * @return the matching transformation rule or <code>null</code> if no
	 *         matching transformation rule could be found.
	 */
	String getTransformation(String itemName);

	/**
	 * Returns the firstByte of the data according to <code>itemName</code> .
	 * In-Binding and RxResponse responses only.
	 * 
	 * @param itemName
	 *            the item for which to find the first byte
	 * 
	 * @return the matching first byte or <code>null</code> if no first byte was
	 *         specified.
	 */
	Byte getFirstByte(String itemName);

	/**
	 * Returns the request according to <code>itemName</code> and
	 * <code>command</code>. Out-binding only.
	 * 
	 * @param itemName
	 *            the item for which to find the request
	 * @param command
	 *            the command for which to find the request
	 * @return the matching request
	 */
	XBeeRequest getRequest(String itemName, Command command);

	/**
	 * Returns all items which are mapped to a XBee In-Binding
	 * 
	 * @return item which are mapped to a XBee In-Binding
	 */
	List<String> getInBindingItemNames();

}
