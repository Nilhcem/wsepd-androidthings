# Waveshare e-Paper Display module driver for Android Things

*A very simple Waveshare eInk display module driver implementation for Android Things*  

![preview][]  

## Download

```groovy
dependencies {
    compile 'com.nilhcem.androidthings:driver-wsepd:0.0.1'
}
```

## Usage

*Tested on [Waveshare 2.9inch e-Paper Module][module_wiki]*

```java
// Access the EPD2X9 display
EPaperDisplay display;
EPaperDisplay.DeviceType epd2x9 = EPaperDisplay.DeviceType.Preset.EPD2X9.deviceType;
display = EPaperDisplay.Factory.create(SPI_NAME, BUSY_GPIO, RESET_GPIO, DC_GPIO, epd2x9);

// Clear screen
display.clear();

// Set pixels
byte[] rawPixels = SampleData.WAVESHARE_LOGO;
display.setPixels(rawPixels);

// Set a bitmap
Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.android);
byte[] bmpPixels = BitmapHelper.bmpToBytes(bmp);
display.setPixels(bmpPixels);

// Refresh the screen
display.refresh();

// Close the display when finished
display.close();
```

### Hardware connection

| e-Paper | Raspberry Pi 3 |
| ------- | -------------- |
| 3.3V    | 3.3V           |
| GND     | GND            |
| DIN     | MOSI (#19)     |
| CLK     | SCLK (#23)     |
| CS      | CE0 (#24)      |
| DC      | BCM25 (#22)    |
| RST     | BCM17 (#11)    |
| BUSY    | BCM24 (#18)    |

## Kudos to

* Novoda, and Blundell for their [InkypHat driver][inkyphat]

[preview]: https://raw.githubusercontent.com/Nilhcem/wsepd-androidthings/master/assets/preview.jpg
[module_wiki]: http://www.waveshare.com/wiki/2.9inch_e-Paper_Module
[inkyphat]: https://www.novoda.com/blog/porting-a-python-library-to-android-things-the-inkyphat/
