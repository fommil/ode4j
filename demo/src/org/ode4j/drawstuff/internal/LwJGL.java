/*************************************************************************
 *                                                                       *
 * Open Dynamics Engine, Copyright (C) 2001,2002 Russell L. Smith.       *
 * All rights reserved.  Email: russ@q12.org   Web: www.q12.org          *
 *                                                                       *
 * This library is free software; you can redistribute it and/or         *
 * modify it under the terms of EITHER:                                  *
 *   (1) The GNU Lesser General Public License as published by the Free  *
 *       Software Foundation; either version 2.1 of the License, or (at  *
 *       your option) any later version. The text of the GNU Lesser      *
 *       General Public License is included with this library in the     *
 *       file LICENSE.TXT.                                               *
 *   (2) The BSD-style license that is included with this library in     *
 *       the file LICENSE-BSD.TXT.                                       *
 *                                                                       *
 * This library is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the files    *
 * LICENSE.TXT and LICENSE-BSD.TXT for more details.                     *
 *                                                                       *
 *************************************************************************/
package org.ode4j.drawstuff.internal;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.ode4j.drawstuff.DrawStuff;
import org.ode4j.drawstuff.DrawStuff.dsFunctions;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.internal.Common;

import static org.cpp4j.Cstdio.*;


// main window and event handling for X11

//#include <ode/odeconfig.h>
//#include "config.h"
//#include <stdlib.h>
//#include <string.h>
//#include <stdarg.h>
//#include <X11/Xlib.h>
//#include <X11/Xatom.h>
//#include <X11/keysym.h>
//#include <GL/glx.h>
//
//#ifdef HAVE_SYS_TIME_H
//#include <sys/time.h>
//#endif
//
//#include <drawstuff/drawstuff.h>
//#include <drawstuff/version.h>
//#include "internal.h"

abstract class LwJGL extends Internal implements DrawStuffApi {

