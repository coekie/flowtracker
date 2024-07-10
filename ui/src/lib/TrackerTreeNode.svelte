<script lang="ts">
  import type {NodeDetail} from '../javatypes';
  import type {ColorByIndex, Coloring} from './coloring';
  import {
    indexInPath,
    PathSelection,
    type ASelection,
    type OnTrackerSelected,
  } from './selection';

  export let onTrackerSelected: OnTrackerSelected | null;
  export let selection: ASelection | null;
  export let coloring: Coloring;
  export let node: NodeDetail;
  /** If this node is expanded; if children are visible. Initially only root node is expanded. */
  export let expanded: boolean = node.path.length == 0;

  let selectionIndex: number | null;
  $: selectionIndex = indexInPath(selection, node.path);
  let colorByIndex: ColorByIndex;
  $: colorByIndex = coloring.calcColorByIndex(node.path);

  function click(node: NodeDetail) {
    if (node.tracker && onTrackerSelected) {
      onTrackerSelected(node.tracker);
    }
    if (node.children.length > 0) {
      expanded = !expanded;
    }
    selection = new PathSelection(node.path);
  }

  /**
   * Maps an index in `node.names` to an index in `node.path`.
   * For example if `node.path` is a/b/c/d and `node.names` (the part shown by this node) is `c/d`
   * then maps nameIndex 0 (c) to 2.
   */
  function pathIndex(nameIndex: number): number {
    return node.path.length - node.names.length + nameIndex;
  }
</script>

<button
  class:folder={!node.tracker}
  class:tracker={!!node.tracker}
  class:expanded
  class:origin={node.tracker?.origin}
  class:sink={node.tracker?.sink}
  class:selected={selectionIndex == node.path.length - 1}
  class:root={node.path.length == 0}
  style="background-color: {colorByIndex[node.path.length - 1] || 'inherit'}"
  on:click={() => click(node)}
>
  {#each node.names as name, i}
    <!--
      When the whole path represented by this node is selected then we draw the selection border around the whole node
      (so apply .selected to the button); but when a sub-path is selected (e.g. 'a/b' in 'a/b/c') then we put the selection
      border only on the specific part (e.g. the 'b' span). Similar for coloring.
      That's a little inconsistent, but it allows clicking on a node in a tree to feel natural (selecting the whole node),
      while still giving an indication of when part of a path (that can't be selected by clicking in the tree, but can be
      selected in PathView) has been selected or colored.
		-->
    {#if i != 0}{'/'}{/if}<span
      class="path-part"
      class:selected={pathIndex(i) === selectionIndex &&
        i != node.names.length - 1}
      style="background-color: {(i != node.names.length - 1 &&
        colorByIndex[pathIndex(i)]) ||
        'inherit'}">{name}</span
    >
  {/each}
</button>
{#if expanded}
  <ul class:root={node.path.length == 0}>
    {#each node.children as child}
      <li>
        <svelte:self
          node={child}
          {onTrackerSelected}
          bind:selection
          {coloring}
        />
      </li>
    {/each}
  </ul>
{/if}

<style>
  button {
    padding: 0 0 0 1.25em;
    color: var(--fg-1);
    cursor: pointer;
    border: none;
    margin: 0;
    text-align: left;
  }

  .selected {
    border: 1px solid blue;
  }

  .folder {
    background: url(/folder.svg) 0 0.1em no-repeat;
    background-size: 1em 1em;
  }

  .folder.expanded {
    background-image: url(/folder-open.svg);
  }

  .tracker {
    background-color: var(--bg-main);
  }

  .tracker.origin {
    background: url(/in.svg) 0 0.1em no-repeat;
    background-size: 1em 1em;
  }

  .tracker.sink {
    background: url(/out.svg) 0 0.1em no-repeat;
    background-size: 1em 1em;
  }

  button.root {
    display: none;
  }

  ul {
    padding: 0.2em 0 0 0.5em;
    list-style: none;
  }

  ul:not(.root) {
    margin: 0 0 0 0.5em;
    border-left: 1px solid rgba(128, 128, 128, 0.4);
  }

  ul.root {
    margin: 0 0 0 0;
  }

  .path-part {
    display: inline-block;
  }
</style>
