<script lang="ts">
  import TrackerTreeNode from './TrackerTreeNode.svelte';
  import type { Tracker, Node } from '../javatypes'
  
  export let selectedTracker: Tracker;

  let showSinks:boolean = true
  let showOrigins:boolean = true

  let rootPromise: Promise<Node>;
  $: rootPromise = fetchTree(showSinks, showOrigins);

  const fetchTree = async (showSinks:boolean, showOrigins:boolean) => {
    const response = await fetch('/tree?' + new URLSearchParams({
      sinks: showSinks.toString(),
      origins: showOrigins.toString(),
    }))
    if (!response.ok) return Promise.reject(response)
    return response.json()
  }
</script>

<div class="tree">
  <div>
    <input type="checkbox" id="sinksCheckbox" bind:checked={showSinks}/>
    <label for="sinksCheckbox">Sinks</label>
    <input type="checkbox" id="originsCheckbox" bind:checked={showOrigins}/>
    <label for="originsCheckbox">Origins</label>
  </div>

  {#await rootPromise}
  <p>Loading...</p>
  {:then root}
  <TrackerTreeNode node={root} bind:selectedTracker={selectedTracker}/>
  {:catch error}
  <p style="color: red">{error.message}</p>
  {/await}
</div>

<style>
.tree {
  overflow-y: auto;
}
</style>
  