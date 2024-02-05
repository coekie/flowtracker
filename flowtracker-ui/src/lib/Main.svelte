<script lang="ts">
  import { onMount } from 'svelte';
  import Settings from './Settings.svelte'

  let trackersPromise = new Promise(() => {});
  let selectedTracker;
  let trackerDetailPromise = new Promise(() => {});

  const fetchTrackers = async () => {
    const response = await fetch('/tracker')
    if (!response.ok) return Promise.reject(response)
		return response.json()
  }

  const selectTracker = async (tracker) => {
    console.log("selected: ", tracker)
    selectedTracker = tracker
    trackerDetailPromise = fetchTrackerDetail(tracker)
  }

  const fetchTrackerDetail = async (tracker) => {
    const response = await fetch('/tracker/' + tracker.id)
		if (!response.ok) throw new Error(response)
		return response.json()
  }

  const tooltip = (part) => {
    return 'source=' + (part.source?.description || 'unknown') + '\n' +
      'sourceOffset=' + part.sourceOffset + '\n' +
      'context=' + part.sourceContext;
  }

  onMount(() => {
    trackersPromise = fetchTrackers();
  })
</script>

<div class="trackersWrapper">
  <div class="trackerList">
    {#await trackersPromise}
      <p>Loading...</p>
    {:then trackers}
      {#each trackers as tracker (tracker.id)}
        <div class="trackerListItem"
            class:trackerListItemSelected={tracker === selectedTracker}
            on:click={selectTracker(tracker)}>
          {tracker.description}
        </div>
      {/each}
    {:catch error}
      <p style="color: red">{error.message}</p>
    {/await}
  </div>
  {#await trackerDetailPromise then trackerDetail}
    <pre class="trackerDetail">{#each trackerDetail.parts as part}<span class="trackerDetailPart"
                                     ng-repeat="part in selectedTrackerDetail.parts track by $index"
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
  .trackerList {
    float: left;
    width: 50%;
    height: 100%;
    overflow-y: auto;
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
  .trackerListItem {
    border: 1px solid gray;
  }
  .trackerListItemSelected {
    background-color: lightblue;
  }
</style>
