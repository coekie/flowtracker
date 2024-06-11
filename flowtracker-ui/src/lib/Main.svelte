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

  /** Width of the split pane showing the tree. On a small screen, use the full width initially */
  let treePaneSize: number = window.innerWidth < 600 ? 100 : 30;
  /** Old width to restore `treePaneSize` to if the tree was hidden and then shown again */
  let restoreTreePaneSize: number = 0;
  $: restoreTreePaneSize =
    treePaneSize > 0 ? treePaneSize : restoreTreePaneSize;

  let sinkView: TrackerDetailView;
  let originView: TrackerDetailView;

  /** Show or hide the split pane that contains the tree */
  function toggleTree() {
    if (treePaneSize == 0) {
      treePaneSize = restoreTreePaneSize;
    } else {
      treePaneSize = 0;
    }
  }

  function onTrackerSelectedInTree(tracker: Tracker) {
    // reset secondaryTracker, to prevent ever having a secondaryTracker
    // that is unrelated to the mainTracker
    secondaryTracker = null;

    mainTracker = tracker;

    // if we selected a tracker when all that's visible is the tree (usually because the screen
    // isn't wide enough) then hide the tree, so we can see the TrackerDetailView.
    if (treePaneSize == 100) {
      treePaneSize = 0;
    }
  }
</script>

<div class="wrapper">
  <div class="panes">
    <Splitpanes theme="my-theme">
      <Pane bind:size={treePaneSize}>
        <TrackerTree
          onTrackerSelected={onTrackerSelectedInTree}
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
    <button
      on:click={toggleTree}
      class="treeToggle"
      class:close={treePaneSize > 0}
      title="Toggle tree"
    />
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

  .treeToggle {
    background: none;
    background-image: url(/left_panel_open.svg);
    background-repeat: no-repeat;
    background-position: center;
    width: 3em;
    height: 100%;
    border: 1px solid black;
  }

  .treeToggle.close {
    background-image: url(/left_panel_close.svg);
  }
</style>
