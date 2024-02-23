<script lang="ts">
  import SettingsView from './SettingsView.svelte'
  import TrackerDetailView from './TrackerDetailView.svelte'
  import type { Tracker, TrackerDetail, TrackerPart } from '../javatypes'
  import TrackerTree from './TrackerTree.svelte';

  let selectedTracker: Tracker;
</script>

<div class="trackersWrapper">
  <TrackerTree bind:selectedTracker={selectedTracker}/>
  <TrackerDetailView bind:selectedTracker={selectedTracker} />
</div>
<SettingsView />

<style>
  .trackersWrapper {
    position: absolute;
    top: 0;
    bottom: 50px;
    width: 100%;
  }
</style>
