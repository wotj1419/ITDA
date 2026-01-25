export interface Scenario {
  projectId: number;
  version: number;
  currentStep: 'PROMPT' | 'PLOT' | 'SCENES';
  prompt: ScenarioPrompt;
  plot: ScenarioPlot;
  scenes: ScenarioScene[];
  updatedAt: string;
}

export interface ScenarioPrompt {
  text: string;
  status: 'DRAFT' | 'APPROVED';
}

export interface ScenarioPlot {
  text: string;
  status: 'DRAFT' | 'APPROVED';
}

export interface ScenarioScene {
  scenarioSceneId: number;
  sceneId: number | null;
  order: number;
  title: string;
  description: string;
}

export interface GenerateScenarioPromptRequest {
  genre: string;
  mood: string;
  keywords?: string[];
  sceneCount: number;
  characterHints?: string;
  backgroundHints?: string;
  referenceStyle?: string;
}