	//Ensure that Display.destroy() is called (TZ)
	//Not sure this works, but it's an attempt at least.
	//-> This should avoid the Problem that a process keeps running with 99%CPU, 
	//   even if the window is closed (clicking on the 'x'). The supposed 
	//   problem is that when clicking 'x', Display.destroy() never gets called
	//   by dsPlatformSimLoop(). 
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
//				Display.destroy();
			}
		});
	}
	
	
	//***************************************************************************
	// error handling for unix

	//static void printMessage (const char *msg1, const char *msg2, va_list ap)
	private static void printMessage (String msg1, String fmt, Object ...  ap)
	{
		fflush (stderr);
		fflush (stdout);
		fprintf (stderr,"\n%s: ",msg1);
		vfprintf (stderr,fmt,ap);
		fprintf (stderr,"\n");
		fflush (stderr);
	}


	//extern "C" void dsError (const char *msg, ...)
	static void dsError (String msg, Object ... ap)
	{
		//  va_list ap;
		//  va_start (ap,msg);
		printMessage ("Error",msg,ap);
		//TZ exit (1);
		throw new RuntimeException();
	}


	//extern "C" void dsDebug (const char *msg, ...)
	static void dsDebug (String msg, Object ... ap)
	{
		//  va_list ap;
		//  va_start (ap,msg);
		printMessage ("INTERNAL ERROR",msg,ap);
		// *((char *)0) = 0;	 ... commit SEGVicide ?
		//TZ abort();
		throw new RuntimeException();
	}


	//extern "C" void dsPrint (const char *msg, ...)
	static void dsPrint (String msg, Object ... ap)
	{
		//  va_list ap;
		//  va_start (ap,msg);
		vprintf (msg,ap);
	}

	//***************************************************************************
	// openGL window

	// X11 display info
	//static Display display;//*display=0;
	//private static int screen=0;
	//static XVisualInfo visual;//*visual=0;		// best visual for openGL
	//static Colormap colormap=null;		// window's colormap
	//static Atom wm_protocols_atom = null;
	//static Atom wm_delete_window_atom = null;

	// window and openGL
	//static Window win=null;			// X11 window, 0 if not initialized
	private static int width=0,height=0;		// window size
	//static GLXContext glx_context=null;	// openGL rendering context
	private static int last_key_pressed=0;		// last key pressed in the window
	private static boolean run=true;			// 1 if simulation running
	private static boolean pause=false;			// 1 if in `pause' mode
	private static boolean singlestep=false;		// 1 if single step key pressed
	private static boolean writeframes=false;		// 1 if frame files to be written


	private static void createMainWindow (int _width, int _height)
	{
		// create Window of size 300x300
		Display.setLocation((Display.getDisplayMode().getWidth() - _width) / 2,
				(Display.getDisplayMode().getHeight() - _height) / 2);
		try {
			Display.setDisplayMode(new DisplayMode(_width, _height));
			Display.setTitle("Simulation");
			Display.setVSyncEnabled(true);  //for VSync (TZ)
			Display.create();
		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}

		try {
			Keyboard.create();
			Mouse.create();
		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}
		
		if (firsttime) {
			//TODO copied from Gears
			System.err.println("GL_VENDOR: " + GL11.glGetString(GL11.GL_VENDOR));
			System.err.println("GL_RENDERER: " + GL11.glGetString(GL11.GL_RENDERER));
			System.err.println("GL_VERSION: " + GL11.glGetString(GL11.GL_VERSION));
			System.err.println();
			System.err.println("glLoadTransposeMatrixfARB() supported: " + 
					GLContext.getCapabilities().GL_ARB_transpose_matrix);
		}


		//	// create X11 display connection
		//  display = XOpenDisplay (null);
		//  if (!display) dsError ("can not open X11 display");
		//  screen = DefaultScreen(display);
		//
		//  // get GL visual
		//  static int attribList[] = {GLX_RGBA, GLX_DOUBLEBUFFER, GLX_DEPTH_SIZE,16,
		//			     GLX_RED_SIZE,4, GLX_GREEN_SIZE,4,
		//			     GLX_BLUE_SIZE,4, None};
		//  visual = glXChooseVisual (display,screen,attribList);
		//  if (!visual) dsError ("no good X11 visual found for OpenGL");

		// create colormap
		//  colormap = XCreateColormap (display,RootWindow(display,screen),
		//			      visual.visual,AllocNone);

		// initialize variables
		//  win = 0;
		width = _width;
		height = _height;
		//  glx_context = 0;
		last_key_pressed = 0;

		if (width < 1 || height < 1) dsDebug ("","bad window width or height");

		// create the window
		//  XSetWindowAttributes attributes;
		//  attributes.background_pixel = BlackPixel(display,screen);
		//  attributes.colormap = colormap;
		//  attributes.event_mask = ButtonPressMask | ButtonReleaseMask |
		//    KeyPressMask | KeyReleaseMask | ButtonMotionMask | PointerMotionHintMask |
		//    StructureNotifyMask;
		//  win = XCreateWindow (display,RootWindow(display,screen),50,50,width,height,
		//		       0,visual.depth, InputOutput,visual.visual,
		//		       CWBackPixel | CWColormap | CWEventMask, attributes);

		// associate a GLX context with the window
		//  glx_context = glXCreateContext (display,visual,0,GL_TRUE);
		//  if (!glx_context) dsError ("can't make an OpenGL context");

		// set the window title
		//  XTextProperty window_name;
		//  window_name.value = "Simulation";//(unsigned char *) "Simulation";
		//  window_name.encoding = XA_STRING;
		//  window_name.format = 8;
		//  window_name.nitems = window_name.value.length;//strlen((char *) window_name.value);
		//  XSetWMName (display,win,window_name);

		// participate in the window manager 'delete yourself' protocol
		//  wm_protocols_atom = XInternAtom (display,"WM_PROTOCOLS",False);
		//  wm_delete_window_atom = XInternAtom (display,"WM_DELETE_WINDOW",False);
		//  if (XSetWMProtocols (display,win,wm_delete_window_atom,1)==0)
		//    dsError ("XSetWMProtocols() call failed");

		// pop up the window
		//  XMapWindow (display,win);
		//  XSync (display,win);
	}


	private static void destroyMainWindow()
	{
		//  glXDestroyContext (display,glx_context);
		//  XDestroyWindow (display,win);
		//  XSync (display,0);
		//  XCloseDisplay(display);
		//  display = 0;
		//  win = 0;
		//  glx_context = 0;
		Keyboard.destroy();
		Mouse.destroy();
		Display.destroy();
	}


