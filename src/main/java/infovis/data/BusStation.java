package infovis.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A {@link BusStation} contains informations about bus stations in the traffic
 * network.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 */
public final class BusStation {

  /**
   * The name of the bus station.
   */
  private final String name;

  /**
   * The unique id of the bus station.
   */
  private final int id;

  /**
   * A sorted set of all bus edges starting with the earliest edge (0 hours 0
   * minutes).
   */
  private final SortedSet<BusEdge> edges = new TreeSet<BusEdge>();

  /**
   * The bus manager.
   */
  private final BusStationManager manager;

  /**
   * Creates a bus station.
   * 
   * @param manager The manager.
   * @param name The name.
   * @param id The id.
   * @param x The x position.
   * @param y The y position.
   */
  BusStation(final BusStationManager manager, final String name, final int id,
      final double x, final double y) {
    this.manager = manager;
    this.name = name;
    this.id = id;
    this.x = x;
    this.y = y;
  }

  /**
   * Getter.
   * 
   * @return The name of the bus station.
   */
  public String getName() {
    return name;
  }

  /**
   * Getter.
   * 
   * @return The id of the bus station.
   */
  public int getId() {
    return id;
  }

  /**
   * Adds an edge to this bus station.
   * 
   * @param line bus line
   * @param dest The destination.
   * @param start The start time.
   * @param end The end time.
   */
  public void addEdge(final BusLine line, final BusStation dest, final BusTime start,
      final BusTime end) {
    edges.add(new BusEdge(line, this, dest, start, end));
  }

  /**
   * Returns all edges associated with this bus station, starting with the edge
   * earliest after the given time. The last edge is the edge before the given
   * time.
   * 
   * @param from The time of the first returned edge.
   * @return An iterable going through the set of edges.
   */
  public Iterable<BusEdge> getEdges(final BusTime from) {
    final Comparator<BusTime> cmp = from.createRelativeComparator();
    BusEdge start = null;
    for(final BusEdge e : edges) {
      if(start == null) {
        start = e;
        continue;
      }
      if(cmp.compare(e.getStart(), start.getStart()) < 0) {
        // must be smaller to get the first one of a row of simultaneous edges
        start = e;
      }
    }
    if(start == null) // empty set
      return new ArrayList<BusEdge>(0);
    final BusEdge s = start;
    final SortedSet<BusEdge> e = edges;
    return new Iterable<BusEdge>() {

      @Override
      public Iterator<BusEdge> iterator() {
        return new Iterator<BusEdge>() {

          private boolean pushedBack;

          private BusEdge cur;

          private Iterator<BusEdge> it;

          {
            it = e.tailSet(s).iterator();
            cur = it.next();
            pushedBack = true;
          }

          private BusEdge pollNext() {
            if(it.hasNext()) {
              final BusEdge e = it.next();
              if(e == s) {
                it = null;
              }
              return it != null ? e : null;
            }
            it = e.iterator(); // can not be empty
            final BusEdge e = it.next();
            if(e == s) {
              it = null;
            }
            return it != null ? e : null;
          }

          @Override
          public boolean hasNext() {
            if(cur == null) return false;
            if(pushedBack) return true;
            cur = pollNext();
            pushedBack = true;
            return cur != null;
          }

          @Override
          public BusEdge next() {
            if(cur == null) return null;
            if(pushedBack) {
              pushedBack = false;
              return cur;
            }
            cur = pollNext();
            return cur;
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }

        };
      }

    };
  }

  /**
   * A route is given by the best edges per bus station.
   * 
   * @author Joschi <josua.krause@googlemail.com>
   */
  private static final class Route {

    /**
     * The bus station.
     */
    private final BusStation station;

    /**
     * The best edge leading to the bus station.
     */
    private BusEdge from;

    /**
     * Creates a route object.
     * 
     * @param station The bus station.
     */
    public Route(final BusStation station) {
      this.station = station;
    }

    /**
     * Getter.
     * 
     * @return The bus station.
     */
    public BusStation getStation() {
      return station;
    }

    /**
     * Getter.
     * 
     * @return The best edge leading to the bus station or <code>null</code> if
     *         it is not assigned yet.
     */
    public BusEdge getFrom() {
      return from;
    }

    /**
     * Getter.
     * 
     * @return Whether there is already a best edge.
     */
    public boolean hasFrom() {
      return from != null;
    }

    /**
     * Setter.
     * 
     * @param from Sets the new best edge.
     */
    public void setFrom(final BusEdge from) {
      this.from = from;
    }

    /**
     * Marks this Route as start.
     */
    public void setStart() {
      from = null;
    }

  }

