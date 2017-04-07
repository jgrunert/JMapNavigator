// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapnavigator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.Timer;

import org.openstreetmap.gui.jmapnavigator.IRouteSolver.RoutingState;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.JMapViewerTree;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.events.JMVCommandEvent;
import org.openstreetmap.gui.jmapviewer.interfaces.JMapViewerEventListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

/**
 * Map navigator main for JMapNavigator.
 *
 * @author Jonas Grunert
 *
 */
public class WorkerVertexVisualizerMain extends JFrame implements JMapViewerEventListener {

	private static final long serialVersionUID = 1L;

	private final JMapViewerTree treeMap;
	private final JMapNavigatorMapController mapController;

	private final JLabel zoomLabel;
	private final JLabel zoomValue;

	private final JLabel mperpLabelName;
	private final JLabel mperpLabelValue;

	//	private final JLabel routeDistLabel;
	private final JLabel routeTimeLabel;

	private List<MapMarkerDot> routeDots = new ArrayList<>();
	private List<MapPolygonImpl> routeLines = new ArrayList<>();

	private static final int MAX_ROUTE_PREVIEW_DOTS = 50;

	private String selectedVertexDirectory = null;
	private int workerCount = 8; // TODO Worker count
	private String vertexStatFileName;
	private JScrollBar vertexSampleScrollBar;



