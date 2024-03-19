<script lang="ts">
  import {Pane, Splitpanes} from 'svelte-splitpanes';

  import SettingsView from './SettingsView.svelte';
  import TrackerDetailView from './TrackerDetailView.svelte';
  import type {Tracker} from '../javatypes';
  import type {ASelection} from './selection';
  import TrackerTree from './TrackerTree.svelte';
  import ColoringView from './ColoringView.svelte';
  import {Coloring} from './coloring';

  /** Tracker that is selected in the tree, shown in the top TrackerDetailView */
  let mainTracker: Tracker | null = null;
  /** Tracker that is selected in the top TrackerDetailView, shown in the bottom TrackerDetailView */
  let secondaryTracker: Tracker | null = null;

  /** The last clicked thing: a PathSelection (e.g. a Tracker) or a RangeSelection */
  let selection: ASelection | null = null;

  let coloring: Coloring = new Coloring();

  let sinkView: TrackerDetailView;
  let originView: TrackerDetailView;
</script>

<div class="wrapper">
  <div class="panes">
    <Splitpanes theme="my-theme">
      <Pane>
        <TrackerTree
          bind:selectedTracker={mainTracker}
          bind:selection
          {coloring}
        />
      </Pane>
      <Pane>
        <Splitpanes horizontal={true} theme="my-theme">
          <Pane>
            <TrackerDetailView
              bind:this={sinkView}
              viewTracker={mainTracker}
              bind:selection
              bind:coloring
              bind:secondaryTracker
              ondblclick={() => originView?.scrollToSelection()}
            />
          </Pane>
          <Pane>
            <TrackerDetailView
              bind:this={originView}
              viewTracker={secondaryTracker}
              bind:selection
              bind:coloring
              targetTracker={mainTracker}
              ondblclick={() => sinkView?.scrollToSelection()}
            />
          </Pane>
        </Splitpanes>
      </Pane>
    </Splitpanes>
  </div>
  <div class="footer">
    <SettingsView /><ColoringView bind:coloring bind:selection />
  </div>
</div>

<style>
  .wrapper {
    display: flex;
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
    display: flex;
    border-top: 3px solid #ccc; /* default: 1px solid #eee */
  }
</style>
