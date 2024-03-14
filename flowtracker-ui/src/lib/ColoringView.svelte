<script lang="ts">
  import { ColorAssignment, Coloring } from './coloring'
  import type { SelectedRange } from './selection'

  export let coloring: Coloring
  export let selection: SelectedRange | null

  function add():void {
    coloring.add(selection)
    coloring = coloring
  }

  function reassign(assignment: ColorAssignment, index:number, e:MouseEvent) {
    if (e.shiftKey) {
      if (selection) {
        // TODO remove if it's already in there. requires equality check on selection.
        assignment.selections.push(selection)
      }
    } else {
      if (selection) {
        assignment.selections = [selection]
      } else {
        coloring.assignments.splice(index, 1)
      }
    }
    coloring = coloring
    e.preventDefault()
  }
</script>

<!-- @component
Component to edit coloring
-->

<div class="coloringwrapper">
<div><b>Coloring:</b></div>
{#each coloring.assignments as assignment, index}
<a href={"#"} on:click={e => reassign(assignment, index, e)} class="square" style="background-color:{assignment.color}">&nbsp;</a>
{/each}
{#if coloring.canAdd()}
<a href={"#"} on:click={add} class="square">+</a>
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
    text-decoration: none;
    text-align: center;
  }
</style>