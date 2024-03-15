<script lang="ts">
  import { Pane, Splitpanes } from 'svelte-splitpanes';

  import SettingsView from './SettingsView.svelte'
  import TrackerDetailView from './TrackerDetailView.svelte'
  import type { Tracker } from '../javatypes'
  import type { SelectedRange } from './selection'
  import TrackerTree from './TrackerTree.svelte';
  import ColoringView from './ColoringView.svelte';
  import { Coloring } from './coloring';

  /** Tracker that is selected in the tree, shown in the top TrackerDetailView */
  let mainTracker: Tracker | null = null

  /** Part used in mainTracker that is selected, shown in the bottom TrackerDetailView */
  let selection: SelectedRange | null = null

  let coloring: Coloring = new Coloring()

  let sinkView: TrackerDetailView
  let originView: TrackerDetailView
</script>

<div class="wrapper">
  <div class="panes">
  <Splitpanes theme="my-theme">
    <Pane>
      <TrackerTree bind:selectedTracker={mainTracker} bind:selection={selection} coloring={coloring}/>
    </Pane>
    <Pane>
      <Splitpanes horizontal={true} theme="my-theme" >
        <Pane>
          <TrackerDetailView
            bind:this={sinkView}
            bind:viewTracker={mainTracker}
            bind:selection={selection}
            bind:coloring={coloring}
            ondblclick={() => originView?.scrollToSelection()}/>
        </Pane>
        <Pane>
          <TrackerDetailView
            bind:this={originView}
            viewTracker={selection ? selection.tracker : null}
            bind:selection={selection}
            bind:coloring={coloring}
            targetTracker={mainTracker}
            ondblclick={() => sinkView?.scrollToSelection()}/>
        </Pane>
      </Splitpanes>
    </Pane>
  </Splitpanes>
</div>
<div class="footer"><SettingsView /><ColoringView bind:coloring={coloring} bind:selection={selection}/></div>
</div>

<style>
  .wrapper {
    display:flex;
    flex-direction: column;
    height: 100%;
    width: 100%;
    overflow: hidden;
  }

  .panes {
    flex: 1;
    overflow: hidden;
  }

  .footer {
    display:flex;
    border-top: 3px solid #ccc; /* default: 1px solid #eee */
  }
</style>
