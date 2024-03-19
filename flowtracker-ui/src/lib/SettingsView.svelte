<script lang="ts">
  import {onMount} from 'svelte';
  import type {Settings} from '../javatypes';
  let settings: Settings;

  const initSettings = async () => {
    const response = await fetch('/settings');
    if (!response.ok) throw new Error(response.statusText);
    settings = await response.json();
    console.log('Loaded settings: ', response, settings);
  };

  const save = () => {
    console.log('Saving settings: ', settings);
    fetch('/settings', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(settings),
    });
  };

  onMount(() => {
    initSettings();
  });
</script>

{#if settings}
  <div class="settingsWrapper">
    <b>Settings:</b>
    <form>
      <label
        ><input
          type="checkbox"
          bind:checked={settings.suspendShutdown}
        />Suspend shutdown</label
      >
      <button type="button" on:click={save}>Save</button>
    </form>
  </div>
{/if}

<style>
  .settingsWrapper {
    margin: 0.5em;
  }
</style>
