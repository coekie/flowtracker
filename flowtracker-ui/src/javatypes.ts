// types for the rest API.

// ideally this would be generated from the java code,
// but this is written manually

// in java: TrackerResource.TrackerResponse
export interface Tracker {
  id: Number
  description: String
  origin: boolean
  sink: boolean
}

// in java: TrackerResource.TrackerPartResponse
export interface TrackerPart {
  tracker: Tracker
  offset: Number
  context: String
}

// in java: TrackerResource.Region
export interface Region {
  content: String
  parts: TrackerPart[]
}

// in java: TrackerResource.TrackerDetailResponse
export interface TrackerDetail {
  regions: Region[]
}

// in java: TreeResource.NodeResponse
export interface Node {
  name: String
  children: Node[]
  tracker: Tracker
}

// in java: SettingsResource.Settings
export interface Settings {
  suspendShutdown: boolean
}