//	private static int mx=0,my=0; 	// mouse position
//	private static int mode = 0;		// mouse button bits
	//static void handleEvent (XEvent &event, dsFunctions *fn)
//	static void handleEvent (XEvent event, dsFunctions fn)
//	{
//		//TZ  static int mx=0,my=0; 	// mouse position
//		//TZ  static int mode = 0;		// mouse button bits

//		switch (event.type) {
//
//		case ButtonPress: {
//			if (event.xbutton.button == Button1) mode |= 1;
//			if (event.xbutton.button == Button2) mode |= 2;
//			if (event.xbutton.button == Button3) mode |= 4;
//			mx = event.xbutton.x;
//			my = event.xbutton.y;
//		}
//		return;
//
//		case ButtonRelease: {
//			if (event.xbutton.button == Button1) mode &= (~1);
//			if (event.xbutton.button == Button2) mode &= (~2);
//			if (event.xbutton.button == Button3) mode &= (~4);
//			mx = event.xbutton.x;
//			my = event.xbutton.x;
//		}
//		return;
//
//		case MotionNotify: {
//			if (event.xmotion.is_hint) {
//				Window root,child;
//				//unsigned 
//				int mask;
//				XQueryPointer (display,win,root,child,event.xbutton.x_root,
//						event.xbutton.y_root,event.xbutton.x,event.xbutton.y,
//						mask);
//			}
//			dsMotion (mode, event.xmotion.x - mx, event.xmotion.y - my);
//			mx = event.xmotion.x;
//			my = event.xmotion.y;
//		}
//		return;

		//Moved to handleKeyboard() TZ
//		case KeyPress: {
//			KeySym key;
//			XLookupString (event.xkey,NULL,0,key,0);
//			if ((event.xkey.state & ControlMask) == 0) {
//				if (key >= ' ' && key <= 126 && fn.command) fn.command (key);
//			}
//			else if (event.xkey.state & ControlMask) {
//				switch (key) {
//				case 't': case 'T':
//					dsSetTextures (dsGetTextures() ^ 1);
//					break;
//				case 's': case 'S':
//					dsSetShadows (dsGetShadows() ^ 1);
//					break;
//				case 'x': case 'X':
//					run = 0;
//					break;
//				case 'p': case 'P':
//					pause ^= 1;
//					singlestep = 0;
//					break;
//				case 'o': case 'O':
//					if (pause) singlestep = 1;
//					break;
//				case 'v': case 'V': {
//					float[] xyz=new float [3], hpr = new float [3];
//					dsGetViewpoint (xyz,hpr);
//					printf ("Viewpoint = (%.4f,%.4f,%.4f,%.4f,%.4f,%.4f)\n",
//							xyz[0],xyz[1],xyz[2],hpr[0],hpr[1],hpr[2]);
//					break;
//				}
//				case 'w': case 'W':
//					writeframes ^= 1;
//					if (writeframes) printf ("Now writing frames to PPM files\n");
//					break;
//				}
//			}
//			last_key_pressed = key;		// a kludgy place to put this...
//		}
//		return;
//
//		case KeyRelease: {
//			// hmmmm...
//		}
//		return;

