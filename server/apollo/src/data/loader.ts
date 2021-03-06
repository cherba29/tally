import * as fs from 'fs';
import * as path from 'path';

import { Budget, BudgetBuilder } from '../core/budget';
import { Month } from '../core/month';
import { loadYamlFile } from './loader_yaml';
import { loadTallyConfig } from './config';
// import {SummaryStatement} from '../core/summary_statement';

function* readdirSync(file_path: string): Generator<string> {
  if (fs.statSync(file_path).isDirectory()) {
    for (const f of fs.readdirSync(file_path)) {
      const child_file = path.join(file_path, f);
      for (const cf of readdirSync(child_file)) {
        if (cf.match(/\.yaml$/)) {
          yield cf;
        }
      }
    }
  } else {
    yield file_path;
  }
  return;
}

export function listFiles(): string[] {
  if (!process.env.TALLY_FILES) {
    throw Error('Process environment variable "TALLY_FILES" has not been specified.');
  }
  const filePaths: string[] = [];
  for (const file_path of readdirSync(process.env.TALLY_FILES)) {
    const relative_file_path = file_path.slice(process.env.TALLY_FILES.length + 1);
    filePaths.push(relative_file_path);
  }
  return filePaths;
}

export function loadBudget(): Budget {
  if (!process.env.TALLY_FILES) {
    throw Error('Process environment variable "TALLY_FILES" has not been specified.');
  }
  const budgetBuilder = new BudgetBuilder();

  const config = loadTallyConfig();
  const start = Month.fromString(config.budget_period.start);
  const end = Month.fromString(config.budget_period.end).next();
  budgetBuilder.setPeriod(start, end);

  const filePaths: string[] = [];
  for (const file_path of readdirSync(process.env.TALLY_FILES)) {
    const relative_file_path = file_path.slice(process.env.TALLY_FILES.length + 1);
    filePaths.push(relative_file_path);
    console.log('Loading ' + relative_file_path);
    const content = fs.readFileSync(file_path, 'utf8');
    loadYamlFile(budgetBuilder, content, relative_file_path);
  }
  console.log(`Done loading ${filePaths.length} file(s)`);
  return budgetBuilder.build();
}
