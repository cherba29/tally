import { BudgetBuilder } from '../core/budget';
import { loadYamlFile } from './loader_yaml';

describe('Creation', () => {
  test('empty', () => {
    const budgetBuilder = new BudgetBuilder();
    const content = '';
    const relative_file_path = 'path/file.yaml';
    loadYamlFile(budgetBuilder, content, relative_file_path);
    const budget = budgetBuilder.build();
    expect(budget.accounts.size).toBe(0);
  });
});
