## About This Fork

The purpose of this fork is to have the old xbee binding for openhab 1.3 working again. The original xbee binding is from here: https://code.google.com/r/diaoulael-xbee/

Please note: This binding is unfinished and there is currently no way for openhab to transmit data to remote XBees. The original author never got around to adding that part of the binding. Currently it is only possible to receive data. Since I don't know how to program Java, everyone is encouraged to finish writing the missing code to allow openhab to transmit!

This fork is currently using nrjavaserial-3.9.3 which is a few versions ahead of the current version bundled with openhab. Supposedly the version openhab is on has finalized the RXTXPort class (which breaks the xbee binding), whereas 3.9.3 doesn't have the class finalized.

This will not build successfully on Maven for Windows. The xbee binding causes a negative time error and Maven aborts. It does work on Linux though, provided you allow Maven enough memory. I found that the virtual Ubuntu machine required at least 2GB ram, and I used: export MAVEN_OPTS="-Xms512m -Xmx1024m -XX:PermSize=256m -XX:MaxPermSize=512m"

## XBee Binding Documentation

See XBeeConfigReadme.txt for instructions and examples.

First thing first - so far, this binding only works for receiving data from an xbee. It is not yet possible to use this binding to transmit data. The original author never got around to coding the transmit part of the binding.

The following was taken from here: https://code.google.com/p/openhab/issues/detail?id=388

Configuration:

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

## Introduction

The open Home Automation Bus (openHAB) project aims at providing a universal integration platform for all things around home automation. It is a pure Java solution, fully based on OSGi. The Equinox OSGi runtime and Jetty as a web server build the core foundation of the runtime.

It is designed to be absolutely vendor-neutral as well as hardware/protocol-agnostic. openHAB brings together different bus systems, hardware devices and interface protocols by dedicated bindings. These bindings send and receive commands and status updates on the openHAB event bus. This concept allows designing user interfaces with a unique look&feel, but with the possibility to operate devices based on a big number of different technologies. Besides the user interfaces, it also brings the power of automation logics across different system boundaries.

For further Information please refer to our homepage http://www.openhab.org. The release binaries can be found in the ['releases' section on Github](https://github.com/openhab/openhab/releases). Nightly Snapshot-Builds can be obtained from [Cloudbees](https://openhab.ci.cloudbees.com/job/openHAB/).


## Demo

To see openHAB in action, you can directly access our demo server - choose one of these options:
- Check out the [Classic UI on the demo server](http://demo.openhab.org:8080/openhab.app?sitemap=demo) (use !WebKit-based browser, e.g. Safari or Chrome)
- Check out the [GreenT UI on the demo server](http://demo.openhab.org:8080/greent/) (use !WebKit-based browser, e.g. Safari or Chrome)
- Install the [native Android client](https://play.google.com/store/apps/details?id=org.openhab.habdroid) from the Google Play Store on your Android 4.x smartphone, which is preconfigured to use the demo server.
- Install the [native iOS client from the AppStore](http://itunes.apple.com/us/app/openhab/id492054521?mt=8) on your iPhone, iPod Touch or iPad, which is preconfigured to use the demo server.
- Try the [REST API](http://demo.openhab.org:8080/rest) directly on the demo server

If you just want to watch for a start, you might also like our [YouTube channel](http://www.youtube.com/playlist?list=PLGlxCdrGUagz6lfgo9SlNLhdwI4la_VSv)!

[![HABDroid](https://developer.android.com/images/brand/en_app_rgb_wo_45.png)](https://play.google.com/store/apps/details?id=org.openhab.habdroid) [![iOSApp](http://raw.github.com/wiki/openhab/openhab/images/app-store-badges.png)](http://itunes.apple.com/us/app/openhab/id492054521?mt=8)


## Quick Start

If you do not care about reading docs and just want to see things running, here are the quick start instructions for you:

1. [Download](http://www.openhab.org/downloads.html) the release version of the openHAB runtime (or alternatively the [latest snapshot build](https://openhab.ci.cloudbees.com/job/openHAB))
1. Unzip it to some local folder
1. [Download](http://www.openhab.org/downloads.html) the demo configuration files
1. Unzip to your openHAB folder
1. run `start.sh` resp. `start.bat`
1. Point your browser at [http://localhost:8080/openhab.app?sitemap=demo](http://localhost:8080/openhab.app?sitemap=demo)

If you want to use more bindings, you can download the [addons.zip](http://www.openhab.org/downloads.html) and extract it into the addons folder of the openHAB runtime.

If you are interested in more details, please see the [setup guide](https://github.com/openhab/openhab/wiki/Quick-Setup-an-openHAB-Server).


## Further Reading

Check out [the presentations](https://github.com/openhab/openhab/wiki/Presentations) that have been done about openHAB so far. If you are interested in the system architecture and its internals, please check out the wiki for the [Architecture](https://github.com/openhab/openhab/wiki).

![](http://raw.github.com/wiki/openhab/openhab/images/features.png)

## Community: How to get Support and How to Contribute

If you are looking for support, please check out the [different support channels](https://github.com/openhab/openhab/wiki/Support-options-for-openHAB) that we provide.

As any good open source project, openHAB welcomes any participation in the project. Read more in the [how to contribute](https://github.com/openhab/openhab/wiki/How-To-Contribute) guide.

If you are a developer and want to jump right into the sources and execute openHAB from within Eclipse, please have a look at the [IDE setup](https://github.com/openhab/openhab/wiki/IDE-Setup) procedures.

[![](http://raw.github.com/wiki/openhab/openhab/images/twitter.png)](http://twitter.com/openHAB)

## Trademark Disclaimer

Product names, logos, brands and other trademarks referred to within the openHAB website are the property of their respective trademark holders. These trademark holders are not affiliated with openHAB or our website. They do not sponsor or endorse our materials.
