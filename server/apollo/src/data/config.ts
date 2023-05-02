import { readFileSync } from 'fs';
import * as path from 'path';
import * as yaml from 'js-yaml';
import { Month } from '@tally/lib';

interface BudgetPeriod {
  start: Month;
  end: Month;
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
  const start = Month.fromString(budget_period.start);
  const end = Month.fromString(budget_period.end);

  if (start.compareTo(end) > 0) {
    throw new Error(
      `File "${configPath}" specified end ${end} before start ${start} in budget_period`
    );
  }

  return { budget_period: { start, end } };
}
