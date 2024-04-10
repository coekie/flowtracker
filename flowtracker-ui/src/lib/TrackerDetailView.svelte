<script lang="ts">
  import {tick} from 'svelte';
  import type {Tracker, TrackerDetail, Region} from '../javatypes';
  import PathView from './PathView.svelte';
  import {
    type ASelection,
    pathStartsWith,
    RangeSelection,
    PathSelection,
  } from './selection';
  import type {Coloring} from './coloring';

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

  export let ondblclick: (() => void) | null = null;

  // pull out the ids, to prevent unnecessary re-fetching when tracker is changed to other instance
  // with same id
  let viewTrackerId: number | undefined;
  $: viewTrackerId = viewTracker?.id;
  let targetTrackerId: number | undefined;
  $: targetTrackerId = targetTracker?.id;

  let trackerDetailPromise: Promise<TrackerDetail>;
  $: trackerDetailPromise = fetchTrackerDetail(viewTrackerId, targetTrackerId);

  let focusRegion: Region | null;

  /** For an ongoing selection (while the mouse button is down), the selection where we started (where the mouse went down) */
  let selectionStart: RangeSelection | null;

  let pre: HTMLPreElement;

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
    if (!response.ok) throw new Error(response.statusText);
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
  function toSelection(region: Region): RangeSelection | null {
    if (targetTracker) {
      return new RangeSelection(viewTracker!, region.offset, region.length);
    } else if (region.parts.length == 1) {
      return new RangeSelection(
        region.parts[0].tracker,
        region.parts[0].offset,
        region.parts[0].length
      );
    } else {
      return null;
    }
  }

  function mousedown(region: Region) {
    selection = selectionStart = toSelection(region);
    updateSecondaryTracker();
  }

  // handle selecting multiple regions, by dragging
  function mousemove(e: MouseEvent, region: Region) {
    // if the button isn't pressed anymore, stop the selection
    if (e.buttons != 1) {
      selectionStart = null;
      return;
    }

    let selectionEnd = toSelection(region);
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
  }

  function mouseup() {
    selectionStart = null;
  }

  function isSelected(region: Region, selection: ASelection | null): boolean {
    if (selection == null || viewTracker == null) {
      return false;
    } else if (selection instanceof RangeSelection) {
      if (targetTracker) {
        return (
          selection.tracker.id == viewTracker.id &&
          region.offset >= selection.offset &&
          region.offset < selection.offset + selection.length
        );
        // else, we're looking at a sink (!targetTracker), so each region has at most one part
      } else if (region.parts.length == 0) {
        return false;
      } else {
        var part = region.parts[0];
        return (
          part.tracker.id == selection.tracker.id &&
          part.offset >= selection.offset &&
          part.offset < selection.offset + selection.length
        );
      }
    } else {
      // selection is a PathSelection
      return region.parts.some(part =>
        pathStartsWith(part.tracker.path, selection.path)
      );
    }
  }

  function updateSecondaryTracker(): void {
    if (selection instanceof RangeSelection) {
      secondaryTracker = selection.tracker;
    }
  }

  function backgroundColor(region: Region, coloring: Coloring): string {
    // we find a matching assignment, and if there are multiple matching then use the most
    // specific one, that is the one with the highest score.
    var bestScore: number = -1;
    var color: string = 'inherit';
    for (const assignment of coloring.assignments) {
      for (const selection of assignment.selections) {
        if (isSelected(region, selection)) {
          const score =
            selection instanceof PathSelection ? selection.path.length : 9999;
          if (score > bestScore) {
            bestScore = score;
            color = assignment.color;
          }
        }
      }
    }
    return color;
  }

  // event for main view so that double-click in one TrackerDetailView causes scrollToSelection in the other
  function dblclick() {
    if (ondblclick) {
      ondblclick();
    }
  }

  /** scroll the first selected region into view */
  export function scrollToSelection() {
    pre?.querySelector('.selected')?.scrollIntoView();
  }

  /** waits for rendering and then scrolls the first selection region into view */
  function scrollToSelectionOnFirstRender(_: HTMLPreElement) {
    tick().then(scrollToSelection);
  }
</script>

{#await trackerDetailPromise then trackerDetail}
  <div class="trackerDetail">
    <div class="path">
      <PathView path={trackerDetail.path} bind:selection {coloring} />
    </div>
    <div class="content">
      {#if trackerDetail.creationStackTrace}
        <pre class="creation">{trackerDetail.creationStackTrace}</pre>
      {/if}
      <pre
        bind:this={pre}
        use:scrollToSelectionOnFirstRender>{#each trackerDetail.regions as region}<a
            class="region"
            href={region.parts.length > 0 ? '#' : undefined}
            on:mouseover={() => focusIn(region)}
            on:mouseout={() => focusOut()}
            on:focus={() => focusIn(region)}
            on:blur={() => focusOut()}
            on:mousedown={() => mousedown(region)}
            on:mousemove={e => mousemove(e, region)}
            on:mouseup={mouseup}
            on:dblclick={dblclick}
            draggable="false"
            style="background-color: {backgroundColor(region, coloring)}"
            class:selected={isSelected(region, selection)}
            class:withSource={region.parts.length > 0}
            class:focus={focusRegion === region}>{region.content}</a
          >{/each}</pre>
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
  .creation {
    margin: 0 0 1em 0;
    border-bottom: 1px solid #ccc;
  }
  .content {
    overflow: auto;
    flex: 1;
    margin: 0;
  }
  pre {
    margin: 0 0 0 0;
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
</style>
