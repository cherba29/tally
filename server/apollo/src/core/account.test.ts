import {Account, Type} from './account';
import {Month} from './month';

test('Account', () => {
  const account = new Account({
    name: 'testAccount',
    type: Type.CHECKING,
    owners: ['bob'],
  });
  expect(account.isClosed(new Month(2021, 2))).toBe(true);
});
