<script lang="ts">
  import type { NodeDetail, Tracker } from '../javatypes'

  export let expanded: boolean = false;
  export let selectedTracker: Tracker | null;
  export let node: NodeDetail;

	function toggle() {
		expanded = !expanded;
	}

	function nodeName(node: NodeDetail):String {
		return node.names.join("/")
	}
</script>

{#if node.tracker}
  <button class="tracker"
    class:expanded
    class:origin={node.tracker.origin}
    class:sink={node.tracker.sink}
    class:selected={node.tracker === selectedTracker}
    on:click={() => selectedTracker = node.tracker}>
    {nodeName(node)}
  </button>
{:else}
  <button class="folder" class:expanded on:click={toggle}>{nodeName(node)}</button>
	{#if expanded}
	<ul>
		{#each node.children as child}
			<li>
				<svelte:self node={child} bind:selectedTracker={selectedTracker}/>
			</li>
		{/each}
	</ul>
	{/if}
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
  