The following are the original instructions taken from here: https://code.google.com/p/openhab/issues/detail?id=388

======================================================================

ZNetRxResponse Pattern: "<(?<responseType>\\w+)@(?<address>([0-9a-zA-Z])+)#(?<dataOffset>\\d+)(\\[(?<dataType>\\w+)\\])?(:(?<firstByte>\\d{1,3}))?"
ZNetRxResponse Example: <znetrxresponse@1122334455667788#0
What it does: This will extract bytes starting from byte 0 from the ZNetRxResponse from the address 1122334455667788. Bytes must be big endian and will be converted to the appropriate type depending on the type of the item. Number => DecimalType (4 bytes float), Dimmer => PercentType (1 byte), SwitchItem => OnOffType (1 byte), ContactItem => OpenCloseType (1 byte).
Use case: Custom XBee sensor with microcontrollers like arduino and multiple inputs. Digital temperature + humidity sensor
UPDATE: It's now possible to specify a dataType for Number items. When unpacking, if dataType is float or int, 4 bytes will be read, if it's byte, a single one will be read. This allows Dimmer/Contact to fit on a single byte (0-255 is enough)
It's also possible to identify each responses as unique using first byte of data as "identifier". If different requests comes from the same address, they can thus be treated differently.
Use case: Temperature (float) & Humidity (byte) every 30s using firstByte to 0
Presence sensor (byte) every change using firstByte to 1

ZNetRxIoSampleResponse Pattern: <(?<responseType>\\w+)@(?<address>([0-9a-zA-Z])+)#(?<pin>[AD][0-9]{1,2})(:(?<transformation>.*))?
ZNetRxIoSampleResponse Example: <znetrxiosampleresponse@1122334455667788#A0:10*x-0.5
What it does: This will read pin A0 from the ZNetRxIoSampleResponse from the address 1122334455667788 and will apply the formula 10*(read value as int 0-1023)-0.5
Use case: Standalone XBee with analog sensor like an temperature sensor. As the XBee has many inputs you can even add a digital sensor like a contact sensor.

Thanks to the low-level implementation it is possible to support more response types and more high-level responses like ZNetExplicitRxResponse and hence provide ZCL support.

For now the binding works for custom requests and DIY scenario.

# Example with Arduino
## Hardware
* An Arduino Fio + XBee module
* Another XBee module to plug into the computer running OpenHab (using this for example: https://www.sparkfun.com/products/9819 )
## Software
* Send Tx request from the Arduino to the other XBee with this https://code.google.com/p/xbee-arduino/source/browse/trunk/examples/Series2_Tx/Series2_Tx.pde on the Arduino
* OpenHab configured to receive ZNetRxResponse on an item
## What it does
This allows everyone willing to build his own sensor to send data to OpenHab. Data can be temperature, humidity, range, light, contact, alarm sensor or even a combination of all that.

# Using a standalone XBee
## Hardware
* An XBee module
* Another XBee module to plug into the computer running OpenHab (using this for example: https://www.sparkfun.com/products/9819 )
## Software
* The XBee module configured to send IO Samples
* OpenHab configured to receive ZNetRxIoSampleResponse
## What it does
XBee modules have analog and digital input pins meaning one can connect basic sensors directly to it with no micro controller. Suitable sensors are almost the same as previously but its raw data coming from the sensors.
I'd say custom contact sensors are very easy to make with this because XBee can be configured to send the state of the sensor every x seconds or whenever the state of the digital input changes.


## Examples

The following was also taken from here: https://code.google.com/p/openhab/issues/detail?id=388

I send a request from 0011223344556677 to the XBee on OpenHAB with temperature (as float) encoded on first 4 bytes and read it from OpenHAB with this:
<znetrxresponse@0011223344556677#0[float]

Same thing with humidity on another sensor. Values fits on one byte so its encoded as such, starting from byte 5. First byte is used as identifier and has to be 0:
<znetrxresponse@1122334455667788#5[byte]:0

This way I can catch another request from the same sensor into a different item. This is a presence sensor so I use a Contact item. First byte is used as identifier and has to be 1:
<znetrxresponse@1122334455667788#1:1
I didnt specified the type here, it should be [byte] but this is obvious because I read a contact item which has only two states (1 bit).

Now more complex, reading analog input of the XBee directly from the IO Sample responses:
<znetrxiosampleresponse@2233445566778899#A0:((x/1024.0)*1.2-0.5)*100
The conversion functions purpose is to transfom the raw analog value (0-1024) to actual temperature, using the sensors specification.


=================================================================

I found the above instructions confusing, so I've added more info below.

Here are some actual examples of items that you can add to demo.tems:

Switch Switch_C_Xbee1			"Xbee Switch 1"		(GF_Corridor, Lights)		{xbee="<znetrxresponse@0013A2004079E6FA#1:1"}
Switch Switch_C_Xbee2			"Xbee Switch 2"		(GF_Corridor, Lights)		{xbee="<znetrxresponse@0013A2004079E6FA#1:2"}
Switch Switch_C_Xbee3			"Xbee Switch 3"		(GF_Corridor, Lights)		{xbee="<znetrxresponse@0013A2004079E6FA#3:5"}

These are obviously switch items, and in this example both are tied to a single XBee. After adding them to the demo setup you'll find them under the ground floor corridor section. The example requires the remote xbee to send a two byte packet.
The big number between the "@" symbol and the "#" symbol is the 64 bit address of the XBee. It should be printed on the XBee itself. Again, in this example a single XBee will be updating the state for both switches so the addresses are the same.
The last number in the binding is the identifier. The first byte of the packet (position 0) is compared to this identifier to see if it matches. If the identifier matches then the packet is assumed to be intended for the binding. If the identifier doesn't match then the packets first byte, the packet is ignored for the binding. For switch 2, the identifier is 2. For switch 1, the identifier is 1. The identifier can be optional when creating bindings, but it will always refer to the 0th byte of data.
The number immediately after the "#" symbol and before the ":" indicates the first byte where data is stored (starting at 0). For a switch, the value at this location in the packet will tell openhab whether or not the switch is on or off. Putting a 0 in this byte tells openhab the switch is off, and putting a 1 means the switch is on. In the above example, since we have identifiers which use the 0th byte, the 1st byte is the next free byte so we will put the data there.
If the remote XBee were to transmit the data 0x02,0x01 it would toggle Xbee Switch 2 ON. If the remote XBee were to send 0x01,0x00 it would toggle Xbee Switch 1 OFF. The packet 0x03,0x01 would be ignored since the identifier (3) doesn't match either binding. The packet 0x01,0x01,0x02 would turn switch 1 on, and the trailing 0x02 would be ignored.

If you wanted to turn on switch 3, you would need to send: 0x05,0x??,0x??,0x01
Any number could exist at the "??" since those are ignored. If anything were trailing after the 0x01 it would be ignored too.

The real benefit of using this binding is that it allows a single Arduino+XBee to have multiple sensors attached, greatly bringing down the cost of hardware. It would be easy using this binding to set up a single Arduino+XBee to monitor several light switches and also report temperature, humidity, light level, and presence (via PIR sensor). Once someone writes the outbound communication portion of this binding, it will be possible to actually control multiple things as well.
