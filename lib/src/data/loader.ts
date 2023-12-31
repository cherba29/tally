import * as fs from 'fs';
import * as path from 'path';
import * as chokidar from 'chokidar';

import { Budget, BudgetBuilder } from '../core/budget';
import { loadYamlFile, parseYamlContent, YamlData } from './loader_yaml';
import { TransactionStatement, buildTransactionStatementTable } from '../statement/transaction';
import { SummaryStatement, buildSummaryStatementTable } from '../statement/summary';
import { Map3 } from '../utils';

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
let processedBudget: ProcessedBudget | undefined = undefined;

export function unwatchBudgetFiles() {
  watchedPath = undefined;
  watcher?.close();
  watcher = undefined;
  processedBudget = undefined;
}

export interface DataPayload {
  budget: Budget;
  statements: Map<string, Map<string, TransactionStatement>>;
  // owner -> accout name -> month -> summary.
  summaries: Map3<SummaryStatement>;
}

class ProcessedBudget {
  private readonly parsedAccountData = new Map<string, YamlData>();
  budget?: Budget = undefined;
  readonly accoutToMonthToTransactionStatement = new Map<string, Map<string, TransactionStatement>>();
  summaryNameMonthMap = new Map3<SummaryStatement>();

  reProcess() {
    const startTimeMs: number = Date.now();
    const budgetBuilder = new BudgetBuilder();
    for (const [filePath, accountData] of this.parsedAccountData) {
      try {
        loadYamlFile(budgetBuilder, accountData, filePath);
        if (!budgetBuilder.accounts.has(accountData?.name ?? '')) {
          console.warn(`warning: ${filePath} is not an account file.`);
        }
      } catch (e) {
        console.error(`error: Failed to add ${filePath}, ${e}`);
        return;
      }
    }
    this.budget = budgetBuilder.build();
    console.log(`Done building budget for ${this.budget.accounts.size} accounts ` 
        + ` in ${Date.now() - startTimeMs}ms`);

    this.accoutToMonthToTransactionStatement.clear();
    this.summaryNameMonthMap.clear();

    const startBuildTransactionStatementsTimeMs: number = Date.now();
    const transactionStatementTable = buildTransactionStatementTable(this.budget);
    for (const stmt of transactionStatementTable) {
      let monthToStatement = this.accoutToMonthToTransactionStatement.get(stmt.account.name);
      if (!monthToStatement) {
        monthToStatement = new Map<string, TransactionStatement>();
        this.accoutToMonthToTransactionStatement.set(stmt.account.name, monthToStatement);
      }
      monthToStatement.set(stmt.month.toString(), stmt);
    }
    console.log(`Done building ${transactionStatementTable.length} transaction statements ` 
                + ` in ${Date.now() - startBuildTransactionStatementsTimeMs}ms`);

    const startBuildSummaryStatementsTimeMs: number = Date.now();
    this.summaryNameMonthMap = buildSummaryStatementTable(transactionStatementTable);
    let numSummaryStatements = this.summaryNameMonthMap.size;
    console.log(`Done building ${numSummaryStatements} summary statements ` 
                + ` in ${Date.now() - startBuildSummaryStatementsTimeMs}ms`);

    console.log(`Done reprocessing ${this.parsedAccountData.size} file(s) ` 
        + `${transactionStatementTable.length} tran statements and ` 
        + `${numSummaryStatements} summaries in ${Date.now() - startTimeMs}ms`);
  }

  addFolder(rootPath: string) {
    const startTimeMs: number = Date.now();
    for (const filePath of readdirSync(rootPath)) {
      const relativeFilePath = filePath.slice(rootPath.length + 1);
      console.log('Loading ' + relativeFilePath);
      this.addFile(rootPath, relativeFilePath);
    }
    console.log(`Done loading ${this.parsedAccountData.size} file(s) in ${Date.now() - startTimeMs}ms`);
  }

  addFile(rootPath: string, relativeFilePath: string) {
    const content = fs.readFileSync(`${rootPath}/${relativeFilePath}`, 'utf8');
    const accountData = parseYamlContent(content, relativeFilePath);
    if (accountData === undefined) {
      throw Error(
        `Failed to parse ${relativeFilePath} content of size ${content.length} fileStat: ${relativeFilePath}`
      );
    }
    this.parsedAccountData.set(relativeFilePath, accountData);
  }
}

export async function loadBudget(): Promise<DataPayload> {
  if (processedBudget !== undefined) {
    return { 
      budget: processedBudget.budget!, 
      statements: processedBudget.accoutToMonthToTransactionStatement, 
      summaries: processedBudget.summaryNameMonthMap,  
    };
  
  }
  if (!process.env.TALLY_FILES) {
    throw Error('Process environment variable "TALLY_FILES" has not been specified.');
  }
  
  processedBudget = new ProcessedBudget();
  
  const pathToData = await realPath(process.env.TALLY_FILES);
  processedBudget.addFolder(pathToData);
  processedBudget.reProcess();

  if (watcher === undefined || watchedPath !== pathToData) {
    if (watcher) {
      unwatchBudgetFiles();
    }
    watchedPath = pathToData;
    watcher = chokidar
      .watch(pathToData, {
        persistent: true,
        // Multiple change events can be emitted as a larger file is being written.
        awaitWriteFinish: {
          stabilityThreshold: 300,
        },
      })
      .on('all', async (eventType: string, fileStat: fs.Stats | undefined) => {
        if (!fileStat || ['add', 'addDir'].includes(eventType)) {
          return;
        }
        console.log(`${eventType} for ${fileStat}`);
        const startTimeMs: number = Date.now();
        const relativeFilePath = `${fileStat}`.slice(pathToData.length + 1);
        if (processedBudget) {
          processedBudget.addFile(pathToData, relativeFilePath);
          processedBudget.reProcess();
        } else {
          console.error(`Unexpected error: processedBudget is unset`);
        }
        console.log(`info: Rebuilt budget in ${Date.now() - startTimeMs}ms`);
      });
    console.log(`info: Watching ${process.env.TALLY_FILES}`);
  }
  return { 
    budget: processedBudget.budget!, 
    statements: processedBudget.accoutToMonthToTransactionStatement, 
    summaries:  processedBudget.summaryNameMonthMap,  
  };
}
