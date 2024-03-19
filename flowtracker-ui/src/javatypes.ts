// types for the rest API.

// ideally this would be generated from the java code,
// but this is written manually

// in java: TrackerResponse
export interface Tracker {
  id: number;
  description: string;
  path: string[];
  origin: boolean;
  sink: boolean;
}

// in java: TrackerResource.TrackerPartResponse
export interface TrackerPart {
  tracker: Tracker;
  offset: number;
  length: number;
}

// in java: TrackerResource.Region
export interface Region {
  offset: number;
  length: number;
  content: string;
  parts: TrackerPart[];
}

// in java: TrackerResource.TrackerDetailResponse
export interface TrackerDetail {
  path: string[] | null;
  regions: Region[];
}

// in java: TreeResource.NodeDetailResponse
export interface NodeDetail {
  names: string[];
  children: NodeDetail[];
  tracker: Tracker | null;

  // added on client side (TrackerTree.enrich)
  path: string[];
}

// in java: SettingsResource.Settings
export interface Settings {
  suspendShutdown: boolean;
}
