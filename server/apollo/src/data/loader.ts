import * as fs from 'fs';
import * as path from 'path';
import * as yaml from 'js-yaml';

function* readdirSync(file_path: string)  {
  if (fs.statSync(file_path).isDirectory()) {
    for (const f of fs.readdirSync(file_path)) {
      const child_file = path.join(file_path, f);
      for (const sf of readdirSync(child_file)) {
        yield child_file;
      }
    };
  }
  return file_path;
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
