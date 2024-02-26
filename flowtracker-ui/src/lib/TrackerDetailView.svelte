<script lang="ts">
  import type { Tracker, TrackerDetail, Region, TrackerPart } from '../javatypes'

  export let selectedTracker: Tracker;
  let trackerDetailPromise: Promise<TrackerDetail>;
  $: trackerDetailPromise = fetchTrackerDetail(selectedTracker);
  let focusRegion: Region | null;

  const fetchTrackerDetail = async (tracker:Tracker) => {
    if (!tracker) {
      return new Promise(() => {})
    }
    const response = await fetch('/tracker/' + tracker.id)
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
    if (region != null) {
      focusRegion = region
    }
  }
  const focusOut = (region: Region) => {
    if (region != null) {
      focusRegion = region
    }
  }
</script>

{#await trackerDetailPromise then trackerDetail}
  <!-- svelte-ignore a11y-no-noninteractive-tabindex -->
  <pre class="trackerDetail">{#each trackerDetail.regions as region}<span class="region"
    role="mark"
    tabindex="0"
    on:mouseover={() => {focusIn(region)}}
    on:mouseout={() => {focusOut(region)}}
    on:focus={() => {focusIn(region)}}
    on:blur={() => {focusOut(region)}}
    class:overWithSource={focusRegion === region && region.parts}
    class:overWithoutSource={focusRegion === region && !region.parts}
    title={tooltip(region)}>{region.content}</span>{/each}</pre>
{/await}

<style>
  .trackerDetail {
    float: right;
    width: 50%;
    height: 100%;
    overflow-y: auto;
  }
  .region {
    /* draw a vertical line after each part, without influencing size */
    margin-right: -1px;
    border-right: 1px solid gray;
  }
  .region:last-child {
    /* undo border */
    margin-right: 0;
    border-right: 0;
  }
  .overWithSource {
    background-color: lightblue;
  }
  .overWithoutSource {
    background-color: lightgray;
  }
</style>
