<script lang="ts">
  import type { NodeDetail, Tracker } from '../javatypes'
  import type { ColorByIndex, Coloring } from './coloring'
  import { indexInPath, type Selected } from './selection'

  export let expanded: boolean = false
  export let selectedTracker: Tracker | null
	export let selection: Selected | null
	export let coloring: Coloring
  export let node: NodeDetail

	let selectionIndex:number|null;
  $: selectionIndex = indexInPath(selection, node.path)
  let colorByIndex:ColorByIndex;
  $: colorByIndex = coloring.calcColorByIndex(node.path)

	function isSelected(selection: Selected|null):boolean {
		if (!selection || selection.type != "path") return false;
		return selection.type == "path" && arraysEqual(selection.path, node.path)
	}

	function arraysEqual(a:String[], b:String[]):boolean {
		return a.length === b.length && a.every((value, index) => value === b[index])
	}

  function backgroundColor(coloring: Coloring):string {
		for (var assignment of coloring.assignments) {
			if (assignment.selections.some(selection => isSelected(selection))) {
				return assignment.color;
			}
		}
		return "inherit"
  }

	function click(node: NodeDetail) {
		if (node.tracker) {
			selectedTracker = node.tracker
		}
		if (node.children.length > 0) {
			expanded = !expanded
		}
		selection = {
			type: "path",
			path: node.path
		}
	}

	/**
	 * Maps an index in `node.names` to an index in `node.path`.
	 * For example if `node.path` is a/b/c/d and `node.names` (the part shown by this node) is `c/d`
	 * then maps nameIndex 0 (c) to 2.
	 */
	function pathIndex(nameIndex:number):number {
		return node.path.length - node.names.length + nameIndex
	}
</script>

<button
  class:folder="{!node.tracker}"
  class:tracker="{!!node.tracker}"
  class:expanded
  class:origin={node.tracker?.origin}
  class:sink={node.tracker?.sink}
  class:selected={selectionIndex == node.path.length - 1}
	class:selected-tracker={node.tracker && node.tracker === selectedTracker}
	style="background-color: {backgroundColor(coloring)}"
  on:click={() => click(node)}>
	{#each node.names as name, i}
	  <!--
			When the whole path represented by this node is selected then we draw the selection border around the whole node
			(so apply .selected to the button); but when a sub-path is selected (e.g. 'a/b' in 'a/b/c') then we put the selection
			border only on the specific part (e.g. the 'b' span). Similar for coloring.
			That's a little inconsistent, but it allows clicking on a node in a tree to feel natural (selecting the whole node),
			while still giving an indication of when part of a path (that can't be selected by clicking in the tree, but can be
			selected in PathView) has been selected or colored.
		 -->
		{#if i != 0}{"/"}{/if}<span class="path-part"
			class:selected={pathIndex(i) === selectionIndex && i != node.names.length - 1}
			style="background-color: {colorByIndex[pathIndex(i)] || "inherit"}"
			>{name}</span>
	{/each}
</button>
{#if expanded}
	<ul>
		{#each node.children as child}
			<li>
				<svelte:self node={child} bind:selectedTracker={selectedTracker} bind:selection={selection} coloring={coloring}/>
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
    border: 1px solid blue
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

	ul {
		padding: 0.2em 0 0 0.5em;
		margin: 0 0 0 0.5em;
		list-style: none;
		border-left: 1px solid rgba(128, 128, 128, 0.4);
	}

	.path-part {
		display: inline-block
	}

	/* li {
		padding: 0.2em 0;
	} */
</style>
  