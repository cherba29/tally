import { RawMonth } from './types';
import {Month} from './core/month';
import { loadTallyConfig } from './data/config'

export default {
  Query: {
    testMessage: (): string => 'Hello World' + process.env.TALLY_FILES,
    accounts: (): Account[]  => [],
    months: (): RawMonth[] => {
      const config = loadTallyConfig();
      const start = Month.fromString(config.budget_period.start);
      const end = Month.fromString(config.budget_period.end).next();
      return Array.from(Month.generate(start, end));
    },
  },
};
