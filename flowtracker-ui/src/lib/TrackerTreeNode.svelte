<script lang="ts">
  import type { NodeDetail, Tracker } from '../javatypes'
  import type { Coloring } from './coloring'
  import type { Selected } from './selection'

  export let expanded: boolean = false
  export let selectedTracker: Tracker | null
	export let selection: Selected | null
	export let coloring: Coloring
  export let node: NodeDetail

	function nodeName(node: NodeDetail):String {
		return node.names.join("/")
	}

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
</script>

<button
  class:folder="{!node.tracker}"
  class:tracker="{!!node.tracker}"
  class:expanded
  class:origin={node.tracker?.origin}
  class:sink={node.tracker?.sink}
  class:selected={isSelected(selection)}
	class:selected-tracker={node.tracker && node.tracker === selectedTracker}
	style="background-color: {backgroundColor(coloring)}"
  on:click={() => click(node)}>
  {nodeName(node)}
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

  button.selected {
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

	/* li {
		padding: 0.2em 0;
	} */
</style>
  