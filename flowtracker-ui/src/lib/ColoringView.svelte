<script lang="ts">
  import { Coloring } from './coloring'
    import type { SelectedRange } from './selection'

  export let coloring: Coloring
  export let selection: SelectedRange | null

  const add = () => {
    coloring.add(selection)
    coloring = coloring
  }
</script>

<!-- @component
Component to edit coloring
-->

<div class="coloringwrapper">
<div><b>Coloring:</b></div>
{#each coloring.assignments as assignment}
<span class="square" style="background-color:{assignment.color}">&nbsp;</span>
{/each}
{#if coloring.canAdd()}
<a class="square add" href={"#"} on:click={add}>+</a>
{/if}
</div>

<style>
  .coloringwrapper {
    margin: .5em
  }

  .square {
    display: inline-block;
    width: 1em;
    height: 1em;
    margin: .1em;
    border: 1px solid black;
  }

  .add {
    text-decoration: none;
    text-align: center;
  }
</style>