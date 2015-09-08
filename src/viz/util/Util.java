package viz.util;

import java.awt.Font;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Util {

    public static void loadUIManager() {
        boolean lafLoaded = false;

        if (isMac()) {
        	String version = System.getProperty("os.version");
        	int v = Integer.parseInt(version.split("\\.")[0]);
            if (v >= 5) {
                System.setProperty("apple.awt.brushMetalLook","true");
            }

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

        if (!lafLoaded) {
            UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
            for (UIManager.LookAndFeelInfo laf : lafs) {
                System.out.println(laf);
            }

            try {
                // set the System Look and Feel in the UIManager
                javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                    	try {
                    	    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    	        if ("Nimbus".equals(info.getName())) {
                    	            UIManager.setLookAndFeel(info.getClassName());
                    	            break;
                    	        }
                    	    }
                    	} catch (Exception e) {
                        try {
                            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//                              UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                        } catch (Exception e2) {
                            e.printStackTrace();
                        }
                    	}
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    public static File[] getFile(String message, boolean bLoadNotSave, File defaultFileOrDir, boolean bAllowMultipleSelection, String description, final String... extensions) {
        if (isMac()) {
            java.awt.Frame frame = new java.awt.Frame();
            java.awt.FileDialog chooser = new java.awt.FileDialog(frame, message,
                    (bLoadNotSave ? java.awt.FileDialog.LOAD : java.awt.FileDialog.SAVE));
            if (defaultFileOrDir != null) {
                if (defaultFileOrDir.isDirectory()) {
                    chooser.setDirectory(defaultFileOrDir.getAbsolutePath());
                } else {
                    chooser.setDirectory(defaultFileOrDir.getParentFile().getAbsolutePath());
                    chooser.setFile(defaultFileOrDir.getName());
                }
            }
            if (description != null) {
                FilenameFilter filter = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        for (int i = 0; i < extensions.length; i++) {
                            if (name.toLowerCase().endsWith(extensions[i].toLowerCase())) {
                                return true;
                            }
                        }
                        return false;
                    }
                };
                chooser.setFilenameFilter(filter);
            }

            //chooser.setMultipleMode(bAllowMultipleSelection);
            chooser.setVisible(true);
            if (chooser.getFile() == null) return null;
            //if (bAllowMultipleSelection) {
            //	return chooser.getFiles();
            //}
            File file = new java.io.File(chooser.getDirectory(), chooser.getFile());
            chooser.dispose();
            frame.dispose();
            return new File[]{file};
        } else {
            // No file name in the arguments so throw up a dialog box...
            java.awt.Frame frame = new java.awt.Frame();
            frame.setTitle(message);
            final JFileChooser chooser = new JFileChooser(defaultFileOrDir);
            chooser.setMultiSelectionEnabled(bAllowMultipleSelection);
            //chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            if (description != null) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(description, extensions);
                chooser.setFileFilter(filter);
            }

            if (bLoadNotSave) {
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    frame.dispose();
                    if (bAllowMultipleSelection) {
                        return chooser.getSelectedFiles();
                    } else {
                        if (chooser.getSelectedFile() == null) {
                            return null;
                        }
                        return new File[]{chooser.getSelectedFile()};
                    }
                }
            } else {
                if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    frame.dispose();
                    if (bAllowMultipleSelection) {
                        return chooser.getSelectedFiles();
                    } else {
                        if (chooser.getSelectedFile() == null) {
                            return null;
                        }
                        return new File[]{chooser.getSelectedFile()};
                    }
                }
            }
        }
        return null;
    }
}
