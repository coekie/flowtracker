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
</script>

<Splitpanes theme="my-theme">
	<Pane>
    <TrackerTree bind:selectedTracker={mainTracker}/>
	</Pane>
	<Pane>
    <Splitpanes horizontal={true} theme="my-theme" >
      <Pane>
        <TrackerDetailView bind:viewTracker={mainTracker} bind:selection={selection}/>
      </Pane>
      <Pane>
        <TrackerDetailView viewTracker={selection ? selection.tracker : null} bind:selection={selection} targetTracker={mainTracker}/>
      </Pane>
    </Splitpanes>
	</Pane>
</Splitpanes>

<SettingsView />

<style>
</style>
