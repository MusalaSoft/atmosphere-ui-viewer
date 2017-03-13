package com.android.uiautomator.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.android.uiautomator.UiAutomatorViewer;
import com.musala.atmosphere.client.ServerConnectionHandler;
import com.musala.atmosphere.client.uiutils.ViewerCommunicator;
import com.musala.atmosphere.client.util.ServerConnectionProperties;
import com.musala.atmosphere.commons.util.Pair;
import com.musala.atmosphere.commons.util.PropertiesLoader;

/**
 * Class representing the remote control action. This class is extending the jface {@link Action} class.
 * 
 * @author yavor.stankov
 *
 */
public class RemoteControlAction extends Action {
    UiAutomatorViewer mViewer;

    private ViewerCommunicator vCommunicator;

    private ServerConnectionHandler serverConnectionHandler;

    /**
     * Creates the remote control action by given {@link UiAutomatorViewer}.
     * 
     * @param viewer
     *        - the given {@link UiAutomatorViewer}
     */
    public RemoteControlAction(UiAutomatorViewer viewer) {
        super("Remote Control");
        mViewer = viewer;

        vCommunicator = new ViewerCommunicator();

        PropertiesLoader loader = PropertiesLoader.getInstance("config.properties");

        ServerConnectionProperties serverConnectionProperties = new ServerConnectionProperties(loader.getPropertyString("server.ip"),
                                                                                               Integer.parseInt(loader.getPropertyString("server.port")),
                                                                                               0);
        serverConnectionHandler = new ServerConnectionHandler(serverConnectionProperties);
        serverConnectionHandler.connect();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageHelper.loadImageDescriptorFromResource("resources/remote-control.png");
    }

    @Override
    public void run() {
        final String deviceSN = pickDevice();
        if (deviceSN == null) {
            return;
        }

        mViewer.setRemoteControlModel(deviceSN);
    }

    private String pickDevice() {
        List<Pair<String, String>> devices = vCommunicator.getAvailableDevices();

        if (devices.isEmpty()) {
            MessageDialog.openError(mViewer.getShell(),
                                    "Error obtaining Device Screenshot",
                                    "No Android devices were detected.");
            return null;
        } else if (devices.size() == 1) {
            return devices.get(0).getKey();
        } else {
            DevicePickerDialog devicePickerDialog = new DevicePickerDialog(mViewer.getShell(), devices);
            if (devicePickerDialog.open() != Window.OK) {
                return null;
            }
            return devicePickerDialog.getSelectedDevice();
        }
    }

    private static class DevicePickerDialog extends Dialog {
        private final List<Pair<String, String>> mDevices;

        private final String[] mDeviceNames;

        private static int sSelectedDeviceIndex;

        public DevicePickerDialog(Shell parentShell, List<Pair<String, String>> devices) {
            super(parentShell);

            mDevices = devices;
            mDeviceNames = new String[mDevices.size()];
            for (int index = 0; index < devices.size(); index++) {
                mDeviceNames[index] = String.format("%s-%s",
                                                    devices.get(index).getValue(),
                                                    devices.get(index).getKey());
            }
        }

        @Override
        protected Control createDialogArea(Composite parentShell) {
            Composite parentComposite = (Composite) super.createDialogArea(parentShell);
            Composite composite = new Composite(parentComposite, SWT.NONE);

            composite.setLayout(new GridLayout(2, false));

            Label label = new Label(composite, SWT.NONE);
            label.setText("Select device: ");

            final Combo combo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
            combo.setItems(mDeviceNames);
            int defaultSelection = sSelectedDeviceIndex < mDevices.size() ? sSelectedDeviceIndex : 0;
            combo.select(defaultSelection);
            sSelectedDeviceIndex = defaultSelection;

            combo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    sSelectedDeviceIndex = combo.getSelectionIndex();
                }
            });

            return parentComposite;
        }

        /**
         * Gets the serial number of the selected device.
         * 
         * @return {@link String} - representing the selected device serial number
         */
        public String getSelectedDevice() {
            return mDevices.get(sSelectedDeviceIndex).getKey();
        }

    }
}
