package infovis.data;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link BusStationManager} takes care of {@link BusStation}s.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 */
public final class BusStationManager {

  /**
   * The backing map for bus station ids.
   */
  private final Map<Integer, BusStation> stations = new HashMap<Integer, BusStation>();

  /**
   * Getter.
   * 
   * @param id The id of a bus station.
   * @return The bus station with the given id.
   */
  public BusStation getForId(final int id) {
    return stations.get(id);
  }

  /**
   * Getter.
   * 
   * @return All registered {@link BusStation}s.
   */
  public Iterable<BusStation> getStations() {
    return stations.values();
  }

  /**
   * Creates a new bus station.
   * 
   * @param name The name.
   * @param id The id. If the id is already used an
   *          {@link IllegalArgumentException} is thrown.
   * @param x The x position.
   * @param y The y position.
   * @return The newly created bus station.
   */
  public BusStation createStation(final String name, final int id, final double x,
      final double y) {
    if(stations.containsKey(id)) throw new IllegalArgumentException("id: " + id
        + " already in use");
    final BusStation bus = new BusStation(this, name, id, x, y);
    stations.put(id, bus);
    return bus;
  }

  /**
   * The maximum amount of time a route can take.
   */
  private int maxTimeHours = 24;

  /**
   * Getter.
   * 
   * @return The maximum amount of time a route can take in hours. This may not
   *         be exact. The value limits the starting time of an edge.
   */
  public int getMaxTimeHours() {
    return maxTimeHours;
  }

  /**
   * Setter.
   * 
   * @param maxTimeHours Sets the maximum amount of time a route can take in
   *          hours.
   */
  public void setMaxTimeHours(final int maxTimeHours) {
    if(maxTimeHours < 0 || maxTimeHours > 24) throw new IllegalArgumentException(
        "max time out of bounds " + maxTimeHours);
    this.maxTimeHours = maxTimeHours;
  }

}