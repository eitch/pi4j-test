package ch.eitchnet.pi4j.oled.fonts;

import ch.eitchnet.pi4j.oled.fonts.DotMatrixFont.FONT_SCAN_MODE;

/**
 * This interface defines the characteristics of a dot matrix font, where each character is represented by a sequence of
 * bytes
 *
 * @author Roland Baudouin
 */
public interface IFont {

	/**
	 * Returns the width of a character in pixels
	 */
	int getFontWidth();

	/**
	 * Returns the height of a character in pixels
	 */
	int getFontHeight();

	/**
	 * Returns the width of a single space in pixels
	 */
	int getSpaceWidth();

	/**
	 * Returns the bytes representing the given character
	 *
	 * @param c the character for which to return the bytes
	 */
	byte[] getBytes(char c);

	/**
	 * Returns the #FONT_SCAN_MODE of this font to be configured on the SH1106 display
	 */
	FONT_SCAN_MODE getScanMode();
}
