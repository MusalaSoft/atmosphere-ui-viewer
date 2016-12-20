package com.android.uiautomator.actions;

import org.eclipse.jface.action.Action;

import com.android.uiautomator.UiAutomatorViewer;
import com.musala.atmosphere.client.uiutils.ViewerCommunicator;

public class RefreshDeviceListAction extends Action{
    UiAutomatorViewer mViewer;
    
    private ViewerCommunicator vCommunicator;

    public RefreshDeviceListAction(UiAutomatorViewer viewer) {
    	super("&Refresh");
        mViewer = viewer;
        
        vCommunicator = new ViewerCommunicator();
	}

    @Override
    public void run() {

    }
}
