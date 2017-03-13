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

package com.android.uiautomator;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;

import com.android.uiautomator.actions.ExpandAllAction;
import com.android.uiautomator.actions.ImageHelper;
import com.android.uiautomator.actions.ToggleNafAction;
import com.android.uiautomator.tree.AttributePair;
import com.android.uiautomator.tree.BasicTreeNode;
import com.android.uiautomator.tree.BasicTreeNodeContentProvider;
import com.android.uiautomator.tree.UiNode;
import com.musala.atmosphere.client.uiutils.ViewerCommunicator;

public class UiAutomatorView extends Composite {
    private static final int IMG_BORDER = 2;

    private static final int SCREEN_REFRESH_TIMEOUT = 300;

    private Composite mScreenshotComposite;

    private StackLayout mStackLayout;

    private Composite mSetScreenshotComposite;

    private Canvas mScreenshotCanvas;

    private TreeViewer mTreeViewer;

    private TableViewer mTableViewer;

    private float mScale = 1.0f;

    private int mDx, mDy;

    private UiAutomatorModel mModel;

    private Image mScreenshot;

    private List<BasicTreeNode> mSearchResult;

    private int mSearchResultIndex;

    private boolean isFirstScreenshotAction;

    private boolean isFirstRemoteControlAction;

    private String currentDeviceSN;

    private ToolItem itemDeleteAndInfo;

    private Text searchTextarea;

    private Cursor mOrginialCursor;

    private ToolItem itemPrev, itemNext;

    private ToolItem coordinateLabel;

    private String mLastSearchedTerm;

    private Cursor mCrossCursor;

    private MouseMoveListener showCoordinatesListener;

    private MouseAdapter toggleListener;

    private MouseAdapter tapListener;

    private PaintListener paintListener;

    private ViewerCommunicator vCommunicator;

    private Thread screenRefresherThread;

    private final Runnable screenRedrawRunnable = new Runnable() {
        public void run() {
            refreshScreenshot();
        }
    };;

    public UiAutomatorView(Composite parent, int style) {
        super(parent, SWT.NONE);
        setLayout(new FillLayout());

        vCommunicator = new ViewerCommunicator();
        isFirstScreenshotAction = true;
        isFirstRemoteControlAction = true;

        SashForm baseSash = new SashForm(this, SWT.HORIZONTAL);
        mOrginialCursor = getShell().getCursor();
        mCrossCursor = new Cursor(getDisplay(), SWT.CURSOR_CROSS);
        mScreenshotComposite = new Composite(baseSash, SWT.BORDER);
        mStackLayout = new StackLayout();
        mScreenshotComposite.setLayout(mStackLayout);

        // draw the canvas with border, so the divider area for sash form can be highlighted
        mScreenshotCanvas = new Canvas(mScreenshotComposite, SWT.BORDER);
        mStackLayout.topControl = mScreenshotCanvas;
        mScreenshotComposite.layout();

        mScreenshotCanvas.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        mSetScreenshotComposite = new Composite(mScreenshotComposite, SWT.NONE);
        mSetScreenshotComposite.setLayout(new GridLayout());

        initializeListeners();

        // right sash is split into 2 parts: upper-right and lower-right
        // both are composites with borders, so that the horizontal divider can be highlighted by
        // the borders
        SashForm rightSash = new SashForm(baseSash, SWT.VERTICAL);

        // upper-right base contains the toolbar and the tree
        Composite upperRightBase = new Composite(rightSash, SWT.BORDER);
        upperRightBase.setLayout(new GridLayout(1, false));

        ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
        toolBarManager.add(new ExpandAllAction(this));
        toolBarManager.add(new ToggleNafAction(this));

        ToolBar searchtoolbar = toolBarManager.createControl(upperRightBase);

        // add search box and navigation buttons for search results
        ToolItem itemSeparator = new ToolItem(searchtoolbar, SWT.SEPARATOR | SWT.RIGHT);

        searchTextarea = new Text(searchtoolbar, SWT.BORDER | SWT.SINGLE | SWT.SEARCH);
        searchTextarea.pack();

        itemSeparator.setWidth(searchTextarea.getBounds().width);
        itemSeparator.setControl(searchTextarea);

        itemPrev = new ToolItem(searchtoolbar, SWT.SIMPLE);
        itemPrev.setImage(ImageHelper.loadImageDescriptorFromResource("resources/prev.png").createImage());

        itemNext = new ToolItem(searchtoolbar, SWT.SIMPLE);
        itemNext.setImage(ImageHelper.loadImageDescriptorFromResource("resources/next.png").createImage());

        itemDeleteAndInfo = new ToolItem(searchtoolbar, SWT.SIMPLE);
        itemDeleteAndInfo.setImage(ImageHelper.loadImageDescriptorFromResource("resources/delete.png").createImage());
        itemDeleteAndInfo.setToolTipText("Clear search results");

        coordinateLabel = new ToolItem(searchtoolbar, SWT.SIMPLE);
        coordinateLabel.setText("");
        coordinateLabel.setEnabled(false);

        // add search function
        searchTextarea.addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent event) {
                if (event.keyCode == SWT.CR) {
                    String term = searchTextarea.getText();

                    if (!term.isEmpty()) {
                        if (term.equals(mLastSearchedTerm)) {
                            nextSearchResult();
                            return;
                        }

                        clearSearchResult();

                        mSearchResult = mModel.searchNode(term);
                        if (!mSearchResult.isEmpty()) {
                            mSearchResultIndex = 0;

                            updateSearchResultSelection();

                            mLastSearchedTerm = term;
                        }
                    }
                }
            }

