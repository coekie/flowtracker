<script lang="ts">
  import { tick } from 'svelte';
  import type { Tracker, TrackerDetail, Region } from '../javatypes'
  import PathView from './PathView.svelte';
  import type { SelectedRange } from './selection'

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
  export let selection: SelectedRange | null;

  export let ondblclick: (() => void) | null = null;

  // pull out the ids, to prevent unnecessary re-fetching when tracker is changed to other instance
  // with same id
  let viewTrackerId: number | undefined;
  $: viewTrackerId = viewTracker?.id
  let targetTrackerId: number | undefined;
  $: targetTrackerId = targetTracker?.id

  let trackerDetailPromise: Promise<TrackerDetail>;
  $: trackerDetailPromise = fetchTrackerDetail(viewTrackerId, targetTrackerId);
  
  let focusRegion: Region | null;

  /** For an ongoing selection (while the mouse button is down), the selection where we started (where the mouse went down) */
  let selectionStart: SelectedRange | null;

  let pre: HTMLPreElement;

  const fetchTrackerDetail = async (viewTrackerId: number | undefined, targetTrackerId: number | undefined) => {
    if (!viewTrackerId) {
      return new Promise(() => {})
    }
    const response = !targetTrackerId
      ? await fetch('/tracker/' + viewTrackerId)
      : await fetch('/tracker/' + viewTrackerId + '/to/' + targetTrackerId)
		if (!response.ok) throw new Error(response.statusText)
		return response.json()
  }

  const tooltip = (region: Region) => {
    if (region.parts.length === 0) {
      return null
    } else {
      let part = region.parts[0];
      return 'source=' + (part.tracker.description || 'unknown') + '\n' +
        'sourceOffset=' + part.offset + '\n' +
        'context=' + part.context;
    }
  }

  const focusIn = (region: Region) => {
    focusRegion = region
  }

  const focusOut = (region: Region) => {
    focusRegion = null
  }

  // convertion a region (of viewTracker) to a SelectedRange.
  // That is in terms of the source tracker, see `selection`.
  const toSelection = (region: Region):SelectedRange | null => {
    if (targetTracker) {
      return {
        tracker: viewTracker!,
        offset: region.offset,
        length: region.length
      }
    } else if (region.parts.length == 1) {
      return region.parts[0]
    } else {
      return null;
    } 
  } 

  const mousedown = (region: Region) => {
    selection = selectionStart = toSelection(region)
  }

  // handle selecting multiple regions, by dragging
  const mousemove = (e:MouseEvent, region: Region) => {
    // if the button isn't pressed anymore, stop the selection
    if (e.buttons != 1) {
      selectionStart = null
      return
    }

    let selectionEnd = toSelection(region)
    // a valid selection must have a start and end with the same tracker
    if (!selectionStart || !selectionEnd || selectionStart.tracker.id != selectionEnd.tracker.id) {
      selection = null
      return
    }
    // you can select from left to right, or right to left
    let start:number = Math.min(selectionStart.offset, selectionEnd.offset)
    let end:number = Math.max(selectionStart.offset + selectionStart.length, selectionEnd.offset + selectionEnd.length)
    selection = {
      tracker: selectionStart.tracker,
      offset: start,
      length: end - start
    }
  }

  const mouseup = () => {
    selectionStart = null;
  }

  const isSelected = (region: Region, selection: SelectedRange | null):boolean => {
    if (selection == null || viewTracker == null) {
      return false;
    } else if (targetTracker) {
      return selection.tracker.id == viewTracker.id
        && region.offset >= selection.offset
        && region.offset < selection.offset + selection.length
      // else, we're looking at a sink (!targetTracker), so each region has at most one part
    } else if (region.parts.length == 0) {
      return false
    } else {
      var part = region.parts[0];
      return part.tracker.id == selection.tracker.id &&
        part.offset >= selection.offset &&
        part.offset < selection.offset + selection.length
    } 
  }

  // event for main view so that double-click in one TrackerDetailView causes scrollToSelection in the other
  const dblclick = () => {
    if (ondblclick) {
      ondblclick()
    }
  }

  /** scroll the first selected region into view */
  export const scrollToSelection = () => {
    pre?.querySelector(".selected")?.scrollIntoView()
  }

  /** waits for rendering and then scrolls the first selection region into view */
  const scrollToSelectionOnFirstRender = (_:HTMLPreElement) => {
    tick().then(scrollToSelection)
  }
</script>

{#await trackerDetailPromise then trackerDetail}
  <div class="trackerDetail">
  <div><PathView path={trackerDetail.path}/></div>
  <pre bind:this={pre} use:scrollToSelectionOnFirstRender>{#each trackerDetail.regions as region}<a class="region"
    href={region.parts.length > 0 ? '#' : undefined}
    on:mouseover={() => focusIn(region)}
    on:mouseout={() => focusOut(region)}
    on:focus={() => focusIn(region)}
    on:blur={() => focusOut(region)}
    on:mousedown={() => mousedown(region)}
    on:mousemove={e => mousemove(e, region)}
    on:mouseup={mouseup}
    on:dblclick={dblclick}
    draggable="false"
    class:focus={focusRegion === region}
    class:selected={isSelected(region, selection)}
    class:withSource={region.parts.length > 0}
    title={tooltip(region)}>{region.content}</a>{/each}</pre>
  </div>
{/await}

<style>
  .trackerDetail {
    width: 100%;
    height: 100%;
    overflow: auto;
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
    background-color: lightcyan;
  }
  .focus {
    background-color: #F0F0F0
  }
  .selected.withSource {
    background-color: lightblue
  }
  .selected {
    background-color: lightgray
  }
</style>
