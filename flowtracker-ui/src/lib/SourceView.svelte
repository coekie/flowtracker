<script lang="ts">
  import {tick} from 'svelte';
  import type {Line, Source} from '../javatypes';
  import type {Coloring} from './coloring';
  import {type ASelection, RangeSelection} from './selection';

  export let trackerId: number;
  /** See TrackerDetailView.selection */
  export let selection: ASelection | null;
  export let coloring: Coloring;

  let sourcePromise: Promise<Source>;
  $: sourcePromise = fetchSource(trackerId);

  let pre: HTMLPreElement;

  const fetchSource = async (trackerId: number) => {
    const response = await fetch('code/' + trackerId);
    if (!response.ok) throw new Error(response.statusText);
    return response.json();
  };

  function isSelected(line: Line, selection: ASelection | null): boolean {
    if (selection instanceof RangeSelection) {
      return line.parts.some(
        part =>
          selection.tracker.id == trackerId &&
          selection.offset + selection.length > part.offset &&
          selection.offset < part.offset + part.length
      );
    }
    return false;
  }

  function backgroundColor(line: Line, coloring: Coloring): string {
    return coloring.backgroundColor(s => isSelected(line, s));
  }

  /** scroll the first selected line into view */
  export function scrollToSelection() {
    pre?.querySelector('.selected')?.scrollIntoView();
  }

  /** waits for rendering and then scrolls the first selected line into view */
  function scrollToSelectionOnFirstRender(_: HTMLPreElement) {
    tick().then(scrollToSelection);
  }
</script>

<!-- @component
Shows source code of a class.
-->
{#await sourcePromise then source}
  <pre
    bind:this={pre}
    use:scrollToSelectionOnFirstRender>{#each source.lines as line}<div
        class:selected={isSelected(line, selection)}
        style="background-color: {backgroundColor(
          line,
          coloring
        )}">{line.content}</div>{/each}</pre>
{/await}

<style>
  pre {
    overflow: auto;
    height: 100%;
  }

  .selected {
    border: 1px solid blue;
  }
</style>
