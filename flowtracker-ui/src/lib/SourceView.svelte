<script lang="ts">
  import type {TrackerDetail} from '../javatypes';

  export let trackerDetail: TrackerDetail;
</script>

<!-- @component
Shows source code of a class.
-->
<span>TODO Source code for {trackerDetail.path?.join('/')}</span>

<style>
</style>
