<script lang="ts">
  import {ColorAssignment, Coloring} from './coloring';
  import type {ASelection} from './selection';

  export let coloring: Coloring;
  export let selection: ASelection | null;

  function add(): void {
    coloring.add(selection);
    coloring = coloring;
  }

  function reassign(assignment: ColorAssignment, index: number, e: MouseEvent) {
    if (selection) {
      if (assignment.remove(selection)) {
        if (assignment.selections.length == 0) {
          coloring.assignments.splice(index, 1);
        }
      } else {
        assignment.selections.push(selection);
      }
    }
    coloring = coloring;
    e.preventDefault();
  }

  function remove(index: number, e: MouseEvent) {
    coloring.assignments.splice(index, 1);
    coloring = coloring;
    e.preventDefault();
  }
</script>

<!-- @component
Component to edit coloring
-->

<div class="coloringwrapper">
  {#each coloring.assignments as assignment, index}
    <a
      href={'javascript:;'}
      on:click={e => reassign(assignment, index, e)}
      on:contextmenu={e => remove(index, e)}
      class="square"
      style="background-color:{assignment.color}">&nbsp;</a
    >
  {/each}
  {#if coloring.canAdd()}
    <a href={'javascript:;'} on:click={add} class="square plus">+</a>
  {/if}
</div>

<style>
  .coloringwrapper {
    margin: 0.5em;
  }

  .square {
    display: inline-block;
    width: 1em;
    height: 1em;
    line-height: 1em; /* center the + */
    margin: 0.1em;
    border: 1px solid black;
    text-decoration: none;
    text-align: center;
    font-weight: bolder;
    font-size: 1.75em;
  }

  .plus {
    background-image: linear-gradient(to bottom right, #ff9999, #99ffff);
  }
</style>
