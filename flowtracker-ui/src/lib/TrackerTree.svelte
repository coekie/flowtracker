<script lang="ts">
  import { onMount } from 'svelte';
  import TrackerTreeNode from './TrackerTreeNode.svelte';
  import type { Tracker, Node } from '../javatypes'
  
  let rootPromise: Promise<Node> = new Promise(() => {});
  export let selectedTracker: Tracker;

  const fetchTree = async () => {
    const response = await fetch('/tree')
    if (!response.ok) return Promise.reject(response)
    return response.json()
  }

  onMount(() => {
      rootPromise = fetchTree();
  })
</script>

<div class="tree">
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
  float: left;
  width: 50%;
  height: 100%;
  overflow-y: auto;
}
</style>
  