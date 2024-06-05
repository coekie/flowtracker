<script lang="ts">
  import type {Source} from '../javatypes';

  export let trackerId: number;

  let sourcePromise: Promise<Source>;
  $: sourcePromise = fetchSource(trackerId);

  const fetchSource = async (trackerId: number) => {
    const response = await fetch('code/' + trackerId);
    if (!response.ok) throw new Error(response.statusText);
    return response.json();
  };
</script>

<!-- @component
Shows source code of a class.
-->
{#await sourcePromise then source}
  <pre>{#each source.lines as line}{line.content}{/each}</pre>
{/await}

<style>
  pre {
    overflow: auto;
    height: 100%;
  }
</style>
