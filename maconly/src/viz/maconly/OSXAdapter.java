package viz.maconly;
/*
 * Adapted from OSXAdapter.java http://code.google.com/p/jam-lib
 *
 * Copyright (c) 2009 JAM Development Team
 *
 * This package is distributed under the Lesser Gnu Public Licence (LGPL)
 *
 * 
 */

import viz.DensiTree;

import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

public class OSXAdapter extends ApplicationAdapter {

    // pseudo-singleton model; no point in making multiple instances
    // of the EAWT application or our adapter
    private static OSXAdapter theAdapter;
    private static com.apple.eawt.Application theApplication;

    // reference to the app where the existing quit, about, prefs code is
    private DensiTree m_dt;

    private OSXAdapter(DensiTree application) {
        this.m_dt = application;
    }

    // implemented handler methods.  These are basically hooks into existing
    // functionality from the main app, as if it came over from another platform.
    public void handleAbout(ApplicationEvent ae) {
        if (m_dt != null) {
            ae.setHandled(true);
            m_dt.a_about.actionPerformed(null);
        } else {
            throw new IllegalStateException("handleAbout: Application instance detached from listener");
        }
    }

    public void handlePreferences(ApplicationEvent ae) {
    	// these are switched off
    }

    public void handleQuit(ApplicationEvent ae) {
        if (m_dt != null) {
            /*
            /	You MUST setHandled(false) if you want to delay or cancel the quit.
            /	This is important for cross-platform development -- have a universal quit
            /	routine that chooses whether or not to quit, so the functionality is identical
            /	on all platforms.  This example simply cancels the AppleEvent-based quit and
            /	defers to that universal method.
            */
            ae.setHandled(false);
            m_dt.a_quit.actionPerformed(null);
        } else {
            throw new IllegalStateException("handleQuit: Application instance detached from listener");
        }
    }


    // The main entry-point for this functionality.  This is the only method
    // that needs to be called at runtime, and it can easily be done using
    // reflection.
    public static void registerMacOSXApplication(DensiTree application) {
        if (theApplication == null) {
            theApplication = new com.apple.eawt.Application();
        }

        if (theAdapter == null) {
            theAdapter = new OSXAdapter(application);
        }
        theApplication.addApplicationListener(theAdapter);
        theApplication.setEnabledPreferencesMenu(false);
    }

	public void handleOpenFile(ApplicationEvent ae) {
        if (m_dt != null) {
            m_dt.doOpen(ae.getFilename());
            ae.setHandled(true);
        } else {
            throw new IllegalStateException("handleOpenFile: Application instance detached from listener");
        }
        System.out.println("handleOpenFile: " + ae.getFilename());
    }
}