package com.nilhcem.androidthings.driver.wsepd;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

public class EPaperDisplayWaveshare implements EPaperDisplay {

    private static final boolean DC_COMMAND = false;
    private static final boolean DC_DATA = true;

    private static final byte CMD_DRIVER_OUTPUT_CONTROL = 0x01;
    private static final byte CMD_BOOSTER_SOFT_START_CONTROL = 0x0c;
    private static final byte CMD_DEEP_SLEEP_MODE = 0x10;
    private static final byte CMD_DATA_ENTRY_MODE_SETTING = 0x11;
    private static final byte CMD_SWRESET = 0x12;
    private static final byte CMD_ACTIVATE_DISPLAY = 0x20;
    private static final byte CMD_DISPLAY_UPDATE_CONTROL_1 = 0x21;
    private static final byte CMD_DISPLAY_UPDATE_CONTROL_2 = 0x22;
    private static final byte CMD_WRITE_RAM = 0x24;
    private static final byte CMD_WRITE_VCOM_REGISTER = 0x2c;
    private static final byte CMD_WRITE_LUT_REGISTER = 0x32;
    private static final byte CMD_SET_DUMMY_PERIOD = 0x3a;
    private static final byte CMD_SET_GATE_LINE_WIDTH = 0x3b;
    private static final byte CMD_BORDER_WAVEFORM_CONTROL = 0x3c;
    private static final byte CMD_SET_RAM_X_START_END_POSITIONS = 0x44;
    private static final byte CMD_SET_RAM_Y_START_END_POSITIONS = 0x45;
    private static final byte CMD_SET_RAM_X_ADDRESS_COUNTER = 0x4e;
    private static final byte CMD_SET_RAM_Y_ADDRESS_COUNTER = 0x4f;
    private static final byte CMD_EMPTY_COMMAND = (byte) 0xff;

    private final SpiDevice spiDevice;
    private final Gpio busyGpio;
    private final Gpio rstGpio;
    private final Gpio dcGpio;
    private final DeviceType specs;

    private final byte[] buffer;

    EPaperDisplayWaveshare(SpiDevice spiDevice, Gpio busyGpio, Gpio rstGpio, Gpio dcGpio, DeviceType deviceType) throws IOException {
        this.spiDevice = spiDevice;
        this.busyGpio = busyGpio;
        this.rstGpio = rstGpio;
        this.dcGpio = dcGpio;
        this.specs = deviceType;

        int xSize = ((specs.xDot % 8 == 0) ? specs.xDot : specs.xDot + (8 - specs.xDot % 8)) / 8;
        buffer = new byte[xSize * specs.yDot];

        init();
    }

    private void init() throws IOException {
        spiDevice.setMode(SpiDevice.MODE0);
        spiDevice.setFrequency(2_000_000); // max speed: 2MHz
        spiDevice.setBitsPerWord(8);
        spiDevice.setBitJustification(SpiDevice.BIT_JUSTIFICATION_MSB_FIRST); // MSB first
        spiDevice.setCsChange(false);

        busyGpio.setDirection(Gpio.DIRECTION_IN);
        busyGpio.setActiveType(Gpio.ACTIVE_HIGH);

        rstGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
        rstGpio.setActiveType(Gpio.ACTIVE_HIGH);

        dcGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        dcGpio.setActiveType(Gpio.ACTIVE_HIGH);

        // Initialize display
        resetDriver();
        sendCommand(CMD_DRIVER_OUTPUT_CONTROL, new byte[]{(byte) ((specs.yDot - 1) % 256), (byte) ((specs.yDot - 1) / 256), (byte) 0x00}); // Panel configuration, Gate selection
        sendCommand(CMD_BOOSTER_SOFT_START_CONTROL, new byte[]{(byte) 0xd7, (byte) 0xd6, (byte) 0x9d}); // X decrease, Y decrease
        sendCommand(CMD_WRITE_VCOM_REGISTER, new byte[]{(byte) 0xa8}); // VCOM 7c
        sendCommand(CMD_SET_DUMMY_PERIOD, new byte[]{(byte) 0x1a}); // 4 dummy line per gate
        sendCommand(CMD_SET_GATE_LINE_WIDTH, new byte[]{0x08}); // 2us per line
        sendCommand(CMD_DATA_ENTRY_MODE_SETTING, new byte[]{0x01}); // Ram data entry mode
        sendCommand(CMD_SET_RAM_X_START_END_POSITIONS, new byte[]{(byte) 0x00, (byte) ((specs.xDot - 1) / 8)});
        sendCommand(CMD_SET_RAM_Y_START_END_POSITIONS, new byte[]{(byte) ((specs.yDot - 1) % 256), (byte) ((specs.yDot - 1) / 256), (byte) 0x00, (byte) 0x00});
        sendCommand(CMD_SET_RAM_X_ADDRESS_COUNTER, new byte[]{(byte) 0x00});
        sendCommand(CMD_SET_RAM_Y_ADDRESS_COUNTER, new byte[]{(byte) ((specs.yDot - 1) % 256), (byte) ((specs.yDot - 1) / 256)});
        sendCommand(CMD_WRITE_LUT_REGISTER, specs.lutDefaultFull);
        busyWait();

        sendCommand(CMD_DISPLAY_UPDATE_CONTROL_2, new byte[]{(byte) 0xc0});
        sendCommand(CMD_ACTIVATE_DISPLAY);
        clear();
    }

    @Override
    public void clear() throws IOException {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) 0xff;
        }
        setPixels(buffer);
    }

    @Override
    public void setPixels(byte[] pixels) throws IOException {
        busyWait();
        sendCommand(CMD_SET_RAM_X_ADDRESS_COUNTER, new byte[]{(byte) 0x00});
        sendCommand(CMD_SET_RAM_Y_ADDRESS_COUNTER, new byte[]{(byte) ((specs.yDot - 1) % 256), (byte) ((specs.yDot - 1) / 256)});

        System.arraycopy(pixels, 0, buffer, 0, Math.min(pixels.length, buffer.length));

        busyWait();
        sendCommand(CMD_WRITE_RAM, buffer, false);
    }

    @Override
    public void refresh() throws IOException {
        busyWait();

        sendCommand(CMD_DISPLAY_UPDATE_CONTROL_2, new byte[]{(byte) 0xc7});
        sendCommand(CMD_ACTIVATE_DISPLAY);
        sendCommand(CMD_EMPTY_COMMAND);
    }

    @Override
    public void close() throws IOException {
        spiDevice.close();
        busyGpio.close();
        rstGpio.close();
        dcGpio.close();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void busyWait() throws IOException {
        for (int i = 0; i < 400; i++) {
            if (!busyGpio.getValue()) {
                break;
            }
            sleep(100);
        }
    }

    private void sendCommand(byte command) throws IOException {
        sendCommand(command, null);
    }

    private void sendCommand(byte command, /*Nullable*/ byte[] data) throws IOException {
        sendCommand(command, data, true);
    }

    private void sendCommand(byte command, /*Nullable*/ byte[] data, boolean singleWrite) throws IOException {
        // Send command
        dcGpio.setValue(DC_COMMAND);
        spiDevice.write(new byte[]{command}, 1);

        // Send data
        if (data != null) {
            dcGpio.setValue(DC_DATA);

            if (singleWrite) {
                spiDevice.write(data, data.length);
            } else {
                for (byte b : data) {
                    spiDevice.write(new byte[]{b}, 1);
                }
            }
        }
    }

    private void resetDriver() throws IOException {
        rstGpio.setValue(false);
        sleep(100);
        rstGpio.setValue(true);
    }
}
