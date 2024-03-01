<script lang="ts">
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

  // pull out the ids, to prevent unnecessary re-fetching when tracker is changed to other instance
  // with same id
  let viewTrackerId: number | undefined;
  $: viewTrackerId = viewTracker?.id
  let targetTrackerId: number | undefined;
  $: targetTrackerId = targetTracker?.id

  let trackerDetailPromise: Promise<TrackerDetail>;
  $: trackerDetailPromise = fetchTrackerDetail(viewTrackerId, targetTrackerId);
  
  let focusRegion: Region | null;

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

  const click = (region: Region) => {
    if (region.parts.length == 0) {
      selection = null
    } else if (!targetTracker) {
      selection = region.parts[0]
    } else {
      selection = {
        tracker: viewTracker!,
        offset: region.offset,
        length: region.length
      }
    }
  }

  const isSelected = (region: Region, selection: SelectedRange | null):boolean => {
    if (selection == null || region.parts.length == 0 || viewTracker == null) {
      return false;
    } else if (targetTracker) {
      return selection.tracker.id == viewTracker.id
        && region.offset >= selection.offset
        && region.offset < selection.offset + selection.length;
    } else {
      // we're looking at a sink (!targetTracker), so each region only has one part
      var part = region.parts[0];
      return part.tracker.id == selection.tracker.id && part.offset == selection.offset;
    }
  }
</script>

{#await trackerDetailPromise then trackerDetail}
  <div class="trackerDetail">
  <div><PathView path={trackerDetail.path}/></div>
  <pre>{#each trackerDetail.regions as region}<a class="region"
    href={region.parts.length > 0 ? '#' : undefined}
    on:mouseover={() => focusIn(region)}
    on:mouseout={() => focusOut(region)}
    on:focus={() => focusIn(region)}
    on:blur={() => focusOut(region)}
    on:click={() => click(region)}
    draggable="false"
    class:overWithSource={focusRegion === region && region.parts.length > 0}
    class:overWithoutSource={focusRegion === region && region.parts.length == 0}
    class:selected={isSelected(region, selection)}
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
  .overWithSource {
    background-color: lightcyan;
  }
  .overWithoutSource {
    background-color: #F0F0F0
  }
  .selected {
    background-color: lightblue
  }
</style>
