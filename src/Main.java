import com.esri.core.geodatabase.Geodatabase;
import com.esri.core.geodatabase.GeodatabaseFeatureTable;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.tasks.query.QueryParameters;
import com.esri.map.FeatureLayer;
import com.esri.map.JMap;
import com.esri.map.LocationOnMap;
import com.esri.toolkit.bookmarks.JExtentBookmark;
import com.esri.toolkit.editing.JEditToolsPicker;
import com.esri.toolkit.editing.JTemplatePicker;
import com.esri.toolkit.legend.JLegend;
import com.esri.toolkit.overlays.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;

public class Main extends JFrame {
    public static void main(String[] args) {
        JFrame window = new MyArcMap();
        window.setVisible(true);
        window.setSize(1000, 800);
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}

class MyArcMap extends JFrame {
    private static final String PATH_TO_GEODATABASE = "C:\\Users\\Mariana\\Desktop\\zavd_new.geodatabase";
    final JMap map = new JMap();
    private static final int LAYER_ID = 1;
    private final String[] ATTR_NAMES =
            new String[]{"NUMBER", "AREA", "ZONE", "Z1", "Z2", "Z3", "Z4", "TYPE", "PERIMETER"};
    private final String[] ATTR_HEADER =
            new String[]{"Number", "Area", "Zone", "Z1", "Z2", "Z3", "Z4", "Type", "Perimeter"};

    private GeodatabaseFeatureTable table;
    Geodatabase geodatabase;

    NavigatorOverlay navigator = new NavigatorOverlay();
    private Container contentPane;


    public MyArcMap() throws HeadlessException {
        contentPane = getContentPane();
        contentPane.add(map, "Center");

        try {
            geodatabase = new Geodatabase(PATH_TO_GEODATABASE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        GeodatabaseFeatureTable geodatabaseFeatureTable;
        GeodatabaseFeatureTable geodatabaseFeatureTable2;

        geodatabaseFeatureTable = geodatabase.getGeodatabaseFeatureTableByLayerId(1);
        geodatabaseFeatureTable2 = geodatabase.getGeodatabaseFeatureTableByLayerId(0);

        FeatureLayer featureLayer = new FeatureLayer(geodatabaseFeatureTable);
        FeatureLayer featureLayer2 = new FeatureLayer(geodatabaseFeatureTable2);


        navigator.setLocation(LocationOnMap.TOP_LEFT);

        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay();
        scaleBarOverlay.setLocation(LocationOnMap.BOTTOM_LEFT);
        map.addMapOverlay(scaleBarOverlay);

        map.getLayers().add(featureLayer);
        map.getLayers().add(featureLayer2);
        map.addMapOverlay(navigator);

        JLegend legend = new JLegend(map);
        legend.setPreferredSize(new Dimension(150, 700));
        contentPane.add(legend, BorderLayout.WEST);

        JExtentBookmark extentBookmarks = new JExtentBookmark(map, "extent.bookmarks");
//            extentBookmarks.setLocation(10, 10);
        extentBookmarks.setPreferredSize(new Dimension(150, 40));
        contentPane.add(extentBookmarks, BorderLayout.EAST);
        JLabel selectedLabel = new JLabel();


        class GraphicSelectedListener implements HitTestListener {
            @Override
            public void featureHit(HitTestEvent event) {
                java.util.List<Feature> hitGraphics = event.getOverlay().getHitFeatures();
                for (Feature graphic : hitGraphics) {
                    int id = (int) graphic.getId();
                    if (featureLayer.isFeatureSelected(id)) {
                        featureLayer.unselectFeature(id);
                        selectedLabel.setText(" ");
                    } else {
                        featureLayer.selectFeature(id);
                        selectedLabel.setText("Area " + featureLayer.getSelectedFeatures().get(0).getAttributeValue("AREA").toString());
                    }
                }
            }
        }
        final HitTestOverlay overlay = new HitTestOverlay(featureLayer);
        overlay.addHitTestListener(new GraphicSelectedListener());
        map.addMapOverlay(overlay);

        JButton openAtrTableButton = new JButton("Open Attributive Table");
        JButton clearSelectionButton = new JButton("Clear Selection");

        clearSelectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                featureLayer.clearSelection();
                selectedLabel.setText("");
            }
        });

        openAtrTableButton.setMaximumSize(new Dimension(10, 10));
        openAtrTableButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame appWindow = createWindow();
                try {
                    appWindow.add(createUI());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                appWindow.setVisible(true);
            }
        });


        JPanel panel = new JPanel();
        panel.add(selectedLabel);
        panel.add(openAtrTableButton);
        panel.add(clearSelectionButton);

        contentPane.add(panel, "North");
