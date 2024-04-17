<script lang="ts">
  import TrackerTreeNode from './TrackerTreeNode.svelte';
  import type {Tracker, NodeDetail} from '../javatypes';
  import type {ASelection} from './selection';
  import type {Coloring} from './coloring';

  export let selectedTracker: Tracker | null;
  export let secondaryTracker: Tracker | null;
  export let selection: ASelection | null;
  export let coloring: Coloring;

  let showSinks: boolean = true;
  let showOrigins: boolean = true;

  let rootPromise: Promise<NodeDetail>;
  $: rootPromise = fetchTree(showSinks, showOrigins);

  async function fetchTree(
    showSinks: boolean,
    showOrigins: boolean
  ): Promise<NodeDetail> {
    let url: string;
    if (showSinks && showOrigins) {
      url = '/all';
    } else if (showSinks) {
      url = '/sinks';
    } else if (showOrigins) {
      url = '/origins';
    } else {
      // nothing, no sinks no origins => return empty root node
      return {
        names: [],
        children: [],
        tracker: null,
        path: [],
      };
    }
    const response = await fetch('tree' + url);
    if (!response.ok) return Promise.reject(response);
    return response.json().then(r => enrich(r, null));
  }

  /** Fill in NodeDetail.path */
  function enrich(node: NodeDetail, parent: NodeDetail | null): NodeDetail {
    node.path = parent ? parent.path.concat(node.names) : [];
    for (const child of node.children) {
      enrich(child, node);
    }
    return node;
  }
</script>

<div class="tree">
  <div>
    <input type="checkbox" id="sinksCheckbox" bind:checked={showSinks} />
    <label for="sinksCheckbox">Sinks</label>
    <input type="checkbox" id="originsCheckbox" bind:checked={showOrigins} />
    <label for="originsCheckbox">Origins</label>
  </div>

  {#await rootPromise}
    <p>Loading...</p>
  {:then root}
    <TrackerTreeNode
      node={root}
      bind:selectedTracker
      bind:secondaryTracker
      bind:selection
      {coloring}
    />
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
