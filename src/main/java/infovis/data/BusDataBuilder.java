package infovis.data;

import static java.lang.Double.*;
import static java.lang.Integer.*;
import infovis.DesktopApp;
import infovis.util.IOUtil;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Loader for bus data in {@code CSV} format.
 * 
 * @author Leo Woerteler
 */
public final class BusDataBuilder {

  /** Maps the external bus station ids to the internal ones. */
  private final Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();
  /** Bus stations. */
  private final List<BusStation> stations = new ArrayList<BusStation>();
  /** Map from station IDs to the bus edges originating at this station. */
  private final List<List<BusEdge>> edges = new ArrayList<List<BusEdge>>();
  /** Walking distances. */
  private final List<List<Integer>> walkingDists = new ArrayList<List<Integer>>();
  /** The overview resource URL. */
  private final URL overview;

  /**
   * Constructor taking the path of the CSV files.
   * 
   * @param overview The overview resource URL, possibly <code>null</code>
   */
  public BusDataBuilder(final URL overview) {
    this.overview = overview;
  }

  /** The default charset - CP-1252 for Excel compatibility. */
  private static final Charset DEFAULT_CS = Charset.forName("CP1252");

  /**
   * Loads a bus station manager from the given path.
   * 
   * @param path The path.
   * @param cs The charset or <code>null</code>.
   * @return The bus station manager.
   * @throws IOException I/O Exception.
   */
  public static BusStationManager loadPath(final String path, final String cs)
      throws IOException {
    return load(null, path, cs != null ? Charset.forName(cs) : DEFAULT_CS);
  }

  /**
   * Loads a bus station manager from the default resource path.
   * 
   * @param path The city.
   * @return The bus station manager.
   * @throws IOException I/O Exception.
   */
  public static BusStationManager loadDefault(final String path) throws IOException {
    return load(DesktopApp.RESOURCES, path, DEFAULT_CS);
  }

  /**
   * Loads the bus system data from CSV files.
   * 
   * @param local The local resource path or <code>null</code> if a direct path
   *          is specified.
   * @param path data file path
   * @param cs The charset
   * @return The bus manager holding the informations.
   * @throws IOException I/O exception
   */
  public static BusStationManager load(final String local, final String path,
      final Charset cs) throws IOException {
    final URL overview = IOUtil.getURL(local, path + "/abstract.svg");
    final BusDataBuilder builder = new BusDataBuilder(overview);

    final CSVReader stops = readerFor(local, path, "stops.csv", cs);
    for(String[] stop; (stop = stops.readNext()) != null;) {
      double abstractX, abstractY;
      if("UNKNOWN".equals(stop[4])) {
        abstractX = abstractY = Double.NaN;
      } else {
        abstractX = parseDouble(stop[4]);
        abstractY = parseDouble(stop[5]);
      }
      builder.createStation(stop[0], parseInt(stop[1]), parseDouble(stop[3]) * 10000,
          -parseDouble(stop[2]) * 10000, abstractX, abstractY);
    }

    final CSVReader walk = readerFor(local, path, "walking-dists.csv", cs);
    for(String[] dist; (dist = walk.readNext()) != null;) {
      builder.setWalkingDistance(parseInt(dist[0]), parseInt(dist[1]), parseInt(dist[2]));
    }

    final Map<String, BusLine> lines = new HashMap<String, BusLine>();
    final CSVReader lineReader = readerFor(local, path, "lines.csv", cs);
    for(String[] line; (line = lineReader.readNext()) != null;) {
      final Color c = new Color(parseInt(line[1]), parseInt(line[2]), parseInt(line[3]));
      lines.put(line[0], createLine(line[0].replace('_', '/'), c));
    }

    final CSVReader edgeReader = readerFor(local, path, "edges.csv", cs);
    for(String[] edge; (edge = edgeReader.readNext()) != null;) {
      final BusLine line = lines.get(edge[0]);
      final int tourNr = parseInt(edge[1]), from = parseInt(edge[2]), to = parseInt(edge[5]);
      final BusTime start = parseTime(edge[3]), end = parseTime(edge[4]);
      builder.addEdge(from, line, tourNr, to, start, end);
    }
    return builder.finish();
  }

  /**
   * Parses a {@link BusTime}.
   * 
   * @param time time string
   * @return resulting {@link BusTime}
   */
  private static BusTime parseTime(final String time) {
    return BusTime.MIDNIGHT.later(0, parseInt(time));
  }

