<script lang="ts">
  export let path : String[];
</script>

<!-- @component
Shows path of a tracker.
That is a path as in TrackerDetailResponse.path, or as n the TrackerTree.
(For now this is trivial, but a separate component because it's planned to become bigger, more interactive)
-->

{#each path as name, i}
  {#if i != 0}{" / "}{/if}
  {name}
{/each}
