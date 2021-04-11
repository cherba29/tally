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
  if (!('start' in config)) {
    throw new Error(`File "${configPath}" does not specify start`);
  }
  if (!('end' in config)) {
    throw new Error(`File "${configPath}" does not specify end`);
  }
  return config as TallyConfig;
}
