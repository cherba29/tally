import * as fs from 'fs';
import * as path from 'path';
import * as chokidar from 'chokidar';

import { Budget, BudgetBuilder } from '../core/budget';
import { loadYamlFile, parseYamlContent, YamlData } from './loader_yaml';
import { TransactionStatement, buildTransactionStatementTable } from '../statement/transaction';
import { SummaryStatement, buildSummaryStatementTable } from '../statement/summary';

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

async function realPath(path: string): Promise<string> {
  const stats = await fs.promises.lstat(path);
  return stats.isSymbolicLink() ? await fs.promises.readlink(path) : path;
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

let watchedPath: string | undefined = undefined;
let watcher: chokidar.FSWatcher | undefined = undefined;
const parsedAccountData = new Map<string, YamlData | undefined>();
let budget: Budget | undefined = undefined;
let statements: TransactionStatement[] | undefined = undefined;
let summaries: SummaryStatement[] | undefined = undefined;

export function unwatchBudgetFiles() {
  watchedPath = undefined;
  watcher?.close();
  watcher = undefined;
  parsedAccountData.clear();
  budget = undefined;
  statements = undefined;
  summaries = undefined;
}

export interface DataPayload {
  budget: Budget;
  statements: TransactionStatement[];
  summaries: SummaryStatement[];
}

export async function loadBudget(): Promise<DataPayload> {
  if (budget !== undefined && statements !== undefined && summaries !== undefined) {
    return { budget, statements, summaries };
  }
  if (!process.env.TALLY_FILES) {
    throw Error('Process environment variable "TALLY_FILES" has not been specified.');
  }
  const budgetBuilder = new BudgetBuilder();

  const filePaths: string[] = [];
  const startTimeMs: number = Date.now();
  const pathToData = await realPath(process.env.TALLY_FILES);
  for (const file_path of readdirSync(pathToData)) {
    const relative_file_path = file_path.slice(pathToData.length + 1);
    filePaths.push(relative_file_path);
    console.log('Loading ' + relative_file_path);
    const content = fs.readFileSync(file_path, 'utf8');
    const accountData = parseYamlContent(content, relative_file_path);
    parsedAccountData.set(relative_file_path, accountData);
    loadYamlFile(budgetBuilder, accountData, relative_file_path);
  }
  console.log(`Done loading ${filePaths.length} file(s) in ${Date.now() - startTimeMs}ms`);
  if (watcher === undefined || watchedPath !== pathToData) {
    if (watcher) {
      unwatchBudgetFiles();
    }
    watchedPath = pathToData;
    watcher = chokidar
      .watch(pathToData, {})
      .on('all', async (eventType: string, fileStat: fs.Stats | undefined) => {
        if (!fileStat || ['add', 'addDir'].includes(eventType)) {
          return;
        }
        console.log(`${eventType} for ${fileStat}`);
        const changedBudgetBuilder = new BudgetBuilder();
        const startTimeMs: number = Date.now();
        const content = fs.readFileSync(`${fileStat}`, 'utf8');
        const relativeFilePath = `${fileStat}`.slice(pathToData.length + 1);
        const accountData = parseYamlContent(content, relativeFilePath);
        parsedAccountData.set(relativeFilePath, accountData);

        for (const [filePath, accountData] of parsedAccountData) {
          try {
            loadYamlFile(changedBudgetBuilder, accountData, filePath);
          } catch (e) {
            console.error(`Failed to add ${filePath}, ${e}`);
            return;
          }
        }
        console.log(
          `Reloaded budget ${parsedAccountData.size} file(s) in ${Date.now() - startTimeMs}ms`
        );
        try {
          budget = changedBudgetBuilder.build();
          statements = buildTransactionStatementTable(budget);
          summaries = [...buildSummaryStatementTable(statements)];
        } catch (e) {
          console.error(`Failed to build budget ${e}`);
          return;
        }
        console.log(`Rebuilt budget in ${Date.now() - startTimeMs}ms`);
      });
    console.log(`Watching ${process.env.TALLY_FILES}`);
  }
  budget = budgetBuilder.build();
  statements = buildTransactionStatementTable(budget);
  summaries = [...buildSummaryStatementTable(statements)];
  return { budget, statements, summaries };
}
