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
    throw Error('Process environment variable "TALLY_FILES" has not been specified.');
  }
  const configPath = path.join(process.env.TALLY_FILES, '_config.yaml');
  return yaml.safeLoad(readFileSync(configPath, 'utf-8')) as TallyConfig;
}
