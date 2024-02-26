<script lang="ts">
  import type { Tracker, TrackerDetail, TrackerPart } from '../javatypes'

  export let selectedTracker: Tracker;
  let trackerDetailPromise: Promise<TrackerDetail>;
  $: trackerDetailPromise = fetchTrackerDetail(selectedTracker);
  let focusPart: TrackerPart | null;

  const fetchTrackerDetail = async (tracker:Tracker) => {
    if (!tracker) {
      return new Promise(() => {})
    }
    const response = await fetch('/tracker/' + tracker.id)
		if (!response.ok) throw new Error(response.statusText)
		return response.json()
  }

  const tooltip = (part: TrackerPart) => {
    return 'source=' + (part.source?.description || 'unknown') + '\n' +
      'sourceOffset=' + part.sourceOffset + '\n' +
      'context=' + part.sourceContext;
  }

  const focusIn = (part: TrackerPart) => {
    if (part != null) {
      focusPart = part
    }
  }
  const focusOut = (part: TrackerPart) => {
    if (part != null) {
      focusPart = part
    }
  }
</script>

{#await trackerDetailPromise then trackerDetail}
  <!-- svelte-ignore a11y-no-noninteractive-tabindex -->
  <pre class="trackerDetail">{#each trackerDetail.parts as part}<span class="trackerDetailPart"
    role="mark"
    tabindex="0"
    on:mouseover={() => {focusIn(part)}}
    on:mouseout={() => {focusOut(part)}}
    on:focus={() => {focusIn(part)}}
    on:blur={() => {focusOut(part)}}
    class:overWithSource={focusPart === part && part.source}
    class:overWithoutSource={focusPart === part && !part.source}
    title={tooltip(part)}>{part.content}</span>{/each}</pre>
{/await}

<style>
  .trackerDetail {
    float: right;
    width: 50%;
    height: 100%;
    overflow-y: auto;
  }
  .trackerDetailPart {
    /* draw a vertical line after each part, without influencing size */
    margin-right: -1px;
    border-right: 1px solid gray;
  }
  .trackerDetailPart:last-child {
    /* undo border */
    margin-right: 0;
    border-right: 0;
  }
  .overWithSource {
    background-color: lightblue;
  }
  .overWithoutSource {
    background-color: lightgray;
  }
</style>
