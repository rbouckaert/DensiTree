package viz.util;


import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileNameExtensionFilter;

import jam.framework.Application;

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
            final UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
            //for (UIManager.LookAndFeelInfo laf : lafs) {
            //    System.out.println(laf + " [[" + laf.getName() + "]]");
            //}
            //System.out.println(UIManager.getCrossPlatformLookAndFeelClassName());

            try {
                // set the System Look and Feel in the UIManager
                javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
					public void run() {
                    	try {
                    	if (isMac()) {
                    	    for (LookAndFeelInfo info : lafs) {
                    	        if ("Mac OS X".equals(info.getName())) {
                    	            UIManager.setLookAndFeel(info.getClassName());
                    	            break;
                    	        }
                    	    }
                    	} else {
                    	    for (LookAndFeelInfo info : lafs) {
                    	        if ("Nimbus".equals(info.getName())) {
                    	            UIManager.setLookAndFeel(info.getClassName());
                    	            break;
                    	        }
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
    
    /**
     * parse a Java version string to an integer of major version like 7, 8, 9, 10, ...
     */
    public static int getMajorJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        // javaVersion should be something like "1.7.0_25"
        String[] version = javaVersion.split("\\.");
        if (version.length > 2) {
            int majorVersion = Integer.parseInt(version[0]);
            if (majorVersion == 1) {
                majorVersion = Integer.parseInt(version[1]);
            }
            return majorVersion;
        }
        try {
            int majorVersion = Integer.parseInt(javaVersion);
            return majorVersion;
        } catch (NumberFormatException e) {
            // ignore
        }
        return -1;
    }

    
       
    public static void macOSXRegistration(Application application) {
        if (isMac()) {
            NewOSXAdapter newOSXAdapter = new Util().new NewOSXAdapter(application);
            try {
                newOSXAdapter.registerMacOSXApplication(application);
            } catch (Throwable e) {
                System.err.println("Exception while loading the OSXAdapter: " + e.getMessage());
                // e.printStackTrace();
            }
        }

    }

    
    private static NewOSXAdapter theAdapter;
    
    /**
     * Since Oracle Java 9, Mac OS specific <code>com.apple.eawt</code> was replaced
     * by <code>java.awt.desktop</code>.
     * This class based on Java 8 will load <code>java.awt.desktop</code>,
     * if Java 9 is used in runtime.
     * Then, <i>about</i> and <i>quit</i> menu item will work properly.
     * The code is inspired from both source code of
     * <a href="https://github.com/rambaut/jam-lib">jam</a> package
     * and <a href="http://www.keystore-explorer.org">KeyStore Explorer</a>.
     */
    public class NewOSXAdapter implements InvocationHandler {
        private Application application;

        public NewOSXAdapter(Application var1) {
            this.application = var1;
        }

        /**
         * Use <code>reflect</code> to load <code>AboutHandler</code> and <code>QuitHandler</code>,
         * to avoid Mac specific classes being required in Java 9 and later.
         * @param var0
         * @throws ClassNotFoundException
         * @throws NoSuchMethodException
         * @throws IllegalAccessException
         * @throws InvocationTargetException
         * @throws InstantiationException
         */
        public void registerMacOSXApplication(Application var0) throws ClassNotFoundException,
                NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

            if (theAdapter == null) {
                theAdapter = new NewOSXAdapter(var0);
            }

            // using reflection to avoid Mac specific classes being required for compiling KSE on other platforms
            Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
            Class<?> quitHandlerClass;
            Class<?> aboutHandlerClass;

            if (getMajorJavaVersion() >= 9) {
                quitHandlerClass = Class.forName("java.awt.desktop.QuitHandler");
                aboutHandlerClass = Class.forName("java.awt.desktop.AboutHandler");
            } else {
                quitHandlerClass = Class.forName("com.apple.eawt.QuitHandler");
                aboutHandlerClass = Class.forName("com.apple.eawt.AboutHandler");
            }

            Object application = applicationClass.getConstructor((Class[]) null).newInstance((Object[]) null);
            Object proxy = Proxy.newProxyInstance(NewOSXAdapter.class.getClassLoader(), new Class<?>[]{
                    quitHandlerClass, aboutHandlerClass}, this);

            applicationClass.getDeclaredMethod("setQuitHandler", quitHandlerClass).invoke(application, proxy);
            applicationClass.getDeclaredMethod("setAboutHandler", aboutHandlerClass).invoke(application, proxy);

        }


        /**
         * Only <i>about</i> and <i>quit</i> are implemented.
         * @param proxy
         * @param method
         * @param args
         * @return
         * @throws Throwable
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("handleAbout".equals(method.getName())) {
                if (this.application != null) {
                    this.application.doAbout();
                } else {
                    throw new IllegalStateException("handleAbout: Application instance detached from listener");
                }
            } else if ("handleQuitRequestWith".equals(method.getName())) {
                if (this.application != null) {
                    this.application.doQuit();
                } else {
                    throw new IllegalStateException("handleQuit: Application instance detached from listener");
                }
            }
            return null;
        }


    }


	// -1 = not initialised
	// 0 = not M1 on Mac
	// 1 = M1 on Mac
	private static int isM1onMac = -1;
	
    /**
     * parse a Java version string to an integer of major version like 7, 8, 9, 10, ...
     */
    public static int isAppleSiliconWithJava17() {
    	if (!Util.isMac()) {
    		isM1onMac = 0;
    	}
    	if (isM1onMac >= 0) {
    		return isM1onMac;
    	}
    	

        String javaVersionString = System.getProperty("java.version");
        // javaVersion should be something like "1.7.0_25"
        String[] version = javaVersionString.split("\\.");
        if (version.length > 2) {
        	isM1onMac = Integer.parseInt(version[0]);
            if (isM1onMac == 1) {
            	isM1onMac = Integer.parseInt(version[1]);
            }
            if (isM1onMac < 17) {
            	isM1onMac = 0;
            } else {
            	isM1onMac = isAppleSillicon();
            }
            return isM1onMac;
        } else if (javaVersionString.contains("-")) {
        	version = javaVersionString.split("-");
        	isM1onMac = Integer.parseInt(version[0]);
            if (isM1onMac < 17) {
            	isM1onMac = 0;
            } else {
            	isM1onMac = isAppleSillicon();
            }
            return isM1onMac;
        }
        try {
        	isM1onMac = Integer.parseInt(javaVersionString);
            if (isM1onMac < 17) {
            	isM1onMac = 0;
            } else {
            	isM1onMac = isAppleSillicon();
            }
            return isM1onMac;
        } catch (NumberFormatException e) {
            // ignore
        }
        return -1;
    }
    
    private static int isAppleSillicon() {
    	// run `uname -m`
    	// returns arm64 on M1 chips, x86_64 otherwise
    	try {
   	 		// String [] args = new String[]{"uname" , "-m"};
   	 		String [] args = new String[]{"sysctl", "-n", "machdep.cpu.brand_string"};
		    Process p = Runtime.getRuntime().exec(args);
		    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        int c;
	        String result = "";
	        while ((c = input.read()) != -1) {
	       	  System.err.print((char)c);
	       	  result += (char)c;
	        }
	        input.close();			
	        p.waitFor();
	        if (result.contains("Apple")) {
	        	return 1;
	        }
    	} catch (Exception e) {
    		e.printStackTrace();
    		
    	}
    	return 0;
    }

}
