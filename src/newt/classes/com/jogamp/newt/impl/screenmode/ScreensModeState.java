package com.jogamp.newt.impl.screenmode;

import java.util.HashMap;

public class ScreensModeState {
	private static HashMap screenModes = new HashMap();
	private static Object lock = new Object();
	
	public ScreensModeState(){
		
	}
	public synchronized void setScreenModeController(ScreenModeController screenModeControler){
		synchronized (lock) {
			screenModes.put(screenModeControler.getScreenFQN(), screenModeControler);	
		}
	}
	
	public synchronized ScreenModeController getScreenModeController(String screenFQN){
		return (ScreenModeController) screenModes.get(screenFQN);
	}
}
