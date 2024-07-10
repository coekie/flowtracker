package com.coekie.flowtracker.tracker;

/*-
 * Copyright 2024 Wouter Coekaerts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.coekie.flowtracker.tracker.TrackerTree.Node;
import com.coekie.flowtracker.util.Config;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds information about a tracked object, mainly its content and source.
 * <ul>
 *   <li>content: the data that passed through the tracked object. e.g. all bytes passed through an
 *   `InputStream` or `OutputStream`. The API for that is in the {@link ByteContentTracker} and
 *   {@link CharContentTracker} interfaces, that trackers optionally implement.
 *   <li>source: associate ranges of its content to their source ranges in other trackers.
 *   For example, for the bytes of a `String` that could be pointing to the range of the tracker
 *   of the `FileInputStream` that the `String` was read from; telling us from which file and where
 *   exactly in that file it came from.
 *   This tracking data can be accessed using
 *   {@link #pushSourceTo(int, WritableTracker, int, int, Growth)}.
 * </ul>
 */
public abstract class Tracker implements WritableTracker {

  private static final AtomicLong idGenerator = new AtomicLong();
  public static boolean trackCreation = false;

  private final long trackerId = nextId();
  private TrackerTree.Node node;
  private StackTraceElement[] creationStackTrace;

  private Tracker twin;

  Tracker() {
  }

  /** Identifier used for this tracker. Mainly used for the API/UI. */
  public long getTrackerId() {
    return trackerId;
  }

  @Override public void setSource(int index, int length, Tracker sourceTracker, int sourceIndex,
      Growth growth) {
    throw new UnsupportedOperationException();
  }

  public abstract int getEntryCount();

  /**
   * Returns if the existing content of this tracker can be changed.
   * Extra content added at the end does not count.
   * Anything added or changed in the beginning or middle does count.
   * <p>
   * In other words, if this method returns true, the source of anything between index 0 and the
   * current {@link #getLength()} is not allowed to change.
   * <p>
   * A tracker can only be considered immutable if the trackers it stores as source are also
   * immutable.
   */
  public boolean isContentMutable() {
    return true;
  }

  /**
   * Returns the length of the content of the object this Tracker is tracking.
   * Note that this is not necessarily equal to the last known index in this tracker,
   * because there may be unknown content at the end (which is included in this length).
   */
  public abstract int getLength();

  /**
   * Put a range of the source of this tracker into the given target tracker. This should be
   * implemented by calling {@link WritableTracker#setSource} on the target, possibly multiple
   * times. Note that it is not <em>this</em> tracker that should be pushed, but the source of it.
   * <p>
   * This is the main way to access tracking data: Trackers only have this visitor-alike style API.
   * To look at a Tracker with a more "getters" style API, there is {@link TrackerSnapshot}.
   *
   * @param index         Index in this tracker of where the range starts.
   * @param targetTracker Tracker of which we're setting the source to this one
   * @param targetIndex   Offset in <tt>targetTracker</tt> of where the range starts.
   * @param targetLength  Size of the range
   * @param growth        the correspondence between our and target range. This also determines the
   *                      length of the relevant range in this tracker. `targetLength` should be a
   *                      multiple of this {@link Growth#targetBlock}.
   */
  public void pushSourceTo(int index, WritableTracker targetTracker, int targetIndex,
      int targetLength, Growth growth) {
    throw new UnsupportedOperationException();
  }

  /** Registers this tracker in the tree, at the given node */
  public Tracker addTo(Node node) {
    node.internalAddTracker(this);
    this.node = node;
    if (trackCreation) {
      // we set the stacktrace in this method, because we only want to track stacktraces of Trackers
      // that have a node. (doing it for every Tracker would be useless, add too much overhead, and
      // lead to infinite recursion).
      creationStackTrace = new Throwable().getStackTrace();
    }
    return this;
  }

  /**
   * The node in the {@link TrackerTree} where this tracker should be shown.
   */
  public TrackerTree.Node getNode() {
    return node;
  }

  /** Stacktrace that was collected when a tracker was created, if `trackCreation` is enabled. */
  public StackTraceElement[] getCreationStackTrace() {
    return creationStackTrace;
  }

  public static void initialize(Config config) {
    trackCreation = config.getBoolean("trackCreation", false);
  }

  /** Generates a unique tracker id ({@link Tracker#getTrackerId()}) */
  public static long nextId() {
    return idGenerator.getAndIncrement();
  }

  /**
   * Other tracker that is closely associated to this one, e.g. linking the input and output on a
   * socket.
   */
  public Tracker twin() {
    return twin;
  }

  /**
   * Initialize the {@link #twin()}, also linking in the other direction when that is not done yet.
   */
  public void initTwin(Tracker twin) {
    this.twin = twin;
    if (twin.twin != this) {
      twin.initTwin(this);
    }
  }
}
