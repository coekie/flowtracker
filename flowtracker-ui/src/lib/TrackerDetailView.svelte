<script lang="ts">
  import type { Tracker, TrackerDetail, Region } from '../javatypes'
  import type { PartPointer } from './selection'

  export let selectedTracker: Tracker | null;
  let trackerDetailPromise: Promise<TrackerDetail>;
  $: trackerDetailPromise = fetchTrackerDetail(selectedTracker);
  let focusRegion: Region | null;
  export let selection: PartPointer | null;

  const fetchTrackerDetail = async (tracker:Tracker | null) => {
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
    focusRegion = region
  }

  const focusOut = (region: Region) => {
    focusRegion = null
  }

  const click = (region: Region) => {
    if (region.parts.length > 0) {
      selection = region.parts[0]
    } else {
      selection = null
    }
  }
</script>

{#await trackerDetailPromise then trackerDetail}
  <!-- svelte-ignore a11y-no-noninteractive-tabindex -->
  <!-- svelte-ignore a11y-click-events-have-key-events -->
  <!-- svelte-ignore a11y-no-noninteractive-element-interactions -->
  <div class="trackerDetail">
  <pre>{#each trackerDetail.regions as region}<span class="region"
    role="mark"
    tabindex="0"
    on:mouseover={() => focusIn(region)}
    on:mouseout={() => focusOut(region)}
    on:focus={() => focusIn(region)}
    on:blur={() => focusOut(region)}
    on:click={() => click(region)}
    class:overWithSource={focusRegion === region && region.parts.length > 0}
    class:overWithoutSource={focusRegion === region && region.parts.length == 0}
    title={tooltip(region)}>{region.content}</span>{/each}</pre>
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
