package org.openhab.binding.xbee.internal.connector;

import gnu.io.PortInUseException;
import gnu.io.RXTXPort;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.util.TooManyListenersException;

import com.rapplogic.xbee.XBeeConnection;
import com.rapplogic.xbee.api.XBeeException;

/**
 * XBee Serial Connector. This class initializes the serial connection and
 * notifies when data is available.
 * 
 * @author Antoine Bertin
 * @since 1.3.0
 */
public class XBeeSerialConnector extends RXTXPort implements XBeeConnection, SerialPortEventListener {

	public XBeeSerialConnector(String serialPort, int baudRate) throws PortInUseException,
			UnsupportedCommOperationException, TooManyListenersException, IOException, XBeeException {
		super(serialPort);
		setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		notifyOnDataAvailable(true);
		addEventListener(this);
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			synchronized (this) {
				this.notify();
			}
		}
	}

}
