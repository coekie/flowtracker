<script lang="ts">
  import { onMount } from 'svelte';
  import Settings from './Settings.svelte'
  import TrackerList from './TrackerList.svelte'
  import type { Tracker, TrackerDetail, TrackerPart } from '../javatypes'

  let selectedTracker: Tracker;
  let trackerDetailPromise: Promise<TrackerDetail>;
  $: trackerDetailPromise = fetchTrackerDetail(selectedTracker);

  const fetchTrackerDetail = async (tracker:Tracker) => {
    if (!tracker) {
      return new Promise(() => {})
    }
    const response = await fetch('/tracker/' + tracker.id)
		if (!response.ok) throw new Error(response.statusText)
		return response.json()
  }

  const tooltip = (part: TrackerPart) => {
    return 'source=' + (part.source?.description || 'unknown') + '\n' +
      'sourceOffset=' + part.sourceOffset + '\n' +
      'context=' + part.sourceContext;
  }
</script>

<div class="trackersWrapper">
  <TrackerList bind:selectedTracker={selectedTracker}/>
  {#await trackerDetailPromise then trackerDetail}
    <pre class="trackerDetail">{#each trackerDetail.parts as part}<span class="trackerDetailPart"
                                     title={tooltip(part)}>{part.content}</span>{/each}</pre>
   {/await}
</div>
<Settings />

<style>
  .trackersWrapper {
    position: absolute;
    top: 0;
    bottom: 50px;
    width: 100%;
  }
  .trackerDetail {
    float: right;
    width: 50%;
    height: 100%;
    overflow-y: auto;
  }
  .trackerDetailPart {
    /* draw a vertical line after each part, without influencing size */
    margin-right: -1px;
    border-right: 1px solid gray;
  }
  .trackerDetailPart:last-child {
    /* undo border */
    margin-right: 0;
    border-right: 0;
  }
</style>
