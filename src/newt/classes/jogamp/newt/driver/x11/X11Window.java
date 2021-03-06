/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

package jogamp.newt.driver.x11;

import jogamp.nativewindow.x11.X11Util;
import jogamp.newt.DisplayImpl;
import jogamp.newt.DisplayImpl.DisplayRunnable;
import jogamp.newt.WindowImpl;
import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.nativewindow.util.Point;

public class X11Window extends WindowImpl {
    private static final String WINDOW_CLASS_NAME = "NewtWindow";

    static {
        X11Display.initSingleton();
    }

    public X11Window() {
    }

    protected void createNativeImpl() {
        final X11Screen screen = (X11Screen) getScreen();
        final X11Display display = (X11Display) screen.getDisplay();
        final GraphicsConfigurationFactory factory = GraphicsConfigurationFactory.getFactory(display.getGraphicsDevice());
        config = factory.chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, screen.getGraphicsScreen());
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("X11Window.createNativeImpl() factory: "+factory+", chosen config: "+config);
        }        
        if (config == null) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        X11GraphicsConfiguration x11config = (X11GraphicsConfiguration) config;
        final long visualID = x11config.getVisualID();
        final int flags = getReconfigureFlags(0, true) & 
                          ( FLAG_IS_ALWAYSONTOP | FLAG_IS_UNDECORATED ) ;        
        setWindowHandle(CreateWindow0(getParentWindowHandle(),
                               display.getEDTHandle(), screen.getIndex(), visualID, 
                               display.getJavaObjectAtom(), display.getWindowDeleteAtom(), 
                               x, y, width, height, flags));
        windowHandleClose = getWindowHandle();
        if (0 == windowHandleClose) {
            throw new NativeWindowException("Error creating window");
        }
    }

    protected void closeNativeImpl() {
        if(0!=windowHandleClose && null!=getScreen() ) {
            X11Display display = (X11Display) getScreen().getDisplay();
            try {
                CloseWindow0(display.getEDTHandle(), windowHandleClose, 
                             display.getJavaObjectAtom(), display.getWindowDeleteAtom());
            } catch (Throwable t) {
                if(DEBUG_IMPLEMENTATION) { 
                    Exception e = new Exception("Warning: closeNativeImpl failed - "+Thread.currentThread().getName(), t);
                    e.printStackTrace();
                }
            } finally {
                windowHandleClose = 0;
            }
        }
    }

    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, int flags) { 
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("X11Window reconfig: "+x+"/"+y+" "+width+"x"+height+", "+
                               getReconfigureFlagsAsString(null, flags));
        }
        if(0 == ( FLAG_IS_UNDECORATED & flags) && 0<=x && 0<=y) {
            final InsetsImmutable i = getInsets();         
            
            // client position -> top-level window position
            x -= i.getLeftWidth() ;
            y -= i.getTopHeight() ;
            if( 0 > x ) { x = 0; }
            if( 0 > y ) { y = 0; }            
        }        
        reconfigureWindow0( getDisplayEDTHandle(), getScreenIndex(), getParentWindowHandle(), getWindowHandle(),
                            x, y, width, height, flags);

        return true;
    }

    protected void reparentNotify(long newParentWindowHandle) {
        if(DEBUG_IMPLEMENTATION) {
            final long p0 = getParentWindowHandle();
            System.err.println("Window.reparentNotify ("+getThreadName()+"): "+toHexString(p0)+" -> "+toHexString(newParentWindowHandle));
        }
    }
    
    protected void requestFocusImpl(boolean force) {
        requestFocus0(getDisplayEDTHandle(), getWindowHandle(), force);
    }

    @Override
    protected void setTitleImpl(final String title) {
        runWithLockedDisplayHandle( new DisplayImpl.DisplayRunnable() {
            public Object run(long dpy) {
                setTitle0(dpy, getWindowHandle(), title);
                return null;
            }
        });
    }

    protected Point getLocationOnScreenImpl(final int x, final int y) {
        // X11Util.GetRelativeLocation: locks display already !
        return X11Util.GetRelativeLocation( getScreen().getDisplay().getHandle(), getScreenIndex(), getWindowHandle(), 0 /*root win*/, x, y);
    }

    protected void updateInsetsImpl(Insets insets) {
        // nop - using event driven insetsChange(..)         
    }
    
    //----------------------------------------------------------------------
    // Internals only
    //
    
    private final long getDisplayEDTHandle() {
        return ((X11Display) getScreen().getDisplay()).getEDTHandle();
    }
    private final Object runWithLockedDisplayHandle(DisplayRunnable action) {
        return ((DisplayImpl) getScreen().getDisplay()).runWithLockedDisplayHandle(action);
        // return runWithTempDisplayHandle(action);
    }

    protected static native boolean initIDs0();
    private native long CreateWindow0(long parentWindowHandle, long display, int screen_index, 
                                            long visualID, long javaObjectAtom, long windowDeleteAtom, 
                                            int x, int y, int width, int height, int flags); 
    private native void CloseWindow0(long display, long windowHandle, long javaObjectAtom, long windowDeleteAtom);
    private native void reconfigureWindow0(long display, int screen_index, long parentWindowHandle, long windowHandle,
                                           int x, int y, int width, int height, int flags);    
    private native void setTitle0(long display, long windowHandle, String title);
    private native void requestFocus0(long display, long windowHandle, boolean force);
    private native long getParentWindow0(long display, long windowHandle);

    private long   windowHandleClose;
}
