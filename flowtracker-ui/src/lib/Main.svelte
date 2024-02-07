<script lang="ts">
  import Settings from './Settings.svelte'
  import TrackerDetailView from './TrackerDetailView.svelte'
  import TrackerList from './TrackerList.svelte'
  import type { Tracker, TrackerDetail, TrackerPart } from '../javatypes'

  let selectedTracker: Tracker;
</script>

<div class="trackersWrapper">
  <TrackerList bind:selectedTracker={selectedTracker}/>
  <TrackerDetailView bind:selectedTracker={selectedTracker} />
</div>
<Settings />

<style>
  .trackersWrapper {
    position: absolute;
    top: 0;
    bottom: 50px;
    width: 100%;
  }
</style>
