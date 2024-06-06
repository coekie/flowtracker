<script lang="ts">
  import type {Line, Source} from '../javatypes';
    import type { Coloring } from './coloring';
    import { type ASelection, RangeSelection } from './selection';

  export let trackerId: number;
  /** See TrackerDetailView.selection */
  export let selection: ASelection | null;
  export let coloring: Coloring;

  let sourcePromise: Promise<Source>;
  $: sourcePromise = fetchSource(trackerId);

  const fetchSource = async (trackerId: number) => {
    const response = await fetch('code/' + trackerId);
    if (!response.ok) throw new Error(response.statusText);
    return response.json();
  };

  function isSelected(line: Line, selection: ASelection | null): boolean {
    if (selection instanceof RangeSelection) {
      return line.parts.some(part => selection.offset + selection.length > part.offset
        && selection.offset < part.offset + part.length
      );
    }
    return false;
  }

  function backgroundColor(line: Line, coloring: Coloring): string {
    return coloring.backgroundColor(s => isSelected(line, s));
  }

</script>

<!-- @component
Shows source code of a class.
-->
{#await sourcePromise then source}
  <pre>{#each source.lines as line}<div
    class:selected={isSelected(line, selection)}
    style="background-color: {backgroundColor(line, coloring)}"
    >{line.content}</div>{/each}</pre>
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