//		case ClientMessage:
//			if (event.xclient.message_type == wm_protocols_atom &&
//					event.xclient.format == 32 &&
//					Atom(event.xclient.data.l[0]) == wm_delete_window_atom) {
//				run = 0;
//				return;
//			}
//			return;
//
//		case ConfigureNotify:
//			width = event.xconfigure.width;
//			height = event.xconfigure.height;
//			return;
//		}
//	}


	// return the index of the highest bit
	//static int getHighBitIndex (unsigned int x)
	private static int getHighBitIndex (int x)
	{
		int i = 0;
		while (x!=0) {
			i++;
			x >>= 1;
		}
		return i-1;
	}


	// shift x left by i, where i can be positive or negative
	//#define SHIFTL(x,i) (((i) >= 0) ? ((x) << (i)) : ((x) >> (-i)))
	//TODO int? double?
	private final int SHIFTL(long x, int i) { 
		return (int) ((i >= 0) ? (x << (i)) : ((x) >> (-i))); 
	}

	private static void captureFrame (int num)
	{
		throw new UnsupportedOperationException();
		//  fprintf (stderr,"capturing frame %04d\n",num);
		//
		//  char s[100];
		//  sprintf (s,"frame/frame%04d.ppm",num);
		//  FILE *f = fopen (s,"wb");
		//  if (!f) dsError ("can't open \"%s\" for writing",s);
		//  fprintf (f,"P6\n%d %d\n255\n",width,height);
		//  XImage *image = XGetImage (display,win,0,0,width,height,~0,ZPixmap);
		//
		//  int rshift = 7 - getHighBitIndex (image.red_mask);
		//  int gshift = 7 - getHighBitIndex (image.green_mask);
		//  int bshift = 7 - getHighBitIndex (image.blue_mask);
		//
		//  for (int y=0; y<height; y++) {
		//    for (int x=0; x<width; x++) {
		//      unsigned long pixel = XGetPixel (image,x,y);
		//      unsigned char b[3];
		//      b[0] = SHIFTL(pixel & image.red_mask,rshift);
		//      b[1] = SHIFTL(pixel & image.green_mask,gshift);
		//      b[2] = SHIFTL(pixel & image.blue_mask,bshift);
		//      fwrite (b,3,1,f);
		//    }
		//  }
		//  fclose (f);
		//  XDestroyImage (image);
	}

	
	/**
	 * Handles the keyboard
	 * @param fn 
	 */
	private void handleKeyboard(dsFunctions fn) {
		Keyboard.poll();
		while(Keyboard.next()) {
			char key = (char) Keyboard.getEventKey();
			if (key == Keyboard.KEY_ESCAPE) {
				run = false;
			}
			
			if (!(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || 
					Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
//				if ((event.xkey.state & ControlMask) == 0) {
				//if (key >= ' ' && key <= 126 && fn.command) fn.command (key);
				char keyChar = Keyboard.getEventCharacter();
				if (keyChar >= ' ' && keyChar <= 126) fn.command (keyChar);
//				System.out.println("cmd-c " +Keyboard.getEventCharacter());
//				  System.out.println("cmd- " + (char)(key+65));

			} else { //if (event.xkey.state & ControlMask) {
				if (key == last_key_pressed) {
					continue;
				}
				switch (key) {
				case Keyboard.KEY_T:
					dsSetTextures (!dsGetTextures());
					break;
				case Keyboard.KEY_S:
					dsSetShadows (!dsGetShadows());
					break;
				case Keyboard.KEY_X:
					run = false;
					break;
				case Keyboard.KEY_P:
					pause = !pause;
					singlestep = false;
					break;
				case Keyboard.KEY_O:
					if (pause) singlestep = true;
					break;
				case Keyboard.KEY_V: {
					float[] xyz=new float [3], hpr = new float [3];
					dsGetViewpoint (xyz,hpr);
					printf ("Viewpoint = (%.4f,%.4f,%.4f,%.4f,%.4f,%.4f)\n",
							xyz[0],xyz[1],xyz[2],hpr[0],hpr[1],hpr[2]);
					break;
				}
				case Keyboard.KEY_W:
					writeframes = !writeframes;
					if (writeframes) printf ("Now writing frames to PPM files\n");
					break;
				}
			}
			last_key_pressed = key;		// a kludgy place to put this...
		}
	}


	/**
	 * handles the mouse
	 */
	private void handleMouse() {
		readBufferedMouse();
	}

	/**
	 * reads a mouse in buffered mode
	 */
	private void readBufferedMouse() {
		// iterate all events, use the last button down
		while(Mouse.next()) {
			if (Mouse.getEventButton() != -1) {
				if (Mouse.getEventButtonState()) {
			}
				//lastButton = Mouse.getEventButton();
			}
		}  

		updateState();
	}

	/**
	 * Updates our "model"
	 *
	 */
	private void updateState() {
		int dx = Mouse.getDX();
		int dy = Mouse.getDY();
		int dw = Mouse.getDWheel();


		// get out if no movement
		if (dx == dy && dx == 0 && dw == 0) {
			return;
		}

		//LWJGL: 0=left 1=right 2=middle
		//GL: 0=left 1=middle 2=right
		
		int mode = 0;
		if (Mouse.isButtonDown(0)) mode |= 1; 
		if (Mouse.isButtonDown(2)) mode |= 2; 
		if (Mouse.isButtonDown(1)) mode |= 4;
		if (mode != 0) {
			//LWJGL has inverted dy wrt C++/GL
			dsMotion (mode, dx, -dy);
		}
		
	}

	
	//void dsPlatformSimLoop (int window_width, int window_height, dsFunctions *fn,
	//			int initial_pause)
	private static boolean firsttime=true;
	void dsPlatformSimLoop (int window_width, int window_height, dsFunctions fn,
			boolean initial_pause)
	{
		pause = initial_pause;
		createMainWindow (window_width, window_height);
		//glXMakeCurrent (display,win,glx_context);
		//TODO ?
		//GLContext.useContext(context);
		try {
			//Sets the context / by TZ
			Display.makeCurrent();
		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		} 

		dsStartGraphics (window_width,window_height,fn);

		//TZ static bool firsttime=true;
		if (firsttime)
		{
			System.err.println();
			System.err.print("Using ode4j version: " + OdeHelper.getVersion());
			System.err.println("  [" + OdeHelper.getConfiguration() + "]");
			System.err.println();
			fprintf
			(
					stderr,
					"\n" +
					"Simulation test environment v%d.%02d\n" +
					"   Ctrl-P : pause / unpause (or say `-pause' on command line).\n" +
					"   Ctrl-O : single step when paused.\n" +
					"   Ctrl-T : toggle textures (or say `-notex' on command line).\n" +
					"   Ctrl-S : toggle shadows (or say `-noshadow' on command line).\n" +
					"   Ctrl-V : print current viewpoint coordinates (x,y,z,h,p,r).\n" +
					"   Ctrl-W : write frames to ppm files: frame/frameNNN.ppm\n" +
					"   Ctrl-X : exit.\n" +
					"\n" +
					"Change the camera position by clicking + dragging in the window.\n" +
					"   Left button - pan and tilt.\n" +
					"   Right button - forward and sideways.\n" +
					"   Left + Right button (or middle button) - sideways and up.\n" +
					"\n",DrawStuff.DS_VERSION >> 8,DrawStuff.DS_VERSION & 0xff
			);
			firsttime = false;
		}

		//if (fn.start) 
		fn.start();

		int frame = 1;
		run = true;
		long startTime = System.currentTimeMillis() + 5000;
		long fps = 0;
		while (run && !Display.isCloseRequested()) {
			//  while (run) {
			// read in and process all pending events for the main window
			//    XEvent event;
			//    while (run && XPending (display)) {
			//      XNextEvent (display,event);
			//      handleEvent (event,fn);
			//    }
			handleKeyboard(fn);
			handleMouse();

			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

			dsDrawFrame (width,height,fn,pause && !singlestep);
			singlestep = false;


			Display.update();
			if (startTime > System.currentTimeMillis()) {
				fps++;
			} else {
				long timeUsed = 5000 + (startTime - System.currentTimeMillis());
				startTime = System.currentTimeMillis() + 5000;
				System.out.println(fps + " frames in " + (float) (timeUsed / 1000f) + " seconds = "
						+ (fps / (timeUsed / 1000f)));
				fps = 0;
			}
			//    glFlush();
			//    glXSwapBuffers (display,win);
			//    XSync (display,0);

			// capture frames if necessary
			if (pause==false && writeframes) {
				captureFrame (frame);
				frame++;
			}
		}

		//if (fn.stop) 
		fn.stop();
		dsStopGraphics();

		destroyMainWindow();
	}


	//extern "C" void dsStop()
	public void dsStop()
	{
		run = false;
	}


	private static double prev=(double)System.nanoTime()/1000000000.0;
	//extern "C" double dsElapsedTime()
	public double dsElapsedTime()
	{
		if (true) {//(HAVE_GETTIMEOFDAY) { //#if HAVE_GETTIMEOFDAY
			//TZ static double prev=0.0;
			//		timeval tv ;
			//
			//		gettimeofday(tv, 0);
			//		double curr = tv.tv_sec + (double) tv.tv_usec / 1000000.0 ;
			double curr = (double)System.nanoTime()/1000000000.0;
			//		if (prev==-1)
			//			prev=curr;
			double retval = curr-prev;
			prev=curr;
			if (retval>1.0) retval=1.0;
			if (retval<Common.dEpsilon) retval=Common.dEpsilon;
			return retval;
		} else { //#else
			return 0.01666; // Assume 60 fps
			//#endif
		}

	}
}
