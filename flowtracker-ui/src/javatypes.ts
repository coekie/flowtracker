// types for the rest API.

// ideally this would be generated from the java code,
// but this is written manually

// in java: TrackerResponse
export interface Tracker {
  id: number;
  path: string[];
  origin: boolean;
  sink: boolean;
}

// in java: TrackerResource.TrackerPartResponse
export interface TrackerPart {
  trackerId: number;
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
  linkedTrackers: {[key: number]: Tracker};
  path: string[] | null;
  creationStackTrace: string | null;
  regions: Region[];
  hasSource: boolean;
}

// in java: TreeResource.NodeDetailResponse
export interface NodeDetail {
  names: string[];
  children: NodeDetail[];
  tracker: Tracker | null;

  // added on client side (TrackerTree.enrich)
  path: string[];
}

// in java: CodeResource.CodeResponse
export interface Code {
  lines: Line[];
}

// in java: CodeResource.Line
export interface Line {
  line: number | null;
  content: string;
  parts: TrackerPart[];
}

// in java: SettingsResource.Settings
export interface Settings {
  snapshot: boolean;
  suspendShutdown: boolean;
}
