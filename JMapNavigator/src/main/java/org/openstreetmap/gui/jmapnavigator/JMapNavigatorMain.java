// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapnavigator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.openstreetmap.gui.jmapnavigator.IRouteSolver.RoutingState;
import org.openstreetmap.gui.jmapnavigator.QueryGeneration.MapNodeCluster;
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
public class JMapNavigatorMain extends JFrame implements JMapViewerEventListener {

	private static final long serialVersionUID = 1L;

	private final JMapViewerTree treeMap;
	private final JMapNavigatorMapController mapController;

	private final JLabel zoomLabel;
	private final JLabel zoomValue;

	private final JLabel mperpLabelName;
	private final JLabel mperpLabelValue;

	//	private final JLabel routeDistLabel;
	private final JLabel routeTimeLabel;

	private static final int MAX_ROUTE_PREVIEW_DOTS = 50;



	/**
	 * Constructs the {@code Demo}.
	 */
	public JMapNavigatorMain(String roadGraphFile) {
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
		final JCheckBox showMapMarker = new JCheckBox("Map markers visible");
		showMapMarker.setSelected(map().getMapMarkersVisible());
		showMapMarker.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				map().setMapMarkerVisible(showMapMarker.isSelected());
			}
		});
		panelBottom.add(showMapMarker);
		///
		final JCheckBox showTreeLayers = new JCheckBox("Tree Layers visible");
		showTreeLayers.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				treeMap.setTreeVisible(showTreeLayers.isSelected());
			}
		});
		panelBottom.add(showTreeLayers);
		///
		final JCheckBox showToolTip = new JCheckBox("ToolTip visible");
		showToolTip.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				map().setToolTipText(null);
			}
		});
		panelBottom.add(showToolTip);
		///
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
		final JCheckBox scrollWrapEnabled = new JCheckBox("Scrollwrap enabled");
		scrollWrapEnabled.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				map().setScrollWrapEnabled(scrollWrapEnabled.isSelected());
			}
		});
		panelBottom.add(scrollWrapEnabled);
		panelBottom.add(button);

		panelTop.add(zoomLabel);
		panelTop.add(zoomValue);
		panelTop.add(mperpLabelName);
		panelTop.add(mperpLabelValue);

		add(treeMap, BorderLayout.CENTER);


		//		final JCheckBox doFastForward = new JCheckBox("FastFollow");
		//		doFastForward.setSelected(mapController.getRouteSolver().isDoFastFollow());
		//		doFastForward.addActionListener(new ActionListener() {
		//
		//			@Override
		//			public void actionPerformed(ActionEvent e) {
		//				mapController.getRouteSolver().setDoFastFollow(doFastForward.isSelected());
		//			}
		//		});
		//		panelTop.add(doFastForward);
		//
		//		final JCheckBox doMotorwayBoost = new JCheckBox("MotorwayBoost");
		//		doMotorwayBoost.setSelected(mapController.getRouteSolver().isDoMotorwayBoost());
		//		doMotorwayBoost.addActionListener(new ActionListener() {
		//
		//			@Override
		//			public void actionPerformed(ActionEvent e) {
		//				mapController.getRouteSolver().setDoMotorwayBoost(doMotorwayBoost.isSelected());
		//			}
		//		});
		//		panelTop.add(doMotorwayBoost);

		//		routeDistLabel = new JLabel("0 km");
		//		panelBottom.add(routeDistLabel);
		routeTimeLabel = new JLabel("0:00");
		panelBottom.add(routeTimeLabel);


		//        final JCheckBox showZoomControls = new JCheckBox("Show zoom controls");
		//        showZoomControls.setSelected(map().getZoomControlsVisible());
		//        showZoomControls.addActionListener(new ActionListener() {
		//            @Override
		//            public void actionPerformed(ActionEvent e) {
		//                map().setZoomContolsVisible(showZoomControls.isSelected());
		//            }
		//        });
		//        panelBottom.add(showZoomControls);
		//        final JCheckBox scrollWrapEnabled = new JCheckBox("Scrollwrap enabled");
		//        scrollWrapEnabled.addActionListener(new ActionListener() {
		//            @Override
		//            public void actionPerformed(ActionEvent e) {
		//                map().setScrollWrapEnabled(scrollWrapEnabled.isSelected());
		//            }
		//        });
		//        panelBottom.add(scrollWrapEnabled);
		//		panelTop.add(buttonFitMarkers);

		panelTop.add(zoomLabel);
		panelTop.add(zoomValue);
		panelTop.add(mperpLabelName);
		panelTop.add(mperpLabelValue);

		//		JButton buttonCalcCarFast = new JButton("Car: Fast");
		//		buttonCalcCarFast.addActionListener(new ActionListener() {
		//
		//			@Override
		//			public void actionPerformed(ActionEvent e) {
		//				mapController.getRouteSolver().startCalculateRoute(TransportMode.Car, RoutingMode.Fastest);
		//			}
		//		});
		//		panelBottom.add(buttonCalcCarFast);
		//
		//		JButton buttonCalcCarShort = new JButton("Car: Short");
		//		buttonCalcCarShort.addActionListener(new ActionListener() {
		//
		//			@Override
		//			public void actionPerformed(ActionEvent e) {
		//				mapController.getRouteSolver().startCalculateRoute(TransportMode.Car, RoutingMode.Shortest);
		//			}
		//		});
		//		panelBottom.add(buttonCalcCarShort);
		//
		//		JButton buttonCalcPedShort = new JButton("Pedestrian");
		//		buttonCalcPedShort.addActionListener(new ActionListener() {
		//
		//			@Override
		//			public void actionPerformed(ActionEvent e) {
		//				mapController.getRouteSolver().startCalculateRoute(TransportMode.Pedestrian, RoutingMode.Shortest);
		//			}
		//		});
		//		panelBottom.add(buttonCalcPedShort);
		//
		//		JButton buttonCalcManiacFast = new JButton("Maniac: Fast");
		//		buttonCalcManiacFast.addActionListener(new ActionListener() {
		//
		//			@Override
		//			public void actionPerformed(ActionEvent e) {
		//				mapController.getRouteSolver().startCalculateRoute(TransportMode.Maniac, RoutingMode.Fastest);
		//			}
		//		});
		//		panelBottom.add(buttonCalcManiacFast);
		//
		//		JButton buttonCalcManiacShort = new JButton("Maniac: Short");
		//		buttonCalcManiacShort.addActionListener(new ActionListener() {
		//
		//			@Override
		//			public void actionPerformed(ActionEvent e) {
		//				mapController.getRouteSolver().startCalculateRoute(TransportMode.Maniac, RoutingMode.Shortest);
		//			}
		//		});
		//		panelBottom.add(buttonCalcManiacShort);

		JButton buttonCalcManiacShort = new JButton("Shortest path");
		buttonCalcManiacShort.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				mapController.getRouteSolver().startCalculateRoute(true);
			}
		});
		panelBottom.add(buttonCalcManiacShort);


		JButton buttonLoadShowPath = new JButton("Load Path");
		buttonLoadShowPath.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter(
						"Text files", "txt");
				chooser.setFileFilter(filter);
				chooser.setCurrentDirectory(new File("../../\\ConcurrentGraph\\ConcurrentGraph\\concurrent-graph\\output"));
				int returnVal = chooser.showOpenDialog(JMapNavigatorMain.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					clearRouteDisplay();
					try (BufferedReader br = new BufferedReader(
							new FileReader(chooser.getSelectedFile().getAbsolutePath()))) {
						String line = br.readLine();

						Coordinate lastCoord = mapController.getRouteSolver().getCoordinatesByIndex(Integer.parseInt(line.split("\t")[0]));
						while ((line = br.readLine()) != null) {
							Coordinate coord = mapController.getRouteSolver().getCoordinatesByIndex(Integer.parseInt(line.split("\t")[0]));
							MapPolygonImpl routPoly = new MapPolygonImpl(Color.BLUE, lastCoord, coord, coord);
							routeLines.add(routPoly);
							map().addMapPolygon(routPoly);
							lastCoord = coord;
						}
					}
					catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		panelBottom.add(buttonLoadShowPath);



		// Generate queries
		final int queryGenerateCount = 64;
		final boolean showCityHotspots = false;
		final boolean showQueries = true;
		final boolean verifyRoutes = true;

		JButton buttonGenerateQueries = new JButton("Generate queries");
		buttonGenerateQueries.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				clearRouteDisplay();

				List<MapNode> mapNodes = new ArrayList<>(mapController.getRouteSolver().getMapNodes().values());

				List<MapNodeCluster> clusterResults = QueryGeneration.cityClustering(mapNodes, 20, 50000);

				Color[] colorPalette = Utils.generateColors(clusterResults.size());
				double pointDrawRate = 0.01;
				Random rd = new Random(0);

				List<Coordinate[]> queryCoords = new ArrayList<>();
				try (PrintWriter writer = new PrintWriter(new FileWriter("queries.txt"))) {

					for (int i = 0; i < queryGenerateCount; i++) {
						// TODO Cluster probability based on size
						MapNodeCluster cluster = clusterResults.get(rd.nextInt(clusterResults.size()));

						MapNode n0, n1;
						Coordinate c0, c1;
						do {
							n0 = cluster.nodes.get(rd.nextInt(cluster.nodes.size()));
							n1 = cluster.nodes.get(rd.nextInt(cluster.nodes.size()));
							c0 = new Coordinate(n0.Lat, n0.Lon);
							c1 = new Coordinate(n1.Lat, n1.Lon);
						} while (verifyRoutes && !mapController.getRouteSolver().checkIfPathExisting(n0.Id, n1.Id));

						queryCoords.add(new Coordinate[] { c0, c1 });

						writer.println("start\t" + n0.Id + "\t" + n1.Id);
					}
				}
				catch (Exception e1) {
					e1.printStackTrace();
				}

				clearRouteDisplay();

				// Visualize cities clusters
				if (showCityHotspots) {
					for (int i = 0; i < clusterResults.size(); i++) {
						Color clusterColor = colorPalette[i];
						MapNodeCluster cluster = clusterResults.get(i);
						double[] centerPt = cluster.center;
						MapMarkerDot dot = new MapMarkerDot(Integer.toString(i), clusterColor,
								new Coordinate(centerPt[0], centerPt[1]), 8);

						Color weakClusterColor = new Color(clusterColor.getRed(), clusterColor.getGreen(),
								clusterColor.getBlue(), 50);
						for (MapNode clusterPt : cluster.nodes) {
							if (rd.nextDouble() < pointDrawRate) {
								MapMarkerDot ptDot = new MapMarkerDot("", weakClusterColor,
										new Coordinate(clusterPt.Lat, clusterPt.Lon), 3);
								map().addMapMarker(ptDot);
							}
						}

						map().addMapMarker(dot);
					}
				}

				// Visualize query routes
				if (showQueries) {
					int queryIndex = 0;
					for (Coordinate[] qCoord : queryCoords) {
						Coordinate c0 = qCoord[0];
						Coordinate c1 = qCoord[1];

						map().addMapMarker(new MapMarkerDot("" + queryIndex, Color.RED, c0, 3));
						map().addMapMarker(new MapMarkerDot("" + queryIndex, Color.BLUE, c1, 3));

						//						MapPolygonImpl routPoly = new MapPolygonImpl(Color.BLUE, c1, c1, c0);
						//						routeLines.add(routPoly);
						map().addMapPolygon(new MapPolygonImpl(Color.BLUE, c1, c1, c0));

						queryIndex++;
					}
				}


				//				try (PrintWriter writer = new PrintWriter(new FileWriter("queries.txt"))) {
				//					for (int i = 0; i < qN; i++) {
				//						IRouteSolver rs = mapController.getRouteSolver();
				//						long n0 = rs.getRandomRouteNodeIndex();
				//						long n1 = rs.getRandomRouteNodeIndex();
				//						Coordinate c0 = rs.getCoordinatesByIndex(n0);
				//						Coordinate c1 = rs.getCoordinatesByIndex(n1);
				//						MapPolygonImpl routPoly = new MapPolygonImpl(Color.BLUE, c0, c1, c1);
				//						routeLines.add(routPoly);
				//						map().addMapPolygon(routPoly);
				//						writer.println("start\t" + n0 + "\t" + n1);
				//					}
				//				}
				//				catch (Exception e1) {
				//					e1.printStackTrace();
				//				}
			}
		});
		panelBottom.add(buttonGenerateQueries);

		add(treeMap, BorderLayout.CENTER);

		//map().addMapMarker(new MapMarkerDot("A", new Coordinate(48.68, 9.00)));
		//map().addMapMarker(new MapMarkerDot("B", new Coordinate(48.84, 9.26)));

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
				if (showToolTip.isSelected()) map().setToolTipText(map().getPosition(p).toString());
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

	private List<MapMarkerDot> routeDots = new ArrayList<>();
	private List<MapPolygonImpl> routeLines = new ArrayList<>();

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
		new JMapNavigatorMain(roadGraphFile).setVisible(true);
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
