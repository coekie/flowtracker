<script lang="ts">
  import {Pane, Splitpanes} from 'svelte-splitpanes';
  export let showSplit: boolean;
</script>

<!-- @component
Split pane showing tracker details and source code.
Wrapper around Splitpanes, mostly to work around a splitpanes bug that makes them not work in tests.
Also makes the split pane optional based on `showSplit`, so that we do not create a split unnecessarily.
-->

{#if !showSplit}
  <slot name="one" />
{:else if navigator.userAgent.includes('jsdom')}
  <!-- for tests: only render the content -->
  <slot name="one" />
  <slot name="two" />
{:else}
  <Splitpanes theme="my-theme">
    <Pane>
      <slot name="one" />
    </Pane>
    <Pane>
      <slot name="two" />
    </Pane>
  </Splitpanes>
{/if}
