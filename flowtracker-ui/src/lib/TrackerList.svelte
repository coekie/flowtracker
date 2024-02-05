<script lang="ts">
  import { onMount } from 'svelte';
  import type { Tracker } from '../javatypes'

  let trackersPromise: Promise<Tracker[]> = new Promise(() => {});
  export let selectedTracker: Tracker;

  const fetchTrackers = async () => {
    const response = await fetch('/tracker')
    if (!response.ok) return Promise.reject(response)
    return response.json()
  }

  onMount(() => {
      trackersPromise = fetchTrackers();
  })
</script>

<div class="trackerList">
  {#await trackersPromise}
  <p>Loading...</p>
  {:then trackers}
  {#each trackers as tracker (tracker.id)}
    <div class="trackerListItem"
        class:trackerListItemSelected={tracker === selectedTracker}
        on:click={() => selectedTracker = tracker}>
    {tracker.description}
    </div>
  {/each}
  {:catch error}
  <p style="color: red">{error.message}</p>
  {/await}
</div>

<style>
.trackerList {
  float: left;
  width: 50%;
  height: 100%;
  overflow-y: auto;
}
.trackerListItem {
  border: 1px solid gray;
}
.trackerListItemSelected {
  background-color: lightblue;
}
</style>
  