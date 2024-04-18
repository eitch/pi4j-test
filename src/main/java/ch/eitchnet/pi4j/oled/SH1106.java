package ch.eitchnet.pi4j.oled;

import ch.eitchnet.pi4j.i2c.EncodedRawI2cBus;
import ch.eitchnet.pi4j.oled.fonts.DotMatrixFont;
import ch.eitchnet.pi4j.oled.fonts.IFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * <p>This class implements the logic to communicate with an SH1106 OLED display. The display is a 132x64 Dot Matrix
 * OLED driver with controller. It is controlled over I2C.</p>
 *
 * <p>Usage is as follows:</p>
 * <pre>
 * SH1106 display = new SH1106(new Pi4jI2cBus(pi4j, false));
 * display.setFont(new DotMatrixFont5x7());
 * display.writeLine(3, "Hello World", true);
 * display.display();
 * </pre>
 *
 * @author Roland Baudouin
 */
public class SH1106 {

	private static final Logger logger = LoggerFactory.getLogger(SH1106.class);

	//	https://www.velleman.eu/downloads/29/infosheets/sh1106_datasheet.pdf

	public static final byte SH1106_I2C_ADDRESS = (byte) 0x3C;
	private static final byte I2C_8BITS_ADDRESS = (byte) 0x78;
	private static final byte SH1106_SETCONTRAST = (byte) 0x81;
	private static final byte SH1106_DISPLAYALLON_RESUME = (byte) 0xA4;
	private static final byte SH1106_DISPLAYALLON = (byte) 0xA5;
	private static final byte SH1106_NORMALDISPLAY = (byte) 0xA6;
	private static final byte SH1106_INVERTDISPLAY = (byte) 0xA7;
	private static final byte SH1106_DISPLAYOFF = (byte) 0xAE;
	private static final byte SH1106_DISPLAYON = (byte) 0xAF;
	private static final byte SH1106_SETDISPLAYOFFSET = (byte) 0xD3;
	private static final byte SH1106_SETCOMPINS = (byte) 0xDA;
	private static final byte SH1106_SETVCOMDETECT = (byte) 0xDB;
	private static final byte SH1106_SETDISPLAYCLOCKDIV = (byte) 0xD5;
	private static final byte SH1106_SETPRECHARGE = (byte) 0xD9;
	private static final byte SH1106_SETMULTIPLEX = (byte) 0xA8;
	private static final byte SH1106_SETLOWCOLUMN = 0x00;
	private static final byte SH1106_SETHIGHCOLUMN = 0x10;
	private static final byte SH1106_SETSTARTLINE = 0x40;
	private static final byte SH1106_MEMORYMODE = 0x20;
	private static final byte SH1106_COLUMNADDR = 0x21;
	private static final byte SH1106_PAGEADDR = 0x22;
	private static final byte SH1106_COMSCANINC = (byte) 0xC0;
	private static final byte SH1106_COMSCANDEC = (byte) 0xC8;
	private static final byte SH1106_SEGREMAP = (byte) 0xA0;
	private static final byte SH1106_CHARGEPUMP = (byte) 0x8D;
	private static final byte SH1106_EXTERNALVCC = 0x1;
	private static final byte SH1106_SWITCHCAPVCC = 0x2;
	private static final byte SH1106_ACTIVATE_SCROLL = 0x2F;
	private static final byte SH1106_DEACTIVATE_SCROLL = 0x2E;
	private static final byte SH1106_SET_VERTICAL_SCROLL_AREA = (byte) 0xA3;
	private static final byte SH1106_RIGHT_HORIZONTAL_SCROLL = 0x26;
	private static final byte SH1106_LEFT_HORIZONTAL_SCROLL = 0x27;
	private static final byte SH1106_VERTICAL_AND_RIGHT_HORIZONTAL_SCROLL = 0x29;
	private static final byte SH1106_VERTICAL_AND_LEFT_HORIZONTAL_SCROLL = 0x2A;

	private final EncodedRawI2cBus i2cBus;
	private IFont font;
	private static final int LCD_PIXEL_WIDTH = 128;
	private static final int LCD_PIXEL_HEIGHT = 64;

	public enum PIXEL_MODE {
		PIXEL_ON,
		PIXEL_OFF,
		PIXEL_TOGGLE
	}

	private final byte[] lcdBuffer = new byte[LCD_PIXEL_WIDTH * LCD_PIXEL_HEIGHT / 8];

