package viz.util;

import java.awt.Font;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

public class Util {

    public static void loadUIManager() {
        boolean lafLoaded = false;

        if (isMac()) {
            System.setProperty("apple.awt.graphics.UseQuartz", "true");
            System.setProperty("apple.awt.antialiasing", "true");
            System.setProperty("apple.awt.rendering", "VALUE_RENDER_QUALITY");

            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.draggableWindowBackground", "true");
            System.setProperty("apple.awt.showGrowBox", "true");

            try {

                try {
                    // We need to do this using dynamic class loading to avoid other platforms
                    // having to link to this class. If the Quaqua library is not on the classpath
                    // it simply won't be used.
                    Class<?> qm = Class.forName("ch.randelshofer.quaqua.QuaquaManager");
                    Method method = qm.getMethod("setExcludedUIs", Set.class);

                    Set<String> excludes = new HashSet<String>();
                    excludes.add("Button");
                    excludes.add("ToolBar");
                    method.invoke(null, excludes);

                } catch (Throwable e) {
                }

                //set the Quaqua Look and Feel in the UIManager
                UIManager.setLookAndFeel(
                        "ch.randelshofer.quaqua.QuaquaLookAndFeel"
                );
                lafLoaded = true;

            } catch (Exception e) {

            }

            UIManager.put("SystemFont", new Font("Lucida Grande", Font.PLAIN, 13));
            UIManager.put("SmallSystemFont", new Font("Lucida Grande", Font.PLAIN, 11));
        }

        try {

            if (!lafLoaded) {
        		UIManager.setLookAndFeel("javax.swing.plaf.metal");
        		if (true) return;

            	
            	if (isMac()) {
            		UIManager.setLookAndFeel("javax.swing.plaf.metal");
            	} else {
                    try {
                        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                            if ("Nimbus".equals(info.getName())) {
                                UIManager.setLookAndFeel(info.getClassName());
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // If Nimbus is not available, you can set the GUI to another look and feel.
                        UIManager.setLookAndFeel("javax.swing.plaf.metal");
                    } 
            	}
                //UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception e) {
        }

    
    }

    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac");
    }

	public static String formatToolTipAsHtml(String sToolTip) {
		String sStr = "<html>";
		int k = 0;
		for (int i = 0; i < sToolTip.length(); i++) {
			char c = sToolTip.charAt(i);
			if (c == '\n') {
				sStr += "<br/>";
				k = 0;
			} else if (Character.isWhitespace(c)) {
				if ( k > 60) {
					sStr += "<br/>";
					k = 0;
				} else {
					sStr += " ";
				}
			} else {
				sStr += c;
			}
			k++;
		}
		sStr += "</html>";
		return sStr;
	}

	public static String formatToolTipAsTeX(String sToolTip) {
		String sStr = "";
		for (int i = 0; i < sToolTip.length(); i++) {
			char c = sToolTip.charAt(i);
			if (c == '\n') {
				sStr += "\n\n";
			} else if (c == '%') {
				sStr += "\\%";
			} else if (c == '_') {
				sStr += "\\_";
			} else {
				sStr += c;
			}
		}
		return sStr;
	}

}