  /**
   * Finds the shortest route to the given station.
   * 
   * @param dest The destination.
   * @param start The start time.
   * @param changeTime The time to change lines.
   * @return The shortest route to the destination or <code>null</code> if there
   *         exists no route to the given destination.
   */
  public Deque<BusEdge> routeTo(final BusStation dest, final BusTime start,
      final int changeTime) {
    final Map<Integer, Route> routes = new HashMap<Integer, Route>();
    iniRoutes(routes);
    if(!findRoutes(routes, dest, start, changeTime)) return null;
    return convertRoutes(routes, dest);
  }

  /**
   * Converts the route objects back into a meaningful path by going from the
   * destination to the start.
   * 
   * @param routes The route map.
   * @param dest The destination.
   * @return The path.
   */
  private Deque<BusEdge> convertRoutes(final Map<Integer, Route> routes,
      final BusStation dest) {
    Route cur = routes.get(dest.getId());
    final Deque<BusEdge> res = new LinkedList<BusEdge>();
    do {
      final BusEdge edge = cur.getFrom();
      res.addFirst(edge);
      cur = routes.get(edge.getFrom().getId());
    } while(!cur.getStation().equals(this));
    return res;
  }

  /**
   * Finds the shortest route.
   * 
   * @param routes The route map.
   * @param dest The destination.
   * @param start The start time.
   * @param changeTime The changing time in minutes.
   * @return Whether there exists an route to the destination.
   */
  private boolean findRoutes(final Map<Integer, Route> routes, final BusStation dest,
      final BusTime start,
      final int changeTime) {
    final Queue<BusEdge> edges = new PriorityQueue<BusEdge>(20,
        BusEdge.createRelativeComparator(start));
    routes.get(getId()).setStart();
    addAllEdges(edges, this, start, changeTime, null);
    while(!edges.isEmpty()) {
      final BusEdge e = edges.poll();
      final int startTime = start.minutesTo(e.getStart());
      final int endTime = start.minutesTo(e.getEnd());
      if(startTime > endTime) { // edge starts before start
        continue;
      }
      final BusStation to = e.getTo();
      if(to.equals(this)) { // edge is back to the start
        continue;
      }
      final Route next = routes.get(to.getId());
      if(next.hasFrom()) { // destination already visited
        continue;
      }
      next.setFrom(e);
      if(to.equals(dest)) { // we are done
        break;
      }
      final BusTime curEnd = e.getEnd();
      addAllEdges(edges, to, curEnd, changeTime, e.getLine());
    }
    return routes.get(dest.getId()).hasFrom();
  }

  /**
   * Adds all edges to the deque. First all edges of the same line are added and
   * then the edges of the other lines.
   * 
   * @param edges The deque.
   * @param station The station where the edges are originating.
   * @param time The current time.
   * @param changeTime The change time.
   * @param line The current bus line or <code>null</code> if there is none.
   */
  private void addAllEdges(final Queue<BusEdge> edges, final BusStation station,
      final BusTime time, final int changeTime, final BusLine line) {
    final int maxTime = Math.max(manager.getMaxTimeHours() * 60 - 1, 0);
    if(line == null) {
      for(final BusEdge edge : station.getEdges(time)) {
        if(!validEdge(edge, time, maxTime)) {
          continue;
        }
        edges.add(edge);
      }
      return;
    }
    for(final BusEdge edge : station.getEdges(time)) {
      if(!validEdge(edge, time, maxTime)) {
        continue;
      }
      if(edge.getLine().equals(line)) {
        edges.add(edge);
      }
    }
    final BusTime nt = time.later(changeTime);
    for(final BusEdge edge : station.getEdges(nt)) {
      if(!validEdge(edge, nt, maxTime - changeTime)) {
        continue;
      }
      if(edge.getLine().equals(line)) {
        continue;
      }
      edges.add(edge);
    }
  }

  /**
   * If an edge is in a valid time span.
   * 
   * @param edge The edge.
   * @param start The start time.
   * @param maxTime The maximum time.
   * @return Whether the start time of the edge is in the given interval.
   */
  private static boolean validEdge(final BusEdge edge, final BusTime start,
      final int maxTime) {
    return start.minutesTo(edge.getStart()) < maxTime;
  }

  /**
   * Initializes the route objects.
   * 
   * @param routes The route map.
   */
  private void iniRoutes(final Map<Integer, Route> routes) {
    for(final BusStation station : manager.getStations()) {
      routes.put(station.getId(), new Route(station));
    }
  }

  /**
   * The default x coordinate for this bus station.
   */
  private final double x;

  /**
   * The default y coordinate for this bus station.
   */
  private final double y;

  /**
   * Getter.
   * 
   * @return The default x coordinate for this bus station.
   */
  public double getDefaultX() {
    return x;
  }

  /**
   * Getter.
   * 
   * @return The default y coordinate for this bus station.
   */
  public double getDefaultY() {
    return y;
  }

  @Override
  public boolean equals(final Object obj) {
    return obj == null ? false : id == ((BusStation) obj).id;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public String toString() {
    return String.format("%s[%s, %d]", getClass().getSimpleName(), name, id);
  }

}
