<script lang="ts">
  import type {ColorByIndex, Coloring} from './coloring';
  import {PathSelection, indexInPath, type ASelection} from './selection';

  export let path: string[] | null;

  export let selection: ASelection | null;
  export let coloring: Coloring;

  let selectionIndex: number | null;
  $: selectionIndex = indexInPath(selection, path);
  let colorByIndex: ColorByIndex;
  $: colorByIndex = coloring.calcColorByIndex(path);

  function click(index: number) {
    if (!path) return;
    selection = new PathSelection(path.slice(0, index + 1));
  }
</script>

<!-- @component
Shows path of a tracker.
That is a path as in TrackerDetailResponse.path, or as in the TrackerTree.
-->
{#if path}
  {#each path as name, i}
    {#if i != 0}{' / '}{/if}
    <button
      class:selected={i === selectionIndex}
      on:click={() => click(i)}
      style="background-color: {colorByIndex[i] || 'inherit'}">{name}</button
    >
  {/each}
{/if}

<style>
  button {
    padding: 0 0 0 0;
    color: var(--fg-1);
    cursor: pointer;
    border: none;
    margin: 0;
    text-align: left;
  }

  button.selected {
    border: 1px solid blue;
  }
</style>
