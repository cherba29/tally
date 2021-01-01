import * as fs from 'fs';
import * as path from 'path';

import {Account} from '../core/account';
import {Budget} from '../core/budget';
import {Statement} from '../core/statement';
import {loadYamlFile} from './loader_yaml';
// import {SummaryStatement} from '../core/summary_statement';

function* readdirSync(file_path: string) : Generator<string> {
  if (fs.statSync(file_path).isDirectory()) {
    for (const f of fs.readdirSync(file_path)) {
      const child_file = path.join(file_path, f);
      for (const cf of readdirSync(child_file)) {
        if (cf.match(/\.yaml$/)) {
          yield cf;
        }
      }
    };
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


export function loadAccounts(): Account[] {
  if (!process.env.TALLY_FILES) {
    throw Error('Process environment variable "TALLY_FILES" has not been specified.');
  }
  for (const file of readdirSync(process.env.TALLY_FILES)) {
    console.log(file);
  }
  return [];
}


function loadBudget(): Budget {
  if (!process.env.TALLY_FILES) {
    throw Error('Process environment variable "TALLY_FILES" has not been specified.');
  }
  const filePaths: string[] = [];
  const budget = new Budget();
  for (const file_path of readdirSync(process.env.TALLY_FILES)) { 
    const relative_file_path = file_path.slice(process.env.TALLY_FILES.length + 1);
    filePaths.push(relative_file_path);
    console.log('Loading ' + relative_file_path);
    const content =  fs.readFileSync(file_path, 'utf8');
    loadYamlFile(budget, content, relative_file_path);
  }
  console.log(`Done loading ${filePaths.length} file(s)`);
  return budget;
}


export function loadStatements(): Statement[] {
  const budget = loadBudget();
  
  const statements: Statement[] = [];
  for (const account of budget.accounts.values()) {
    for (const month of budget.months) {
      statements.push({
        name: account.name,
        month,
        inFlows: 0,
        outFlows: 0,
        income: 0,
        totalPayments: 0,
        totalTransfers: 0,
      });
    }
  }
  console.log(statements);
  return statements;
}