	/**
	 * Constructs the {@code Demo}.
	 */
	public WorkerVertexVisualizerMain(String roadGraphFile) {
		super("JMapViewer Demo");
		setSize(400, 400);

		treeMap = new JMapViewerTree("Zones", roadGraphFile);
		mapController = treeMap.getMapController();

		// Listen to the map viewer for user operations so components will
		// receive events and update
		map().addJMVListener(this);

		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		JPanel panel = new JPanel();
		JPanel panelTop = new JPanel();
		JPanel panelBottom = new JPanel();
		JPanel helpPanel = new JPanel();

		mperpLabelName = new JLabel("Meters/Pixels: ");
		mperpLabelValue = new JLabel(String.format("%s", map().getMeterPerPixel()));

		zoomLabel = new JLabel("Zoom: ");
		zoomValue = new JLabel(String.format("%s", map().getZoom()));

		add(panel, BorderLayout.NORTH);
		add(helpPanel, BorderLayout.SOUTH);
		panel.add(panelTop, BorderLayout.NORTH);
		panel.add(panelBottom, BorderLayout.SOUTH);
		JLabel helpLabel = new JLabel("Use right mouse button to move,\n " + "left double click or mouse wheel to zoom.");
		helpPanel.add(helpLabel);
		JButton button = new JButton("FitMapMarkers");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				map().setDisplayToFitMapMarkers();
			}
		});
		JComboBox<TileSource> tileSourceSelector = new JComboBox<>(
				new TileSource[] { new OsmTileSource.Mapnik(), new OsmTileSource.CycleMap(), new BingAerialTileSource(), });
		tileSourceSelector.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				map().setTileSource((TileSource) e.getItem());
			}
		});
		JComboBox<TileLoader> tileLoaderSelector;
		tileLoaderSelector = new JComboBox<>(new TileLoader[] { new OsmTileLoader(map()) });
		tileLoaderSelector.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				map().setTileLoader((TileLoader) e.getItem());
			}
		});
		map().setTileLoader((TileLoader) tileLoaderSelector.getSelectedItem());
		panelTop.add(tileSourceSelector);
		panelTop.add(tileLoaderSelector);


		final JCheckBox showTileGrid = new JCheckBox("Tile grid visible");
		showTileGrid.setSelected(map().isTileGridVisible());
		showTileGrid.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				map().setTileGridVisible(showTileGrid.isSelected());
			}
		});
		panelBottom.add(showTileGrid);
		final JCheckBox showZoomControls = new JCheckBox("Show zoom controls");
		showZoomControls.setSelected(map().getZoomControlsVisible());
		showZoomControls.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				map().setZoomContolsVisible(showZoomControls.isSelected());
			}
		});
		panelBottom.add(showZoomControls);

		panelBottom.add(button);

		panelTop.add(zoomLabel);
		panelTop.add(zoomValue);
		panelTop.add(mperpLabelName);
		panelTop.add(mperpLabelValue);

		add(treeMap, BorderLayout.CENTER);

		routeTimeLabel = new JLabel("0:00");
		panelBottom.add(routeTimeLabel);

		panelTop.add(zoomLabel);
		panelTop.add(zoomValue);
		panelTop.add(mperpLabelName);
		panelTop.add(mperpLabelValue);

		JButton buttonCalcManiacShort = new JButton("Shortest path");
		buttonCalcManiacShort.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				mapController.getRouteSolver().startCalculateRoute(true);
			}
		});
		panelBottom.add(buttonCalcManiacShort);


		JButton loadWorkerVertices = new JButton("Load WorkerVertices");
		loadWorkerVertices.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				vertexStatFileName = "_allVertexStats.txt";
				loadNewWorkerVertices();
			}
		});
		panelBottom.add(loadWorkerVertices);

		JButton loadWorkerVertices2 = new JButton("Load ActiveVertices");
		loadWorkerVertices2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				vertexStatFileName = "_actVertexStats.txt";
				loadNewWorkerVertices();
			}
		});
		panelBottom.add(loadWorkerVertices2);

		vertexSampleScrollBar = new JScrollBar(0, 0, 0, 0, 0);
		vertexSampleScrollBar.addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(AdjustmentEvent arg0) {
				if (selectedVertexDirectory != null) {
					loadWorkerVerticesSample(selectedVertexDirectory, workerCount, vertexSampleScrollBar.getValue());
				}
			}

		});
		vertexSampleScrollBar.setPreferredSize(new Dimension(200, 20));
		panelBottom.add(vertexSampleScrollBar);


		add(treeMap, BorderLayout.CENTER);

		map().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					map().getAttribution().handleAttribution(e.getPoint(), true);
				}
			}
		});

		map().addMouseMotionListener(new MouseAdapter() {

			@Override
			public void mouseMoved(MouseEvent e) {
				Point p = e.getPoint();
				boolean cursorHand = map().getAttribution().handleAttributionCursor(p);
				if (cursorHand) {
					map().setCursor(new Cursor(Cursor.HAND_CURSOR));
				}
				else {
					map().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
				//				if (showToolTip.isSelected()) map().setToolTipText(map().getPosition(p).toString());
			}
		});


		// Poll timer
		Timer timer = new Timer(500, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (mapController.getRouteSolver().getRoutingState() == RoutingState.Routing
						|| mapController.getRouteSolver().getNeedsDispalyRefresh()) {
					refreshRouteDisplay();
					mapController.getRouteSolver().resetNeedsDispalyRefresh();
				}
			}
		});
		timer.start();
	}


	private void clearRouteDisplay() {
		// Clear dots
		for (MapMarkerDot dot : routeDots) {
			map().removeMapMarker(dot);
		}
		routeDots.clear();

		// Clear lines
		for (MapPolygonImpl line : routeLines) {
			map().removeMapPolygon(line);
		}
		routeLines.clear();

		// Display start and target
		MapMarkerDot start = new MapMarkerDot("Start", Color.BLUE, mapController.getRouteSolver().getStartCoordinate());
		MapMarkerDot targ = new MapMarkerDot("Target", Color.RED, mapController.getRouteSolver().getTargetCoordinate());
		map().addMapMarker(start);
		map().addMapMarker(targ);
		routeDots.add(start);
		routeDots.add(targ);
	}

	private void refreshRouteDisplay() {
		clearRouteDisplay();

		if (mapController.getRouteSolver().getRoutingState() == RoutingState.Standby) {
			Coordinate lastCoord = null;
			for (Coordinate coord : mapController.getRouteSolver().getCalculatedRoute()) {
				if (lastCoord != null) {
					MapPolygonImpl routPoly = new MapPolygonImpl(Color.BLUE, lastCoord, coord, coord);
					routeLines.add(routPoly);
					map().addMapPolygon(routPoly);
				}
				lastCoord = coord;
			}

			//			routeDistLabel.setText(((int) mapController.getRouteSolver().getDistOfRoute() / 1000.0f) + " km");
			double timeOfRouteHours = mapController.getRouteSolver().getTimeOfRoute() / 3600;
			int timeHours = (int) timeOfRouteHours;
			int timeMinutes = (int) (60 * (timeOfRouteHours - timeHours));
			routeTimeLabel.setText(timeHours + ":" + timeMinutes + " h");
		}


		if (mapController.getRouteSolver().getRoutingState() != RoutingState.NotReady) {
			List<Coordinate> routingPreviewDots = mapController.getRouteSolver().getRoutingPreviewDots();

			for (int i = Math.max(0, routingPreviewDots.size() - MAX_ROUTE_PREVIEW_DOTS); i < routingPreviewDots.size(); i++) {
				MapMarkerDot dot = new MapMarkerDot(new Color(255 - 255 * (routingPreviewDots.size() - i) / MAX_ROUTE_PREVIEW_DOTS, 0,
						255 * (routingPreviewDots.size() - i) / MAX_ROUTE_PREVIEW_DOTS), routingPreviewDots.get(i));
				map().addMapMarker(dot);
				routeDots.add(dot);
			}
		}

		if (mapController.getRouteSolver().getRoutingState() == RoutingState.Routing) {
			Coordinate candCoord = mapController.getRouteSolver().getBestCandidateCoords();
			if (candCoord != null) {
				MapMarkerDot dot = new MapMarkerDot(Color.GREEN, candCoord);
				map().addMapMarker(dot);
				routeDots.add(dot);
			}
		}
	}



	private void loadNewWorkerVertices() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setCurrentDirectory(
				new File(selectedVertexDirectory != null ? selectedVertexDirectory
						: new File("../../").getAbsolutePath()));
		chooser.setAcceptAllFileFilterUsed(false);
		int returnVal = chooser.showOpenDialog(WorkerVertexVisualizerMain.this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			selectedVertexDirectory = chooser.getSelectedFile().getPath();

			int sampleCount = 0;
			try (BufferedReader reader = new BufferedReader(
					new FileReader(selectedVertexDirectory + File.separator + "worker0" + vertexStatFileName))) {
				while (reader.readLine() != null) {
					sampleCount++;
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			vertexSampleScrollBar.setValue(0);
			vertexSampleScrollBar.setMaximum(sampleCount);

			loadWorkerVerticesSample(selectedVertexDirectory, workerCount, 0);
		}
	}

	private void loadWorkerVerticesSample(String statsDir, int workerCount, int sampleIndex) {
		clearRouteDisplay();
		map().removeAll();
		map().removeAllMapMarkers();
		map().removeAllMapPolygons();

		Color[] colorPalette = Utils.generateColors(workerCount);
		IRouteSolver routeSolver = mapController.getRouteSolver();
		for(int iW = 0; iW < workerCount; iW++) {
			Color col = colorPalette[iW];

			try (BufferedReader reader = new BufferedReader(
					new FileReader(statsDir + File.separator + "worker" + iW + vertexStatFileName))) {
				for (int i = 0; i < sampleIndex; i++) {
					reader.readLine();
				}

				String line = reader.readLine();
				if (line != null) {
					String[] lineSplit = line.split(";");
					for (int i = 0; i < lineSplit.length; i++) {
						String s = lineSplit[i];
						if (!s.isEmpty())
							map().addMapMarker(new MapMarkerDot("", col,
									routeSolver.getCoordinatesByIndex(Integer.parseInt(s)), 3));
					}
				}
				else {
					System.out.println("No sample " + sampleIndex + " for worker " + iW);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	private JMapViewer map() {
		return treeMap.getViewer();
	}


	/**
	 * @param args
	 *            Main program arguments
	 */
	public static void main(String[] args) {
		String roadGraphFile = "data" + File.separator + "graph.bin";
		if (args.length >= 1) roadGraphFile = args[0];
		new WorkerVertexVisualizerMain(roadGraphFile).setVisible(true);
	}

	private void updateZoomParameters() {
		if (mperpLabelValue != null) mperpLabelValue.setText(String.format("%s", map().getMeterPerPixel()));
		if (zoomValue != null) zoomValue.setText(String.format("%s", map().getZoom()));
	}

	@Override
	public void processCommand(JMVCommandEvent command) {
		if (command.getCommand().equals(JMVCommandEvent.COMMAND.ZOOM) || command.getCommand().equals(JMVCommandEvent.COMMAND.MOVE)) {
			updateZoomParameters();
		}
	}
}
