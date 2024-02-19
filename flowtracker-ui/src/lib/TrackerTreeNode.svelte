<script lang="ts">
  import { onMount } from 'svelte';
  import type { Node, Tracker } from '../javatypes'

  export let expanded: boolean = false;
  export let selectedTracker: Tracker;
  export let node: Node;

	function toggle() {
		expanded = !expanded;
	}
</script>

<button class="folder" class:expanded on:click={toggle}>{node.name}</button>

{#if expanded}
	<ul>
		{#each node.children as child}
			<li>
				<svelte:self node={child} bind:selectedTracker={selectedTracker}/>
			</li>
		{/each}
		{#each node.trackers as tracker}
			<li>
        <button class="tracker"
          class:expanded
          class:origin={tracker.origin}
          class:sink={tracker.sink}
          class:selected={tracker === selectedTracker}
          on:click={() => selectedTracker = tracker}>
          {tracker.description}
        </button>
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
  