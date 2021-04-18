import { readFileSync } from 'fs';
import * as path from 'path';
import * as yaml from 'js-yaml';

interface BudgetPeriod {
  start: string;
  end: string;
}

interface TallyConfig {
  budget_period: BudgetPeriod;
}

export function loadTallyConfig(): TallyConfig {
  if (!process.env.TALLY_FILES) {
    throw new Error('Process environment variable "TALLY_FILES" has not been specified.');
  }
  const configPath = path.join(process.env.TALLY_FILES, '_config.yaml');
  const config = yaml.safeLoad(readFileSync(configPath, 'utf-8'));
  if (!config || typeof config === 'string') {
    throw new Error(`Could not parse file at "${configPath}"`);
  }
  if (!('budget_period' in config)) {
    throw new Error(`File "${configPath}" does not specify budget_period`);
  }
  const budget_period = (config as { budget_period: { start?: string; end?: string } })[
    'budget_period'
  ];
  if (!budget_period.start) {
    throw new Error(`File "${configPath}" does not specify start in budget_period`);
  }
  if (!budget_period.end) {
    throw new Error(`File "${configPath}" does not specify end in budget_period`);
  }
  return config as TallyConfig;
}
