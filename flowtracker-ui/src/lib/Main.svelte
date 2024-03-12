<script lang="ts">
  import { Pane, Splitpanes } from 'svelte-splitpanes';

  import SettingsView from './SettingsView.svelte'
  import TrackerDetailView from './TrackerDetailView.svelte'
  import type { Tracker } from '../javatypes'
  import type { SelectedRange } from './selection'
  import TrackerTree from './TrackerTree.svelte';

  /** Tracker that is selected in the tree, shown in the top TrackerDetailView */
  let mainTracker: Tracker | null;

  /** Part used in mainTracker that is selected, shown in the bottom TrackerDetailView */
  let selection: SelectedRange | null = null;

  let sinkView:TrackerDetailView;
  let originView:TrackerDetailView;
</script>

<Splitpanes theme="my-theme">
	<Pane>
    <TrackerTree bind:selectedTracker={mainTracker}/>
	</Pane>
	<Pane>
    <Splitpanes horizontal={true} theme="my-theme" >
      <Pane>
        <TrackerDetailView
          bind:this={sinkView}
          bind:viewTracker={mainTracker}
          bind:selection={selection}
          ondblclick={() => originView?.scrollToSelection()}/>
      </Pane>
      <Pane>
        <TrackerDetailView
          bind:this={originView}
          viewTracker={selection ? selection.tracker : null}
          bind:selection={selection} targetTracker={mainTracker}
          ondblclick={() => sinkView?.scrollToSelection()}/>
      </Pane>
    </Splitpanes>
	</Pane>
</Splitpanes>

<SettingsView />

<style>
</style>
