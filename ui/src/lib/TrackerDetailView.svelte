<script lang="ts">
  import {tick} from 'svelte';
  import type {Tracker, TrackerDetail, Region} from '../javatypes';
  import PathView from './PathView.svelte';
  import {
    type ASelection,
    type OnTrackerSelected,
    pathStartsWith,
    RangeSelection,
    scrollSelectedIntoView,
  } from './selection';
  import type {Coloring} from './coloring';
  import CodeView from './CodeView.svelte';
  import TrackerDetailSplit from './TrackerDetailSplit.svelte';

  /** Main tracker that's being shown */
  export let viewTracker: Tracker | null;

  /**
   * Tracker to which some content of viewTracker was copied.
   * This tracker is being shown in another TrackerDetailView,
   */
  export let targetTracker: Tracker | null = null;

  /**
   * The selected range of a tracker.
   *
   * This is in terms of the source tracker:
   * for the top view that is the trackers referenced in the parts;
   * for the bottom view that is the tracker being shown.
   */
  export let selection: ASelection | null;

  export let coloring: Coloring;

  /**
   * Last Tracker to be selected.
   *
   * When a range is selected in the main view, then we update the secondary view to show the tracker
   * associated to that range. But when something else is selected (a path), then the secondaryTracker
   * does not change. So the secondaryTracker is more or less the last `selection` that was a Tracker.
   * Keeping track of secondaryTracker separately from `selection` makes it possible to select paths
   * (e.g. in the secondary view) without the secondary view disappearing.
   */
  export let secondaryTracker: Tracker | null = null;

  export let onRangeSelected: (() => void) | null = null;
  export let onTwinSelected: OnTrackerSelected | null = null;

  // pull out the ids, to prevent unnecessary re-fetching when tracker is changed to other instance
  // with same id
  let viewTrackerId: number | undefined;
  $: viewTrackerId = viewTracker?.id;
  let targetTrackerId: number | undefined;
  $: targetTrackerId = targetTracker?.id;

  /**
   * Are we looking at a tracker as an origin. In that case, selection corresponds to regions in viewTracker itself.
   * If false we're looking at it as a sink. In that case, selection corresponds to the source trackers (trackers
   * referred to in parts).
   */
  let viewAsOrigin: boolean;
  $: viewAsOrigin =
    targetTracker != null || (viewTracker != null && viewTracker.origin);

  /** Loaded detailed info (content, tracking) of viewTracker */
  let trackerDetailPromise: Promise<TrackerDetail>;
  $: trackerDetailPromise = fetchTrackerDetail(viewTrackerId, targetTrackerId);

  /** The region that currently has focus; that the mouse is hoovering over */
  let focusRegion: Region | null;

  /** For an ongoing selection (while the mouse button is down), the selection where we started (where the mouse went down) */
  let selectionStart: RangeSelection | null;

  /** Show creation stacktrace (when available). Toggled with button in upper right corner. */
  let showCreation: boolean = false;

  // references to some specific child tags/components
  let pre: HTMLPreElement;
  let codeView: CodeView;

  /**
   * Position of the split between content and code. Binding to a variable here so that when selecting another
   * tracker (which destroys and recreates the view) that position is maintained.
   */
  let splitPosition: number = 30;

  /** Position to scroll to after loading */
  let scrollPositionToRestore: number | undefined;

  const fetchTrackerDetail = async (
    viewTrackerId: number | undefined,
    targetTrackerId: number | undefined
  ) => {
    if (!viewTrackerId) {
      return new Promise(() => {});
    }
    const response = !targetTrackerId
      ? await fetch('tracker/' + viewTrackerId)
      : await fetch('tracker/' + viewTrackerId + '_to_' + targetTrackerId);
    if (!response.ok) {
      if (response.status == 404) {
        alert(
          'Tracker data not found. Application may have restarted while you were browsing. Please refresh.'
        );
      }
      throw new Error(response.statusText);
    }
    return response.json();
  };

  function focusIn(region: Region) {
    focusRegion = region;
  }

  function focusOut() {
    focusRegion = null;
  }

  // convertion a region (of viewTracker) to a RangeSelection.
  // That is in terms of the source tracker, see `selection`.
  function toSelection(
    region: Region,
    trackerDetail: TrackerDetail
  ): RangeSelection | null {
    if (viewAsOrigin) {
      return new RangeSelection(viewTracker!, region.offset, region.length);
    } else if (region.parts.length == 1) {
      return new RangeSelection(
        trackerDetail.linkedTrackers[region.parts[0].trackerId],
        region.parts[0].offset,
        region.parts[0].length
      );
    } else {
      return null;
    }
  }

  function mousedown(region: Region, trackerDetail: TrackerDetail) {
    selection = selectionStart = toSelection(region, trackerDetail);
    updateSecondaryTracker();
    rangeSelected();
  }

  // handle selecting multiple regions, by dragging
  function mousemove(
    e: MouseEvent,
    region: Region,
    trackerDetail: TrackerDetail
  ) {
    // if the button isn't pressed anymore, stop the selection
    if (e.buttons != 1) {
      selectionStart = null;
      return;
    }

    let selectionEnd = toSelection(region, trackerDetail);
    // a valid selection must have a start and end with the same tracker
    if (
      !selectionStart ||
      !selectionEnd ||
      selectionStart.tracker.id != selectionEnd.tracker.id
    ) {
      selection = null;
      return;
    }
    // you can select from left to right, or right to left
    let start: number = Math.min(selectionStart.offset, selectionEnd.offset);
    let end: number = Math.max(
      selectionStart.offset + selectionStart.length,
      selectionEnd.offset + selectionEnd.length
    );
    selection = new RangeSelection(selectionStart.tracker, start, end - start);
    updateSecondaryTracker();
    rangeSelected();
  }

  function mouseup() {
    selectionStart = null;
  }

  function isSelected(
    region: Region,
    selection: ASelection | null,
    trackerDetail: TrackerDetail
  ): boolean {
    if (selection == null || viewTracker == null) {
      return false;
    } else if (selection instanceof RangeSelection) {
      if (viewAsOrigin) {
        return (
          selection.tracker.id == viewTracker.id &&
          region.offset >= selection.offset &&
          region.offset < selection.offset + selection.length
        );
        // else, we're looking at a sink (!viewAsOrigin), so each region has at most one part
      } else if (region.parts.length == 0) {
        return false;
      } else {
        var part = region.parts[0];
        return (
          part.trackerId == selection.tracker.id &&
          part.offset >= selection.offset &&
          part.offset < selection.offset + selection.length
        );
      }
    } else {
      // selection is a PathSelection
      return region.parts.some(part =>
        pathStartsWith(
          trackerDetail.linkedTrackers[part.trackerId].path,
          selection.path
        )
      );
    }
  }

  function updateSecondaryTracker(): void {
    if (selection instanceof RangeSelection && !viewAsOrigin) {
      secondaryTracker = selection.tracker;
    }
  }

  function backgroundColor(
    region: Region,
    coloring: Coloring,
    trackerDetail: TrackerDetail
  ): string {
    return coloring.backgroundColor(s => isSelected(region, s, trackerDetail));
  }

  // event for main view so that clicking in one TrackerDetailView causes scrollToSelection in the other
  function rangeSelected() {
    // tick: wait for 'selected' style to be updated, so that we scroll to the right element
    tick().then(() => {
      if (onRangeSelected) {
        onRangeSelected();
      }
      codeView?.scrollToSelection();
    });
  }

  /** scroll the first selected region into view */
  export function scrollToSelection() {
    scrollToSelectionInPre();
    codeView?.scrollToSelection();
  }

  /** The element wrapping pre that has the scroll bars */
  function preScrollParent(): HTMLElement | null {
    return pre?.parentElement;
  }

  export function scrollToSelectionInPre() {
    let scrollParent = preScrollParent();
    if (scrollParent) {
      scrollSelectedIntoView(scrollParent);
    }
  }

  /** waits for rendering and then scrolls the first selected region into view */
  function scrollToSelectionOnFirstRender(_: HTMLPreElement) {
    tick().then(() => {
      if (scrollPositionToRestore) {
        preScrollParent()?.scroll({top: scrollPositionToRestore});
      } else {
        scrollToSelectionInPre();
      }
    });
  }

  function gotoTwin(trackerDetail: TrackerDetail): void {
    if (onTwinSelected && trackerDetail.twin) {
      onTwinSelected(trackerDetail.twin);
    }
  }

  /** Current scroll position */
  export function scrollPosition(): number | undefined {
    return preScrollParent()?.scrollTop;
  }

  /** Restore scroll position after loading of next detail (gotten from `scrollPosition()`) */
  export function restoreScrollPosition(scrollPosition: number | undefined) {
    scrollPositionToRestore = scrollPosition;
  }
