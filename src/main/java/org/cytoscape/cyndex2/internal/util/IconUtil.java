package org.cytoscape.cyndex2.internal.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.UIManager;

import org.cytoscape.util.swing.TextIcon;

public final class IconUtil {

	public static final Color NDEX_LOGO_COLOR = new Color(0, 124, 205);
	public static final Color ICON_COLOR_1 = UIManager.getColor("CyColor.complement(-2)");
	public static final Color ICON_COLOR_2 = UIManager.getColor("CyColor.complement(+1)");
	
	public static final String ICON_NDEX_LOGO = "\ue906";
	public static final String ICON_NDEX_CLOUD = "\ue903";
	public static final String ICON_NDEX_CLOUD_ARROW_DOWN = "\ue905";
	public static final String ICON_NDEX_CLOUD_ARROW_UP = "\ue904";
	public static final String ICON_NDEX_ACCOUNT = "\ue900";
	public static final String ICON_NDEX_ACCOUNT_MINUS = "\ue901";
	public static final String ICON_NDEX_ACCOUNT_PLUS = "\ue902";
	
	public static final String[] LAYERED_OPEN_ICON = new String[] { ICON_NDEX_CLOUD, ICON_NDEX_CLOUD_ARROW_DOWN };
	public static final String[] LAYERED_SAVE_ICON = new String[] { ICON_NDEX_CLOUD, ICON_NDEX_CLOUD_ARROW_UP };
	public static final Color[] LAYERED_OPEN_SAVE_COLORS = new Color[] { ICON_COLOR_2, Color.WHITE };
	
	private static Font iconFont;

	static {
		try {
			iconFont = Font.createFont(Font.TRUETYPE_FONT, IconUtil.class.getResourceAsStream("/fonts/cyndex.ttf"));
		} catch (FontFormatException e) {
			throw new RuntimeException();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
	
	public static Font getIconFont(float size) {
		return iconFont.deriveFont(size);
	}
	
	private IconUtil() {
		// restrict instantiation
	}

	public static Icon getNdexIcon() {
		Font iconFont = getIconFont(32f);
		return new TextIcon(IconUtil.ICON_NDEX_LOGO, iconFont, IconUtil.ICON_COLOR_1, 32, 32);
	}
}
