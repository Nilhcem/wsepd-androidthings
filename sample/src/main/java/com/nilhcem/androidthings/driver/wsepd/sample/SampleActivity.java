package com.nilhcem.androidthings.driver.wsepd.sample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import com.nilhcem.androidthings.driver.wsepd.BitmapHelper;
import com.nilhcem.androidthings.driver.wsepd.EPaperDisplay;

import java.io.IOException;

public class SampleActivity extends Activity {

    private static final String TAG = SampleActivity.class.getSimpleName();

    private static final String SPI_NAME = "SPI0.0";
    private static final String BUSY_GPIO = "BCM24";
    private static final String RESET_GPIO = "BCM17";
    private static final String DC_GPIO = "BCM25";

    private EPaperDisplay display;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            display = EPaperDisplay.Factory.create(SPI_NAME, BUSY_GPIO, RESET_GPIO, DC_GPIO, EPaperDisplay.DeviceType.Preset.EPD2X9.deviceType);

            // Clear screen
            display.clear();
            Thread.sleep(1000);

            // Draw waveshare logo
            display.setPixels(SampleData.WAVESHARE_LOGO);
            display.refresh();
            Thread.sleep(1000);

            // Draw a black-on-white bitmap
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.android);
            display.setPixels(BitmapHelper.bmpToBytes(bmp));
            display.refresh();
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Error initializing display", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            display.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing display", e);
        }
    }
}
