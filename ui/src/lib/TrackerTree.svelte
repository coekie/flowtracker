<script lang="ts">
  import TrackerTreeNode from './TrackerTreeNode.svelte';
  import type {NodeDetail} from '../javatypes';
  import {
    pathStartsWith,
    type ASelection,
    type OnTrackerSelected,
  } from './selection';
  import type {Coloring} from './coloring';
  import {onMount} from 'svelte';

  export let onTrackerSelected: OnTrackerSelected | null;
  export let selection: ASelection | null;
  export let coloring: Coloring;

  let showSinks: boolean = true;
  let showOrigins: boolean = true;

  let rootPromise: Promise<NodeDetail>;
  $: rootPromise = fetchTree(showSinks, showOrigins);
  let root: NodeDetail;

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
    const response = await fetch('tree' + url, {cache: 'no-cache'});
    if (!response.ok) return Promise.reject(response);
    return response.json().then(r => {
      const enriched = enrich(r, null);
      root = enriched;
      applyUrlHash(enriched);
      return enriched;
    });
  }

  /** Fill in NodeDetail.path */
  function enrich(node: NodeDetail, parent: NodeDetail | null): NodeDetail {
    node.path = parent ? parent.path.concat(node.names) : [];
    for (const child of node.children) {
      enrich(child, node);
    }
    return node;
  }

  /** Open the tracker specified in the URL */
  function applyUrlHash(root: NodeDetail): void {
    const hash = window.location.hash;
    if (hash != null && hash.length != 0) {
      var node = root;
      var remaining = hash.substring(1).split('/').map(decodeURIComponent);

      while (remaining.length > 0) {
        let next = node.children.find(child =>
          pathStartsWith(remaining, child.names)
        );
        if (!next) {
          next = node.children.find(child =>
            looselyStartsWith(remaining, child.names)
          );
        }
        if (!next) {
          return;
        }
        remaining = remaining.splice(next.names.length);
        node = next;
      }
      if (node.tracker && onTrackerSelected) {
        onTrackerSelected(node.tracker);
      }
    }
  }

  // like pathStartsWith, but handles wildcard (*) at the end of each element.
  // this is so that we can make links into a demo to a node that contains semi-random
  // elements like port numbers.
  function looselyStartsWith(a: string[], b: string[]): boolean {
    return (
      a &&
      b &&
      a.length >= b.length &&
      b.every((n, i) =>
        a[i].endsWith('*')
          ? n.startsWith(a[i].substring(n.length - 1))
          : a[i] == n
      )
    );
  }

  onMount(() => {
    const listener = () => {
      if (root) {
        applyUrlHash(root);
      }
    };
    window.addEventListener('popstate', listener);
    return () => window.removeEventListener('popstate', listener);
  });
</script>

<div class="tree">
  <!-- Checkboxes to show & hide sinks or origins. Disabled for now because it's ugly. -->
  <div style="display: none">
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
      {onTrackerSelected}
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