</script>

{#await trackerDetailPromise then trackerDetail}
  <div class="trackerDetail">
    <div class="path">
      <PathView path={trackerDetail.path} bind:selection {coloring} />
      <span class="header-buttons">
        {#if trackerDetail.twin}
          <button
            class="goto-twin"
            on:click={() => gotoTwin(trackerDetail)}
            title="Go to twin (switch between input and output)"
          />
        {/if}
        {#if trackerDetail.creationStackTrace}
          <button
            class="toggle-creation"
            on:click={() => (showCreation = !showCreation)}
            title="Toggle creation stacktrace"
          />
        {/if}
      </span>
    </div>
    <div class="split">
      <TrackerDetailSplit
        showSplit={trackerDetail.hasSource}
        bind:splitPosition
      >
        <div class="content" slot="one">
          {#if showCreation && trackerDetail.creationStackTrace}
            <pre class="creation">{trackerDetail.creationStackTrace}</pre>
          {/if}
          <pre
            bind:this={pre}
            use:scrollToSelectionOnFirstRender>{#each trackerDetail.regions as region}{#if region.twinContent != null}<span
                  class="twin-content">{region.twinContent}</span
                >{/if}<a
                class="region"
                href={region.parts.length > 0 ? 'javascript:;' : undefined}
                on:mouseover={() => focusIn(region)}
                on:mouseout={() => focusOut()}
                on:focus={() => focusIn(region)}
                on:blur={() => focusOut()}
                on:mousedown={() => mousedown(region, trackerDetail)}
                on:mousemove={e => mousemove(e, region, trackerDetail)}
                on:mouseup={mouseup}
                draggable="false"
                style="background-color: {backgroundColor(
                  region,
                  coloring,
                  trackerDetail
                )}"
                class:selected={isSelected(region, selection, trackerDetail)}
                class:withSource={region.parts.length > 0}
                class:focus={focusRegion === region}>{region.content}</a
              >{/each}</pre>
        </div>
        <CodeView
          bind:this={codeView}
          trackerId={viewTrackerId || -1}
          slot="two"
          {selection}
          {coloring}
        />
      </TrackerDetailSplit>
    </div>
  </div>
{/await}

<style>
  .trackerDetail {
    width: 100%;
    height: 100%;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }
  .path {
    border-bottom: 1px solid #ccc;
  }
  .header-buttons {
    float: right;
    margin-right: 0.5em;
  }
  .goto-twin {
    border: none;
    background: none;
    width: 1.2em;
    height: 1.2em;
    background-image: url(/swap_horiz.svg);
    background-size: contain;
  }
  .toggle-creation {
    border: none;
    background: none;
    width: 1.2em;
    height: 1.2em;
    background-image: url(/stacks.svg);
    background-size: contain;
  }
  .split {
    flex: 1;
    overflow: hidden;
  }
  .creation {
    margin: 0 0 1em 0;
    border-bottom: 1px solid #ccc;
  }
  .content {
    overflow-x: hidden;
    overflow-y: auto;
    height: 100%;
    margin: 0;
  }
  pre {
    margin: 0 0 0 0;
    white-space: break-spaces;
    word-wrap: break-word;
  }
  .region {
    /* draw a vertical line after each part, without influencing size */
    margin-right: -1px;
    border-right: 1px solid gray;
    text-decoration: none;
    color: inherit;
  }
  .region:last-child {
    /* undo border */
    margin-right: 0;
    border-right: 0;
  }
  .focus.withSource {
    border: 1px solid lightblue;
  }
  .focus {
    border: 1px solid lightgray;
  }
  .selected.withSource {
    border: 1px solid blue;
  }
  .selected {
    border: 1px solid gray;
  }
  .twin-content {
    color: lightgray;
  }
</style>
