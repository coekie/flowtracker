<script lang="ts">
  import SettingsView from './SettingsView.svelte'
  import TrackerDetailView from './TrackerDetailView.svelte'
  import type { Tracker } from '../javatypes'
  import type { SelectedRange } from './selection'
  import TrackerTree from './TrackerTree.svelte';

  let selectedTracker: Tracker;
  let selection: SelectedRange;
</script>

<div class="wrapper">
  <div class="left">
    <TrackerTree bind:selectedTracker={selectedTracker}/>
  </div>
  <div class="right">
    <div class="updown">
      <TrackerDetailView bind:selectedTracker={selectedTracker} bind:selection={selection}/>
    </div>
    <div class="updown">
      <TrackerDetailView selectedTracker={selection ? selection.tracker : null} selection={selection} targetTracker={selectedTracker}/>
    </div>
  </div>
</div>
<SettingsView />

<style>
  .wrapper {
    position: absolute;
    display: flex;
    top: 0;
    bottom: 50px;
    width: 100%;
  }

  .left {
    flex-basis: 30%;
    flex-grow: 1;
    overflow: hidden;
  }

  .right {
    flex-basis: 70%;
    flex-grow: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .updown {
    flex-basis: 50%;
    overflow: hidden;
  }
</style>
