<script lang="ts">
  import TrackerTreeNode from './TrackerTreeNode.svelte'
  import type { Tracker, NodeDetail } from '../javatypes'
  import type { Selected } from './selection'
  import type { Coloring } from './coloring'
  
  export let selectedTracker: Tracker | null;
  export let selection: Selected | null;
  export let coloring: Coloring

  let showSinks:boolean = true
  let showOrigins:boolean = true

  let rootPromise: Promise<NodeDetail>;
  $: rootPromise = fetchTree(showSinks, showOrigins);

  const fetchTree = async (showSinks:boolean, showOrigins:boolean) => {
    const response = await fetch('/tree?' + new URLSearchParams({
      sinks: showSinks.toString(),
      origins: showOrigins.toString(),
    }))
    if (!response.ok) return Promise.reject(response)
    return response.json().then(r => enrich(r, null))
  }

  /** Fill in NodeDetail.path */
  const enrich = (node:NodeDetail, parent:NodeDetail | null):NodeDetail => {
    node.path = parent ? parent.path.concat(node.names) : []
    for (const child of node.children) {
      enrich(child, node)
    }
    return node
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
  <TrackerTreeNode node={root} bind:selectedTracker={selectedTracker} bind:selection={selection} coloring={coloring}/>
  {:catch error}
  <p style="color: red">{error.message}</p>
  {/await}
</div>

<style>
.tree {
  overflow-y: auto;
  height: 100%;
}
</style>
  