  /**
   * Creates a {@link CSVReader} suitable for Microsoft Excel CSV files.
   * 
   * @param local The local resource path or <code>null</code> if a direct path
   *          is specified.
   * @param path sub-directory inside the resource directory
   * @param file CSV file
   * @param cs The charset.
   * @return reader
   * @throws IOException I/O exception
   */
  private static CSVReader readerFor(final String local, final String path,
      final String file, final Charset cs) throws IOException {
    return new CSVReader(IOUtil.charsetReader(
        IOUtil.getResource(local, path + '/' + file), cs), ';');
  }

  /**
   * Creates a new bus station.
   * 
   * @param name The name.
   * @param id The id. If the id is already used an
   *          {@link IllegalArgumentException} is thrown.
   * @param x The x position.
   * @param y The y position.
   * @param abstractX The abstract x position.
   * @param abstractY The abstract y position.
   * @return The newly created bus station.
   */
  public BusStation createStation(final String name, final int id, final double x,
      final double y, final double abstractX, final double abstractY) {
    if(idMap.containsKey(id)) throw new IllegalArgumentException("id: " + id
        + " already in use");
    // keep bus station ids dense
    final int realId = stations.size();
    idMap.put(id, realId);
    final List<BusEdge> edgeList = new ArrayList<BusEdge>();
    edges.add(edgeList);
    final List<Integer> walking = new ArrayList<Integer>();
    walkingDists.add(walking);
    final BusStation bus = new BusStation(name, realId, x, y, abstractX, abstractY,
        edgeList, walking);
    stations.add(bus);
    return bus;
  }

  /**
   * Creates a new bus line.
   * 
   * @param line The name.
   * @param color The color.
   * @return The bus line.
   */
  public static BusLine createLine(final String line, final Color color) {
    return new BusLine(line, color);
  }

  /**
   * Adds an edge to this bus station.
   * 
   * @param station bus station to start from
   * @param line bus line
   * @param tourNr tour number, unique per line
   * @param dest The destination
   * @param start The start time.
   * @param end The end time.
   * @return added edge
   */
  public BusEdge addEdge(final BusStation station, final BusLine line, final int tourNr,
      final BusStation dest, final BusTime start, final BusTime end) {
    final BusEdge edge = new BusEdge(line, tourNr, station, dest, start, end);
    edges.get(station.getId()).add(edge);
    return edge;
  }

  /**
   * Adds an edge to this bus station.
   * 
   * @param stationID bus station ID
   * @param line bus line
   * @param tourNr tour number, unique per line
   * @param destID The destination's station ID
   * @param start The start time.
   * @param end The end time.
   * @return added edge
   */
  public BusEdge addEdge(final int stationID, final BusLine line, final int tourNr,
      final int destID, final BusTime start, final BusTime end) {
    return addEdge(getStation(stationID), line, tourNr, getStation(destID), start, end);
  }

  /**
   * Sets the walking distance between two bus stations.
   * 
   * @param a first station
   * @param b second station
   * @param secs walking time in seconds
   */
  public void setWalkingDistance(final BusStation a, final BusStation b, final int secs) {
    final int id1 = a.getId(), id2 = b.getId();
    set(walkingDists.get(id1), id2, secs, -1);
    set(walkingDists.get(id2), id1, secs, -1);
  }

  /**
   * Sets the walking distance between two bus stations.
   * 
   * @param id1 first station's ID
   * @param id2 second station's ID
   * @param secs walking time in seconds
   */
  public void setWalkingDistance(final int id1, final int id2, final int secs) {
    setWalkingDistance(getStation(id1), getStation(id2), secs);
  }

  /**
   * Sets the value at a specific position in a list. If the list is too short,
   * it is extended by the given default element.
   * 
   * @param <T> type of the list's elements
   * @param list the list
   * @param pos position
   * @param val value to be set
   * @param def default element
   */
  private static <T> void set(final List<T> list, final int pos, final T val, final T def) {
    while(list.size() < pos) {
      list.add(def);
    }
    if(list.size() == pos) {
      list.add(val);
    } else {
      list.set(pos, val);
    }
  }

  /**
   * Gets the station from the stations map.
   * 
   * @param id station ID
   * @return associated station
   * @throws IllegalArgumentException if the ID has no associated station
   */
  private BusStation getStation(final int id) {
    final BusStation station = stations.get(idMap.get(id));
    if(station == null) throw new IllegalArgumentException("Unknown station: " + id);
    return station;
  }


  /**
   * Finishes the building process and returns the bus station manager.
   * 
   * @return bus station manager
   */
  public BusStationManager finish() {
    for(final List<BusEdge> e : edges) {
      Collections.sort(e);
    }
    return new BusStationManager(stations, overview);
  }

}