//        contentPane.add(openAtrTableButton);

        JTemplatePicker jPicker = new JTemplatePicker(map);
        //panel.add(jPicker, BorderLayout.WEST);
        jPicker.setIconWidth(40);
        jPicker.setIconHeight(30);
        jPicker.setShowNames(true);
        jPicker.setWatchMap(true);
        // show an attribute editor when a feature is added
        jPicker.setShowAttributeEditor(true);


        JEditToolsPicker editToolsPicker = new JEditToolsPicker(map);
        editToolsPicker.setCreationOverlay(jPicker.getOverlay());
        //panel.add(editToolsPicker, BorderLayout.NORTH);

    }


    //***********************
    private void executeQueryInBatch(JTextField txtQuery, final DefaultTableModel tblModelQueryResult) {

        String damageType = txtQuery.getText();
        if (table == null || damageType == null || damageType.isEmpty()) {
            return;
        }

        if (tblModelQueryResult.getColumnCount() == 0) {
            for (String attrHeader : ATTR_HEADER) {
                tblModelQueryResult.addColumn(attrHeader);
            }
        }
        tblModelQueryResult.setRowCount(0);

        QueryParameters query = new QueryParameters();
        query.setOutFields(ATTR_NAMES);
        query.setWhere(damageType);

        table.queryFeatures(query, new CallbackListener<FeatureResult>() {

            @Override
            public void onError(Throwable e) {
                JOptionPane.showMessageDialog(contentPane, wrap("Error: " + e.getLocalizedMessage()), "", JOptionPane.ERROR_MESSAGE);
            }

            @Override
            public void onCallback(FeatureResult objs) {
                for (Object objFeature : objs) {
                    Feature feature = (Feature) objFeature;

                    Object[] rowData = new Object[ATTR_NAMES.length];
                    int index = 0;
                    for (String attrName : ATTR_NAMES) {
                        rowData[index++] = feature.getAttributeValue(attrName);
                    }
                    tblModelQueryResult.addRow(rowData);
                }
            }
        });
    }
    //***********************

    private static String wrap(String str) {
        // create a HTML string that wraps text when longer
        return "<html><p style='width:200px;'>" + str + "</html>";
    }

    public Container createUI() throws Exception {
        Container contentPane2 = new JPanel();
        contentPane2.setLayout(new BorderLayout());


        JTextArea lblQuery = new JTextArea("");
        lblQuery.append("");
        lblQuery.setEditable(false);
        lblQuery.setForeground(Color.BLACK);
        lblQuery.setBackground(null);
        lblQuery.setMaximumSize(new Dimension(600, 20));

        final JTextField txtQuery = new JTextField();
        txtQuery.setText("AREA > 2");
        txtQuery.setMaximumSize(new Dimension(350, 20));

        final DefaultTableModel tblModelQueryResult = new DefaultTableModel();
        JTable tablQueryResult = new JTable(tblModelQueryResult);
        JScrollPane tblQueryResultScrollable = new JScrollPane(tablQueryResult);



        JButton btnQuery = new JButton("Query");
        btnQuery.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeQueryInBatch(txtQuery, tblModelQueryResult);
            }
        });
        btnQuery.setMaximumSize(new Dimension(150, 20));
        btnQuery.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JPanel controlPanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(controlPanel, BoxLayout.Y_AXIS);
        controlPanel.setLayout(boxLayout);
        controlPanel.setBorder(new LineBorder(new Color(0, 0, 0, 100), 5, false));

        controlPanel.add(lblQuery);
        controlPanel.add(txtQuery);
        controlPanel.add(btnQuery);

        controlPanel.add(tblQueryResultScrollable);

        contentPane2.add(controlPanel);

        try {
            table = geodatabase.getGeodatabaseFeatureTableByLayerId(LAYER_ID);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(contentPane2,
                    wrap("Error: " + e.getLocalizedMessage()) + "\r\nSee notes for this application.");
        }

        return contentPane2;
    }

    private JFrame createWindow() {
        JFrame window1 = new JFrame();
        window1.setBounds(100, 100, 1000, 700);
        window1.setDefaultCloseOperation(window1.DISPOSE_ON_CLOSE);
        window1.getContentPane().setLayout(new BorderLayout(0, 0));
        window1.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                super.windowClosing(windowEvent);
                //if (geodatabase != null) geodatabase.dispose();
            }
        });
        return window1;
    }

}