	public SH1106(EncodedRawI2cBus i2cBus) throws IOException, InterruptedException {
		this.i2cBus = i2cBus;
		init();
	}

	public void init() throws IOException, InterruptedException {
		// Init sequence for 128x64 OLED module
		sendCommand(SH1106_DISPLAYOFF);                    // 0xAE
		sendCommand(SH1106_SETDISPLAYCLOCKDIV);            // 0xD5
		sendCommand((byte) 0x80);                          // the suggested ratio 0x80
		sendCommand(SH1106_SETMULTIPLEX);                  // 0xA8
		sendCommand((byte) 0x3F);
		sendCommand(SH1106_SETDISPLAYOFFSET);              // 0xD3
		sendCommand((byte) 0x00);                          // no offset

		sendCommand(SH1106_SETSTARTLINE);                  // line #0 0x40
		sendCommand(SH1106_CHARGEPUMP);                    // 0x8D
		//		if (vccstate == SH1106_EXTERNALVCC) {
		sendCommand((byte) 0x10);
		//		} else {
		//			sendCommand(0x14);
		//		}
		sendCommand(SH1106_MEMORYMODE);                    // 0x20
		sendCommand((byte) 0x00);                          // 0x0 act like ks0108
		sendCommand((byte) (SH1106_SEGREMAP | 0x1));
		sendCommand(SH1106_COMSCANDEC);
		sendCommand(SH1106_SETCOMPINS);                    // 0xDA
		sendCommand((byte) 0x12);
		sendCommand(SH1106_SETCONTRAST);                   // 0x81
		//		if (vccstate == SH1106_EXTERNALVCC) {
		sendCommand((byte) 0x9F);
		//		} else {
		//			sendCommand(0xCF);
		//		}
		sendCommand(SH1106_SETPRECHARGE);                  // 0xd9
		//		if (vccstate == SH1106_EXTERNALVCC) {
		sendCommand((byte) 0x22);
		//		} else {
		//			sendCommand(0xF1);
		//		}
		sendCommand(SH1106_SETVCOMDETECT);                 // 0xDB
		sendCommand((byte) 0x40);
		sendCommand(SH1106_DISPLAYALLON_RESUME);           // 0xA4
		sendCommand(SH1106_NORMALDISPLAY);                 // 0xA6
	}

	public void setFont(IFont font) {
		this.font = font;
	}