            @Override
            public void keyPressed(KeyEvent event) {
            }
        });

        SelectionListener selectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                if (selectionEvent.getSource() == itemPrev) {
                    prevSearchResult();
                } else if (selectionEvent.getSource() == itemNext) {
                    nextSearchResult();
                } else if (selectionEvent.getSource() == itemDeleteAndInfo) {
                    searchTextarea.setText("");
                    clearSearchResult();
                }
            }
        };

        itemPrev.addSelectionListener(selectionListener);
        itemNext.addSelectionListener(selectionListener);
        itemDeleteAndInfo.addSelectionListener(selectionListener);

        searchtoolbar.pack();
        searchtoolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mTreeViewer = new TreeViewer(upperRightBase, SWT.NONE);
        mTreeViewer.setContentProvider(new BasicTreeNodeContentProvider());

        // default LabelProvider uses toString() to generate text to display
        mTreeViewer.setLabelProvider(new LabelProvider());
        mTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent selectionChangedEvent) {
                BasicTreeNode selectedNode = null;

                if (selectionChangedEvent.getSelection() instanceof IStructuredSelection) {
                    IStructuredSelection selection = (IStructuredSelection) selectionChangedEvent.getSelection();
                    Object firstElement = selection.getFirstElement();

                    if (firstElement instanceof BasicTreeNode) {
                        selectedNode = (BasicTreeNode) firstElement;
                    }
                }

                mModel.setSelectedNode(selectedNode);

                redrawScreenshot();

                if (selectedNode != null) {
                    loadAttributeTable();
                }
            }
        });

        Tree tree = mTreeViewer.getTree();
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        // move focus so that it's not on tool bar (looks weird)
        tree.setFocus();

        // lower-right base contains the detail group
        Composite lowerRightBase = new Composite(rightSash, SWT.BORDER);
        lowerRightBase.setLayout(new FillLayout());

        Group grpNodeDetail = new Group(lowerRightBase, SWT.NONE);
        grpNodeDetail.setLayout(new FillLayout(SWT.HORIZONTAL));
        grpNodeDetail.setText("Node Detail");

        Composite tableContainer = new Composite(grpNodeDetail, SWT.NONE);

        TableColumnLayout columnLayout = new TableColumnLayout();
        tableContainer.setLayout(columnLayout);

        mTableViewer = new TableViewer(tableContainer, SWT.NONE | SWT.FULL_SELECTION);
        Table table = mTableViewer.getTable();
        table.setLinesVisible(true);
        // use ArrayContentProvider here, it assumes the input to the TableViewer
        // is an array, where each element represents a row in the table
        mTableViewer.setContentProvider(new ArrayContentProvider());

        TableViewerColumn tableViewerColumnKey = new TableViewerColumn(mTableViewer, SWT.NONE);
        TableColumn tblclmnKey = tableViewerColumnKey.getColumn();

        tableViewerColumnKey.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof AttributePair) {
                    // first column, shows the attribute name
                    return ((AttributePair) element).key;
                }
                return super.getText(element);
            }
        });

        columnLayout.setColumnData(tblclmnKey, new ColumnWeightData(1, ColumnWeightData.MINIMUM_WIDTH, true));

        TableViewerColumn tableViewerColumnValue = new TableViewerColumn(mTableViewer, SWT.NONE);
        tableViewerColumnValue.setEditingSupport(new AttributeTableEditingSupport(mTableViewer));

        TableColumn tblclmnValue = tableViewerColumnValue.getColumn();
        columnLayout.setColumnData(tblclmnValue, new ColumnWeightData(2, ColumnWeightData.MINIMUM_WIDTH, true));
        tableViewerColumnValue.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof AttributePair) {
                    // second column, shows the attribute value
                    return ((AttributePair) element).value;
                }

                return super.getText(element);
            }
        });

        // sets the ratio of the vertical split: left 5 vs right 3
        baseSash.setWeights(new int[] {5, 3});
    }

    protected void prevSearchResult() {
        if (mSearchResult == null)
            return;
        if (mSearchResult.isEmpty()) {
            mSearchResult = null;
            return;
        }
        mSearchResultIndex = mSearchResultIndex - 1;
        if (mSearchResultIndex < 0) {
            mSearchResultIndex += mSearchResult.size();
        }
        updateSearchResultSelection();
    }

    protected void clearSearchResult() {
        itemDeleteAndInfo.setText("");
        mSearchResult = null;
        mSearchResultIndex = 0;
        mLastSearchedTerm = "";
        mScreenshotCanvas.redraw();
    }

    protected void nextSearchResult() {
        if (mSearchResult == null)
            return;
        if (mSearchResult.isEmpty()) {
            mSearchResult = null;
            return;
        }
        mSearchResultIndex = (mSearchResultIndex + 1) % mSearchResult.size();
        updateSearchResultSelection();
    }

    private void updateSearchResultSelection() {
        updateTreeSelection(mSearchResult.get(mSearchResultIndex));
        itemDeleteAndInfo.setText("" + (mSearchResultIndex + 1) + "/" + mSearchResult.size());
    }

    private int getScaledSize(int size) {
        if (mScale == 1.0f) {
            return size;
        } else {
            return new Double(Math.floor((size * mScale))).intValue();
        }
    }

    private int getInverseScaledSize(int size) {
        if (mScale == 1.0f) {
            return size;
        } else {
            return new Double(Math.floor((size / mScale))).intValue();
        }
    }

    private void updateScreenshotTransformation() {
        Rectangle canvas = mScreenshotCanvas.getBounds();
        Rectangle image = mScreenshot.getBounds();
        float scaleX = (canvas.width - 2 * IMG_BORDER - 1) / (float) image.width;
        float scaleY = (canvas.height - 2 * IMG_BORDER - 1) / (float) image.height;

        // use the smaller scale here so that we can fit the entire screenshot
        mScale = Math.min(scaleX, scaleY);
        // calculate translation values to center the image on the canvas
        mDx = (canvas.width - getScaledSize(image.width) - IMG_BORDER * 2) / 2 + IMG_BORDER;
        mDy = (canvas.height - getScaledSize(image.height) - IMG_BORDER * 2) / 2 + IMG_BORDER;
    }

    private class AttributeTableEditingSupport extends EditingSupport {

        private TableViewer mViewer;

        public AttributeTableEditingSupport(TableViewer viewer) {
            super(viewer);
            mViewer = viewer;
        }

        @Override
        protected boolean canEdit(Object arg0) {
            return true;
        }

        @Override
        protected CellEditor getCellEditor(Object arg0) {
            return new TextCellEditor(mViewer.getTable());
        }

        @Override
        protected Object getValue(Object o) {
            return ((AttributePair) o).value;
        }

        @Override
        protected void setValue(Object arg0, Object arg1) {
        }
    }

    /**
     * Causes a redraw of the canvas.
     *
     * The drawing code of canvas will handle highlighted nodes and etc based on data retrieved from Model
     */
    public void redrawScreenshot() {
        if (mScreenshot == null) {
            mStackLayout.topControl = mSetScreenshotComposite;
        } else {
            mStackLayout.topControl = mScreenshotCanvas;
        }
        mScreenshotComposite.layout();

        mScreenshotCanvas.redraw();
    }

    public void setInputHierarchy(Object input) {
        mTreeViewer.setInput(input);
    }

    public void loadAttributeTable() {
        // update the lower right corner table to show the attributes of the node
        mTableViewer.setInput(mModel.getSelectedNode().getAttributesArray());
    }

    public void expandAll() {
        mTreeViewer.expandAll();
    }

    public void updateTreeSelection(BasicTreeNode node) {
        mTreeViewer.setSelection(new StructuredSelection(node), true);
    }

    /**
     * Sets the appropriate listeners, the screenshot and the hierarchy for the UI hierarchy view.
     * 
     * @param model
     *        - the UI Automator model created for the current UI hierarchy
     * @param screenshot
     *        - the given screenshot image
     */
    public void setUiHierarchyModel(UiAutomatorModel model, Image screenshot) {
        if (screenRefresherThread != null && screenRefresherThread.isAlive()) {
            screenRefresherThread.interrupt();
        }

        if (isFirstScreenshotAction) {
            addScreenshotActionListeners();
            isFirstScreenshotAction = false;
            isFirstRemoteControlAction = true;

            if (tapListener != null) {
                mScreenshotCanvas.removeMouseListener(tapListener);
            }
        }

        mModel = model;

        if (mScreenshot != null) {
            mScreenshot.dispose();
        }
        mScreenshot = screenshot;

        clearSearchResult();

        redrawScreenshot();

        // load xml into tree
        BasicTreeNode wrapper = new BasicTreeNode();
        // putting another root node on top of existing root node
        // because Tree seems to like to hide the root node
        wrapper.addChild(mModel.getXmlRootNode());
        setInputHierarchy(wrapper);
        mTreeViewer.getTree().setFocus();
    }

    /**
     * Sets the appropriate listeners for the remote control view.
     * 
     * @param deviceSN
     *        - the serial number of the device that will be remotely controlled
     */
    public void setRemoteControlModel(String deviceSN) {
        if (isFirstRemoteControlAction || !currentDeviceSN.equals(deviceSN)) {
            mModel = null;
            this.currentDeviceSN = deviceSN;

            isFirstScreenshotAction = true;
            isFirstRemoteControlAction = false;

            if (toggleListener != null) {
                mScreenshotCanvas.removeMouseListener(toggleListener);
            }
            if (showCoordinatesListener != null) {
                mScreenshotCanvas.removeMouseMoveListener(showCoordinatesListener);
            }

            addRemoteControlActionListeners();
        }

        screenRefresherThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(SCREEN_REFRESH_TIMEOUT);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }

                    Display.getDefault().syncExec(screenRedrawRunnable);
                }
            }
        });

        screenRefresherThread.start();
    }

    private void refreshScreenshot() {
        Image image = null;

        File screenshot = new File(vCommunicator.getScreenshot(currentDeviceSN));
        if (screenshot != null) {
            try {
                ImageData[] data = new ImageLoader().load(screenshot.getAbsolutePath());

                if (data.length < 1) {
                    throw new RuntimeException("Unable to load image: " + screenshot.getAbsolutePath());
                }

                image = new Image(Display.getDefault(), data[0]);

            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        if (mScreenshot != null) {
            mScreenshot.dispose();
        }

        mScreenshot = image;

        redrawScreenshot();
    }

    private void addRemoteControlActionListeners() {
        mScreenshotCanvas.addMouseListener(tapListener);
    }

    private void addScreenshotActionListeners() {
        mScreenshotCanvas.addMouseListener(toggleListener);
        mScreenshotCanvas.addMouseMoveListener(showCoordinatesListener);
    }

    private void initializeListeners() {
        mScreenshotCanvas.addListener(SWT.MouseEnter, new Listener() {
            @Override
            public void handleEvent(Event arg0) {
                getShell().setCursor(mCrossCursor);
            }
        });

        mScreenshotCanvas.addListener(SWT.MouseExit, new Listener() {
            @Override
            public void handleEvent(Event arg0) {
                getShell().setCursor(mOrginialCursor);
            }
        });

        tapListener = new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent mouseEvent) {
                int x = getInverseScaledSize(mouseEvent.x - mDx);
                int y = getInverseScaledSize(mouseEvent.y - mDy);
                vCommunicator.tapScreen(currentDeviceSN, x, y);
            }
        };

        showCoordinatesListener = new MouseMoveListener() {

            @Override
            public void mouseMove(MouseEvent mouseEvent) {
                if (mModel != null) {
                    int x = getInverseScaledSize(mouseEvent.x - mDx);
                    int y = getInverseScaledSize(mouseEvent.y - mDy);
                    // show coordinate
                    coordinateLabel.setText(String.format("(%d,%d)", x, y));
                    if (mModel.isExploreMode()) {
                        BasicTreeNode node = mModel.updateSelectionForCoordinates(x, y);
                        if (node != null) {
                            updateTreeSelection(node);
                        }
                    }
                }
            }
        };

        toggleListener = new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                if (mModel != null) {
                    mModel.toggleExploreMode();
                    redrawScreenshot();
                }
            }
        };

        paintListener = new PaintListener() {
            @Override
            public void paintControl(PaintEvent paintEvent) {
                if (mScreenshot != null) {
                    updateScreenshotTransformation();

                    // shifting the image here, so that there's a border around screen shot
                    // this makes highlighting red rectangles on the screen shot edges more visible
                    Transform transform = new Transform(paintEvent.gc.getDevice());
                    transform.translate(mDx, mDy);
                    transform.scale(mScale, mScale);

                    paintEvent.gc.setTransform(transform);
                    paintEvent.gc.drawImage(mScreenshot, 0, 0);

                    // this resets the transformation to identity transform, i.e. no change
                    // we don't use transformation here because it will cause the line pattern
                    // and line width of highlight rect to be scaled, causing to appear to be blurry
                    paintEvent.gc.setTransform(null);

                    if (mModel != null) {
                        if (mModel.shouldShowNafNodes()) {
                            // highlight the "Not Accessibility Friendly" nodes
                            paintEvent.gc.setForeground(paintEvent.gc.getDevice().getSystemColor(SWT.COLOR_YELLOW));
                            paintEvent.gc.setBackground(paintEvent.gc.getDevice().getSystemColor(SWT.COLOR_YELLOW));

                            for (Rectangle r : mModel.getNafNodes()) {
                                paintEvent.gc.setAlpha(50);
                                paintEvent.gc.fillRectangle(mDx + getScaledSize(r.x),
                                                            mDy + getScaledSize(r.y),
                                                            getScaledSize(r.width),
                                                            getScaledSize(r.height));
                                paintEvent.gc.setAlpha(255);
                                paintEvent.gc.setLineStyle(SWT.LINE_SOLID);
                                paintEvent.gc.setLineWidth(2);
                                paintEvent.gc.drawRectangle(mDx + getScaledSize(r.x),
                                                            mDy + getScaledSize(r.y),
                                                            getScaledSize(r.width),
                                                            getScaledSize(r.height));
                            }
                        }

                        // draw the search result rects
                        if (mSearchResult != null) {
                            for (BasicTreeNode result : mSearchResult) {
                                if (result instanceof UiNode) {
                                    UiNode uiNode = (UiNode) result;

                                    Rectangle rect = new Rectangle(uiNode.x, uiNode.y, uiNode.width, uiNode.height);
                                    paintEvent.gc.setForeground(paintEvent.gc.getDevice()
                                                                             .getSystemColor(SWT.COLOR_YELLOW));
                                    paintEvent.gc.setLineStyle(SWT.LINE_DASH);
                                    paintEvent.gc.setLineWidth(1);
                                    paintEvent.gc.drawRectangle(mDx + getScaledSize(rect.x),
                                                                mDy + getScaledSize(rect.y),
                                                                getScaledSize(rect.width),
                                                                getScaledSize(rect.height));
                                }
                            }
                        }

                        // draw the mouseover rects
                        Rectangle rect = mModel.getCurrentDrawingRect();
                        if (rect != null) {
                            paintEvent.gc.setForeground(paintEvent.gc.getDevice().getSystemColor(SWT.COLOR_RED));
                            if (mModel.isExploreMode()) {
                                // when we highlight nodes dynamically on mouse move,
                                // use dashed borders
                                paintEvent.gc.setLineStyle(SWT.LINE_DASH);
                                paintEvent.gc.setLineWidth(1);
                            } else {
                                // when highlighting nodes on tree node selection,
                                // use solid borders
                                paintEvent.gc.setLineStyle(SWT.LINE_SOLID);
                                paintEvent.gc.setLineWidth(2);
                            }
                            paintEvent.gc.drawRectangle(mDx + getScaledSize(rect.x),
                                                        mDy + getScaledSize(rect.y),
                                                        getScaledSize(rect.width),
                                                        getScaledSize(rect.height));
                        }
                    }
                }
            }
        };

        mScreenshotCanvas.addPaintListener(paintListener);
    }

    public boolean shouldShowNafNodes() {
        return mModel != null ? mModel.shouldShowNafNodes() : false;
    }

    public void toggleShowNaf() {
        if (mModel != null) {
            mModel.toggleShowNaf();
        }
    }
}
