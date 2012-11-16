package infovis.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * A device independent resource identifier. It can either be an URL, a file, or
 * a system resource (located in the jar).
 * 
 * @author Joschi <josua.krause@googlemail.com>
 */
public final class Resource {

  /** The CP-1252 character set for Excel compatibility. */
  public static final Charset CP1252 = Charset.forName("CP1252");

  /** The UTF-8 character set. */
  public static final Charset UTF8 = Charset.forName("UTF-8");

  /** Resource path. */
  public static final String RESOURCES = "src/main/resources";

  /**
   * The local resource path or <code>null</code> if a direct path is specified.
   */
  private final String local;

  /** The path part. */
  private final String path;

  /** The file part or <code>null</code> if no file is specified. */
  private final String file;

  /** The character set. */
  private final Charset cs;

  /**
   * Creates a system resource (located in the jar).
   * 
   * @param name The name of the system resource.
   */
  public Resource(final String name) {
    this(RESOURCES, name, defaultCharset(name));
  }

  /**
   * Creates a resource.
   * 
   * @param local The local resource path or <code>null</code> if a direct path
   *          is specified.
   * @param resource The resource to get
   * @param cs The character set or <code>null</code>.
   */
  public Resource(final String local, final String resource, final String cs) {
    this(local, resource, cs != null ? Charset.forName(cs) : defaultCharset(resource));
  }

  /**
   * Creates a resource.
   * 
   * @param local The local resource path or <code>null</code> if a direct path
   *          is specified.
   * @param resource The resource to get.
   * @param cs The character set.
   */
  public Resource(final String local, final String resource, final Charset cs) {
    this(local, resource, null, cs);
  }

  /**
   * Creates a resource.
   * 
   * @param local resource path or <code>null</code> if a direct path is
   *          specified.
   * @param path The path part.
   * @param file The file part.
   * @param cs The character set.
   */
  private Resource(final String local, final String path, final String file,
      final Charset cs) {
    this.local = local;
    this.path = path;
    this.file = file;
    this.cs = cs;
  }

  /**
   * Gets the {@link URL} of a resource.
   * 
   * @return the URL
   * @throws IOException if the resource can't be found
   */
  public URL getURL() throws IOException {
    final String resource = getResource();
    if(hasDirectFile()) return new File(resource).toURI().toURL();
    final URL url = Resource.class.getResource('/' + resource);
    if(url != null) return url;
    final File f = new File(local, resource);
    if(!f.canRead()) throw new IOException(f.getName() + " doesn't exist");
    return f.toURI().toURL();
  }

  /**
   * Getter.
   * 
   * @return The combined path and file field.
   */
  private String getResource() {
    if(file == null) return path;
    return endsWithDelim(path) ? path + file : path + "/" + file;
  }

  /**
   * Computes the parent of a path or removes the trailing delimiter.
   * 
   * @param path The path.
   * @return The parent or the path without trailing delimiter.
   */
  private static String parent(final String path) {
    final int i = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    return path.substring(0, i + 1);
  }

  /**
   * Checks whether the path ends with a delimiter.
   * 
   * @param path The path.
   * @return <code>true</code> if the path ends with a delimiter.
   */
  private static boolean endsWithDelim(final String path) {
    final char end = path.charAt(path.length() - 1);
    return end == '/' || end == '\\';
  }

  /**
   * Computes the parent of a path.
   * 
   * @param path The path.
   * @return The parent.
   */
  private static String getParent(final String path) {
    String f = path;
    while(endsWithDelim(f)) {
      f = parent(f);
    }
    return parent(f);
  }

  /**
   * Getter.
   * 
   * @return The parent of the resource.
   */
  public Resource getParent() {
    return new Resource(local, getParent(getResource()), cs);
  }

  /**
   * Adds a file part to the resource.
   * 
   * @param name The name of the file.
   * @return The resource pointing to the file.
   */
  public Resource getFile(final String name) {
    return new Resource(local, getResource(), name, cs);
  }

  /**
   * Getter.
   * 
   * @return Whether there is no local part.
   */
  public boolean hasDirectFile() {
    return local == null;
  }

  /**
   * Creates a direct file.
   * 
   * @return The file object.
   */
  public File directFile() {
    if(!hasDirectFile()) throw new IllegalStateException("has local part: " + local);
    return new File(getResource());
  }

  /**
   * Checks whether the resource has content. This method does not guarantee
   * whether the resource has content the next time the stream to it is opened,
   * though.
   * 
   * @return If non empty content could be obtained.
   */
  public boolean hasContent() {
    URL url;
    try {
      url = getURL();
    } catch(final IOException e) {
      return false;
    }
    InputStream in = null;
    try {
      in = url.openStream();
      final boolean content = in.read() >= 0;
      in.close();
      return content;
    } catch(final IOException e) {
      if(in != null) {
        try {
          in.close();
        } catch(final IOException _) {
          // ignore
        }
      }
      return false;
    }
  }

  /**
   * Creates a {@link Reader}.
   * 
   * @return reader or <code>null</code> if not found.
   * @throws IOException I/O exception
   */
  public Reader reader() throws IOException {
    if(!hasContent()) return null;
    final URL url = getURL();
    return charsetReader(url.openStream());
  }

  /**
   * Creates a {@link BufferedReader} from the given {@link InputStream} that
   * interprets the incoming bytes using the given character set.
   * 
   * @param in input stream
   * @return reader
   */
  private BufferedReader charsetReader(final InputStream in) {
    return new BufferedReader(new InputStreamReader(in, cs));
  }

  /**
   * Whether the given resource is a ZIP file.
   * 
   * @return Whether the resource is a ZIP file.
   */
  public boolean isZip() {
    return isZip(getResource());
  }

  /**
   * Getter.
   * 
   * @return The character set.
   */
  public Charset getCharset() {
    return cs;
  }

  /**
   * Whether the given path points to a ZIP file.
   * 
   * @param path The path.
   * @return Whether it is a ZIP file.
   */
  private static boolean isZip(final String path) {
    return path.endsWith(".zip");
  }

  /**
   * The default character set. <code>CP-1252</code> for Excel compatibility and
   * <code>UTF-8</code> for ZIP files.
   * 
   * @param path The path of the resource.
   * @return The default character set.
   */
  private static Charset defaultCharset(final String path) {
    return isZip(path) ? UTF8 : CP1252;
  }

}