	public void clear(boolean draw) throws IOException, InterruptedException {
		Arrays.fill(lcdBuffer, (byte) 0x00);
		if (!draw)
			return;

		sendCommand(SH1106_DISPLAYOFF);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Writes a line of text to the display buffer using the configured font. This does not yet communicate with the
	 * LCD.
	 */
	public void writeLine(int line, String string) {
		writeLine(line, string, true);
	}

	/**
	 * Writes a line of text to the display buffer using the configured font. This does not yet communicate with the
	 * LCD.
	 */
	public void writeLine(int line, String string, boolean trim) {
		if (line < 1 || line > 8) {
			logger.error("Ignoring illegal line number {}", line);
			return;
		}
		if (this.font == null)
			throw new IllegalStateException("No font set!");
		if (this.font.getScanMode() != DotMatrixFont.FONT_SCAN_MODE.FONT_SCAN_V)
			throw new IllegalStateException("Unhandled font scan mode " + this.font.getScanMode());

		int charWidth = this.font.getFontWidth() + this.font.getSpaceWidth();

		if (trim && string.length() * charWidth > LCD_PIXEL_WIDTH) {
			int overflow = (string.length() * charWidth) - LCD_PIXEL_WIDTH;
			int overFlowCharCount = (overflow / charWidth) + 1;
			//logger.warn("Trimming " + overFlowCharCount + " from string.");
			string = string.substring(0, string.length() - overFlowCharCount);
		}

		byte[] bytes = new byte[string.length() * charWidth];
		char[] chars = new char[string.length()];
		string.getChars(0, string.length(), chars, 0);

		int pos = 0;
		for (char c : chars) {
			byte[] charBytes = this.font.getBytes(c);
			System.arraycopy(charBytes, 0, bytes, pos, this.font.getFontWidth());
			pos += charWidth;
		}
		int destPos = (line - 1) * LCD_PIXEL_WIDTH;
		int writeLength = bytes.length;
		if (destPos + writeLength >= this.lcdBuffer.length) {
			writeLength = this.lcdBuffer.length - destPos;
			logger.error("Writing too much data! Trimming to {}", writeLength);
		}
		System.arraycopy(bytes, 0, this.lcdBuffer, destPos, writeLength);
	}

	/**
	 * Draws the given image to the display buffer. Does not yet communicate with the LCD.
	 */
	public void drawImage(String imagePath, PIXEL_MODE mode) throws IOException {
		File imageFile = new File(imagePath);
		BufferedImage bImage = ImageIO.read(imageFile);
		drawImage(imageFile.getName(), bImage, mode);
	}

	/**
	 * Draws the given image to the display buffer. Does not yet communicate with the LCD.
	 */
	public void drawImage(String imageName, BufferedImage bImage, PIXEL_MODE mode) throws IOException {

		int imageWidth = bImage.getWidth();
		int imageHeight = bImage.getHeight();

		int drawWidth = Math.min(imageWidth, LCD_PIXEL_WIDTH);
		int drawHeight = Math.min(imageHeight, LCD_PIXEL_HEIGHT);
		if (drawWidth != imageWidth || drawHeight != imageHeight)
			logger.warn("Image {} ({}x{}) is larger than display. Truncating.", imageName, imageWidth, imageHeight);

		for (int x = 0; x < drawWidth; x++) {
			for (int y = 0; y < drawHeight; y++) {
				int rgb = bImage.getRGB(x, y);
				if (rgb != -1)
					drawPixel(x, y, mode);
			}
		}
	}

	/**
	 * Draws a pixel, i.e. turns the pixel at the given coordinates on or off
	 */
	public void drawPixel(int x, int y, PIXEL_MODE mode) {
		long buffer = 1L << y;
		for (int j = 0; j < 8; j++) {
			long mask = (0xFFL << (j * 8));
			switch (mode) {
				case PIXEL_ON -> lcdBuffer[x + (j * LCD_PIXEL_WIDTH)] |= (byte) ((buffer & mask) >> (j * 8));
				case PIXEL_OFF -> lcdBuffer[x + (j * LCD_PIXEL_WIDTH)] &= ~(byte) ((buffer & mask) >> (j * 8));
				case PIXEL_TOGGLE -> lcdBuffer[x + (j * LCD_PIXEL_WIDTH)] ^= (byte) ((buffer & mask) >> (j * 8));
			}
		}
	}

	/**
	 * Writes the display data to the LCD, then turns on the display.
	 */
	public void display() throws IOException, InterruptedException {
		sendCommand(SH1106_DISPLAYOFF);

		sendCommand(SH1106_SETLOWCOLUMN);  // low col = 0
		sendCommand(SH1106_SETHIGHCOLUMN);  // hi col = 0
		sendCommand(SH1106_SETSTARTLINE); // line #0

		int height = 64;
		int width = 132;
		byte m_row = 0;
		byte m_col = 2;

		height >>= 3;
		width >>= 3;

		int p = 0;

		for (int i = 0; i < height; i++) {

			// send a bunch of data in one transmission
			sendCommand((byte) (0xB0 + i + m_row));//set page address
			sendCommand((byte) (m_col & 0xf));//set lower column address
			sendCommand((byte) (0x10 | (m_col >> 4)));//set higher column address

			for (int j = 0; j < 8; j++) {
				byte[] buffer = new byte[width];
				System.arraycopy(this.lcdBuffer, p, buffer, 0, width);
				sendData(buffer);
				p += width;
			}
		}

		sendCommand(SH1106_DISPLAYON);
		//		try {
		//			Thread.sleep(100);
		//		} catch (InterruptedException e) {
		//			throw new RuntimeException(e);
		//		}
	}

	private void sendCommand(byte b) throws IOException, InterruptedException {
		this.i2cBus.sendI2c(new byte[]{I2C_8BITS_ADDRESS, 0x00, b});
	}

	private void sendData(byte[] b) throws IOException, InterruptedException {
		byte[] buffer = new byte[b.length + 2];
		buffer[0] = I2C_8BITS_ADDRESS;
		buffer[1] = 0x40;
		System.arraycopy(b, 0, buffer, 2, b.length);
		this.i2cBus.sendI2c(buffer);
	}

	@Override
	public String toString() {
		return "SH1106 OLED Display";
	}
}
