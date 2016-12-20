/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.uiautomator.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.android.uiautomator.UiAutomatorModel;
import com.android.uiautomator.UiAutomatorViewer;
import com.musala.atmosphere.client.uiutils.ViewerCommunicator;
import com.musala.atmosphere.client.util.Server;
import com.musala.atmosphere.commons.util.Pair;

@Server(connectionRetryLimit = 0, ip = "localhost", port = 1980)
public class ScreenshotAction extends Action {
    UiAutomatorViewer mViewer;
    private boolean mCompressed;
    
    private ViewerCommunicator vCommunicator;

    public ScreenshotAction(UiAutomatorViewer viewer, boolean compressed) {
        super("&Device Screenshot "+ (compressed ? "with Compressed Hierarchy" : "")
                +"(uiautomator dump" + (compressed ? " --compressed)" : ")"));
        mViewer = viewer;
        mCompressed = compressed;
        
        vCommunicator = new ViewerCommunicator();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        if(mCompressed)
            return ImageHelper.loadImageDescriptorFromResource("resources/screenshotcompressed.png");
        else
            return ImageHelper.loadImageDescriptorFromResource("resources/screenshot.png");
    }
    
    @Override
    public void run() {
    	UiAutomatorModel model;
    	File uiXml = null;
    	
    	final String deviceSN = pickDevice();
        if (deviceSN == null) {
            return;
        }
    	
		uiXml = new File(vCommunicator.getUiHierarchy(deviceSN));
		
        try {
            model = new UiAutomatorModel(uiXml);
        } catch (Exception e) {
            // FIXME: show error
            return;
        }

        Image img = null;
        File screenshot = new File(vCommunicator.getScreenshot(deviceSN));
        if (screenshot != null) {
            try {
                ImageData[] data = new ImageLoader().load(screenshot.getAbsolutePath());

                // "data" is an array, probably used to handle images that has multiple frames
                // i.e. gifs or icons, we just care if it has at least one here
                if (data.length < 1) {
                    throw new RuntimeException("Unable to load image: "
                            + screenshot.getAbsolutePath());
                }

                img = new Image(Display.getDefault(), data[0]);
            } catch (Exception e) {
                // FIXME: show error
                return;
            }
        }

        mViewer.setModel(model, uiXml, img);
    }

    private String pickDevice() {
        List<Pair<String,String>> devices = vCommunicator.getAvailableDevices();
        
        if (devices.size() == 0) {
            MessageDialog.openError(mViewer.getShell(),
                    "Error obtaining Device Screenshot",
                    "No Android devices were detected.");
            return null;
        } else if (devices.size() == 1) {
            return devices.get(0).getKey();
        } else {
            DevicePickerDialog dlg = new DevicePickerDialog(mViewer.getShell(), devices);
            if (dlg.open() != Window.OK) {
                return null;
            }
            return dlg.getSelectedDevice();
        }
    }

    private static class DevicePickerDialog extends Dialog {
        private final List<Pair<String,String>> mDevices;
        private final String[] mDeviceNames;
        private static int sSelectedDeviceIndex;

        public DevicePickerDialog(Shell parentShell, List<Pair<String,String>> devices) {
            super(parentShell);

            mDevices = devices;
            mDeviceNames = new String[mDevices.size()];
            for (int i = 0; i < devices.size(); i++) {
                mDeviceNames[i] = String.format("%s-%s", devices.get(i).getValue(), devices.get(i).getKey());
            }
        }

        @Override
        protected Control createDialogArea(Composite parentShell) {
            Composite parent = (Composite) super.createDialogArea(parentShell);
            Composite c = new Composite(parent, SWT.NONE);

            c.setLayout(new GridLayout(2, false));

            Label l = new Label(c, SWT.NONE);
            l.setText("Select device: ");

            final Combo combo = new Combo(c, SWT.BORDER | SWT.READ_ONLY);
            combo.setItems(mDeviceNames);
            int defaultSelection =
                    sSelectedDeviceIndex < mDevices.size() ? sSelectedDeviceIndex : 0;
            combo.select(defaultSelection);
            sSelectedDeviceIndex = defaultSelection;

            combo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    sSelectedDeviceIndex = combo.getSelectionIndex();
                }
            });

            return parent;
        }

        public String getSelectedDevice() {
            return mDevices.get(sSelectedDeviceIndex).getKey();
        }
    }
}
